package com.dik.videoedit.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dik.videoedit.models.Clip
import com.dik.videoedit.models.Project
import com.dik.videoedit.ui.theme.Blue
import com.dik.videoedit.ui.theme.PurpleTrack

/**
 * Timeline: klip ditampilkan berdampingan (lebar ~ durasi), playhead biru mengikuti posisi putar.
 * Ketuk/geser pada klip untuk memilih sekaligus memindahkan posisi putar.
 */
@Composable
fun Timeline(
    project: Project,
    selected: Int,
    globalMs: Long,
    onSelect: (Int) -> Unit,
    onSeek: (Long) -> Unit,
    onAddClip: () -> Unit,
    onAddMusic: () -> Unit,
) {
    val clips = project.clips
    val total = project.totalDurationMs.coerceAtLeast(1L)
    // Bobot minimal agar klip yang sangat pendek tetap bisa diketuk.
    val weights = remember(clips) { clips.map { maxOf(it.effectiveMs.toFloat(), total * 0.07f) } }
    val weightSum = weights.sum()
    val starts = remember(clips) {
        var acc = 0L
        clips.map { val s = acc; acc += it.effectiveMs; s }
    }

    fun locate(frac: Float): Pair<Int, Long> {
        var target = frac.coerceIn(0f, 1f) * weightSum
        for (i in clips.indices) {
            if (target <= weights[i] || i == clips.lastIndex) {
                val f = (target / weights[i]).coerceIn(0f, 1f)
                return i to (starts[i] + (f * clips[i].effectiveMs).toLong())
            }
            target -= weights[i]
        }
        return 0 to 0L
    }

    fun fractionOf(ms: Long): Float {
        for (i in clips.indices) {
            val end = starts[i] + clips[i].effectiveMs
            if (ms <= end || i == clips.lastIndex) {
                val eff = clips[i].effectiveMs
                val f = if (eff > 0) ((ms - starts[i]).toFloat() / eff).coerceIn(0f, 1f) else 0f
                return (weights.take(i).sum() + f * weights[i]) / weightSum
            }
        }
        return 0f
    }

    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.weight(1f)) {
            Box(Modifier.fillMaxWidth()) {
                BoxWithConstraints(Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(10.dp))) {
                    val trackWidth = maxWidth
                    Row(
                        Modifier.fillMaxSize()
                            .pointerInput(clips, weights) {
                                detectTapGestures { o ->
                                    val (i, t) = locate(o.x / size.width)
                                    onSelect(i); onSeek(t)
                                }
                            }
                            .pointerInput(clips, weights) {
                                detectHorizontalDragGestures(
                                    onDragStart = { o ->
                                        val (i, t) = locate(o.x / size.width)
                                        onSelect(i); onSeek(t)
                                    },
                                    onHorizontalDrag = { change, _ ->
                                        val (i, t) = locate(change.position.x / size.width)
                                        onSelect(i); onSeek(t)
                                        change.consume()
                                    },
                                )
                            },
                    ) {
                        clips.forEachIndexed { i, c ->
                            ClipStrip(c, i == selected, Modifier.weight(weights[i]).fillMaxHeight())
                        }
                    }
                    // Playhead
                    Box(
                        Modifier.offset(x = trackWidth * fractionOf(globalMs) - 1.dp)
                            .width(2.dp).fillMaxHeight().background(Blue)
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            onClick = onAddClip, shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(60.dp),
        ) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Add, "Tambah video", tint = Color.White) }
        }
    }
    Spacer(Modifier.height(6.dp))
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        val music = project.music
        if (music != null) {
            val frac = (music.durationMs.toFloat() / total).coerceIn(0.12f, 1f)
            Row(
                Modifier.fillMaxWidth(frac).height(30.dp).clip(RoundedCornerShape(8.dp))
                    .background(PurpleTrack).clickable(onClick = onAddMusic).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.MusicNote, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(music.name, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        } else {
            Row(
                Modifier.fillMaxWidth().height(30.dp).clip(RoundedCornerShape(8.dp))
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(8.dp))
                    .clickable(onClick = onAddMusic).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.MusicNote, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Tambahkan musik", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ClipStrip(clip: Clip, selected: Boolean, modifier: Modifier) {
    BoxWithConstraints(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .border(
                if (selected) 2.dp else 0.5.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(6.dp),
            )
    ) {
        val count = (maxWidth / 48.dp).toInt().coerceIn(1, 6)
        Row(Modifier.fillMaxSize()) {
            repeat(count) { k ->
                val t = clip.trimStartMs + (clip.trimmedMs * (k + 0.5f) / count).toLong()
                VideoThumb(clip.uri, t, Modifier.weight(1f).fillMaxHeight(), sizePx = 120)
            }
        }
    }
}
