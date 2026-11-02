package com.dik.videoedit.videoprocessing

import android.graphics.Paint

object TextFit {
    /** Mengecilkan ukuran huruf (px) bila teks lebih lebar dari [maxWidthPx]. Dipakai preview DAN export. */
    fun fitPx(text: String, desiredPx: Float, maxWidthPx: Float): Float {
        if (text.isEmpty() || maxWidthPx <= 0f) return desiredPx
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = desiredPx }
        val width = paint.measureText(text) + desiredPx * 0.3f // ruang padding latar
        return if (width > maxWidthPx) desiredPx * maxWidthPx / width else desiredPx
    }
}
