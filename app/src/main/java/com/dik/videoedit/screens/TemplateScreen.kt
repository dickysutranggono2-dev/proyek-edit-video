package com.dik.videoedit.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop169
import androidx.compose.material.icons.filled.CropPortrait
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dik.videoedit.models.TemplatePreset
import com.dik.videoedit.models.Templates
import com.dik.videoedit.viewmodels.HomeViewModel

private fun iconFor(id: String): ImageVector = when (id) {
    "reels" -> Icons.Filled.CropPortrait
    "square" -> Icons.Filled.CropSquare
    "wide" -> Icons.Filled.Crop169
    "slowmo" -> Icons.Filled.SlowMotionVideo
    "timelapse" -> Icons.Filled.Speed
    "bright" -> Icons.Filled.FilterVintage
    else -> Icons.Filled.Movie
}

/** Template = preset awal (rasio, kecepatan, filter). Pilih template, pilih video, lalu langsung edit. */
@Composable
fun TemplateScreen(vm: HomeViewModel, onOpenProject: (String) -> Unit) {
    var pending by remember { mutableStateOf<TemplatePreset?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val t = pending
        pending = null
        vm.createProject(uris, t, onOpenProject)
    }

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text("Template", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Pilih gaya awal, lalu pilih video dari galeri. Semua pengaturan masih bisa diubah di editor.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.padding(top = 8.dp))
            }
        }
        items(Templates.all, key = { it.id }) { t ->
            Surface(
                onClick = {
                    pending = t
                    picker.launch(arrayOf("video/*"))
                },
                shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) { Icon(iconFor(t.id), null, tint = MaterialTheme.colorScheme.primary) }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(t.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text(t.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
