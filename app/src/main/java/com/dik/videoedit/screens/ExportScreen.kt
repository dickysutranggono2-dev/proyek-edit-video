package com.dik.videoedit.screens

import com.dik.videoedit.i18n.tr
import android.Manifest
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import com.dik.videoedit.components.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dik.videoedit.LocalAppContainer
import com.dik.videoedit.components.PremiumDialog
import com.dik.videoedit.components.PrimaryButton
import com.dik.videoedit.components.SecondaryButton
import com.dik.videoedit.components.rememberPremiumStatus
import com.dik.videoedit.ui.theme.Blue
import com.dik.videoedit.ui.theme.Success
import com.dik.videoedit.videoeditor.formatSize
import com.dik.videoedit.videoeditor.formatTime
import com.dik.videoedit.videoprocessing.ExportNotifier
import com.dik.videoedit.viewmodels.ExportViewModel
import com.dik.videoedit.viewmodels.ExportViewModel.Phase

@Composable
fun ExportScreen(projectId: String, onBack: () -> Unit, onFinish: () -> Unit) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val vm: ExportViewModel = viewModel(
        key = "export-$projectId",
        factory = viewModelFactory { initializer { ExportViewModel(container, projectId) } },
    )
    val s by vm.state.collectAsState()
    val settings by container.settings.settings.collectAsState(initial = com.dik.videoedit.storage.AppSettings())
    val premiumUntil by container.premium.until.collectAsState(initial = 0L)
    val (premiumActive, _) = rememberPremiumStatus(premiumUntil)

    var showPremium by remember { mutableStateOf(false) }
    var confirmCancel by remember { mutableStateOf(false) }

    // Izin: penyimpanan (Android 9-) lalu notifikasi (Android 13+), baru mulai export.
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.start() }
    val beginAfterStorage = {
        if (settings.notifications && Build.VERSION.SDK_INT >= 33 && !ExportNotifier.hasPermission(context)) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            vm.start()
        }
    }
    val storageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) beginAfterStorage()
        else Toast.makeText(context, tr("Izin penyimpanan diperlukan untuk menyimpan ke galeri."), Toast.LENGTH_LONG).show()
    }
    val begin = {
        if (Build.VERSION.SDK_INT < 29) storageLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        else beginAfterStorage()
    }

    val project = s.project
    val needsPremium = (project?.usesPremium == true) || s.removeWatermark

    BackHandler(enabled = s.phase is Phase.Running) { confirmCancel = true }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when {
            s.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            project == null -> Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(tr("Proyek tidak ditemukan."))
                Spacer(Modifier.height(12.dp))
                SecondaryButton("Kembali", onBack)
            }
            else -> when (val phase = s.phase) {
                Phase.Setup -> SetupView(vm, s, premiumActive, onBack, onStart = {
                    if (needsPremium && !premiumActive) showPremium = true else begin()
                })
                Phase.Running -> RunningView(s.progress) { confirmCancel = true }
                is Phase.Done -> DoneView(phase, onFinish)
                is Phase.Failed -> StatusView(
                    icon = { Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(56.dp)) },
                    title = "Export gagal", body = phase.message,
                    primary = "Coba Lagi" to { vm.backToSetup() },
                    secondary = "Kembali ke Editor" to onBack,
                )
            }
        }
    }

    if (showPremium) {
        PremiumDialog(
            onDismiss = { showPremium = false },
            onGranted = { showPremium = false; begin() },
        )
    }

    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text(tr("Batalkan export?")) },
            text = { Text(tr("Proses yang berjalan akan dihentikan.")) },
            confirmButton = { TextButton(onClick = { confirmCancel = false; vm.cancel() }) { Text(tr("Batalkan Export")) } },
            dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text(tr("Lanjutkan")) } },
        )
    }
}

@Composable
private fun SetupView(vm: ExportViewModel, s: ExportViewModel.State, premiumActive: Boolean, onBack: () -> Unit, onStart: () -> Unit) {
    val p = s.project ?: return
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali") }
            Text(tr("Export Video"), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))

        Text(tr("Resolusi"), fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(480, 720, 1080).forEach { r ->
                FilterChip(
                    selected = s.shortSide == r, onClick = { vm.setQuality(r) },
                    enabled = r in s.resolutions, label = { Text("${r}p") },
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        Text(tr("Frame rate (FPS)"), fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            s.fpsOptions.forEach { f ->
                FilterChip(
                    selected = s.fps == f, onClick = { vm.setFps(f) },
                    label = { Text(if (f == 0) "${tr("Asli")} (${s.sourceFps})" else "$f fps") },
                )
            }
        }
        Text(tr("Hanya opsi yang didukung perangkat dan tidak melebihi FPS video asli."), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))

        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(tr("Hapus watermark"), fontWeight = FontWeight.SemiBold)
                        if (!premiumActive) {
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Filled.Lock, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(if (com.dik.videoedit.AppInfo.UNLOCK_ALL) "Tulisan \"VideoEdit\" tidak akan muncul di pojok video." else "Fitur premium. Tanpa ini, tulisan \"VideoEdit\" muncul di pojok video.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = s.removeWatermark, onCheckedChange = vm::setRemoveWatermark)
            }
        }
        Spacer(Modifier.height(16.dp))

        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("${tr("Perkiraan ukuran file")}: ±${formatSize(s.estimateBytes)}", fontWeight = FontWeight.SemiBold, color = Blue)
                Text("${tr("Durasi")} ${formatTime(p.totalDurationMs)} • ${p.clips.size} ${tr("klip")}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (p.usesPremium && !com.dik.videoedit.AppInfo.UNLOCK_ALL) {
            Spacer(Modifier.height(10.dp))
            Text(
                tr("Proyek ini memakai filter/efek/transisi premium. Anda akan diminta menonton iklan bila akses premium belum aktif."),
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(24.dp))
        PrimaryButton("Mulai Export", onStart)
    }
}

@Composable
private fun RunningView(progress: Int, onCancel: () -> Unit) {
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.DisposableEffect(Unit) {
        view.keepScreenOn = true // jaga layar menyala selama export
        onDispose { view.keepScreenOn = false }
    }
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        Icon(Icons.Filled.Movie, null, tint = Blue, modifier = Modifier.size(96.dp))
        Spacer(Modifier.height(16.dp))
        Text(tr("Mengekspor Video..."), fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            tr("Mohon tunggu, proses ini mungkin memakan waktu sebentar.\nJangan tutup aplikasi selama export berjalan."),
            textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = { progress / 100f }, modifier = Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Text("$progress%", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.weight(1f))
        SecondaryButton("Batal", onCancel)
    }
}

@Composable
private fun DoneView(done: Phase.Done, onFinish: () -> Unit) {
    val context = LocalContext.current
    StatusView(
        icon = {
            Box(Modifier.size(110.dp).clip(CircleShape).background(Success), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Check, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(64.dp))
            }
        },
        title = "Berhasil!",
        body = "${tr("Video telah disimpan di galeri Anda")} (${formatSize(done.sizeBytes)}).",
        primary = "Buka Galeri" to {
            val view = Intent(Intent.ACTION_VIEW).setDataAndType(done.uri, "video/mp4").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            try { context.startActivity(view) } catch (e: Exception) {
                Toast.makeText(context, tr("Tidak ada aplikasi untuk membuka video."), Toast.LENGTH_SHORT).show()
            }
        },
        secondary = "Selesai" to onFinish,
        primaryIcon = Icons.Filled.Image,
    )
}

@Composable
private fun StatusView(
    icon: @Composable () -> Unit,
    title: String,
    body: String,
    primary: Pair<String, () -> Unit>,
    secondary: Pair<String, () -> Unit>,
    primaryIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        icon()
        Spacer(Modifier.height(20.dp))
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(body, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        PrimaryButton(primary.first, primary.second, icon = primaryIcon)
        Spacer(Modifier.height(10.dp))
        SecondaryButton(secondary.first, secondary.second)
    }
}
