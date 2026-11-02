package com.dik.videoedit.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dik.videoedit.AppInfo
import com.dik.videoedit.BuildConfig
import com.dik.videoedit.viewmodels.SettingsViewModel

private enum class Dlg { QUALITY, THEME, PRIVACY, TERMS }

private const val PRIVACY_TEXT = """VideoEdit memproses video Anda sepenuhnya di perangkat. Video, musik, dan proyek Anda tidak diunggah ke server kami.

Data yang disimpan di perangkat: daftar proyek (nama, pengaturan edit, dan alamat file sumber) serta pengaturan aplikasi. Anda dapat menghapusnya kapan saja dengan menghapus proyek atau data aplikasi.

Iklan: aplikasi menampilkan iklan reward melalui Google AdMob. AdMob dapat mengumpulkan data seperti ID iklan dan informasi perangkat sesuai kebijakan Google. Iklan hanya dimuat ketika Anda memilih membuka fitur premium.

Izin: akses internet (hanya untuk iklan), notifikasi (memberi tahu ketika export selesai), dan penyimpanan di Android 9 ke bawah (menyimpan hasil export ke galeri)."""

private const val TERMS_TEXT = """Dengan memakai VideoEdit, Anda setuju bahwa:

1. Anda bertanggung jawab atas konten yang Anda edit dan bagikan, termasuk hak cipta video dan musik yang Anda gunakan.
2. Fitur premium dibuka sementara (30 menit) setelah Anda menonton satu iklan sampai selesai.
3. Aplikasi disediakan apa adanya. Simpan salinan video asli Anda sebelum mengedit.
4. Hasil export disimpan di galeri pada folder Movies/VideoEdit."""

@Composable
fun SettingsScreen(vm: SettingsViewModel) {
    val s by vm.settings.collectAsState()
    val context = LocalContext.current
    var dialog by remember { mutableStateOf<Dlg?>(null) }

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.setNotifications(granted)
        if (!granted) Toast.makeText(context, "Izin notifikasi ditolak.", Toast.LENGTH_SHORT).show()
    }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Pengaturan", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        SectionTitle("Umum")
        SettingsCard {
            SettingRow(Icons.Filled.HighQuality, "Kualitas Video Default", "${s.defaultQuality}p") { dialog = Dlg.QUALITY }
            SettingRow(
                Icons.Filled.Palette, "Tema Aplikasi",
                when (s.theme) { "dark" -> "Gelap"; "system" -> "Sistem"; else -> "Terang" },
            ) { dialog = Dlg.THEME }
            SettingRow(Icons.Filled.Notifications, "Notifikasi", null, trailing = {
                Switch(checked = s.notifications, onCheckedChange = { on ->
                    if (on && Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else vm.setNotifications(on)
                })
            }) { }
        }
        Spacer(Modifier.height(20.dp))

        SectionTitle("Tentang")
        SettingsCard {
            SettingRow(Icons.Filled.Info, "Versi Aplikasi", "v${BuildConfig.VERSION_NAME}", chevron = false) { }
            SettingRow(Icons.Filled.PrivacyTip, "Kebijakan Privasi", null) { dialog = Dlg.PRIVACY }
            SettingRow(Icons.Filled.Description, "Syarat & Ketentuan", null) { dialog = Dlg.TERMS }
            SettingRow(Icons.Filled.Email, "Hubungi Kami", null) {
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(AppInfo.SUPPORT_EMAIL))
                    putExtra(Intent.EXTRA_SUBJECT, "VideoEdit v${BuildConfig.VERSION_NAME}")
                }
                try { context.startActivity(intent) } catch (e: Exception) {
                    Toast.makeText(context, "Tidak ada aplikasi email terpasang.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    when (dialog) {
        Dlg.QUALITY -> ChoiceDialog(
            "Kualitas Video Default", listOf(480, 720, 1080).map { "${it}p" to it },
            selected = s.defaultQuality, onSelect = { vm.setQuality(it); dialog = null }, onDismiss = { dialog = null },
        )
        Dlg.THEME -> ChoiceDialog(
            "Tema Aplikasi", listOf("Terang" to "light", "Gelap" to "dark", "Ikuti Sistem" to "system"),
            selected = s.theme, onSelect = { vm.setTheme(it); dialog = null }, onDismiss = { dialog = null },
        )
        Dlg.PRIVACY -> TextDialog("Kebijakan Privasi", PRIVACY_TEXT) { dialog = null }
        Dlg.TERMS -> TextDialog("Syarat & Ketentuan", TERMS_TEXT) { dialog = null }
        null -> Unit
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column { content() }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    value: String?,
    chevron: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(14.dp))
            Text(title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            if (value != null) Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (trailing != null) trailing()
            else if (chevron) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (label, value) ->
                    Row(
                        Modifier.fillMaxWidth().selectable(selected = value == selected, role = Role.RadioButton, onClick = { onSelect(value) }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = value == selected, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(label, modifier = Modifier.padding(vertical = 12.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } },
    )
}

@Composable
private fun TextDialog(title: String, body: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { Text(body, fontSize = 14.sp) } },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } },
    )
}
