package com.dik.videoedit.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.dik.videoedit.LocalAppContainer
import com.dik.videoedit.components.PremiumDialog
import com.dik.videoedit.components.Timeline
import com.dik.videoedit.components.rememberPremiumStatus
import com.dik.videoedit.models.Project
import com.dik.videoedit.videoeditor.PreviewController
import com.dik.videoedit.videoeditor.formatTime
import com.dik.videoedit.videoprocessing.TextFit
import com.dik.videoedit.viewmodels.EditorViewModel
import kotlinx.coroutines.delay

enum class Tool { TRIM, CROP, SPEED, MUSIC, TEXT, FILTER, EFFECT }

@Composable
fun EditorScreen(projectId: String, onBack: () -> Unit, onExport: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: EditorViewModel = viewModel(
        key = "editor-$projectId",
        factory = viewModelFactory { initializer { EditorViewModel(container, projectId) } },
    )
    val ui by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm) { vm.messages.collect { snackbar.showSnackbar(it) } }

    val project = ui.project
    when {
        ui.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        project == null || project.clips.isEmpty() -> Column(
            Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Proyek tidak dapat dibuka. Video sumbernya mungkin sudah dihapus atau dipindahkan.")
            Spacer(Modifier.height(16.dp))
            Button(onClick = onBack) { Text("Kembali") }
        }
        else -> EditorContent(vm, ui, project, snackbar, onBack, onExport)
    }
}

@Composable
private fun EditorContent(
    vm: EditorViewModel,
    ui: EditorViewModel.State,
    project: Project,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onExport: () -> Unit,
) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val controller = remember { PreviewController(context) }
    DisposableEffect(controller) { onDispose { controller.release() } }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { controller.pause() }
    LaunchedEffect(project) { controller.update(project) }

    // Posisi putar (diperbarui berkala; ringan).
    var globalMs by remember { mutableLongStateOf(0L) }
    var playingIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    LaunchedEffect(controller) {
        while (true) {
            controller.syncClipParams()
            isPlaying = controller.player.isPlaying
            globalMs = controller.globalMs()
            val idx = controller.player.currentMediaItemIndex
            if (isPlaying && idx != playingIndex) vm.select(idx)
            playingIndex = idx
            delay(100)
        }
    }

    // Premium
    val premiumUntil by container.premium.until.collectAsState(initial = 0L)
    val (premiumActive, premiumMinutes) = rememberPremiumStatus(premiumUntil)
    var showPremium by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val requirePremium: (() -> Unit) -> Unit = { action ->
        if (premiumActive) action() else { pendingAction = action; showPremium = true }
    }

    var tool by remember { mutableStateOf<Tool?>(null) }

    val addVideo = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        if (uris.isNotEmpty()) vm.addClips(uris)
    }
    val pickAudio = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.setMusic(uri)
    }
    val launchAddVideo = { addVideo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) }
    val launchPickAudio = { pickAudio.launch(arrayOf("audio/*")) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            // Top bar
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
                    IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali") }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { controller.pause(); onExport() },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(40.dp),
                ) { Text("Export", fontWeight = FontWeight.SemiBold) }
            }

            // Preview
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)) {
                val ratio = project.outputRatio
                val parentRatio = maxWidth / maxHeight
                val sizeMod = if (ratio > parentRatio) Modifier.fillMaxWidth().aspectRatioCompat(ratio)
                else Modifier.fillMaxHeight().aspectRatioCompat(ratio, matchHeightFirst = true)
                Box(
                    Modifier.align(Alignment.Center).then(sizeMod).clip(RoundedCornerShape(14.dp)).background(Color.Black)
                        .clickable { controller.togglePlay() },
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                setShutterBackgroundColor(android.graphics.Color.BLACK)
                                player = controller.player
                            }
                        },
                        modifier = Modifier.matchParentSize(),
                    )
                    TextOverlays(project)
                    if (!isPlaying) {
                        Box(
                            Modifier.align(Alignment.Center).size(64.dp).clip(CircleShape).background(Color(0x99000000)),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Filled.PlayArrow, "Putar", tint = Color.White, modifier = Modifier.size(38.dp)) }
                    }
                }
            }

            // Waktu + kontrol
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f)) {
                    Text(formatTime(globalMs), fontWeight = FontWeight.Bold)
                    Text(" / ${formatTime(project.totalDurationMs)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { controller.togglePlay() }) {
                    Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (isPlaying) "Jeda" else "Putar", modifier = Modifier.size(30.dp))
                }
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = vm::undo, enabled = ui.canUndo) { Icon(Icons.AutoMirrored.Filled.Undo, "Urungkan") }
                    IconButton(onClick = vm::redo, enabled = ui.canRedo) { Icon(Icons.AutoMirrored.Filled.Redo, "Ulangi") }
                }
            }

            // Timeline
            Timeline(
                project = project, selected = ui.selected, globalMs = globalMs,
                onSelect = vm::select,
                onSeek = { controller.seekToGlobal(it) },
                onAddClip = { launchAddVideo() },
                onAddMusic = { tool = Tool.MUSIC },
            )
            Spacer(Modifier.height(10.dp))

            // Toolbar
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToolTile("Potong", Icons.Filled.ContentCut, Modifier.weight(1f)) { tool = Tool.TRIM }
                    ToolTile("Pisah", Icons.Filled.VerticalSplit, Modifier.weight(1f)) {
                        val idx = controller.player.currentMediaItemIndex
                        val clip = project.clips.getOrNull(idx)
                        if (clip != null) vm.split(idx, clip.trimStartMs + controller.player.currentPosition)
                    }
                    ToolTile("Crop", Icons.Filled.Crop, Modifier.weight(1f)) { tool = Tool.CROP }
                    ToolTile("Kecepatan", Icons.Filled.Speed, Modifier.weight(1f)) { tool = Tool.SPEED }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToolTile("Musik", Icons.Filled.MusicNote, Modifier.weight(1f)) { tool = Tool.MUSIC }
                    ToolTile("Teks", Icons.Filled.TextFields, Modifier.weight(1f)) { tool = Tool.TEXT }
                    ToolTile("Filter", Icons.Filled.FilterVintage, Modifier.weight(1f)) { tool = Tool.FILTER }
                    ToolTile("Efek", Icons.Filled.AutoAwesome, Modifier.weight(1f)) { tool = Tool.EFFECT }
                }
            }
        }
    }

    // Panel alat (bottom sheet)
    tool?.let { t ->
        ModalBottomSheet(
            onDismissRequest = { tool = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.fillMaxWidth().imePadding().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
                when (t) {
                    Tool.TRIM -> TrimPanel(vm, project, ui.selected)
                    Tool.CROP -> CropPanel(vm, project)
                    Tool.SPEED -> SpeedPanel(vm, project, ui.selected)
                    Tool.MUSIC -> MusicPanel(vm, project, ui.selected, onPickAudio = { launchPickAudio() })
                    Tool.TEXT -> TextPanel(vm, project)
                    Tool.FILTER -> FilterPanel(vm, project, premiumActive, premiumMinutes, requirePremium)
                    Tool.EFFECT -> EffectPanel(vm, project, premiumActive, premiumMinutes, requirePremium)
                }
            }
        }
    }

    if (ui.busy) {
        Box(Modifier.fillMaxSize().background(Color(0x66000000)), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    }

    if (showPremium) {
        PremiumDialog(
            onDismiss = { showPremium = false; pendingAction = null },
            onGranted = {
                showPremium = false
                pendingAction?.invoke()
                pendingAction = null
            },
        )
    }
}

private fun Modifier.aspectRatioCompat(ratio: Float, matchHeightFirst: Boolean = false): Modifier =
    this.then(Modifier.aspectRatio(ratio, matchHeightFirst))

@Composable
private fun ToolTile(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick, modifier = modifier.height(64.dp), shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp,
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Teks di preview digambar dengan Compose; ukuran & posisinya mengikuti rumus yang sama dengan export. */
@Composable
private fun TextOverlays(project: Project) {
    if (project.texts.isEmpty()) return
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        val shortSide = minOf(wPx, hPx)
        project.texts.forEach { t ->
            val px = TextFit.fitPx(t.text, shortSide * t.size.fraction, wPx * 0.92f)
            Text(
                t.text,
                color = Color(t.color),
                fontSize = with(density) { px.toSp() },
                maxLines = 1, softWrap = false,
                modifier = Modifier.align(BiasAlignment(0f, -t.position.anchorY))
                    .background(Color(t.backgroundColor))
                    .padding(horizontal = 4.dp),
            )
        }
    }
}
