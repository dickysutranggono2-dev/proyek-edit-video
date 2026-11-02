package com.dik.videoedit.videoprocessing

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.dik.videoedit.models.Clip
import com.dik.videoedit.models.MusicTrack
import kotlin.math.roundToInt

/** Membaca metadata video/audio TANPA memuat isi file ke memori. */
object VideoProbe {

    fun probeVideo(context: Context, uri: Uri): Clip? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            if (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) != "yes") return null
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                ?: return null
            var w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: return null
            var h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: return null
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            if (rotation == 90 || rotation == 270) {
                val t = w; w = h; h = t
            }
            Clip(
                uri = uri.toString(),
                name = displayName(context, uri) ?: "video.mp4",
                sourceDurationMs = duration,
                width = w,
                height = h,
                frameRate = frameRate(context, uri),
            )
        } catch (e: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    fun probeAudio(context: Context, uri: Uri): MusicTrack? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                ?: return null
            if (duration < 500) return null
            MusicTrack(uri = uri.toString(), name = displayName(context, uri) ?: "Musik", durationMs = duration)
        } catch (e: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun frameRate(context: Context, uri: Uri): Int {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(context, uri, null)
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                if (f.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true) {
                    if (!f.containsKey(MediaFormat.KEY_FRAME_RATE)) return 30
                    return try {
                        f.getInteger(MediaFormat.KEY_FRAME_RATE)
                    } catch (e: ClassCastException) {
                        f.getFloat(MediaFormat.KEY_FRAME_RATE).roundToInt()
                    }.coerceIn(1, 120)
                }
            }
            30
        } catch (e: Exception) {
            30
        } finally {
            runCatching { extractor.release() }
        }
    }

    fun displayName(context: Context, uri: Uri): String? = try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    } catch (e: Exception) {
        null
    }
}
