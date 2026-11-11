package com.dik.videoedit.screens

import com.dik.videoedit.i18n.tr
import androidx.compose.foundation.BorderStroke
import com.dik.videoedit.videoprocessing.LookLut
import com.dik.videoedit.models.Looks
import com.dik.videoedit.models.FilterPreset
import com.dik.videoedit.models.CaptionStyles
import com.dik.videoedit.models.AdjustKey
import com.dik.videoedit.models.Adjust
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import com.dik.videoedit.components.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dik.videoedit.models.AnimationType
import com.dik.videoedit.models.AspectOption
import com.dik.videoedit.models.AudioTrack
import com.dik.videoedit.models.EffectType
import com.dik.videoedit.models.FilterType
import com.dik.videoedit.models.Project
import com.dik.videoedit.models.SpeedRamp
import com.dik.videoedit.models.TextColors
import com.dik.videoedit.models.TextItem
import com.dik.videoedit.models.TextPosition
import com.dik.videoedit.models.TextSize
import com.dik.videoedit.models.TransitionType
import com.dik.videoedit.videoeditor.formatSpeed
import com.dik.videoedit.videoeditor.formatTimeTenth
import com.dik.videoedit.viewmodels.EditorViewModel

@Composable
private fun PanelTitle(text: String, hint: String? = null) {
    Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    if (hint != null) Text(hint, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun Label(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(6.dp))
}

/** Crop & rotasi: rasio 9:16 / 1:1 / 16:9 dan putar 90°. */
@Composable
internal fun CropPanel(vm: EditorViewModel, project: Project) {
    PanelTitle(tr("Crop & Rotasi"), tr("Video diisi penuh ke rasio yang dipilih (bagian tepi dipotong)."))
    Label(tr("Rasio"))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AspectOption.entries.forEach { a ->
            FilterChip(selected = project.aspect == a, onClick = { vm.setAspect(a) }, label = { Text(tr(a.label)) })
        }
    }
    Spacer(Modifier.height(14.dp))
    Label("${tr("Rotasi saat ini")}: ${project.rotation}°")
    OutlinedButton(onClick = { vm.rotate() }) {
        Icon(Icons.Filled.RotateRight, null, Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp)); Text(tr("Putar 90°"))
    }
}

private val SPEEDS = listOf(0.25f, 0.5f, 0.75f, 1f, 1.5f, 2f, 3f)

@Composable
internal fun SpeedPanel(vm: EditorViewModel, project: Project, selected: Int) {
    val clip = project.clips.getOrNull(selected) ?: return
    PanelTitle(tr("Kecepatan"), "${tr("Berlaku untuk klip")} ${selected + 1}. ${tr("Suara ikut menyesuaikan tanpa mengubah nada.")}")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SPEEDS.forEach { s ->
            FilterChip(selected = clip.speed == s, onClick = { vm.setSpeed(s, false) }, label = { Text(formatSpeed(s)) })
        }
    }
    TextButton(onClick = { vm.setSpeed(clip.speed, true) }) { Text("${tr("Terapkan")} ${formatSpeed(clip.speed)} ${tr("ke semua klip")}") }

    Spacer(Modifier.height(10.dp))
    Label(tr("Slow motion"))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = clip.speed == 0.5f, onClick = { vm.setSpeed(0.5f, false) }, label = { Text(tr("Slow 0,5x")) })
        FilterChip(selected = clip.speed == 0.25f, onClick = { vm.setSpeed(0.25f, false) }, label = { Text(tr("Super Slow 0,25x")) })
    }
    Spacer(Modifier.height(10.dp))
    Label(tr("Speed ramp"))
    Text(
        tr("Klip dibagi otomatis menjadi beberapa bagian dengan kecepatan berbeda (minimal 3 detik)."),
        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(6.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SpeedRamp.entries.forEach { r ->
            FilterChip(selected = false, onClick = { vm.speedRamp(r) }, label = { Text(tr(r.label)) })
        }
    }
}

/** Volume suara asli klip video yang dipilih. */
@Composable
internal fun ClipVolumePanel(vm: EditorViewModel, project: Project, selected: Int) {
    val clip = project.clips.getOrNull(selected) ?: return
    PanelTitle("${tr("Volume Klip")} ${selected + 1}", tr("Suara asli video. Atur ke 0% untuk membisukan."))
    VolumeSlider("Volume", clip.volume, key = clip.id) { vm.setClipVolume(it) }
    NoiseReductionSwitch(vm, project)
}

/** Volume klip audio yang dipilih. */
@Composable
internal fun AudioVolumePanel(vm: EditorViewModel, project: Project, track: AudioTrack) {
    PanelTitle(tr("Volume Audio"), track.name)
    VolumeSlider("Volume", track.volume, key = track.id) { vm.setAudioVolume(track.id, it) }
    NoiseReductionSwitch(vm, project)
}

/** Fade in dan fade out klip audio (maksimal separuh durasi klip, atau 5 detik). */
@Composable
internal fun AudioFadePanel(vm: EditorViewModel, track: AudioTrack) {
    val maxMs = minOf(5000L, track.durationMs / 2).coerceAtLeast(100L).toFloat()
    var fadeIn by remember(track.id, track.fadeInMs) { mutableFloatStateOf(track.fadeInMs.toFloat().coerceAtMost(maxMs)) }
    var fadeOut by remember(track.id, track.fadeOutMs) { mutableFloatStateOf(track.fadeOutMs.toFloat().coerceAtMost(maxMs)) }
    PanelTitle(tr("Fade Audio"), track.name)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(tr("Fade in"), fontSize = 14.sp)
        Text(formatTimeTenth(fadeIn.toLong()).drop(3) + " ${tr("dtk")}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
    Slider(
        value = fadeIn, onValueChange = { fadeIn = it }, valueRange = 0f..maxMs,
        onValueChangeFinished = { vm.setAudioFade(track.id, fadeIn.toLong(), fadeOut.toLong()) },
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(tr("Fade out"), fontSize = 14.sp)
        Text(formatTimeTenth(fadeOut.toLong()).drop(3) + " ${tr("dtk")}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
    Slider(
        value = fadeOut, onValueChange = { fadeOut = it }, valueRange = 0f..maxMs,
        onValueChangeFinished = { vm.setAudioFade(track.id, fadeIn.toLong(), fadeOut.toLong()) },
    )
}

/** Kurangi bising untuk semua audio; diterapkan pada hasil export. */
@Composable
private fun NoiseReductionSwitch(vm: EditorViewModel, project: Project) {
    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(tr("Kurangi bising"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                tr("Membuang dengung rendah dan desis pelan. Berlaku untuk semua audio dan terdengar pada hasil export."),
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = project.noiseReduction, onCheckedChange = { vm.setNoiseReduction(it) })
    }
}

@Composable
private fun VolumeSlider(label: String, value: Float, key: Any, onCommit: (Float) -> Unit) {
    var v by remember(key, value) { mutableFloatStateOf(value) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 14.sp)
        Text("${(v * 100).toInt()}%", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
    Slider(value = v, onValueChange = { v = it }, valueRange = 0f..1f, onValueChangeFinished = { onCommit(v) })
}

@Composable
internal fun TextPanel(vm: EditorViewModel, project: Project) {
    var text by remember { mutableStateOf("") }
    var position by remember { mutableStateOf(TextPosition.BOTTOM) }
    var size by remember { mutableStateOf(TextSize.MEDIUM) }
    var color by remember { mutableStateOf(TextColors.all.first()) }

    val textItems = project.texts.filter { !it.plain && !it.caption }
    PanelTitle(tr("Teks"), tr("Teks tampil sepanjang video."))
    OutlinedTextField(
        value = text, onValueChange = { text = it.take(30).replace("\n", " ") },
        label = { Text(tr("Isi teks (maks. 30 huruf)")) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(10.dp))
    Label(tr("Posisi"))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextPosition.entries.forEach { p -> FilterChip(position == p, { position = p }, { Text(tr(p.label)) }) }
    }
    Label(tr("Ukuran"))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextSize.entries.forEach { s -> FilterChip(size == s, { size = s }, { Text(tr(s.label)) }) }
    }
    Label(tr("Warna"))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TextColors.all.forEach { c ->
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(Color(c))
                    .border(
                        if (c == color) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        CircleShape,
                    )
                    .clickable { color = c },
            )
        }
    }
    Spacer(Modifier.height(12.dp))
    Button(
        onClick = {
            vm.addText(TextItem(text = text.trim(), position = position, color = color, size = size))
            text = ""
        },
        enabled = text.isNotBlank() && textItems.size < 5,
        modifier = Modifier.fillMaxWidth(),
    ) { Text(if (textItems.size < 5) "Tambah Teks" else "Maksimal 5 teks") }

    if (textItems.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        Label(tr("Teks di video"))
        textItems.forEach { t ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t.text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(tr(t.position.label), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(onClick = { vm.removeText(t.id) }) { Icon(Icons.Filled.Delete, "Hapus teks") }
            }
        }
    }
}

private val EMOJIS = listOf(
    "😀", "😂", "😍", "😎", "🥳", "😭", "🔥", "❤️",
    "👍", "👏", "🎉", "⭐", "✨", "💯", "🎵", "📍",
    "☀️", "🌈", "🌸", "🐶", "🐱", "🍕", "⚽", "🚗",
)

/** Stiker emoji dan overlay foto (ditumpuk di atas video, tampil sepanjang video). */
@Composable
internal fun StickerPanel(vm: EditorViewModel, project: Project, onPickImage: () -> Unit) {
    var emoji by remember { mutableStateOf(EMOJIS.first()) }
    var position by remember { mutableStateOf(TextPosition.CENTER) }
    var anchorX by remember { mutableFloatStateOf(0f) }
    var size by remember { mutableStateOf(TextSize.MEDIUM) }
    val items = project.texts.filter { it.plain }
    val full = project.texts.count { !it.caption } >= 12

    PanelTitle(tr("Stiker & Overlay"), tr("Tambah emoji atau foto dari galeri di atas video."))
    Label(tr("Pilih stiker"))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        EMOJIS.forEach { e ->
            Box(
                Modifier.size(44.dp).clip(CircleShape)
                    .background(if (e == emoji) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .clickable { emoji = e },
                contentAlignment = Alignment.Center,
            ) { Text(e, fontSize = 24.sp) }
        }
    }
    Spacer(Modifier.height(8.dp))
    Label(tr("Posisi"))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Kiri" to -0.75f, "Tengah" to 0f, "Kanan" to 0.75f).forEach { (name, x) ->
            FilterChip(anchorX == x, { anchorX = x }, { Text(name) })
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextPosition.entries.forEach { p -> FilterChip(position == p, { position = p }, { Text(tr(p.label)) }) }
    }
    Label(tr("Ukuran"))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextSize.entries.forEach { s -> FilterChip(size == s, { size = s }, { Text(tr(s.label)) }) }
    }
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = {
                vm.addText(TextItem(text = emoji, plain = true, position = position, anchorX = anchorX, size = size))
            },
            enabled = !full, modifier = Modifier.weight(1f),
        ) { Text(tr("Tambah Stiker")) }
        OutlinedButton(onClick = onPickImage, enabled = !full, modifier = Modifier.weight(1f)) { Text(tr("Overlay Foto")) }
    }
    if (full) Text(tr("Maksimal 12 teks, stiker, dan overlay."), fontSize = 12.sp, color = MaterialTheme.colorScheme.error)

    if (items.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        Label(tr("Stiker & overlay di video"))
        items.forEach { t ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (t.imagePath != null) "Foto" else t.text, modifier = Modifier.weight(1f), fontSize = 18.sp)
                Text(tr(t.position.label), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(onClick = { vm.removeText(t.id) }) { Icon(Icons.Filled.Delete, "Hapus") }
            }
        }
    }
}

/** Animasi otomatis sepanjang video (versi sederhana dari keyframe). */
@Composable
internal fun AnimationPanel(
    vm: EditorViewModel, project: Project, premiumActive: Boolean, premiumMinutes: Int,
    requirePremium: (() -> Unit) -> Unit,
) {
    PanelTitle(tr("Animasi"), tr("Gerakan zoom atau geser otomatis. Terlihat di preview dan hasil export."))
    PremiumNote(premiumActive, premiumMinutes)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AnimationType.entries.forEach { a ->
            FilterChip(
                selected = project.animation == a,
                onClick = { if (a.premium) requirePremium { vm.setAnimation(a) } else vm.setAnimation(a) },
                label = { Text(tr(a.label)) },
                leadingIcon = if (a.premium && !premiumActive) ({ LockIcon() }) else null,
            )
        }
    }
}

@Composable
private fun PremiumNote(active: Boolean, minutes: Int) {
    if (com.dik.videoedit.AppInfo.UNLOCK_ALL) return
    Text(
        if (active) "${tr("Premium aktif • sekitar")} $minutes ${tr("menit lagi")}" else "Item bergembok membutuhkan menonton 1 iklan (membuka semua fitur premium 30 menit).",
        fontSize = 12.sp,
        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun LockIcon() = Icon(Icons.Filled.Lock, null, Modifier.size(16.dp))

private val SAMPLE_COLORS = listOf(0xFF5B9BD5.toInt(), 0xFFE0AC8C.toInt(), 0xFF4E8F3A.toInt(), 0xFFB0B0B0.toInt())

/** Filter preset berkategori + penyesuaian warna manual (mirip panel Filter/Sesuaikan di CapCut). */
@Composable
internal fun FilterPanel(vm: EditorViewModel, project: Project) {
    var tab by remember { mutableIntStateOf(0) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Filter", "Sesuaikan").forEachIndexed { i, name ->
            FilterChip(selected = tab == i, onClick = { tab = i }, label = { Text(tr(name)) })
        }
    }
    Spacer(Modifier.height(10.dp))
    if (tab == 0) LookTab(vm, project) else AdjustTab(vm, project)
}

@Composable
private fun LookTab(vm: EditorViewModel, project: Project) {
    var category by remember { mutableStateOf(Looks.categories.first()) }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Looks.categories.forEach { c ->
            FilterChip(selected = category == c, onClick = { category = c }, label = { Text(tr(c)) })
        }
    }
    Spacer(Modifier.height(10.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        LookTile(tr("Asli"), null, project.look.isEmpty()) { vm.setLook("") }
        Looks.inCategory(category).forEach { preset ->
            LookTile(preset.label, preset, project.look == preset.id) { vm.setLook(preset.id) }
        }
    }
    if (project.look.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        var strength by remember(project.lookStrength) { mutableFloatStateOf(project.lookStrength) }
        Label("${tr("Kekuatan filter")}: ${(strength * 100).toInt()}")
        Slider(value = strength, onValueChange = { strength = it }, valueRange = 0f..1f, onValueChangeFinished = { vm.setLookStrength(strength) })
    }
}

@Composable
private fun LookTile(label: String, preset: FilterPreset?, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label, maxLines = 1) })
}

@Composable
private fun AdjustTab(vm: EditorViewModel, project: Project) {
    var key by remember { mutableStateOf(AdjustKey.BRIGHTNESS) }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AdjustKey.entries.forEach { k ->
            FilterChip(
                selected = key == k, onClick = { key = k },
                label = { Text(tr(k.label) + if (k.get(project.adjust) != 0f) " •" else "") },
            )
        }
    }
    Spacer(Modifier.height(10.dp))
    val current = key.get(project.adjust)
    var v by remember(key, current) { mutableFloatStateOf(current) }
    Label("${tr(key.label)}: ${(v * 100).toInt()}")
    Slider(value = v, onValueChange = { v = it }, valueRange = -1f..1f, onValueChangeFinished = { vm.setAdjust(key, v) })
    TextButton(onClick = { vm.resetAdjust() }, enabled = !project.adjust.isNeutral) { Text(tr("Reset semua")) }
}

@Composable
internal fun EffectPanel(
    vm: EditorViewModel, project: Project, premiumActive: Boolean, premiumMinutes: Int,
    requirePremium: (() -> Unit) -> Unit,
) {
    PanelTitle(tr("Efek & Transisi"))
    PremiumNote(premiumActive, premiumMinutes)
    Label(tr("Efek video"))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        EffectType.entries.forEach { e ->
            FilterChip(
                selected = project.effect == e,
                onClick = { if (e.premium) requirePremium { vm.setEffect(e) } else vm.setEffect(e) },
                label = { Text(tr(e.label)) },
                leadingIcon = if (e.premium && !premiumActive) ({ LockIcon() }) else null,
            )
        }
    }
    Spacer(Modifier.height(14.dp))
    Label(tr("Transisi antar klip") + if (project.clips.size < 2) " (butuh 2 klip atau lebih)" else "")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TransitionType.entries.forEach { t ->
            FilterChip(
                selected = project.transition == t,
                onClick = { if (t.premium) requirePremium { vm.setTransition(t) } else vm.setTransition(t) },
                label = { Text(tr(t.label)) },
                leadingIcon = if (t.premium && !premiumActive) ({ LockIcon() }) else null,
            )
        }
    }
    Text(
        tr("Transisi terlihat pada hasil export. Preview menampilkan potongan langsung."),
        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Keterangan (subtitle) yang tampil pada waktu tertentu, dengan template gaya dan impor .srt. */
@Composable
internal fun CaptionPanel(vm: EditorViewModel, project: Project, positionMs: () -> Long, onImport: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var durationMs by remember { mutableLongStateOf(3000L) }
    var styleId by remember { mutableStateOf(CaptionStyles.all.first().id) }
    val style = CaptionStyles.byId(styleId)
    val captions = project.texts.filter { it.caption }.sortedBy { it.startMs }

    PanelTitle(tr("Keterangan"), tr("Teks yang tampil pada waktu tertentu (subtitle). Geser garis putih ke posisi awal, lalu tambahkan."))
    Label(tr("Template"))
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CaptionStyles.all.forEach { st ->
            FilterChip(
                selected = styleId == st.id,
                onClick = { styleId = st.id; if (captions.isNotEmpty()) vm.setCaptionStyle(st) },
                label = { Text(tr(st.label)) },
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = text, onValueChange = { text = it.take(60).replace("\n", " ") },
        label = { Text(tr("Isi keterangan")) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    Label(tr("Lama tampil"))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(2000L, 3000L, 5000L, 8000L).map { it to "${it / 1000} ${tr("dtk")}" }.forEach { (ms, name) ->
            FilterChip(selected = durationMs == ms, onClick = { durationMs = ms }, label = { Text(name) })
        }
    }
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = { vm.addCaption(text.trim(), positionMs(), durationMs, style); text = "" },
        enabled = text.isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
    ) { Text(tr("Tambah di posisi sekarang")) }
    OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) { Text(tr("Impor keterangan (.srt / .vtt)")) }
    Text(
        tr("Keterangan otomatis dari suara belum tersedia karena butuh layanan online. Ketik manual, atau impor file .srt dari layanan lain."),
        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (captions.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        Label("${tr("Daftar keterangan")} (${captions.size})")
        captions.forEach { t ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatTimeTenth(t.startMs), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(56.dp))
                Text(t.text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                IconButton(onClick = { vm.removeText(t.id) }) { Icon(Icons.Filled.Delete, "Hapus keterangan") }
            }
        }
    }
}
