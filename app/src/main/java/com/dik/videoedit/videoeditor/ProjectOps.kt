package com.dik.videoedit.videoeditor

import com.dik.videoedit.models.Clip
import com.dik.videoedit.models.Project
import java.util.UUID

/** Operasi murni pada proyek (tanpa efek samping), mudah diuji. */
object ProjectOps {
    const val MIN_CLIP_MS = 500L
    private const val MIN_SPLIT_EDGE_MS = 300L

    fun updateClip(p: Project, index: Int, change: (Clip) -> Clip): Project =
        p.copy(clips = p.clips.mapIndexed { i, c -> if (i == index) change(c) else c })

    fun trim(p: Project, index: Int, startMs: Long, endMs: Long): Project = updateClip(p, index) {
        val s = startMs.coerceIn(0L, it.sourceDurationMs - MIN_CLIP_MS)
        val e = endMs.coerceIn(s + MIN_CLIP_MS, it.sourceDurationMs)
        it.copy(trimStartMs = s, trimEndMs = e)
    }

    /** Membagi klip pada posisi sumber [atSourceMs]; null bila terlalu dekat ke tepi klip. */
    fun split(p: Project, index: Int, atSourceMs: Long): Project? {
        val clip = p.clips.getOrNull(index) ?: return null
        if (atSourceMs < clip.trimStartMs + MIN_SPLIT_EDGE_MS || atSourceMs > clip.trimEndMs - MIN_SPLIT_EDGE_MS) return null
        val first = clip.copy(id = UUID.randomUUID().toString(), trimEndMs = atSourceMs)
        val second = clip.copy(id = UUID.randomUUID().toString(), trimStartMs = atSourceMs)
        val list = p.clips.toMutableList()
        list[index] = first
        list.add(index + 1, second)
        return p.copy(clips = list)
    }

    fun delete(p: Project, index: Int): Project =
        if (p.clips.size <= 1) p else p.copy(clips = p.clips.filterIndexed { i, _ -> i != index })

    fun move(p: Project, index: Int, delta: Int): Project {
        val target = index + delta
        if (index !in p.clips.indices || target !in p.clips.indices) return p
        val list = p.clips.toMutableList()
        val item = list.removeAt(index)
        list.add(target, item)
        return p.copy(clips = list)
    }
}
