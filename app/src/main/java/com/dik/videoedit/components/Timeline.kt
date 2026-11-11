package com.dik.videoedit.components

import com.dik.videoedit.i18n.tr
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dik.videoedit.models.AudioTrack
import com.dik.videoedit.models.Clip
import com.dik.videoedit.models.Focus
import com.dik.videoedit.models.Project
import com.dik.videoedit.ui.theme.PurpleTrack
import com.dik.videoedit.ui.theme.TealTrack
import com.dik.videoedit.videoeditor.ProjectOps
import com.dik.videoedit.videoeditor.formatTime
import com.dik.videoedit.videoeditor.formatTimeTenth
import com.dik.videoedit.videoprocessing.WaveformCache
import com.dik.videoedit.videoprocessing.WaveformExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Frame yang ditampilkan di preview selama handle trim sedang digeser. */
data class TrimPreview(val uri: String, val sourceMs: Long)

private class ClipDrag(val index: Int, val startMs: Long, val endMs: Long)
private class AudioDrag(val id: String, val startMs: Long, val trimStartMs: Long, val trimEndMs: Long)

/** Posisi & ukuran sebuah blok di timeline, dibaca oleh [TimelineLayout] lewat parent data. */
private class BlockData(val startMs: Long, val durationMs: Long, val row: Int) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any = this@BlockData
}

private fun Modifier.timelineBlock(startMs: Long, durationMs: Long, row: Int): Modifier =
    this.then(BlockData(startMs, durationMs, row))

private const val MAX_BLOCK_PX = 200_000
private const val MAX_TIMELINE_PX = 180_000f
private const val MIN_DP_PER_SEC = 8f
private const val MAX_DP_PER_SEC = 400f
private val RULER_STEPS = listOf(
    100L, 200L, 500L, 1000L, 2000L, 5000L, 10_000L, 15_000L, 30_000L,
    60_000L, 120_000L, 300_000L, 600_000L,
)

/**
 * Timeline gaya editor video modern:
 * - Playhead tetap di tengah; timeline bergeser saat diputar atau digeser dengan jari. Cubit untuk zoom.
 * - Track video di atas. Ketuk klip untuk memilih; dua handle putih di ujungnya bisa ditarik untuk trim.
 * - Track audio tepat di bawah video (beberapa baris bila tumpang tindih), dengan waveform. Geser untuk
 *   memindah, tarik handle putih untuk trim.
 *
 * Semua perubahan dilaporkan lewat callback dan diterapkan ke proyek (lalu ke preview dan export).
 */
@Composable
fun Timeline(
    project: Project,
    focus: Focus,
    selectedClip: Int,
    selectedAudio: String?,
    positionMs: () -> Long,
    onScrubStart: () -> Unit,
    onSeek: (Long) -> Unit,
    onScrubEnd: () -> Unit,
    onSelectClip: (Int) -> Unit,
    onSelectAudio: (String) -> Unit,
    onClearFocus: () -> Unit,
    onTrimClip: (index: Int, startMs: Long, endMs: Long, rightEdge: Boolean) -> Unit,
    onTrimPreview: (TrimPreview?) -> Unit,
    onMoveAudio: (id: String, startMs: Long) -> Unit,
    onTrimAudio: (id: String, startMs: Long, trimStartMs: Long, trimEndMs: Long) -> Unit,
    onAddClip: () -> Unit,
    onAddAudio: () -> Unit,
) {
    val density = LocalDensity.current
    val clips = project.clips
    val totalMs = project.totalDurationMs.coerceAtLeast(1L)
    val extentMs = project.timelineEndMs.coerceAtLeast(1L)

    // Zoom: dp per detik, dibatasi agar lebar total timeline tidak melewati batas ukuran layout Compose.
    var dpPerSec by remember { mutableFloatStateOf(60f) }
    val maxDpPerSec = remember(extentMs, density.density) {
        (MAX_TIMELINE_PX * 1000f / extentMs / density.density).coerceIn(MIN_DP_PER_SEC, MAX_DP_PER_SEC)
    }
    val zoom = dpPerSec.coerceIn(MIN_DP_PER_SEC, maxDpPerSec)
    val pxPerMs = zoom * density.density / 1000f

    val latestPx by rememberUpdatedState(pxPerMs)
    val latestMaxZoom by rememberUpdatedState(maxDpPerSec)
    val latestTotal by rememberUpdatedState(totalMs)
    val latestPosition by rememberUpdatedState(positionMs)
    val latestSeek by rememberUpdatedState(onSeek)
    val latestScrubStart by rememberUpdatedState(onScrubStart)
    val latestScrubEnd by rememberUpdatedState(onScrubEnd)
    val latestClear by rememberUpdatedState(onClearFocus)

    val starts = remember(clips) { ProjectOps.clipStarts(clips) }
    val lanes = remember(project.audio) { ProjectOps.assignLanes(project.audio) }
    val laneOf = lanes.first
    val laneCount = lanes.second

    var clipDrag by remember { mutableStateOf<ClipDrag?>(null) }
    var audioDrag by remember { mutableStateOf<AudioDrag?>(null) }

    val videoRowH = with(density) { 54.dp.roundToPx() }
    val audioRowH = with(density) { 40.dp.roundToPx() }
    val addRowH = with(density) { 30.dp.roundToPx() }
    val gapPx = with(density) { 5.dp.roundToPx() }
    // Baris 0 = video, 1..laneCount = audio, laneCount+1 = tombol "Tambah audio".
    val rowHeights = remember(laneCount, videoRowH, audioRowH, addRowH) {
        IntArray(laneCount + 2) { i ->
            when {
                i == 0 -> videoRowH
                i <= laneCount -> audioRowH
                else -> addRowH
            }
        }
    }

    // ---- logika drag handle ------------------------------------------------------------------

    fun dragClip(index: Int, left: Boolean, accPx: Float) {
        val clip = clips.getOrNull(index) ?: return
        val minLen = minOf(ProjectOps.MIN_CLIP_MS, clip.trimmedMs)
        val deltaSrc = (accPx / pxPerMs * clip.speed).toLong()
        val s: Long
        val e: Long
        if (left) {
            s = (clip.trimStartMs + deltaSrc).coerceIn(0L, clip.trimEndMs - minLen)
            e = clip.trimEndMs
        } else {
            s = clip.trimStartMs
            e = (clip.trimEndMs + deltaSrc).coerceIn(clip.trimStartMs + minLen, clip.sourceDurationMs)
        }
        clipDrag = ClipDrag(index, s, e)
        onTrimPreview(TrimPreview(clip.uri, if (left) s else (e - 1L).coerceAtLeast(0L)))
    }

    fun endClipDrag(index: Int, left: Boolean) {
        val d = clipDrag
        clipDrag = null
        onTrimPreview(null)
        latestScrubEnd()
        val clip = clips.getOrNull(index)
        if (d != null && clip != null && (d.startMs != clip.trimStartMs || d.endMs != clip.trimEndMs)) {
            onTrimClip(index, d.startMs, d.endMs, !left)
        }
    }

    fun dragAudioHandle(a: AudioTrack, left: Boolean, accPx: Float) {
        val deltaTimeline = accPx / pxPerMs
        val minLen = minOf(ProjectOps.MIN_AUDIO_MS, a.trimmedMs)
        if (left) {
            // Batas: posisi awal >= 0, awal potongan >= 0, dan klip tidak lebih pendek dari minLen.
            val lo = maxOf(-a.startMs.toFloat(), -a.trimStartMs / a.speed)
            val hi = (a.trimmedMs - minLen) / a.speed
            val dl = deltaTimeline.coerceIn(lo, max(lo, hi))
            audioDrag = AudioDrag(a.id, a.startMs + dl.toLong(), a.trimStartMs + (dl * a.speed).toLong(), a.trimEndMs)
        } else {
            val e = (a.trimEndMs + (deltaTimeline * a.speed).toLong())
                .coerceIn(a.trimStartMs + minLen, a.sourceDurationMs)
            audioDrag = AudioDrag(a.id, a.startMs, a.trimStartMs, e)
        }
    }

    fun endAudioDrag(a: AudioTrack) {
        val d = audioDrag
        audioDrag = null
        latestScrubEnd()
        if (d == null || d.id != a.id) return
        val trimChanged = d.trimStartMs != a.trimStartMs || d.trimEndMs != a.trimEndMs
        if (trimChanged) {
            onTrimAudio(a.id, d.startMs, d.trimStartMs, d.trimEndMs)
        } else if (d.startMs != a.startMs) {
            onMoveAudio(a.id, d.startMs)
        }
    }

    // ---- tampilan ---------------------------------------------------------------------------

    val playheadColor = MaterialTheme.colorScheme.primary
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(Unit) { detectTapGestures(onTap = { latestClear() }) }
            .pointerInput(Unit) {
                var posD = 0.0
                detectTimelineGestures(
                    onStart = {
                        latestScrubStart()
                        posD = latestPosition().toDouble()
                    },
                    onPanZoom = { panX, zoomChange ->
                        if (zoomChange != 1f) {
                            dpPerSec = (dpPerSec * zoomChange).coerceIn(MIN_DP_PER_SEC, latestMaxZoom)
                        }
                        if (panX != 0f) {
                            posD = (posD - panX / latestPx).coerceIn(0.0, latestTotal.toDouble())
                            latestSeek(posD.toLong())
                        }
                    },
                    onEnd = { latestScrubEnd() },
                )
            },
    ) {
        Column(Modifier.fillMaxWidth()) {
            TimeRuler(positionMs, pxPerMs, Modifier.fillMaxWidth().height(22.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 190.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 4.dp, bottom = 6.dp)
            ) {
                TimelineLayout(
                    positionMs = positionMs,
                    pxPerMs = pxPerMs,
                    rowHeightsPx = rowHeights,
                    rowGapPx = gapPx,
                    modifier = Modifier.fillMaxWidth().clipToBounds(),
                ) {
                    clips.forEachIndexed { i, clip ->
                        key(clip.id) {
                            val drag = clipDrag?.takeIf { it.index == i }
                            val s = drag?.startMs ?: clip.trimStartMs
                            val e = drag?.endMs ?: clip.trimEndMs
                            val visStart = starts[i] + ((s - clip.trimStartMs) / clip.speed).toLong()
                            val visDur = ((e - s) / clip.speed).toLong().coerceAtLeast(1L)
                            ClipBlock(
                                modifier = Modifier.timelineBlock(visStart, visDur, 0),
                                clip = clip,
                                startSrc = s,
                                endSrc = e,
                                widthDp = with(density) { (visDur * pxPerMs).toDp() },
                                selected = focus == Focus.CLIP && selectedClip == i,
                                onClick = { onSelectClip(i) },
                                onHandleStart = {
                                    latestScrubStart()
                                    clipDrag = ClipDrag(i, clip.trimStartMs, clip.trimEndMs)
                                },
                                onHandleDrag = { left, acc -> dragClip(i, left, acc) },
                                onHandleEnd = { left -> endClipDrag(i, left) },
                            )
                        }
                    }
                    project.audio.forEach { a ->
                        key(a.id) {
                            val drag = audioDrag?.takeIf { it.id == a.id }
                            val start = drag?.startMs ?: a.startMs
                            val ts = drag?.trimStartMs ?: a.trimStartMs
                            val te = drag?.trimEndMs ?: a.trimEndMs
                            val dur = ((te - ts) / a.speed).toLong().coerceAtLeast(1L)
                            AudioBlock(
                                modifier = Modifier.timelineBlock(start, dur, (laneOf[a.id] ?: 0) + 1),
                                track = a,
                                trimStart = ts,
                                trimEnd = te,
                                durationMs = dur,
                                selected = focus == Focus.AUDIO && selectedAudio == a.id,
                                onClick = { onSelectAudio(a.id) },
                                onMoveStart = {
                                    onSelectAudio(a.id)
                                    latestScrubStart()
                                    audioDrag = AudioDrag(a.id, a.startMs, a.trimStartMs, a.trimEndMs)
                                },
                                onMove = { acc ->
                                    audioDrag = AudioDrag(
                                        a.id,
                                        (a.startMs + (acc / pxPerMs).toLong()).coerceAtLeast(0L),
                                        a.trimStartMs, a.trimEndMs,
                                    )
                                },
                                onMoveEnd = { endAudioDrag(a) },
                                onHandleDrag = { left, acc ->
                                    if (audioDrag == null) {
                                        latestScrubStart()
                                        audioDrag = AudioDrag(a.id, a.startMs, a.trimStartMs, a.trimEndMs)
                                    }
                                    dragAudioHandle(a, left, acc)
                                },
                                onHandleEnd = { endAudioDrag(a) },
                            )
                        }
                    }
                    AddAudioBar(Modifier.timelineBlock(0L, totalMs, laneCount + 1), onAddAudio)
                }
            }
        }

        // Playhead (garis vertikal di tengah)
        Canvas(Modifier.matchParentSize()) {
            val x = size.width / 2f
            drawLine(Color.White, Offset(x, 0f), Offset(x, size.height), strokeWidth = 4.dp.toPx())
            drawLine(playheadColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 2.dp.toPx())
        }

        // Tombol tambah video, tetap di ujung kanan baris video.
        Surface(
            onClick = onAddClip,
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 26.dp, end = 6.dp).size(width = 38.dp, height = 54.dp),
        ) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Add, "Tambah video", tint = Color.White) }
        }
    }
}

// ---- gesture: geser (scrub) + cubit (zoom) ----------------------------------------------------

/**
 * Seperti detectTransformGestures, tetapi memberi tahu awal/akhir gesture dan hanya aktif bila anak
 * (handle trim, geser audio, scroll vertikal) tidak memakai gesturenya.
 */
private suspend fun PointerInputScope.detectTimelineGestures(
    onStart: () -> Unit,
    onPanZoom: (panX: Float, zoom: Float) -> Unit,
    onEnd: () -> Unit,
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var active = false
        var accPan = 0f
        var accZoom = 1f
        val slop = viewConfiguration.touchSlop
        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.any { it.isConsumed }
            if (!canceled) {
                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()
                if (!active) {
                    accPan += panChange.x
                    accZoom *= zoomChange
                    if (abs(accPan) > slop || abs(accZoom - 1f) > 0.04f) {
                        active = true
                        onStart()
                        onPanZoom(accPan, accZoom)
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                    }
                } else {
                    if (panChange.x != 0f || zoomChange != 1f) onPanZoom(panChange.x, zoomChange)
                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                }
            }
        } while (!canceled && event.changes.any { it.pressed })
        if (active) onEnd()
    }
}

// ---- layout: menempatkan blok berdasarkan waktu --------------------------------------------------

/**
 * Menempatkan setiap anak (yang memakai [timelineBlock]) pada x = tengah + (mulai - posisi) * pxPerMs dengan
 * lebar durasi * pxPerMs. Posisi playhead dibaca saat penempatan, jadi saat video diputar hanya posisi
 * yang dihitung ulang (tanpa recomposition).
 */
@Composable
private fun TimelineLayout(
    positionMs: () -> Long,
    pxPerMs: Float,
    rowHeightsPx: IntArray,
    rowGapPx: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val width = constraints.maxWidth
        val tops = IntArray(rowHeightsPx.size)
        var acc = 0
        for (i in rowHeightsPx.indices) {
            tops[i] = acc
            acc += rowHeightsPx[i] + rowGapPx
        }
        val totalH = (acc - rowGapPx).coerceAtLeast(0)
        val datas = measurables.map { it.parentData as? BlockData }
        val placeables = measurables.mapIndexed { i, m ->
            val d = datas[i]
            if (d == null) {
                m.measure(Constraints(0, width, 0, totalH))
            } else {
                val row = d.row.coerceIn(0, rowHeightsPx.lastIndex)
                val w = (d.durationMs * pxPerMs).roundToInt().coerceIn(2, MAX_BLOCK_PX)
                m.measure(Constraints.fixed(w, rowHeightsPx[row]))
            }
        }
        layout(width, totalH) {
            val center = width / 2
            val pos = positionMs()
            placeables.forEachIndexed { i, p ->
                val d = datas[i]
                if (d != null) {
                    val row = d.row.coerceIn(0, rowHeightsPx.lastIndex)
                    p.place(center + ((d.startMs - pos) * pxPerMs).roundToInt(), tops[row])
                }
            }
        }
    }
}

// ---- penggaris waktu -----------------------------------------------------------------------------

@Composable
private fun TimeRuler(positionMs: () -> Long, pxPerMs: Float, modifier: Modifier) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER } }
    Canvas(modifier) {
        val w = size.width
        val center = w / 2f
        val pos = positionMs()
        paint.color = color.toArgb()
        paint.textSize = 10.sp.toPx()
        val minGapPx = 56.dp.toPx()
        val stepMs = RULER_STEPS.firstOrNull { it * pxPerMs >= minGapPx } ?: RULER_STEPS.last()
        var t = max(0L, ((pos - center / pxPerMs) / stepMs).toLong() * stepMs - stepMs)
        val baseY = size.height - 4.dp.toPx()
        while (true) {
            val x = center + (t - pos) * pxPerMs
            if (x > w + 40f) break
            if (x > -40f) {
                val label = if (stepMs < 1000L) formatTimeTenth(t) else formatTime(t)
                drawContext.canvas.nativeCanvas.drawText(label, x, baseY - 6.dp.toPx(), paint)
                drawCircle(color, radius = 1.5.dp.toPx(), center = Offset(x, baseY))
                drawCircle(color.copy(alpha = 0.5f), radius = 1.dp.toPx(), center = Offset(x + stepMs * pxPerMs / 2f, baseY))
            }
            t += stepMs
        }
    }
}

// ---- blok klip video -----------------------------------------------------------------------------

private fun durationLabel(ms: Long): String =
    if (ms < 60_000L) String.format(Locale.US, "%.1fs", ms / 1000f) else formatTime(ms)

@Composable
private fun ClipBlock(
    modifier: Modifier,
    clip: Clip,
    startSrc: Long,
    endSrc: Long,
    widthDp: Dp,
    selected: Boolean,
    onClick: () -> Unit,
    onHandleStart: () -> Unit,
    onHandleDrag: (left: Boolean, accPx: Float) -> Unit,
    onHandleEnd: (left: Boolean) -> Unit,
) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        // Thumbnail: waktu dibulatkan ke 0,5 detik agar cache gambar tetap terpakai saat trim/zoom.
        val count = ceil(widthDp.value / 52f).toInt().coerceIn(1, 24)
        Row(Modifier.matchParentSize()) {
            repeat(count) { k ->
                val raw = startSrc + ((endSrc - startSrc) * (k + 0.5f) / count).toLong()
                VideoThumb(clip.uri, (raw / 500L) * 500L, Modifier.weight(1f).fillMaxHeight(), sizePx = 120)
            }
        }
        Text(
            durationLabel(((endSrc - startSrc) / clip.speed).toLong()),
            color = Color.White,
            fontSize = 10.sp,
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = if (selected) 18.dp else 4.dp, bottom = 2.dp)
                .background(Color(0x99000000), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp),
        )
        if (selected) {
            Box(Modifier.matchParentSize().border(2.dp, Color.White, shape))
            TrimHandle(
                left = true,
                onStart = onHandleStart,
                onDrag = { onHandleDrag(true, it) },
                onEnd = { onHandleEnd(true) },
            )
            TrimHandle(
                left = false,
                onStart = onHandleStart,
                onDrag = { onHandleDrag(false, it) },
                onEnd = { onHandleEnd(false) },
            )
        }
    }
}

/**
 * Handle putih di ujung klip. Area sentuh 28dp (lebih lebar dari gambarnya) agar mudah ditarik dengan jari.
 * [onDrag] menerima total geseran dalam piksel sejak awal tarikan.
 */
@Composable
private fun BoxScope.TrimHandle(
    left: Boolean,
    onStart: () -> Unit,
    onDrag: (accPx: Float) -> Unit,
    onEnd: () -> Unit,
) {
    val latestStart by rememberUpdatedState(onStart)
    val latestDrag by rememberUpdatedState(onDrag)
    val latestEnd by rememberUpdatedState(onEnd)
    val handleShape = if (left) RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp)
    else RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp)
    Box(
        Modifier
            .align(if (left) Alignment.CenterStart else Alignment.CenterEnd)
            .width(28.dp)
            .fillMaxHeight()
            .pointerInput(Unit) {
                var acc = 0f
                detectHorizontalDragGestures(
                    onDragStart = {
                        acc = 0f
                        latestStart()
                    },
                    onDragEnd = { latestEnd() },
                    onDragCancel = { latestEnd() },
                    onHorizontalDrag = { change, dx ->
                        change.consume()
                        acc += dx
                        latestDrag(acc)
                    },
                )
            },
        contentAlignment = if (left) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        Box(
            Modifier.width(14.dp).fillMaxHeight().background(Color.White, handleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.width(2.dp).height(18.dp).background(Color(0xFF9AA5B8), RoundedCornerShape(1.dp)))
        }
    }
}

// ---- blok klip audio -----------------------------------------------------------------------------

@Composable
private fun AudioBlock(
    modifier: Modifier,
    track: AudioTrack,
    trimStart: Long,
    trimEnd: Long,
    durationMs: Long,
    selected: Boolean,
    onClick: () -> Unit,
    onMoveStart: () -> Unit,
    onMove: (accPx: Float) -> Unit,
    onMoveEnd: () -> Unit,
    onHandleDrag: (left: Boolean, accPx: Float) -> Unit,
    onHandleEnd: () -> Unit,
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(6.dp)
    val base = if (track.extracted) TealTrack else PurpleTrack
    val peaks by produceState<FloatArray?>(initialValue = null, track.uri) {
        value = withContext(Dispatchers.IO) { WaveformCache.get(context, track.uri) { !isActive } }
    }
    val latestMoveStart by rememberUpdatedState(onMoveStart)
    val latestMove by rememberUpdatedState(onMove)
    val latestMoveEnd by rememberUpdatedState(onMoveEnd)

    Box(
        modifier
            .clip(shape)
            .background(base)
            .clickable(onClick = onClick)
            .pointerInput(Unit) {
                var acc = 0f
                detectHorizontalDragGestures(
                    onDragStart = {
                        acc = 0f
                        latestMoveStart()
                    },
                    onDragEnd = { latestMoveEnd() },
                    onDragCancel = { latestMoveEnd() },
                    onHorizontalDrag = { change, dx ->
                        change.consume()
                        acc += dx
                        latestMove(acc)
                    },
                )
            }
    ) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val data = peaks
            val barW = 2.dp.toPx()
            val n = min(4000, max(1, (w / 3.5.dp.toPx()).toInt()))
            val span = (trimEnd - trimStart).toFloat()
            val cy = h * 0.64f
            val maxH = h * 0.6f
            val barColor = Color.White.copy(alpha = 0.9f)
            for (k in 0 until n) {
                val m: Float = if (data == null || data.isEmpty()) {
                    0.18f
                } else {
                    val a = trimStart + span * k / n
                    val b = trimStart + span * (k + 1) / n
                    val i0 = (a / WaveformExtractor.BUCKET_MS).toInt().coerceIn(0, data.lastIndex)
                    val i1 = (b / WaveformExtractor.BUCKET_MS).toInt().coerceIn(i0, data.lastIndex)
                    var mx = 0f
                    for (i in i0..i1) if (data[i] > mx) mx = data[i]
                    mx
                }
                val bh = max(2.dp.toPx(), m * maxH)
                val x = (k + 0.5f) * (w / n)
                drawLine(barColor, Offset(x, cy - bh / 2f), Offset(x, cy + bh / 2f), strokeWidth = barW, cap = StrokeCap.Round)
            }
            // Bayangan segitiga menandai fade in / fade out.
            val dur = durationMs.coerceAtLeast(1L).toFloat()
            val fi = (track.fadeInMs / dur * w).coerceIn(0f, w)
            val fo = (track.fadeOutMs / dur * w).coerceIn(0f, w)
            if (fi > 1f) {
                drawPath(
                    Path().apply { moveTo(0f, 0f); lineTo(fi, 0f); lineTo(0f, h); close() },
                    Color.Black.copy(alpha = 0.25f),
                )
            }
            if (fo > 1f) {
                drawPath(
                    Path().apply { moveTo(w, 0f); lineTo(w - fo, 0f); lineTo(w, h); close() },
                    Color.Black.copy(alpha = 0.25f),
                )
            }
        }
        Row(
            Modifier.align(Alignment.TopStart).padding(start = if (selected) 18.dp else 5.dp, top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.MusicNote, null, tint = Color.White, modifier = Modifier.size(11.dp))
            Spacer(Modifier.width(2.dp))
            Text(track.name, color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (selected) {
            Box(Modifier.matchParentSize().border(2.dp, Color.White, shape))
            TrimHandle(left = true, onStart = {}, onDrag = { onHandleDrag(true, it) }, onEnd = onHandleEnd)
            TrimHandle(left = false, onStart = {}, onDrag = { onHandleDrag(false, it) }, onEnd = onHandleEnd)
        }
    }
}

@Composable
private fun AddAudioBar(modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Add, null, tint = tint, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(tr("Tambah audio"), fontSize = 11.sp, color = tint, maxLines = 1)
    }
}
