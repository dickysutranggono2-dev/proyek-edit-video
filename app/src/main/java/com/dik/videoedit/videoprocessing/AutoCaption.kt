package com.dik.videoedit.videoprocessing

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import android.content.Context
import com.dik.videoedit.models.Clip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.math.exp
import kotlin.math.ln

/**
 * Keterangan otomatis dengan Whisper tiny yang berjalan di HP (ONNX Runtime), tanpa internet dan tanpa API key.
 * Model diunduh sekali lewat [WhisperModel].
 */
object AutoCaption {
    class CaptionException(message: String) : Exception(message)

    /** Teks berwaktu; waktu relatif terhadap awal bagian klip yang di-trim (ms, sebelum kecepatan). */
    data class Seg(val startMs: Long, val endMs: Long, val text: String)

    private const val NEG = -1e9f
    private const val MAX_TOKENS = 200
    private const val HEADS = 6L
    private const val HEAD_DIM = 64L

    private class Tokens(
        val byId: Array<String?>,
        val special: Map<String, Int>,
    ) {
        val sot = special["<|startoftranscript|>"] ?: 50258
        val eot = special["<|endoftext|>"] ?: 50257
        val transcribe = special["<|transcribe|>"] ?: 50359
        val tsBegin = (special["<|notimestamps|>"] ?: 50363) + 1
        val langEnd = special["<|translate|>"] ?: (sot + 100)

        fun decode(ids: List<Int>): String {
            val bytes = ByteArrayOutputStream()
            for (id in ids) {
                val s = byId.getOrNull(id) ?: continue
                for (ch in s) bytes.write(CHAR_TO_BYTE[ch] ?: continue)
            }
            return String(bytes.toByteArray(), Charsets.UTF_8).trim()
        }

        companion object {
            private val CHAR_TO_BYTE: Map<Char, Int> by lazy {
                val base = ArrayList<Int>()
                for (b in 33..126) base += b
                for (b in 161..172) base += b
                for (b in 174..255) base += b
                val bs = base.toMutableList()
                val cs = base.toMutableList()
                var n = 0
                for (b in 0..255) if (b !in base) { bs += b; cs += 256 + n; n++ }
                val m = HashMap<Char, Int>()
                for (i in bs.indices) m[cs[i].toChar()] = bs[i]
                m
            }

            fun load(json: String): Tokens {
                val o = JSONObject(json)
                val byId = arrayOfNulls<String>(52000)
                val vocab = o.getJSONObject("model").getJSONObject("vocab")
                val keys = vocab.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val id = vocab.getInt(k)
                    if (id in byId.indices) byId[id] = k
                }
                val special = HashMap<String, Int>()
                val added = o.optJSONArray("added_tokens")
                if (added != null) {
                    for (i in 0 until added.length()) {
                        val a = added.getJSONObject(i)
                        special[a.getString("content")] = a.getInt("id")
                    }
                }
                return Tokens(byId, special)
            }
        }
    }

    /** Pengenal suara: memuat model sekali, bisa dipakai untuk banyak klip. Tutup setelah selesai. */
    class Recognizer(private val context: Context) : AutoCloseable {
        private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
        private val enc: OrtSession
        private val dec: OrtSession
        private val tok: Tokens
        private val encIn: String
        private val pastIn: List<String>
        private val cacheName: String?

        init {
            if (!WhisperModel.isReady(context)) throw CaptionException("Model belum diunduh.")
            try {
                val threads = Runtime.getRuntime().availableProcessors().coerceIn(2, 4)
                val opts = OrtSession.SessionOptions().apply { setIntraOpNumThreads(threads) }
                enc = env.createSession(WhisperModel.file(context, "encoder.onnx").path, opts)
                dec = env.createSession(WhisperModel.file(context, "decoder.onnx").path, opts)
                tok = Tokens.load(WhisperModel.file(context, "tokenizer.json").readText())
            } catch (e: CaptionException) {
                throw e
            } catch (e: Throwable) {
                throw CaptionException("Model tidak bisa dimuat. Hapus lalu unduh ulang modelnya. (${e.message})")
            }
            encIn = enc.inputNames.first()
            val names = dec.inputNames
            pastIn = names.filter { it.startsWith("past_key_values") }
            cacheName = names.firstOrNull { it.contains("use_cache") }
        }

        override fun close() {
            runCatching { enc.close() }
            runCatching { dec.close() }
        }

        private fun floatTensor(arr: FloatArray, shape: LongArray): OnnxTensor {
            val bb = ByteBuffer.allocateDirect(maxOf(arr.size, 1) * 4).order(ByteOrder.nativeOrder())
            val fb = bb.asFloatBuffer()
            fb.put(arr)
            fb.rewind()
            if (arr.isEmpty()) fb.limit(0)
            return OnnxTensor.createTensor(env, fb, shape)
        }

        private fun longTensor(ids: IntArray): OnnxTensor {
            val bb = ByteBuffer.allocateDirect(ids.size * 8).order(ByteOrder.nativeOrder())
            val lb = bb.asLongBuffer()
            for (v in ids) lb.put(v.toLong())
            lb.rewind()
            return OnnxTensor.createTensor(env, lb, longArrayOf(1, ids.size.toLong()))
        }

        private fun presentName(past: String) = past.replace("past_key_values", "present")

        private fun step(ids: IntArray, hs: OnnxTensor, past: Map<String, OnnxTensor>?): OrtSession.Result {
            val inputs = HashMap<String, OnnxTensor>()
            val own = ArrayList<OnnxTensor>()
            try {
                val idT = longTensor(ids); own += idT
                inputs["input_ids"] = idT
                inputs["encoder_hidden_states"] = hs
                cacheName?.let {
                    val t = OnnxTensor.createTensor(env, booleanArrayOf(past != null)); own += t
                    inputs[it] = t
                }
                for (p in pastIn) {
                    if (past != null) inputs[p] = past.getValue(p)
                    else {
                        val t = floatTensor(FloatArray(0), longArrayOf(1, HEADS, 0, HEAD_DIM)); own += t
                        inputs[p] = t
                    }
                }
                return dec.run(inputs)
            } finally {
                own.forEach { it.close() }
            }
        }

        private fun lastLogits(res: OrtSession.Result, seqLen: Int): FloatArray {
            val t = res.get("logits").get() as OnnxTensor
            val v = (t.info as TensorInfo).shape.last().toInt()
            val fb = t.floatBuffer
            val arr = FloatArray(v)
            fb.position((seqLen - 1) * v)
            fb.get(arr)
            return arr
        }

        private fun detectLanguage(hs: OnnxTensor): Int {
            val res = step(intArrayOf(tok.sot), hs, null)
            try {
                val logits = lastLogits(res, 1)
                var best = tok.sot + 1
                for (i in tok.sot + 1 until tok.langEnd.coerceAtMost(logits.size)) if (logits[i] > logits[best]) best = i
                return best
            } finally {
                res.close()
            }
        }

        /** Mengurai token (dengan timestamp) menjadi segmen; waktu dalam ms relatif ke awal jendela. */
        private fun parse(seq: List<Int>, windowMs: Long): List<Seg> {
            val out = ArrayList<Seg>()
            var start = -1L
            val buf = ArrayList<Int>()
            for (t in seq) {
                if (t >= tok.tsBegin) {
                    val ms = (t - tok.tsBegin) * 20L
                    if (start < 0) start = ms
                    else {
                        if (buf.isNotEmpty()) {
                            val text = tok.decode(buf)
                            if (text.isNotEmpty()) out += Seg(start, maxOf(ms, start + 200L), text)
                            buf.clear()
                        }
                        start = ms
                    }
                } else if (t < tok.eot) buf += t
            }
            if (buf.isNotEmpty()) {
                val text = tok.decode(buf)
                val s = if (start < 0) 0L else start
                if (text.isNotEmpty()) out += Seg(s, maxOf(windowMs, s + 500L), text)
            }
            return out
        }

        private fun applyRules(logits: FloatArray, seq: List<Int>) {
            val v = logits.size
            val ts = tok.tsBegin
            for (i in tok.eot + 1 until minOf(ts, v)) logits[i] = NEG
            if (seq.isEmpty()) {
                for (i in 0 until minOf(ts, v)) logits[i] = NEG
                for (i in (ts + 51) until v) logits[i] = NEG
            } else {
                val lastTs = seq.last() >= ts
                val penTs = seq.size < 2 || seq[seq.size - 2] >= ts
                if (lastTs) {
                    if (penTs) for (i in ts until v) logits[i] = NEG
                    else for (i in 0 until tok.eot) logits[i] = NEG
                }
                val lastT = seq.lastOrNull { it >= ts }
                if (lastT != null) {
                    val minT = if (lastTs && !penTs) lastT else lastT + 1
                    for (i in ts until minOf(minT, v)) logits[i] = NEG
                }
            }
            // log-softmax untuk memutuskan timestamp vs teks
            var mx = NEG
            for (x in logits) if (x > mx) mx = x
            var sum = 0.0
            for (x in logits) sum += exp((x - mx).toDouble())
            val lse = mx + ln(sum).toFloat()
            var tsSum = 0.0
            for (i in ts until v) tsSum += exp((logits[i] - lse).toDouble())
            val tsLog = if (tsSum > 0) ln(tsSum).toFloat() else NEG
            var maxText = NEG
            for (i in 0 until minOf(ts, v)) { val lp = logits[i] - lse; if (lp > maxText) maxText = lp }
            if (tsLog > maxText) for (i in 0 until minOf(ts, v)) logits[i] = NEG
        }

        /** Dekode satu jendela 30 detik; mengembalikan token hasil (termasuk timestamp). */
        private fun decodeWindow(hs: OnnxTensor, langToken: Int): List<Int> {
            val prompt = intArrayOf(tok.sot, langToken, tok.transcribe)
            val res0 = step(prompt, hs, null)
            var cur = res0
            val seq = ArrayList<Int>()
            try {
                val encPast = HashMap<String, OnnxTensor>()
                for (p in pastIn) if (p.contains(".encoder.")) encPast[p] = res0.get(presentName(p)).get() as OnnxTensor
                var logits = lastLogits(res0, prompt.size)
                var sameRun = 0
                while (seq.size < MAX_TOKENS) {
                    applyRules(logits, seq)
                    var best = 0
                    for (i in 1 until logits.size) if (logits[i] > logits[best]) best = i
                    if (best == tok.eot) break
                    if (seq.isNotEmpty() && best == seq.last() && best < tok.tsBegin) sameRun++ else sameRun = 0
                    if (sameRun >= 8) break
                    seq += best
                    val pastMap = HashMap<String, OnnxTensor>()
                    for (p in pastIn) {
                        pastMap[p] = if (p.contains(".encoder.")) encPast.getValue(p)
                        else cur.get(presentName(p)).get() as OnnxTensor
                    }
                    val nr = step(intArrayOf(best), hs, pastMap)
                    if (cur !== res0) cur.close()
                    cur = nr
                    logits = lastLogits(cur, 1)
                }
            } finally {
                if (cur !== res0) cur.close()
                res0.close()
            }
            return seq
        }

        /**
         * Mengenali ucapan pada bagian trim sebuah klip. [language] kode bahasa ("id", "en", ...) atau null
         * untuk deteksi otomatis. [onProgress] 0..1.
         */
        suspend fun transcribe(clip: Clip, language: String?, onProgress: (Float) -> Unit): List<Seg> =
            withContext(Dispatchers.Default) {
                val pcm = AudioPcm.decode(context, clip.uri, clip.trimStartMs, clip.trimEndMs)
                if (pcm.isEmpty()) return@withContext emptyList<Seg>()
                val out = ArrayList<Seg>()
                var langToken = language?.let { tok.special["<|$it|>"] } ?: -1
                var offset = 0
                while (offset < pcm.size) {
                    coroutineContext.ensureActive()
                    onProgress(offset.toFloat() / pcm.size)
                    val len = minOf(WhisperMel.CHUNK, pcm.size - offset)
                    var sq = 0.0
                    for (i in offset until offset + len) sq += pcm[i].toDouble() * pcm[i]
                    val rms = Math.sqrt(sq / len)
                    var advance = len
                    if (rms >= 0.002 && len >= 4000) {
                        val mel = WhisperMel.compute(pcm, offset, len)
                        val feat = floatTensor(mel, longArrayOf(1, WhisperMel.N_MELS.toLong(), WhisperMel.FRAMES.toLong()))
                        try {
                            enc.run(mapOf(encIn to feat)).use { encRes ->
                                val hs = encRes.get(0) as OnnxTensor
                                if (langToken < 0) langToken = detectLanguage(hs)
                                coroutineContext.ensureActive()
                                val seq = decodeWindow(hs, langToken)
                                val windowMs = len * 1000L / AudioPcm.RATE
                                val segs = parse(seq, windowMs)
                                val baseMs = offset * 1000L / AudioPcm.RATE
                                for (s in segs) {
                                    out += Seg(baseMs + s.startMs, baseMs + minOf(s.endMs, windowMs), s.text)
                                }
                                // Jendela penuh: lanjut dari akhir ucapan terakhir supaya kalimat tidak terpotong.
                                val lastEnd = segs.lastOrNull()?.endMs ?: 0L
                                if (len == WhisperMel.CHUNK && lastEnd in 2000L until windowMs - 500L) {
                                    advance = (lastEnd * AudioPcm.RATE / 1000L).toInt()
                                }
                            }
                        } finally {
                            feat.close()
                        }
                    }
                    offset += advance.coerceAtLeast(AudioPcm.RATE)
                }
                onProgress(1f)
                out
            }
    }
}
