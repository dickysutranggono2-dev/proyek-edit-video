package com.dik.videoedit.videoeditor

import java.util.Locale

/** 75000 -> "01:15" */
fun formatTime(ms: Long): String {
    val s = ms.coerceAtLeast(0L) / 1000
    return String.format(Locale.US, "%02d:%02d", s / 60, s % 60)
}

/** 75300 -> "01:15.3" (presisi 0,1 detik untuk panel potong). */
fun formatTimeTenth(ms: Long): String {
    val t = ms.coerceAtLeast(0L) / 100
    return String.format(Locale.US, "%02d:%02d.%d", t / 600, (t / 10) % 60, t % 10)
}

fun formatSize(bytes: Long): String =
    if (bytes >= 1_048_576L) String.format(Locale.US, "%.1f MB", bytes / 1_048_576.0)
    else String.format(Locale.US, "%d KB", maxOf(1L, bytes / 1024))

fun formatSpeed(speed: Float): String =
    (if (speed % 1f == 0f) speed.toInt().toString() else speed.toString().replace('.', ',')) + "x"
