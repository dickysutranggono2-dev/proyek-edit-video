package com.dik.videoedit.videoprocessing

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Log-mel spectrogram persis seperti Whisper (80 mel, n_fft 400, hop 160, jendela 30 detik). */
object WhisperMel {
    const val N_FFT = 400
    const val HOP = 160
    const val N_MELS = 80
    const val CHUNK = 480_000
    const val FRAMES = 3000
    private const val BINS = N_FFT / 2 + 1

    private val window = FloatArray(N_FFT) { (0.5 - 0.5 * cos(2.0 * PI * it / N_FFT)).toFloat() }
    private val cosTab: FloatArray by lazy {
        FloatArray(BINS * N_FFT) { cos(2.0 * PI * ((it / N_FFT).toLong() * (it % N_FFT) % N_FFT) / N_FFT).toFloat() }
    }
    private val sinTab: FloatArray by lazy {
        FloatArray(BINS * N_FFT) { sin(2.0 * PI * ((it / N_FFT).toLong() * (it % N_FFT) % N_FFT) / N_FFT).toFloat() }
    }

    // ---- filterbank mel skala Slaney (sama dengan librosa) ----
    private fun hzToMel(f: Double): Double {
        val fSp = 200.0 / 3
        val minLogHz = 1000.0
        val minLogMel = minLogHz / fSp
        val logStep = ln(6.4) / 27.0
        return if (f < minLogHz) f / fSp else minLogMel + ln(f / minLogHz) / logStep
    }

    private fun melToHz(m: Double): Double {
        val fSp = 200.0 / 3
        val minLogHz = 1000.0
        val minLogMel = minLogHz / fSp
        val logStep = ln(6.4) / 27.0
        return if (m < minLogMel) fSp * m else minLogHz * exp(logStep * (m - minLogMel))
    }

    private val filters: Array<FloatArray> by lazy {
        val melMin = hzToMel(0.0)
        val melMax = hzToMel(8000.0)
        val melF = DoubleArray(N_MELS + 2) { melToHz(melMin + (melMax - melMin) * it / (N_MELS + 1)) }
        val fft = DoubleArray(BINS) { it * 16000.0 / N_FFT }
        Array(N_MELS) { i ->
            val enorm = 2.0 / (melF[i + 2] - melF[i])
            FloatArray(BINS) { k ->
                val lower = (fft[k] - melF[i]) / (melF[i + 1] - melF[i])
                val upper = (melF[i + 2] - fft[k]) / (melF[i + 2] - melF[i + 1])
                (max(0.0, min(lower, upper)) * enorm).toFloat()
            }
        }
    }

    /** Sampel [from] sepanjang [len] (maks 30 detik) menjadi array [N_MELS x FRAMES] baris demi baris. */
    fun compute(samples: FloatArray, from: Int, len: Int): FloatArray {
        val n = min(len, CHUNK)
        val active = min(FRAMES, (n + N_FFT / 2) / HOP + 1)
        val logSpec = FloatArray(N_MELS * FRAMES) { -10f }
        val fr = FloatArray(N_FFT)
        val power = FloatArray(BINS)
        val cosT = cosTab
        val sinT = sinTab
        val fb = filters

        for (t in 0 until active) {
            for (i in 0 until N_FFT) {
                var p = t * HOP + i - N_FFT / 2
                if (p < 0) p = -p
                if (p >= CHUNK) p = 2 * (CHUNK - 1) - p
                fr[i] = if (p < n) samples[from + p] * window[i] else 0f
            }
            for (k in 0 until BINS) {
                var re = 0f
                var im = 0f
                val base = k * N_FFT
                for (i in 0 until N_FFT) {
                    val v = fr[i]
                    re += v * cosT[base + i]
                    im += v * sinT[base + i]
                }
                power[k] = re * re + im * im
            }
            for (m in 0 until N_MELS) {
                val w = fb[m]
                var s = 0f
                for (k in 0 until BINS) s += w[k] * power[k]
                logSpec[m * FRAMES + t] = log10(max(s, 1e-10f))
            }
        }
        var mx = -Float.MAX_VALUE
        for (v in logSpec) if (v > mx) mx = v
        val floor = mx - 8f
        for (i in logSpec.indices) logSpec[i] = (max(logSpec[i], floor) + 4f) / 4f
        return logSpec
    }
}
