package com.dik.videoedit.storage

/** Pembaca file keterangan .srt dan .vtt sederhana. */
object Srt {
    data class Cue(val startMs: Long, val endMs: Long, val text: String)

    private val TIME = Regex("""(?:(\d+):)?(\d{1,2}):(\d{2})[,.](\d{1,3})\s*-->\s*(?:(\d+):)?(\d{1,2}):(\d{2})[,.](\d{1,3})""")

    private fun ms(h: String, m: String, s: String, f: String): Long =
        (h.ifEmpty { "0" }.toLong() * 3600 + m.toLong() * 60 + s.toLong()) * 1000 + f.padEnd(3, '0').take(3).toLong()

    fun parse(content: String): List<Cue> {
        val out = ArrayList<Cue>()
        val blocks = content.replace("\r", "").removePrefix("﻿").split(Regex("\n\\s*\n"))
        for (block in blocks) {
            val lines = block.lines().filter { it.isNotBlank() }
            val idx = lines.indexOfFirst { TIME.containsMatchIn(it) }
            if (idx < 0) continue
            val m = TIME.find(lines[idx]) ?: continue
            val g = m.groupValues
            val start = ms(g[1], g[2], g[3], g[4])
            val end = ms(g[5], g[6], g[7], g[8])
            val text = lines.drop(idx + 1).joinToString(" ") { it.replace(Regex("<[^>]*>"), "").trim() }.trim()
            if (text.isNotEmpty() && end > start) out += Cue(start, end, text)
        }
        return out
    }
}
