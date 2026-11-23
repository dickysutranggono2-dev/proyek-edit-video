package com.dik.videoedit.videoprocessing

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.nio.ByteOrder

/** Mendekode audio sebuah klip (bagian trim) menjadi PCM mono float 16 kHz untuk Whisper. */
object AudioPcm {
    const val RATE = 16_000

    private class FloatList {
        var data = FloatArray(1 shl 16)
        var size = 0
        fun add(v: Float) {
            if (size == data.size) data = data.copyOf(data.size * 2)
            data[size++] = v
        }
    }

    /** Menurunkan sample rate dengan merata-ratakan sampel sumber yang jatuh pada tiap sampel keluaran. */
    private class Down(val srcRate: Int, val out: FloatList) {
        var n = 0L
        var k = -1L
        var acc = 0.0
        var cnt = 0
        fun add(s: Float) {
            val kk = n * RATE / srcRate
            if (kk != k) {
                if (cnt > 0) out.add((acc / cnt).toFloat())
                k = kk; acc = 0.0; cnt = 0
            }
            acc += s; cnt++; n++
        }
        fun flush() {
            if (cnt > 0) out.add((acc / cnt).toFloat())
            cnt = 0
        }
    }

    /** Hasil kosong bila klip tidak punya audio. [maxSeconds] membatasi panjang yang diproses. */
    fun decode(context: Context, uri: String, startMs: Long, endMs: Long, maxSeconds: Int = 900): FloatArray {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, Uri.parse(uri), null)
            var track = -1
            var fmt: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                    track = i; fmt = f; break
                }
            }
            if (track < 0 || fmt == null) return FloatArray(0)
            extractor.selectTrack(track)
            val startUs = startMs * 1000
            val endUs = endMs * 1000
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            val mime = fmt.getString(MediaFormat.KEY_MIME) ?: return FloatArray(0)
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(fmt, null, null, 0)
            codec.start()

            var srcRate = if (fmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE) else 44100
            var channels = if (fmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 2
            var pcmFloat = false
            val out = FloatList()
            var down = Down(srcRate, out)
            val maxSamples = maxSeconds * RATE

            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            while (!outputDone && out.size < maxSamples) {
                if (!inputDone) {
                    val ii = codec.dequeueInputBuffer(10_000)
                    if (ii >= 0) {
                        val ib = codec.getInputBuffer(ii)!!
                        val size = extractor.readSampleData(ib, 0)
                        if (size < 0 || extractor.sampleTime > endUs) {
                            codec.queueInputBuffer(ii, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(ii, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val oi = codec.dequeueOutputBuffer(info, 10_000)
                if (oi >= 0) {
                    val ob = codec.getOutputBuffer(oi)
                    if (info.size > 0 && ob != null) {
                        ob.position(info.offset)
                        ob.limit(info.offset + info.size)
                        ob.order(ByteOrder.nativeOrder())
                        val bufStartUs = info.presentationTimeUs
                        val ch = channels.coerceAtLeast(1)
                        val frames: Int
                        val shorts = if (!pcmFloat) ob.asShortBuffer() else null
                        val floats = if (pcmFloat) ob.asFloatBuffer() else null
                        frames = (if (pcmFloat) floats!!.remaining() else shorts!!.remaining()) / ch
                        val first = if (bufStartUs < startUs) ((startUs - bufStartUs) * srcRate / 1_000_000L).toInt() else 0
                        val last = minOf(frames.toLong(), (endUs - bufStartUs) * srcRate / 1_000_000L).toInt()
                        for (f in first.coerceAtLeast(0) until last) {
                            var sum = 0f
                            for (c in 0 until ch) {
                                val idx = f * ch + c
                                sum += if (pcmFloat) floats!!.get(idx) else shorts!!.get(idx) / 32768f
                            }
                            down.add(sum / ch)
                        }
                    }
                    codec.releaseOutputBuffer(oi, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                } else if (oi == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val nf = codec.outputFormat
                    val newRate = if (nf.containsKey(MediaFormat.KEY_SAMPLE_RATE)) nf.getInteger(MediaFormat.KEY_SAMPLE_RATE) else srcRate
                    channels = if (nf.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) nf.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else channels
                    pcmFloat = nf.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
                        nf.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_FLOAT
                    if (newRate != srcRate) {
                        down.flush()
                        srcRate = newRate
                        down = Down(srcRate, out)
                    }
                }
            }
            down.flush()
            return out.data.copyOf(out.size)
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }
}
