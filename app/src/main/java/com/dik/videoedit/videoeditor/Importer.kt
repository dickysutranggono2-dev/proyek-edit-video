package com.dik.videoedit.videoeditor

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.io.File
import java.util.UUID
import com.dik.videoedit.models.Clip
import com.dik.videoedit.models.AudioTrack
import com.dik.videoedit.videoprocessing.VideoProbe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Import video/musik dari galeri/penyimpanan. Hanya membaca metadata, file tidak disalin. */
object Importer {
    private const val MIN_DURATION_MS = 500L

    data class VideoResult(val clips: List<Clip>, val failed: Int)

    suspend fun importVideos(context: Context, uris: List<Uri>): VideoResult = withContext(Dispatchers.IO) {
        val clips = mutableListOf<Clip>()
        var failed = 0
        for (uri in uris) {
            val isImage = runCatching { context.contentResolver.getType(uri) }.getOrNull()?.startsWith("image/") == true
            val stable = stableUri(context, uri)
            val clip = if (isImage) VideoProbe.probeImage(context, stable) else VideoProbe.probeVideo(context, stable)
            if (clip == null || clip.sourceDurationMs < MIN_DURATION_MS) failed++ else clips += clip
        }
        VideoResult(clips, failed)
    }

    suspend fun importAudio(context: Context, uri: Uri): AudioTrack? = withContext(Dispatchers.IO) {
        VideoProbe.probeAudio(context, stableUri(context, uri))
    }

    /**
     * Memastikan file tetap bisa dibuka setelah aplikasi ditutup. Bila izin baca bisa dipertahankan, URI dipakai
     * apa adanya; bila tidak (misalnya dari pemilih foto), file disalin ke penyimpanan aplikasi.
     */
    private fun stableUri(context: Context, uri: Uri): Uri {
        if (uri.scheme == "file") return uri
        try {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val kept = context.contentResolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }
            if (kept) return uri
        } catch (e: Exception) {
            // Penyedia konten ini tidak mendukung izin persisten.
        }
        return copyToApp(context, uri) ?: uri
    }

    private fun copyToApp(context: Context, uri: Uri): Uri? = try {
        val name = (VideoProbe.displayName(context, uri) ?: "media").replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val dir = File(context.filesDir, "media/${UUID.randomUUID()}").apply { mkdirs() }
        val out = File(dir, name)
        val input = context.contentResolver.openInputStream(uri)
        if (input == null) null else {
            input.use { i -> out.outputStream().use { o -> i.copyTo(o) } }
            Uri.fromFile(out)
        }
    } catch (e: Exception) {
        null
    }

    fun canRead(context: Context, uri: String): Boolean = try {
        context.contentResolver.openAssetFileDescriptor(Uri.parse(uri), "r")?.use { true } ?: false
    } catch (e: Exception) {
        false
    }

    fun failedMessage(failed: Int): String =
        "$failed video tidak dapat dibuka (format tidak didukung atau file rusak)."
}
