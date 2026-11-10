package com.dik.videoedit.videoprocessing

import android.graphics.Color
import androidx.media3.common.Effect
import androidx.media3.effect.SingleColorLut
import com.dik.videoedit.models.Adjust
import com.dik.videoedit.models.FilterPreset
import com.dik.videoedit.models.Looks
import com.dik.videoedit.models.Project
import kotlin.math.pow

/**
 * Filter preset + penyesuaian manual dihitung menjadi satu tabel warna 3D (LUT). Satu efek GPU
 * saja, sama persis di preview dan export. Rumus yang sama dipakai untuk gambar kecil filter.
 */
object LookLut {
    private const val SIZE = 17

    private fun luma(r: Float, g: Float, b: Float) = 0.2126f * r + 0.7152f * g + 0.0722f * b

    /** Terapkan [a] ke warna [c] (r,g,b 0..1) di tempat. */
    private fun applyAdjust(c: FloatArray, a: Adjust, lowTint: Int, highTint: Int) {
        if (a.exposure != 0f) {
            val k = 2f.pow(a.exposure * 1.5f)
            for (i in 0..2) c[i] *= k
        }
        if (a.brightness != 0f) for (i in 0..2) c[i] += a.brightness * 0.25f
        if (a.contrast != 0f) {
            val k = 1f + a.contrast * 0.8f
            for (i in 0..2) c[i] = (c[i] - 0.5f) * k + 0.5f
        }
        if (a.warmth != 0f) {
            c[0] += a.warmth * 0.12f
            c[2] -= a.warmth * 0.12f
        }
        if (a.tint != 0f) {
            c[1] -= a.tint * 0.08f
            c[0] += a.tint * 0.04f
            c[2] += a.tint * 0.04f
        }
        var l = luma(c[0], c[1], c[2]).coerceIn(0f, 1f)
        if (a.shadows != 0f) {
            val w = (1f - l) * (1f - l) * a.shadows * 0.3f
            for (i in 0..2) c[i] += w
        }
        if (a.highlights != 0f) {
            val w = l * l * a.highlights * 0.3f
            for (i in 0..2) c[i] += w
        }
        if (a.fade != 0f) {
            val f = a.fade * 0.14f
            for (i in 0..2) c[i] = f + c[i] * (1f - f)
        }
        if (a.saturation != 0f) {
            l = luma(c[0], c[1], c[2])
            val k = 1f + a.saturation
            for (i in 0..2) c[i] = l + (c[i] - l) * k
        }
        if (lowTint != 0 || highTint != 0) {
            l = luma(c[0], c[1], c[2]).coerceIn(0f, 1f)
            if (lowTint != 0) tone(c, lowTint, (1f - l) * (1f - l) * 0.16f)
            if (highTint != 0) tone(c, highTint, l * l * 0.16f)
        }
        for (i in 0..2) c[i] = c[i].coerceIn(0f, 1f)
    }

    private fun tone(c: FloatArray, argb: Int, amount: Float) {
        val tr = Color.red(argb) / 255f
        val tg = Color.green(argb) / 255f
        val tb = Color.blue(argb) / 255f
        val tl = luma(tr, tg, tb)
        c[0] += (tr - tl) * amount * 2f
        c[1] += (tg - tl) * amount * 2f
        c[2] += (tb - tl) * amount * 2f
    }

    /** Warna hasil akhir untuk satu piksel (dipakai LUT dan gambar kecil filter). */
    fun transform(c: FloatArray, preset: FilterPreset?, strength: Float, adjust: Adjust) {
        if (preset != null && strength > 0f) {
            val o0 = c[0]; val o1 = c[1]; val o2 = c[2]
            applyAdjust(c, preset.adjust, preset.lowTint, preset.highTint)
            c[0] = o0 + (c[0] - o0) * strength
            c[1] = o1 + (c[1] - o1) * strength
            c[2] = o2 + (c[2] - o2) * strength
        }
        if (!adjust.isNeutral) applyAdjust(c, adjust, 0, 0)
    }

    fun transformColor(argb: Int, preset: FilterPreset?, strength: Float, adjust: Adjust): Int {
        val c = floatArrayOf(Color.red(argb) / 255f, Color.green(argb) / 255f, Color.blue(argb) / 255f)
        transform(c, preset, strength, adjust)
        return Color.rgb((c[0] * 255f + 0.5f).toInt(), (c[1] * 255f + 0.5f).toInt(), (c[2] * 255f + 0.5f).toInt())
    }

    /** Efek LUT untuk proyek ini, atau null bila tidak ada filter/penyesuaian. */
    fun effect(p: Project): Effect? {
        val preset = if (p.look.isEmpty()) null else Looks.byId(p.look)
        if (preset == null && p.adjust.isNeutral) return null
        val n = SIZE
        val cube = Array(n) { ri -> Array(n) { gi -> IntArray(n) { bi ->
            val c = floatArrayOf(ri / (n - 1f), gi / (n - 1f), bi / (n - 1f))
            transform(c, preset, p.lookStrength, p.adjust)
            Color.rgb((c[0] * 255f + 0.5f).toInt(), (c[1] * 255f + 0.5f).toInt(), (c[2] * 255f + 0.5f).toInt())
        } } }
        return SingleColorLut.createFromCube(cube)
    }
}
