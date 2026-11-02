package com.dik.videoedit.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dik.videoedit.AppContainer
import com.dik.videoedit.models.AspectOption
import com.dik.videoedit.models.Clip
import com.dik.videoedit.models.EffectType
import com.dik.videoedit.models.FilterType
import com.dik.videoedit.models.Project
import com.dik.videoedit.models.TextItem
import com.dik.videoedit.models.TransitionType
import com.dik.videoedit.videoeditor.Importer
import com.dik.videoedit.videoeditor.ProjectOps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditorViewModel(private val c: AppContainer, private val projectId: String) : ViewModel() {

    data class State(
        val project: Project? = null,
        val selected: Int = 0,
        val canUndo: Boolean = false,
        val canRedo: Boolean = false,
        val loading: Boolean = true,
        val busy: Boolean = false,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 8)

    private val undoStack = ArrayDeque<Project>()
    private val redoStack = ArrayDeque<Project>()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val p = c.projects.get(projectId)
        if (p == null) {
            _state.update { it.copy(loading = false) }
            return
        }
        val readable = withContext(Dispatchers.IO) { p.clips.filter { Importer.canRead(c.context, it.uri) } }
        var fixed = p
        if (readable.size != p.clips.size) {
            fixed = p.copy(clips = readable)
            messages.tryEmit("${p.clips.size - readable.size} video sumber tidak ditemukan dan dilepas dari proyek.")
            save(fixed)
        }
        _state.update { it.copy(project = fixed, loading = false) }
    }

    // ---- infrastruktur ---------------------------------------------------------------------

    private fun save(p: Project) {
        c.appScope.launch { c.projects.save(p) }
    }

    private fun commit(selected: Int? = null, change: (Project) -> Project) {
        val cur = _state.value.project ?: return
        val next = change(cur)
        if (next == cur) return
        undoStack.addLast(cur)
        if (undoStack.size > 40) undoStack.removeFirst()
        redoStack.clear()
        apply(next.copy(updatedAt = System.currentTimeMillis()), selected)
    }

    private fun apply(p: Project, selected: Int? = null) {
        _state.update {
            it.copy(
                project = p,
                selected = (selected ?: it.selected).coerceIn(0, (p.clips.size - 1).coerceAtLeast(0)),
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
            )
        }
        save(p)
    }

    fun undo() {
        val cur = _state.value.project ?: return
        val prev = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(cur)
        apply(prev)
    }

    fun redo() {
        val cur = _state.value.project ?: return
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(cur)
        apply(next)
    }

    fun select(index: Int) {
        _state.update {
            val last = (it.project?.clips?.lastIndex ?: 0).coerceAtLeast(0)
            it.copy(selected = index.coerceIn(0, last))
        }
    }

    private fun commitClip(change: (Clip) -> Clip) {
        val i = _state.value.selected
        commit { p -> ProjectOps.updateClip(p, i, change) }
    }

    // ---- editing ---------------------------------------------------------------------------

    fun trim(startMs: Long, endMs: Long) {
        val i = _state.value.selected
        commit { ProjectOps.trim(it, i, startMs, endMs) }
    }

    /** [atSourceMs] = posisi pada video sumber (trimStart + posisi di dalam klip). */
    fun split(index: Int, atSourceMs: Long) {
        val cur = _state.value.project ?: return
        if (ProjectOps.split(cur, index, atSourceMs) == null) {
            messages.tryEmit("Geser posisi putar ke tengah klip dulu, lalu tekan Pisah.")
            return
        }
        commit(selected = index) { ProjectOps.split(it, index, atSourceMs) ?: it }
    }

    fun deleteSelected() {
        val cur = _state.value.project ?: return
        if (cur.clips.size <= 1) {
            messages.tryEmit("Proyek harus memiliki minimal satu klip.")
            return
        }
        val i = _state.value.selected
        commit(selected = (i - 1).coerceAtLeast(0)) { ProjectOps.delete(it, i) }
    }

    fun moveSelected(delta: Int) {
        val i = _state.value.selected
        commit(selected = i + delta) { ProjectOps.move(it, i, delta) }
    }

    fun setSpeed(speed: Float, applyToAll: Boolean) {
        if (applyToAll) {
            commit { p -> p.copy(clips = p.clips.map { it.copy(speed = speed) }) }
        } else {
            commitClip { it.copy(speed = speed) }
        }
    }

    fun setClipVolume(v: Float) = commitClip { it.copy(volume = v.coerceIn(0f, 1f)) }

    fun addClips(uris: List<Uri>) {
        val before = _state.value.project?.clips?.size ?: return
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val result = Importer.importVideos(c.context, uris)
            _state.update { it.copy(busy = false) }
            if (result.failed > 0) messages.tryEmit(Importer.failedMessage(result.failed))
            if (result.clips.isNotEmpty()) {
                commit(selected = before) { it.copy(clips = it.clips + result.clips) }
            }
        }
    }

    fun setAspect(a: AspectOption) = commit { it.copy(aspect = a) }

    fun rotate() = commit { it.copy(rotation = (it.rotation + 90) % 360) }

    fun setMusic(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val track = Importer.importAudio(c.context, uri)
            _state.update { it.copy(busy = false) }
            if (track == null) messages.tryEmit("File audio tidak dapat dibuka.")
            else commit { it.copy(music = track) }
        }
    }

    fun setMusicVolume(v: Float) = commit { p -> p.copy(music = p.music?.copy(volume = v.coerceIn(0f, 1f))) }

    fun removeMusic() = commit { it.copy(music = null) }

    fun addText(item: TextItem) = commit { it.copy(texts = it.texts + item) }

    fun removeText(id: String) = commit { p -> p.copy(texts = p.texts.filterNot { it.id == id }) }

    fun setFilter(f: FilterType) = commit { it.copy(filter = f) }

    fun setEffect(e: EffectType) = commit { it.copy(effect = e) }

    fun setTransition(t: TransitionType) = commit { it.copy(transition = t) }
}
