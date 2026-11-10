package com.dik.videoedit.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dik.videoedit.AppContainer
import com.dik.videoedit.models.Project
import com.dik.videoedit.videoprocessing.DeviceCaps
import com.dik.videoedit.videoprocessing.ExportEngine
import com.dik.videoedit.videoprocessing.ExportNotifier
import com.dik.videoedit.videoprocessing.ExportOptions
import com.dik.videoedit.videoprocessing.ExportPlan
import com.dik.videoedit.videoprocessing.GallerySaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExportViewModel(private val c: AppContainer, private val projectId: String) : ViewModel() {

    sealed interface Phase {
        data object Setup : Phase
        data object Running : Phase
        data class Done(val uri: Uri, val sizeBytes: Long) : Phase
        data class Failed(val message: String) : Phase
    }

    data class State(
        val project: Project? = null,
        val loading: Boolean = true,
        val shortSide: Int = 720,
        val fps: Int = 0,
        val removeWatermark: Boolean = com.dik.videoedit.AppInfo.UNLOCK_ALL,
        val resolutions: List<Int> = listOf(480, 720, 1080),
        val fpsOptions: List<Int> = listOf(0),
        val sourceFps: Int = 30,
        val estimateBytes: Long = 0L,
        val phase: Phase = Phase.Setup,
        val progress: Int = 0,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val engine = ExportEngine(c.context)
    private var progressJob: Job? = null
    private var outFile: File? = null

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val p = c.projects.get(projectId)
        if (p == null) {
            _state.update { it.copy(loading = false) }
            return
        }
        val ratio = p.outputRatio
        val resolutions = listOf(480, 720, 1080).filter { res ->
            val (w, h) = ExportPlan.outputSize(ratio, res)
            DeviceCaps.sizeSupported(w, h)
        }.ifEmpty { listOf(480) }
        val preferred = c.settings.settings.first().defaultQuality
        val quality = resolutions.lastOrNull { it <= preferred } ?: resolutions.first()

        val src = ExportPlan.sourceFps(p)
        val (w, h) = ExportPlan.outputSize(ratio, quality)
        val fpsOptions = listOf(0) + listOf(60, 30, 24).filter { it < src && DeviceCaps.fpsSupported(w, h, it) }

        _state.update {
            it.copy(
                project = p, loading = false, shortSide = quality, resolutions = resolutions,
                fpsOptions = fpsOptions, sourceFps = src,
            ).withEstimate()
        }
    }

    private fun State.withEstimate(): State {
        val p = project ?: return this
        return copy(estimateBytes = ExportPlan.estimateBytes(p, ExportOptions(shortSide, fps, removeWatermark)))
    }

    fun setQuality(v: Int) = _state.update { it.copy(shortSide = v).withEstimate() }
    fun setFps(v: Int) = _state.update { it.copy(fps = v).withEstimate() }
    fun setRemoveWatermark(v: Boolean) = _state.update { it.copy(removeWatermark = v) }

    fun backToSetup() = _state.update { it.copy(phase = Phase.Setup, progress = 0) }

    fun start() {
        val s = _state.value
        val p = s.project ?: return
        if (s.phase is Phase.Running) return

        val dir = File(c.context.cacheDir, "exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val out = File(dir, "VideoEdit_$stamp.mp4")
        outFile = out
        _state.update { it.copy(phase = Phase.Running, progress = 0) }

        try {
            engine.start(
                p, ExportOptions(s.shortSide, s.fps, s.removeWatermark), out.absolutePath,
                object : ExportEngine.Callback {
                    override fun onDone(sizeBytes: Long) {
                        progressJob?.cancel()
                        viewModelScope.launch { finish(out) }
                    }

                    override fun onError(message: String) {
                        progressJob?.cancel()
                        out.delete()
                        _state.update { it.copy(phase = Phase.Failed(message)) }
                    }
                },
            )
        } catch (e: Exception) {
            out.delete()
            _state.update { it.copy(phase = Phase.Failed("Export tidak dapat dimulai: ${e.message ?: "kesalahan tidak diketahui"}")) }
            return
        }

        progressJob = viewModelScope.launch {
            while (isActive) {
                val pr = engine.progress()
                if (pr >= 0) _state.update { it.copy(progress = pr) }
                delay(400)
            }
        }
    }

    private suspend fun finish(file: File) {
        val uri = withContext(Dispatchers.IO) { GallerySaver.save(c.context, file, file.name) }
        val size = file.length()
        file.delete()
        if (uri == null) {
            _state.update { it.copy(phase = Phase.Failed("Video selesai diproses tetapi gagal disimpan ke galeri. Periksa izin dan ruang penyimpanan.")) }
            return
        }
        _state.update { it.copy(phase = Phase.Done(uri, size), progress = 100) }
        if (c.settings.settings.first().notifications) ExportNotifier.notifyDone(c.context, uri)
    }

    fun cancel() {
        engine.cancel()
        progressJob?.cancel()
        outFile?.delete()
        _state.update { it.copy(phase = Phase.Setup, progress = 0) }
    }

    override fun onCleared() {
        if (_state.value.phase is Phase.Running) {
            engine.cancel()
            outFile?.delete()
        }
    }
}
