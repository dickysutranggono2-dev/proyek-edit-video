package com.dik.videoedit.videoprocessing

import com.dik.videoedit.models.Project
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * @param shortSide sisi terpendek hasil (480/720/1080). Untuk video vertikal 1080p = 1080x1920.
 * @param fps 0 = ikuti sumber, selain itu FPS target.
 */
data class ExportOptions(
    val shortSide: Int,
    val fps: Int,
    val removeWatermark: Boolean,
)

object ExportPlan {
    private fun even(v: Float): Int = (v.roundToInt() / 2) * 2

    fun outputSize(ratio: Float, shortSide: Int): Pair<Int, Int> =
        if (ratio >= 1f) even(shortSide * ratio) to even(shortSide.toFloat())
        else even(shortSide.toFloat()) to even(shortSide / ratio)

    fun sourceFps(p: Project): Int = (p.clips.firstOrNull()?.frameRate ?: 30).coerceIn(1, 60)

    fun effectiveFps(p: Project, o: ExportOptions): Int =
        if (o.fps > 0) min(o.fps, sourceFps(p)) else sourceFps(p)

    /** Bitrate video target (bps) berdasarkan jumlah piksel dan FPS. */
    fun videoBitrate(w: Int, h: Int, fps: Int): Int =
        (w.toLong() * h * fps * 0.09).toInt().coerceIn(600_000, 16_000_000)

    private const val AUDIO_BITRATE = 128_000

    fun estimateBytes(p: Project, o: ExportOptions): Long {
        val (w, h) = outputSize(p.outputRatio, o.shortSide)
        val bits = (videoBitrate(w, h, effectiveFps(p, o)) + AUDIO_BITRATE).toDouble() * (p.totalDurationMs / 1000.0)
        return (bits / 8).toLong()
    }
}
