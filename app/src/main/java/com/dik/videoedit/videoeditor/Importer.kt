package com.dik.videoedit.videoeditor

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.dik.videoedit.models.Clip
import com.dik.videoedit.models.MusicTrack
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
            persistAccess(context, uri)
            val clip = VideoProbe.probeVideo(context, uri)
            if (clip == null || clip.sourceDurationMs < MIN_DURATION_MS) failed++ else clips += clip
        }
        VideoResult(clips, failed)
    }

    suspend fun importAudio(context: Context, uri: Uri): MusicTrack? = withContext(Dispatchers.IO) {
        persistAccess(context, uri)
        VideoProbe.probeAudio(context, uri)
    }

    /** Mencoba mempertahankan izin baca agar proyek tetap bisa dibuka setelah aplikasi ditutup. */
    private fun persistAccess(context: Context, uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {
            // Tidak semua penyedia konten mendukung izin persisten; abaikan.
        }
    }

    fun canRead(context: Context, uri: String): Boolean = try {
        context.contentResolver.openAssetFileDescriptor(Uri.parse(uri), "r")?.use { true } ?: false
    } catch (e: Exception) {
        false
    }

    fun failedMessage(failed: Int): String =
        "$failed video tidak dapat dibuka (format tidak didukung atau file rusak)."
}
