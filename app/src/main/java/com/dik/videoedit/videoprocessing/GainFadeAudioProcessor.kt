package com.dik.videoedit.videoprocessing

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.AudioProcessor.UnhandledAudioFormatException
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer

/**
 * Mengatur volume dan fade in/out satu klip audio pada hasil export.
 * Rumusnya sama dengan [com.dik.videoedit.models.AudioTrack.gainAt] yang dipakai preview.
 * Posisi dihitung dari jumlah frame yang sudah diproses sejak awal klip.
 */
class GainFadeAudioProcessor(
    private val volume: Float,
    private val fadeInUs: Long,
    private val fadeOutUs: Long,
    private val totalUs: Long,
) : BaseAudioProcessor() {

    private var framesDone = 0L

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT && inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) {
            throw UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat
    }

    override fun onFlush() {
        framesDone = 0L
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val size = inputBuffer.remaining()
        if (size == 0) return
        val out = replaceOutputBuffer(size)
        val channels = inputAudioFormat.channelCount
        val rate = inputAudioFormat.sampleRate.toDouble()
        val isFloat = inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT
        val bytesPerFrame = channels * (if (isFloat) 4 else 2)
        val frames = size / bytesPerFrame

        for (f in 0 until frames) {
            val gain = gainAt(((framesDone + f) / rate * 1_000_000.0).toLong())
            for (ch in 0 until channels) {
                if (isFloat) {
                    out.putFloat(inputBuffer.getFloat() * gain)
                } else {
                    val v = (inputBuffer.getShort() * gain).toInt().coerceIn(-32768, 32767)
                    out.putShort(v.toShort())
                }
            }
        }
        framesDone += frames
        // Sisa byte yang tidak membentuk satu frame utuh (seharusnya tidak ada) diteruskan apa adanya.
        while (inputBuffer.hasRemaining()) out.put(inputBuffer.get())
        out.flip()
    }

    private fun gainAt(tUs: Long): Float {
        var g = 1f
        if (fadeInUs > 0 && tUs < fadeInUs) g = minOf(g, tUs.toFloat() / fadeInUs)
        if (fadeOutUs > 0 && tUs > totalUs - fadeOutUs) g = minOf(g, (totalUs - tUs).toFloat() / fadeOutUs)
        return volume * g.coerceIn(0f, 1f)
    }
}
