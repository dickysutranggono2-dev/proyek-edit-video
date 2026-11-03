package com.dik.videoedit.videoprocessing

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow

/**
 * Membuat data waveform (puncak amplitudo per [BUCKET_MS]) dengan men-decode trek audio file
 * (file audio atau video). Dipanggil di thread latar. File tidak diubah dan tidak dimuat penuh ke RAM.
 */
object WaveformExtractor {
    const val BUCKET_MS = 50
    private const val MAX_ANALYZE_MS = 20 * 60_000L

    /** @return puncak ternormalisasi 0..1, atau null bila file tidak dapat dibaca/di-decode. */
    fun extract(context: Context, uri: Uri, shouldStop: () -> Boolean): FloatArray? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, uri, null)
            var track = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                    track = i; format = f; break
                }
            }
            if (track < 0 || format == null) return null
            extractor.selectTrack(track)

            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            var sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) format.getInteger(MediaFormat.KEY_SAMPLE_RATE) else 44100
            var channels = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 2
            var pcmFloat = false

            val durationMs = if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) / 1000 else 0L
            var peaks = FloatArray(((durationMs / BUCKET_MS) + 8).toInt().coerceAtLeast(64))
            var lastBucket = 0

            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            var frames = 0L

            while (!outputDone) {
                if (shouldStop()) return null
                if (!inputDone) {
                    val inIndex = codec.dequeueInputBuffer(10_000)
                    if (inIndex >= 0) {
                        val buf = codec.getInputBuffer(inIndex)
                        val n = if (buf != null) extractor.readSampleData(buf, 0) else -1
                        if (n < 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inIndex, 0, n, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outIndex = codec.dequeueOutputBuffer(info, 10_000)
                if (outIndex >= 0) {
                    val out = codec.getOutputBuffer(outIndex)
                    if (out != null && info.size > 0) {
                        out.position(info.offset)
                        out.limit(info.offset + info.size)
                        out.order(ByteOrder.LITTLE_ENDIAN)
                        val ch = channels.coerceAtLeast(1)
                        val sampleCount = info.size / (if (pcmFloat) 4 else 2)
                        val frameCount = sampleCount / ch
                        val fb = if (pcmFloat) out.asFloatBuffer() else null
                        val sb = if (!pcmFloat) out.asShortBuffer() else null
                        for (f in 0 until frameCount) {
                            var m = 0f
                            for (c in 0 until ch) {
                                val v = if (fb != null) abs(fb.get()) else abs(sb!!.get().toInt()) / 32768f
                                if (v > m) m = v
                            }
                            val bucket = ((frames + f) * 1000L / sampleRate / BUCKET_MS).toInt()
                            if (bucket >= peaks.size) peaks = peaks.copyOf(max(bucket + 64, peaks.size * 2))
                            if (m > peaks[bucket]) peaks[bucket] = m
                            lastBucket = bucket
                        }
                        frames += frameCount
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val of = codec.outputFormat
                    if (of.containsKey(MediaFormat.KEY_SAMPLE_RATE)) sampleRate = of.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    if (of.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) channels = of.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    pcmFloat = of.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
                        of.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_FLOAT
                }
                if (frames * 1000L / sampleRate > MAX_ANALYZE_MS) break
            }

            val size = (lastBucket + 1).coerceAtLeast(1)
            val result = peaks.copyOf(size)
            val top = result.maxOrNull() ?: 0f
            if (top <= 0f) return FloatArray(size) { 0.05f }
            // Normalisasi + sedikit pelunakan agar bagian pelan tetap terlihat.
            for (i in result.indices) result[i] = (result[i] / top).toDouble().pow(0.6).toFloat()
            return result
        } catch (e: Exception) {
            return null
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }
}

/** Cache waveform di memori (per URI) agar tidak di-decode ulang tiap timeline digambar. */
object WaveformCache {
    private val cache = object : LinkedHashMap<String, FloatArray>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, FloatArray>?): Boolean = size > 16
    }

    /** Array kosong berarti gagal dibaca (jangan dicoba ulang terus-menerus). */
    fun get(context: Context, uri: String, shouldStop: () -> Boolean): FloatArray {
        synchronized(cache) { cache[uri] }?.let { return it }
        val peaks = WaveformExtractor.extract(context.applicationContext, Uri.parse(uri), shouldStop)
        if (peaks == null && shouldStop()) return FloatArray(0)
        val value = peaks ?: FloatArray(0)
        synchronized(cache) { cache[uri] = value }
        return value
    }
}
