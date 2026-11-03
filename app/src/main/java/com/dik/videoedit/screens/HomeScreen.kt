package com.dik.videoedit.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.dik.videoedit.components.AppLogo
import com.dik.videoedit.components.PrimaryButton
import com.dik.videoedit.components.VideoThumb
import com.dik.videoedit.models.Project
import com.dik.videoedit.videoeditor.formatTime
import com.dik.videoedit.viewmodels.HomeViewModel

@Composable
fun HomeScreen(vm: HomeViewModel, onOpenProject: (String) -> Unit) {
    val projects by vm.projects.collectAsState()
    val busy by vm.busy.collectAsState()
    var showAll by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Project?>(null) }
    var deleting by remember { mutableStateOf<Project?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        vm.createProject(uris, null, onOpenProject)
    }

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(16.dp))
                AppLogo(92.dp)
                Spacer(Modifier.height(10.dp))
                Text("VideoEdit", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    "Edit video dengan mudah\ndan cepat!", textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(22.dp))
                PrimaryButton(
                    "Proyek Baru", icon = Icons.Filled.Add,
                    onClick = { picker.launch(arrayOf("video/*")) },
                )
                Spacer(Modifier.height(22.dp))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Proyek Terakhir", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (projects.size > 3) {
                    Row(Modifier.clickable { showAll = !showAll }, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (showAll) "Ringkas" else "Lihat Semua",
                            color = MaterialTheme.colorScheme.primary, fontSize = 14.sp,
                        )
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        if (projects.isEmpty()) {
            item {
                Text(
                    "Belum ada proyek. Tekan \"Proyek Baru\" untuk memilih video dari galeri.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }
        items(if (showAll) projects else projects.take(3), key = { it.id }) { p ->
            ProjectRow(p, onClick = { onOpenProject(p.id) }, onRename = { renaming = p }, onDelete = { deleting = p })
        }
    }

    if (busy) {
        Dialog(onDismissRequest = {}) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                    Spacer(Modifier.width(16.dp))
                    Text("Membaca video…")
                }
            }
        }
    }

    renaming?.let { p ->
        var name by remember(p.id) { mutableStateOf(p.name) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("Ganti Nama Proyek") },
            text = {
                OutlinedTextField(
                    value = name, onValueChange = { name = it.take(60) }, singleLine = true,
                    keyboardOptions = KeyboardOptions.Default,
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.rename(p, name); renaming = null }, enabled = name.isNotBlank()) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("Batal") } },
        )
    }

    deleting?.let { p ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Hapus proyek?") },
            text = { Text("\"${p.name}\" akan dihapus dari aplikasi. Video asli di galeri tidak ikut terhapus.") },
            confirmButton = { TextButton(onClick = { vm.delete(p.id); deleting = null }) { Text("Hapus") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Batal") } },
        )
    }
}

@Composable
private fun ProjectRow(p: Project, onClick: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            val first = p.clips.firstOrNull()
            if (first != null) {
                VideoThumb(first.uri, first.trimStartMs, Modifier.size(width = 72.dp, height = 54.dp).clip(RoundedCornerShape(10.dp)))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(p.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${formatTime(p.totalDurationMs)} • ${p.clips.size} klip",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Menu proyek") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Ganti Nama") }, onClick = { menu = false; onRename() })
                    DropdownMenuItem(text = { Text("Hapus") }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}
