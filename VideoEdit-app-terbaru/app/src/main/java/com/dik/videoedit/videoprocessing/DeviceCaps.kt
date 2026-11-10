package com.dik.videoedit.videoprocessing

import android.media.MediaCodecInfo
import android.media.MediaCodecList

/** Menanyakan kemampuan encoder H.264 perangkat (resolusi dan FPS yang benar-benar didukung). */
object DeviceCaps {
    private const val AVC = "video/avc"

    private val caps: List<MediaCodecInfo.VideoCapabilities> by lazy {
        runCatching {
            MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
                .filter { it.isEncoder && it.supportedTypes.any { t -> t.equals(AVC, ignoreCase = true) } }
                .mapNotNull { runCatching { it.getCapabilitiesForType(AVC).videoCapabilities }.getOrNull() }
        }.getOrDefault(emptyList())
    }

    fun sizeSupported(w: Int, h: Int): Boolean {
        if (caps.isEmpty()) return true // tidak bisa memastikan; biarkan encoder fallback yang menangani
        return caps.any { runCatching { it.isSizeSupported(w, h) || it.isSizeSupported(h, w) }.getOrDefault(false) }
    }

    fun fpsSupported(w: Int, h: Int, fps: Int): Boolean {
        if (caps.isEmpty()) return fps <= 30
        return caps.any {
            runCatching {
                it.areSizeAndRateSupported(w, h, fps.toDouble()) || it.areSizeAndRateSupported(h, w, fps.toDouble())
            }.getOrDefault(false)
        }
    }
}
