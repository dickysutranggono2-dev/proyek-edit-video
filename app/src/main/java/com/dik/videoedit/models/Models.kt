package com.dik.videoedit.models

import java.util.UUID

/** Satu klip video dalam timeline. Semua durasi dalam milidetik. */
data class Clip(
    val id: String = UUID.randomUUID().toString(),
    val uri: String,
    val name: String,
    val sourceDurationMs: Long,
    /** Dimensi tampilan (sudah memperhitungkan rotasi metadata). */
    val width: Int,
    val height: Int,
    val frameRate: Int,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = sourceDurationMs,
    val speed: Float = 1f,
    val volume: Float = 1f,
) {
    val trimmedMs: Long get() = trimEndMs - trimStartMs

    /** Durasi di timeline setelah kecepatan diterapkan. */
    val effectiveMs: Long get() = (trimmedMs / speed).toLong()
}

enum class AspectOption(val label: String, val ratio: Float?) {
    ORIGINAL("Asli", null),
    PORTRAIT("9:16", 9f / 16f),
    SQUARE("1:1", 1f),
    LANDSCAPE("16:9", 16f / 9f),
}

/** anchorY memakai koordinat NDC OpenGL (+1 = atas, -1 = bawah). */
enum class TextPosition(val label: String, val anchorY: Float) {
    TOP("Atas", 0.80f),
    CENTER("Tengah", 0f),
    BOTTOM("Bawah", -0.80f),
}

/** fraction = tinggi huruf relatif terhadap sisi terpendek frame. */
enum class TextSize(val label: String, val fraction: Float) {
    SMALL("Kecil", 0.055f),
    MEDIUM("Sedang", 0.08f),
    LARGE("Besar", 0.115f),
}

object TextColors {
    val all: List<Int> = listOf(
        0xFFFFFFFF.toInt(), 0xFFFFD54F.toInt(), 0xFF63B3FF.toInt(),
        0xFFFF6B6B.toInt(), 0xFF000000.toInt(),
    )
}

data class TextItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val position: TextPosition = TextPosition.BOTTOM,
    val color: Int = 0xFFFFFFFF.toInt(),
    val size: TextSize = TextSize.MEDIUM,
) {
    /** Latar semi-transparan agar teks tetap terbaca di video terang/gelap. */
    val backgroundColor: Int get() = if (color == 0xFF000000.toInt()) 0x66FFFFFF else 0x66000000
}

enum class FilterType(val label: String, val premium: Boolean) {
    NONE("Asli", false),
    BRIGHT("Cerah", false),
    GRAYSCALE("Hitam Putih", true),
    SEPIA("Sepia", true),
    WARM("Hangat", true),
    COOL("Dingin", true),
    VIVID("Vivid", true),
}

enum class EffectType(val label: String, val premium: Boolean) {
    NONE("Tanpa Efek", false),
    ZOOM("Zoom", true),
    MIRROR("Cermin", true),
    BLUR("Blur", true),
    INVERT("Negatif", true),
}

enum class TransitionType(val label: String, val premium: Boolean) {
    CUT("Potong", false),
    FADE_BLACK("Fade Hitam", true),
    FADE_WHITE("Fade Putih", true),
}

data class MusicTrack(
    val uri: String,
    val name: String,
    val durationMs: Long,
    val volume: Float = 0.8f,
)

data class Project(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val clips: List<Clip>,
    val aspect: AspectOption = AspectOption.ORIGINAL,
    /** Rotasi searah jarum jam: 0, 90, 180, 270. */
    val rotation: Int = 0,
    val music: MusicTrack? = null,
    val texts: List<TextItem> = emptyList(),
    val filter: FilterType = FilterType.NONE,
    val effect: EffectType = EffectType.NONE,
    val transition: TransitionType = TransitionType.CUT,
) {
    val totalDurationMs: Long get() = clips.sumOf { it.effectiveMs }

    val usesPremium: Boolean get() = filter.premium || effect.premium || transition.premium

    /** Rasio (lebar/tinggi) frame hasil akhir. */
    val outputRatio: Float
        get() {
            aspect.ratio?.let { return it }
            val c = clips.firstOrNull() ?: return 16f / 9f
            val r = c.width.toFloat() / c.height.coerceAtLeast(1)
            return if (rotation % 180 != 0) 1f / r else r
        }
}

/** Preset awal untuk layar Template. */
data class TemplatePreset(
    val id: String,
    val title: String,
    val description: String,
    val aspect: AspectOption = AspectOption.ORIGINAL,
    val speed: Float = 1f,
    val filter: FilterType = FilterType.NONE,
)

object Templates {
    val all: List<TemplatePreset> = listOf(
        TemplatePreset("reels", "Reels & Shorts", "Format vertikal 9:16 untuk Instagram, TikTok, dan YouTube Shorts", aspect = AspectOption.PORTRAIT),
        TemplatePreset("square", "Feed Persegi", "Rasio 1:1 yang pas untuk feed dan postingan", aspect = AspectOption.SQUARE),
        TemplatePreset("wide", "Layar Lebar", "Rasio 16:9 untuk YouTube dan layar TV", aspect = AspectOption.LANDSCAPE),
        TemplatePreset("slowmo", "Gerak Lambat", "Kecepatan 0,5x untuk momen yang dramatis", speed = 0.5f),
        TemplatePreset("timelapse", "Timelapse", "Kecepatan 3x untuk perjalanan atau proses panjang", speed = 3f),
        TemplatePreset("bright", "Cerah & Segar", "Filter Cerah agar video lebih hidup", filter = FilterType.BRIGHT),
    )
}
