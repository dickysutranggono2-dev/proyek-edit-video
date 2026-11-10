package com.dik.videoedit.models

/**
 * Penyesuaian warna manual (panel "Sesuaikan"). Semua nilai -1..1, 0 = tidak berubah.
 * Dipakai juga sebagai "resep" tiap filter preset.
 */
data class Adjust(
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val exposure: Float = 0f,
    val warmth: Float = 0f,
    val tint: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val fade: Float = 0f,
) {
    val isNeutral: Boolean
        get() = brightness == 0f && contrast == 0f && saturation == 0f && exposure == 0f &&
            warmth == 0f && tint == 0f && highlights == 0f && shadows == 0f && fade == 0f
}

enum class AdjustKey(val label: String) {
    BRIGHTNESS("Kecerahan"),
    CONTRAST("Kontras"),
    SATURATION("Saturasi"),
    EXPOSURE("Eksposur"),
    WARMTH("Suhu"),
    TINT("Rona"),
    HIGHLIGHTS("Sorotan"),
    SHADOWS("Bayangan"),
    FADE("Pudar");

    fun get(a: Adjust): Float = when (this) {
        BRIGHTNESS -> a.brightness
        CONTRAST -> a.contrast
        SATURATION -> a.saturation
        EXPOSURE -> a.exposure
        WARMTH -> a.warmth
        TINT -> a.tint
        HIGHLIGHTS -> a.highlights
        SHADOWS -> a.shadows
        FADE -> a.fade
    }

    fun set(a: Adjust, v: Float): Adjust = when (this) {
        BRIGHTNESS -> a.copy(brightness = v)
        CONTRAST -> a.copy(contrast = v)
        SATURATION -> a.copy(saturation = v)
        EXPOSURE -> a.copy(exposure = v)
        WARMTH -> a.copy(warmth = v)
        TINT -> a.copy(tint = v)
        HIGHLIGHTS -> a.copy(highlights = v)
        SHADOWS -> a.copy(shadows = v)
        FADE -> a.copy(fade = v)
    }
}

/** Filter preset. [lowTint]/[highTint]: warna ARGB untuk bayangan/sorotan (0 = tanpa), kekuatannya tetap kecil. */
data class FilterPreset(
    val id: String,
    val label: String,
    val category: String,
    val adjust: Adjust,
    val lowTint: Int = 0,
    val highTint: Int = 0,
    val featured: Boolean = false,
)

object Looks {
    val categories = listOf(
        "Unggulan", "Baru", "Hit", "Kehidupan", "Lanskap", "Potret", "Mono",
        "Film", "Retro", "Senja", "Pantai", "Aksi", "Awal 2000-an",
    )

    private const val TEAL = 0xFF1F7A8C.toInt()
    private const val ORANGE = 0xFFE8913A.toInt()
    private const val BLUE = 0xFF2F5FD0.toInt()
    private const val PINK = 0xFFE86A9A.toInt()
    private const val GOLD = 0xFFF0B84A.toInt()
    private const val GREEN = 0xFF3E9B5A.toInt()
    private const val PURPLE = 0xFF7A4FC0.toInt()

    private fun p(
        id: String, label: String, cat: String,
        b: Float = 0f, c: Float = 0f, s: Float = 0f, e: Float = 0f, w: Float = 0f, t: Float = 0f,
        h: Float = 0f, sh: Float = 0f, f: Float = 0f, lo: Int = 0, hi: Int = 0, star: Boolean = false,
    ) = FilterPreset(id, label, cat, Adjust(b, c, s, e, w, t, h, sh, f), lo, hi, star)

    val all: List<FilterPreset> by lazy { listOf(
        // Kehidupan
        p("jelas", "Jelas", "Kehidupan", c = 0.18f, s = 0.2f, h = -0.1f, sh = 0.15f, star = true),
        p("segar", "Segar", "Kehidupan", b = 0.06f, c = 0.1f, s = 0.28f, w = -0.05f, star = true),
        p("cerah", "Cerah", "Kehidupan", b = 0.08f, c = 0.12f),
        p("rapi", "Rapi", "Kehidupan", c = 0.12f, s = -0.08f, h = -0.15f, sh = 0.2f),
        p("bokeh", "Hangat Lembut", "Kehidupan", b = 0.05f, c = -0.05f, w = 0.18f, f = 0.15f),
        p("kafe", "Kafe", "Kehidupan", c = 0.1f, s = -0.05f, w = 0.25f, f = 0.1f, hi = ORANGE),
        p("harian", "Harian", "Kehidupan", c = 0.08f, s = 0.12f),
        p("bersih", "Bersih", "Kehidupan", b = 0.1f, c = 0.05f, s = -0.05f, sh = 0.2f),
        // Lanskap
        p("hutan", "Hutan", "Lanskap", c = 0.14f, s = 0.3f, w = -0.05f, t = -0.1f, lo = GREEN),
        p("gunung", "Gunung", "Lanskap", c = 0.2f, s = 0.12f, w = -0.12f, h = -0.2f, star = true),
        p("langit_biru", "Langit Biru", "Lanskap", c = 0.12f, s = 0.35f, w = -0.2f, hi = BLUE),
        p("kabut", "Kabut", "Lanskap", b = 0.05f, c = -0.15f, s = -0.2f, f = 0.3f),
        p("kota_neon", "Kota Neon", "Lanskap", c = 0.22f, s = 0.3f, w = -0.1f, lo = PURPLE, hi = TEAL),
        p("danau", "Danau", "Lanskap", c = 0.1f, s = 0.2f, w = -0.15f, t = -0.05f, lo = TEAL),
        p("musim_gugur", "Musim Gugur", "Lanskap", c = 0.12f, s = 0.15f, w = 0.3f, hi = ORANGE),
        p("jernih", "Jernih HD", "Lanskap", c = 0.25f, s = 0.18f, h = -0.15f, sh = 0.1f),
        // Potret
        p("porselen", "Porselen", "Potret", b = 0.1f, c = -0.08f, s = -0.1f, w = 0.05f, f = 0.12f, star = true),
        p("bersinar", "Bersinar", "Potret", b = 0.1f, c = 0.05f, s = 0.1f, w = 0.12f, sh = 0.15f),
        p("fokus", "Fokus", "Potret", c = 0.18f, s = -0.05f, h = -0.1f),
        p("matte", "Matte", "Potret", c = -0.1f, s = -0.1f, f = 0.35f),
        p("remaja", "Remaja", "Potret", b = 0.08f, s = 0.22f, w = 0.08f, t = 0.12f, hi = PINK),
        p("logam", "Logam", "Potret", c = 0.15f, s = -0.3f, w = -0.1f, lo = TEAL),
        p("peach", "Peach", "Potret", b = 0.06f, c = -0.05f, s = 0.08f, w = 0.12f, t = 0.2f, f = 0.1f, hi = PINK),
        p("senyap", "Senyap", "Potret", c = 0.05f, s = -0.4f, w = -0.05f),
        // Mono
        p("hp", "Hitam Putih", "Mono", s = -1f, c = 0.1f, star = true),
        p("hp_kontras", "HP Kontras", "Mono", s = -1f, c = 0.4f, sh = -0.1f),
        p("hp_lembut", "HP Lembut", "Mono", s = -1f, c = -0.1f, f = 0.25f),
        p("noir", "Noir", "Mono", b = -0.06f, s = -1f, c = 0.5f, h = -0.2f),
        p("matte_noir", "Matte Noir", "Mono", s = -1f, c = 0.05f, f = 0.4f),
        p("perak", "Perak", "Mono", b = 0.04f, s = -1f, c = 0.2f, w = -0.1f, hi = BLUE),
        p("kertas", "Kertas Koran", "Mono", s = -0.8f, c = 0.15f, w = 0.15f, f = 0.2f),
        p("senja_mono", "Mono Hangat", "Mono", s = -1f, c = 0.2f, w = 0.35f),
        // Film
        p("film_dingin", "Film Dingin", "Film", c = 0.12f, s = -0.15f, w = -0.2f, f = 0.2f, lo = TEAL, hi = ORANGE, star = true),
        p("film_hangat", "Film Hangat", "Film", c = 0.1f, s = -0.05f, w = 0.25f, f = 0.2f, lo = TEAL, hi = GOLD),
        p("sinema", "Sinema", "Film", c = 0.2f, s = 0.05f, f = 0.12f, lo = TEAL, hi = ORANGE),
        p("kodak", "Warna Analog", "Film", c = 0.08f, s = 0.12f, w = 0.12f, f = 0.18f),
        p("fuji", "Hijau Analog", "Film", c = 0.1f, s = 0.05f, w = -0.05f, t = -0.15f, f = 0.18f, lo = GREEN),
        p("bleach", "Pucat Sinema", "Film", c = 0.3f, s = -0.45f, h = -0.1f),
        p("drama", "Drama", "Film", c = 0.3f, s = -0.1f, sh = -0.15f, h = -0.2f),
        p("sutradara", "Sutradara", "Film", b = -0.03f, c = 0.18f, s = -0.1f, w = 0.08f, f = 0.1f, lo = TEAL),
        // Retro
        p("retro70", "Retro 70an", "Retro", b = 0.02f, c = -0.05f, s = 0.05f, w = 0.35f, t = 0.1f, f = 0.28f, hi = GOLD, star = true),
        p("vhs", "Kaset VHS", "Retro", c = -0.1f, s = 0.12f, w = 0.08f, t = 0.15f, f = 0.3f, lo = BLUE, hi = PINK),
        p("polaroid", "Polaroid", "Retro", b = 0.06f, c = -0.08f, s = -0.05f, w = 0.18f, f = 0.3f),
        p("sepia", "Sepia", "Retro", s = -0.55f, w = 0.5f, c = 0.05f, f = 0.12f),
        p("foto_tua", "Foto Tua", "Retro", c = -0.12f, s = -0.3f, w = 0.3f, f = 0.4f),
        p("memudar", "Memudar", "Retro", b = 0.04f, c = -0.2f, s = -0.15f, f = 0.5f),
        p("koran_tua", "Cetak Lawas", "Retro", c = 0.1f, s = -0.5f, w = 0.2f, f = 0.25f),
        // Senja
        p("senja", "Senja", "Senja", c = 0.12f, s = 0.22f, w = 0.4f, t = 0.08f, hi = ORANGE, star = true),
        p("emas", "Jam Emas", "Senja", b = 0.04f, c = 0.1f, s = 0.15f, w = 0.35f, hi = GOLD),
        p("senja_ungu", "Senja Ungu", "Senja", c = 0.15f, s = 0.2f, w = 0.12f, t = 0.3f, lo = PURPLE, hi = PINK),
        p("matahari", "Matahari Terbit", "Senja", b = 0.06f, c = 0.08f, s = 0.18f, w = 0.3f, hi = GOLD),
        p("romansa", "Romansa", "Senja", b = 0.05f, c = 0.05f, s = 0.1f, w = 0.2f, t = 0.25f, f = 0.12f, hi = PINK),
        p("api", "Langit Api", "Senja", c = 0.25f, s = 0.4f, w = 0.45f, hi = ORANGE),
        // Pantai
        p("laut", "Laut", "Pantai", c = 0.12f, s = 0.3f, w = -0.25f, hi = TEAL, star = true),
        p("pulau", "Pulau Biru", "Pantai", b = 0.05f, c = 0.1f, s = 0.38f, w = -0.15f),
        p("liburan", "Liburan", "Pantai", b = 0.08f, c = 0.12f, s = 0.25f, w = 0.12f),
        p("tropis", "Tropis", "Pantai", c = 0.14f, s = 0.4f, t = -0.1f, hi = GREEN),
        p("pasir", "Pasir", "Pantai", b = 0.08f, c = 0.05f, s = -0.05f, w = 0.25f, f = 0.12f),
        p("karang", "Karang", "Pantai", c = 0.1f, s = 0.25f, w = -0.3f, t = -0.1f, lo = TEAL),
        p("ombak", "Ombak", "Pantai", b = 0.06f, c = 0.05f, s = 0.12f, w = -0.1f, f = 0.15f),
        // Aksi
        p("aksi", "Aksi", "Aksi", c = 0.3f, s = 0.12f, sh = -0.1f, h = -0.15f, star = true),
        p("gelap", "Gelap Tegas", "Aksi", b = -0.08f, c = 0.35f, s = -0.1f, e = -0.1f),
        p("baja", "Baja", "Aksi", c = 0.28f, s = -0.35f, w = -0.15f, lo = BLUE),
        p("panas", "Panas", "Aksi", c = 0.25f, s = 0.18f, w = 0.3f),
        p("tegang", "Tegang", "Aksi", b = -0.05f, c = 0.3f, s = -0.2f, w = -0.1f, lo = TEAL),
        p("tajam", "Tajam", "Aksi", c = 0.22f, s = 0.15f, h = -0.25f, sh = 0.1f),
        // Awal 2000-an
        p("y2k", "Y2K Biru", "Awal 2000-an", b = 0.08f, c = 0.1f, s = 0.3f, w = -0.25f, f = 0.1f, hi = BLUE, star = true),
        p("digicam", "Kamera Digital", "Awal 2000-an", b = 0.1f, c = 0.18f, s = 0.12f, w = 0.05f, e = 0.1f),
        p("flash", "Lampu Kilat", "Awal 2000-an", b = 0.12f, c = 0.25f, s = -0.05f, e = 0.18f, h = -0.2f),
        p("pop", "Pop", "Awal 2000-an", b = 0.05f, c = 0.14f, s = 0.5f, t = 0.1f),
        p("cd", "Piringan", "Awal 2000-an", c = 0.1f, s = 0.1f, w = -0.2f, t = 0.12f, f = 0.15f, lo = PURPLE, hi = TEAL),
        p("kampus", "Kampus", "Awal 2000-an", b = 0.05f, c = 0.08f, s = 0.05f, w = 0.08f, f = 0.2f),
    ).let { list ->
        // "Baru" dan "Hit" memilih ulang filter yang sudah ada agar mudah dijangkau.
        val withExtra = list + generated()
        val newIds = listOf("sinema", "y2k", "film_dingin", "senja_ungu", "matte_noir", "peach", "kota_neon", "digicam")
        val hitIds = listOf("jelas", "noir", "senja", "film_hangat", "laut", "porselen", "retro70", "aksi")
        withExtra + newIds.mapNotNull { id -> list.firstOrNull { it.id == id } }.map { it.copy(category = "Baru", featured = false) } +
            hitIds.mapNotNull { id -> list.firstOrNull { it.id == id } }.map { it.copy(category = "Hit", featured = false) }
    } }


    // ---- filter tambahan: tiap kategori punya resep dasar, lalu diberi variasi bernama --------------

    private class Base(val b: Float, val c: Float, val s: Float, val w: Float, val t: Float, val f: Float, val lo: Int, val hi: Int)

    private val bases: Map<String, Base> by lazy { mapOf(
        "Kehidupan" to Base(0.04f, 0.1f, 0.15f, 0f, 0f, 0f, 0, 0),
        "Lanskap" to Base(0f, 0.14f, 0.22f, -0.05f, 0f, 0f, 0, 0),
        "Potret" to Base(0.06f, 0.02f, 0f, 0.08f, 0.05f, 0.08f, 0, 0),
        "Mono" to Base(0f, 0.2f, -1f, 0f, 0f, 0.1f, 0, 0),
        "Film" to Base(0f, 0.15f, -0.08f, 0.05f, 0f, 0.18f, TEAL, ORANGE),
        "Retro" to Base(0.02f, -0.08f, -0.05f, 0.25f, 0.08f, 0.3f, 0, GOLD),
        "Senja" to Base(0.02f, 0.12f, 0.2f, 0.35f, 0.05f, 0f, 0, ORANGE),
        "Pantai" to Base(0.06f, 0.1f, 0.3f, -0.12f, -0.05f, 0.05f, TEAL, 0),
        "Aksi" to Base(-0.02f, 0.28f, 0.05f, 0f, 0f, 0f, BLUE, 0),
        "Awal 2000-an" to Base(0.08f, 0.14f, 0.2f, 0f, 0.08f, 0.12f, 0, BLUE),
    ).let { it + mapOf("Hit" to it.getValue("Kehidupan")) } }

    // Tiap variasi: nama, perubahan (kecerahan, kontras, saturasi, suhu, rona, pudar) dan warna nada.
    private class Var(val n: String, val db: Float, val dc: Float, val ds: Float, val dw: Float, val dt: Float, val df: Float, val lo: Int = 0, val hi: Int = 0)

    private val names: Map<String, List<String>> by lazy { mapOf(
        "Kehidupan" to listOf("Pagi Cerah", "Hari Biasa", "Rumah", "Sahabat", "Jalan-jalan", "Makan Enak", "Kota Hidup", "Santai", "Ceria", "Natural"),
        "Lanskap" to listOf("Sawah", "Bukit", "Awan", "Embun", "Padang Hijau", "Air Terjun", "Salju", "Gurun", "Rimba", "Cakrawala"),
        "Potret" to listOf("Lembut", "Kulit Halus", "Wajah Cerah", "Mata Tajam", "Studio", "Elegan", "Manis", "Klasik", "Segar Muda", "Hangat Manis"),
        "Mono" to listOf("Abu Lembut", "Grafit", "Arang", "Tinta", "Kabut Abu", "Putih Tinggi", "Hitam Dalam", "Dokumenter", "Jalanan", "Seni"),
        "Film" to listOf("Malam Kota", "Layar Lebar", "Analog 35", "Teater", "Detektif", "Romantis Film", "Epik", "Indie", "Thriller", "Klasik Hollywood"),
        "Retro" to listOf("Kaset Lama", "Disko", "Album Keluarga", "Kartu Pos", "Arcade", "Vintage Kuning", "Layar Tabung", "Jadul", "Majalah Lama", "Radio Tua"),
        "Senja" to listOf("Merah Jambu", "Jingga Pekat", "Langit Magenta", "Sore Emas", "Remang", "Fajar", "Cahaya Hangat", "Bara", "Lembayung", "Pelangi Sore"),
        "Pantai" to listOf("Biru Tosca", "Laguna", "Pasir Putih", "Selancar", "Pulau Kecil", "Teluk", "Siang Laut", "Karang Cerah", "Kapal Layar", "Camar"),
        "Aksi" to listOf("Kejar", "Ledakan", "Balap", "Medan Laga", "Olahraga", "Pertarungan", "Adrenalin", "Garang", "Baja Dingin", "Sirkuit"),
        "Awal 2000-an" to listOf("Handycam", "MTV", "Warnet", "Ponsel Lipat", "Foto Cetak", "Kamera Saku", "Pesta Sekolah", "Boyband", "Lensa Murah", "Cerah Murah"),
        "Hit" to listOf("Viral", "Populer", "Estetik", "Favorit", "Terlaris", "Kekinian", "Mantap", "Juara", "Top Harian", "Pilihan Editor"),
    ) }

    private val vars: List<Var> by lazy { listOf(
        Var("", 0.03f, 0.04f, 0.1f, 0.05f, 0f, 0f),
        Var("", -0.03f, 0.1f, -0.05f, -0.08f, 0f, 0.05f, 0, 0),
        Var("", 0.06f, -0.04f, 0.05f, 0.12f, 0.05f, 0.08f),
        Var("", -0.05f, 0.14f, 0.12f, 0f, -0.06f, 0f, PURPLE, 0),
        Var("", 0.02f, 0.06f, -0.12f, -0.12f, 0.04f, 0.1f, 0, PINK),
        Var("", 0.08f, 0f, 0.15f, 0.08f, 0f, 0.04f, 0, GOLD),
        Var("", -0.02f, 0.18f, 0f, 0.15f, 0.08f, 0.02f, TEAL, 0),
        Var("", 0.04f, -0.08f, 0.2f, -0.05f, -0.04f, 0.12f),
        Var("", -0.06f, 0.08f, 0.06f, 0.06f, 0.1f, 0f, 0, ORANGE),
        Var("", 0.1f, 0.02f, -0.08f, -0.02f, 0f, 0.15f, BLUE, 0),
    ) }

    private fun slug(s: String) = s.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private fun generated(): List<FilterPreset> {
        val out = ArrayList<FilterPreset>()
        names.forEach { (cat, list) ->
            val base = bases.getValue(cat)
            list.forEachIndexed { i, name ->
                val v = vars[i % vars.size]
                val mono = cat == "Mono"
                out += FilterPreset(
                    id = "x_${slug(cat)}_${slug(name)}",
                    label = name,
                    category = cat,
                    adjust = Adjust(
                        brightness = (base.b + v.db).coerceIn(-0.4f, 0.4f),
                        contrast = (base.c + v.dc).coerceIn(-0.5f, 0.6f),
                        saturation = if (mono) -1f else (base.s + v.ds).coerceIn(-0.8f, 0.8f),
                        warmth = (base.w + v.dw).coerceIn(-0.6f, 0.6f),
                        tint = (base.t + v.dt).coerceIn(-0.4f, 0.4f),
                        fade = (base.f + v.df).coerceIn(0f, 0.6f),
                    ),
                    lowTint = if (v.lo != 0) v.lo else base.lo,
                    highTint = if (v.hi != 0) v.hi else base.hi,
                )
            }
        }
        return out
    }

    fun inCategory(cat: String): List<FilterPreset> =
        if (cat == "Unggulan") all.filter { it.featured } else all.filter { it.category == cat }

    fun byId(id: String): FilterPreset? = all.firstOrNull { it.id == id }
}

/** Template gaya keterangan (subtitle). */
data class CaptionStyle(
    val id: String,
    val label: String,
    val color: Int,
    val bg: Int?,
    val bold: Boolean,
    val size: TextSize = TextSize.MEDIUM,
    val position: TextPosition = TextPosition.BOTTOM,
)

object CaptionStyles {
    val all = listOf(
        CaptionStyle("default", "Standar", 0xFFFFFFFF.toInt(), null, false),
        CaptionStyle("bold", "Tebal", 0xFFFFFFFF.toInt(), 0x99000000.toInt(), true),
        CaptionStyle("kuning", "Kuning", 0xFFFFD54F.toInt(), 0x00000000, true),
        CaptionStyle("merah", "Merah", 0xFFFF4D4D.toInt(), 0xCCFFFFFF.toInt(), true),
        CaptionStyle("biru", "Biru", 0xFFFFFFFF.toInt(), 0xCC1F5FD0.toInt(), true),
        CaptionStyle("hijau", "Hijau", 0xFF0B3D1E.toInt(), 0xDD7BE495.toInt(), true),
        CaptionStyle("besar", "Besar Tengah", 0xFFFFFFFF.toInt(), 0x00000000, true, TextSize.LARGE, TextPosition.CENTER),
        CaptionStyle("kecil", "Kecil Atas", 0xFFFFFFFF.toInt(), 0x66000000, false, TextSize.SMALL, TextPosition.TOP),
    )

    fun byId(id: String): CaptionStyle = all.firstOrNull { it.id == id } ?: all.first()
}
