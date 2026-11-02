package com.dik.videoedit.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
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
import com.dik.videoedit.models.AspectOption
import com.dik.videoedit.models.EffectType
import com.dik.videoedit.models.FilterType
import com.dik.videoedit.models.Project
import com.dik.videoedit.models.TextColors
import com.dik.videoedit.models.TextItem
import com.dik.videoedit.models.TextPosition
import com.dik.videoedit.models.TextSize
import com.dik.videoedit.models.TransitionType
import com.dik.videoedit.videoeditor.ProjectOps
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

/** Potong: atur titik awal/akhir klip terpilih, urutan klip, dan hapus klip. */
@Composable
internal fun TrimPanel(vm: EditorViewModel, project: Project, selected: Int) {
    val clip = project.clips.getOrNull(selected) ?: return
    var range by remember(clip.id, clip.trimStartMs, clip.trimEndMs) {
        mutableStateOf(clip.trimStartMs.toFloat()..clip.trimEndMs.toFloat())
    }
    PanelTitle("Potong Klip ${selected + 1} dari ${project.clips.size}", clip.name)
    RangeSlider(
        value = range,
        onValueChange = { if (it.endInclusive - it.start >= ProjectOps.MIN_CLIP_MS) range = it },
        valueRange = 0f..clip.sourceDurationMs.toFloat(),
        onValueChangeFinished = { vm.trim(range.start.toLong(), range.endInclusive.toLong()) },
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Mulai ${formatTimeTenth(range.start.toLong())}", fontSize = 13.sp)
        Text("Durasi ${formatTimeTenth((range.endInclusive - range.start).toLong())}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text("Selesai ${formatTimeTenth(range.endInclusive.toLong())}", fontSize = 13.sp)
    }
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { vm.moveSelected(-1) }, enabled = selected > 0) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp)); Text("Geser")
        }
        OutlinedButton(onClick = { vm.moveSelected(1) }, enabled = selected < project.clips.lastIndex) {
            Text("Geser"); Spacer(Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp))
        }
        OutlinedButton(onClick = { vm.deleteSelected() }, enabled = project.clips.size > 1) {
            Icon(Icons.Filled.Delete, null, Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp)); Text("Hapus")
        }
    }
}

/** Crop & rotasi: rasio 9:16 / 1:1 / 16:9 dan putar 90°. */
@Composable
internal fun CropPanel(vm: EditorViewModel, project: Project) {
    PanelTitle("Crop & Rotasi", "Video diisi penuh ke rasio yang dipilih (bagian tepi dipotong).")
    Label("Rasio")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AspectOption.entries.forEach { a ->
            FilterChip(selected = project.aspect == a, onClick = { vm.setAspect(a) }, label = { Text(a.label) })
        }
    }
    Spacer(Modifier.height(14.dp))
    Label("Rotasi saat ini: ${project.rotation}°")
    OutlinedButton(onClick = { vm.rotate() }) {
        Icon(Icons.Filled.RotateRight, null, Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp)); Text("Putar 90°")
    }
}

private val SPEEDS = listOf(0.25f, 0.5f, 0.75f, 1f, 1.5f, 2f, 3f)

@Composable
internal fun SpeedPanel(vm: EditorViewModel, project: Project, selected: Int) {
    val clip = project.clips.getOrNull(selected) ?: return
    PanelTitle("Kecepatan", "Berlaku untuk klip ${selected + 1}. Suara ikut menyesuaikan tanpa mengubah nada.")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SPEEDS.forEach { s ->
            FilterChip(selected = clip.speed == s, onClick = { vm.setSpeed(s, false) }, label = { Text(formatSpeed(s)) })
        }
    }
    TextButton(onClick = { vm.setSpeed(clip.speed, true) }) { Text("Terapkan ${formatSpeed(clip.speed)} ke semua klip") }
}

@Composable
internal fun MusicPanel(vm: EditorViewModel, project: Project, selected: Int, onPickAudio: () -> Unit) {
    val clip = project.clips.getOrNull(selected)
    PanelTitle("Musik & Volume")
    val music = project.music
    if (music == null) {
        Button(onClick = onPickAudio) {
            Icon(Icons.Filled.MusicNote, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Pilih Musik")
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.MusicNote, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(music.name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            TextButton(onClick = onPickAudio) { Text("Ganti") }
            IconButton(onClick = { vm.removeMusic() }) { Icon(Icons.Filled.Delete, "Hapus musik") }
        }
        VolumeSlider("Volume musik", music.volume, key = music.uri) { vm.setMusicVolume(it) }
    }
    if (clip != null) {
        Spacer(Modifier.height(8.dp))
        VolumeSlider("Volume suara video (klip ${selected + 1})", clip.volume, key = clip.id) { vm.setClipVolume(it) }
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

    PanelTitle("Teks", "Teks tampil sepanjang video.")
    OutlinedTextField(
        value = text, onValueChange = { text = it.take(30).replace("\n", " ") },
        label = { Text("Isi teks (maks. 30 huruf)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(10.dp))
    Label("Posisi")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextPosition.entries.forEach { p -> FilterChip(position == p, { position = p }, { Text(p.label) }) }
    }
    Label("Ukuran")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextSize.entries.forEach { s -> FilterChip(size == s, { size = s }, { Text(s.label) }) }
    }
    Label("Warna")
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
        enabled = text.isNotBlank() && project.texts.size < 5,
        modifier = Modifier.fillMaxWidth(),
    ) { Text(if (project.texts.size < 5) "Tambah Teks" else "Maksimal 5 teks") }

    if (project.texts.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        Label("Teks di video")
        project.texts.forEach { t ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t.text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(t.position.label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(onClick = { vm.removeText(t.id) }) { Icon(Icons.Filled.Delete, "Hapus teks") }
            }
        }
    }
}

@Composable
private fun PremiumNote(active: Boolean, minutes: Int) {
    Text(
        if (active) "Premium aktif • sekitar $minutes menit lagi" else "Item bergembok membutuhkan menonton 1 iklan (membuka semua fitur premium 30 menit).",
        fontSize = 12.sp,
        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun LockIcon() = Icon(Icons.Filled.Lock, null, Modifier.size(16.dp))

@Composable
internal fun FilterPanel(
    vm: EditorViewModel, project: Project, premiumActive: Boolean, premiumMinutes: Int,
    requirePremium: (() -> Unit) -> Unit,
) {
    PanelTitle("Filter Warna")
    PremiumNote(premiumActive, premiumMinutes)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterType.entries.forEach { f ->
            FilterChip(
                selected = project.filter == f,
                onClick = { if (f.premium) requirePremium { vm.setFilter(f) } else vm.setFilter(f) },
                label = { Text(f.label) },
                leadingIcon = if (f.premium && !premiumActive) ({ LockIcon() }) else null,
            )
        }
    }
}

@Composable
internal fun EffectPanel(
    vm: EditorViewModel, project: Project, premiumActive: Boolean, premiumMinutes: Int,
    requirePremium: (() -> Unit) -> Unit,
) {
    PanelTitle("Efek & Transisi")
    PremiumNote(premiumActive, premiumMinutes)
    Label("Efek video")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        EffectType.entries.forEach { e ->
            FilterChip(
                selected = project.effect == e,
                onClick = { if (e.premium) requirePremium { vm.setEffect(e) } else vm.setEffect(e) },
                label = { Text(e.label) },
                leadingIcon = if (e.premium && !premiumActive) ({ LockIcon() }) else null,
            )
        }
    }
    Spacer(Modifier.height(14.dp))
    Label("Transisi antar klip" + if (project.clips.size < 2) " (butuh 2 klip atau lebih)" else "")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TransitionType.entries.forEach { t ->
            FilterChip(
                selected = project.transition == t,
                onClick = { if (t.premium) requirePremium { vm.setTransition(t) } else vm.setTransition(t) },
                label = { Text(t.label) },
                leadingIcon = if (t.premium && !premiumActive) ({ LockIcon() }) else null,
            )
        }
    }
    Text(
        "Transisi terlihat pada hasil export. Preview menampilkan potongan langsung.",
        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
