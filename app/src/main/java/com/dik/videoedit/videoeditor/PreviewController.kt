package com.dik.videoedit.videoeditor

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.dik.videoedit.models.Clip
import com.dik.videoedit.models.Project
import com.dik.videoedit.videoprocessing.EffectsFactory

/**
 * Preview memakai ExoPlayer: satu playlist berisi klip yang sudah di-trim, efek visual yang sama
 * dengan export, dan pemutar kedua untuk musik latar. Video tidak pernah dimuat penuh ke RAM.
 */
class PreviewController(context: Context) {
    val player: ExoPlayer = ExoPlayer.Builder(context).build()
    private val musicPlayer: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        repeatMode = Player.REPEAT_MODE_ALL
    }

    private var clips: List<Clip> = emptyList()
    private var clipSignature = ""
    private var effectSignature: String? = null
    private var musicUri: String? = null
    private var hasMusic = false

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!hasMusic) return
                if (isPlaying) {
                    musicPlayer.seekTo(musicPositionFor(globalMs()))
                    musicPlayer.play()
                } else {
                    musicPlayer.pause()
                }
            }
        })
    }

    /** Sinkronkan pemutar dengan proyek. Aman dipanggil berulang; hanya bekerja bila ada perubahan. */
    fun update(p: Project) {
        clips = p.clips

        val fx = "${p.rotation}|${p.aspect}|${p.filter}|${p.effect}|${p.outputRatio}|" +
            "${p.clips.firstOrNull()?.width}x${p.clips.firstOrNull()?.height}"
        if (fx != effectSignature) {
            // Wajib dipanggil sebelum prepare() pertama.
            player.setVideoEffects(EffectsFactory.previewEffects(p))
            effectSignature = fx
        }

        val sig = p.clips.joinToString("|") { "${it.id}:${it.uri}:${it.trimStartMs}:${it.trimEndMs}" }
        if (sig != clipSignature) {
            val oldIndex = player.currentMediaItemIndex
            val oldPos = player.currentPosition
            val items = p.clips.map { c ->
                MediaItem.Builder()
                    .setUri(c.uri)
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(c.trimStartMs)
                            .setEndPositionMs(c.trimEndMs)
                            .build()
                    )
                    .build()
            }
            val index = oldIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
            val maxPos = ((p.clips.getOrNull(index)?.trimmedMs ?: 1L) - 1).coerceAtLeast(0L)
            player.setMediaItems(items, index, oldPos.coerceIn(0L, maxPos))
            player.prepare()
            clipSignature = sig
        }

        val m = p.music
        hasMusic = m != null
        if (m?.uri != musicUri) {
            musicUri = m?.uri
            if (m != null) {
                musicPlayer.setMediaItem(MediaItem.fromUri(m.uri))
                musicPlayer.prepare()
            } else {
                musicPlayer.stop()
                musicPlayer.clearMediaItems()
            }
        }
        musicPlayer.volume = m?.volume ?: 0f
        if (!hasMusic || !player.isPlaying) musicPlayer.pause()
    }

    /** Kecepatan & volume bisa berbeda tiap klip; terapkan sesuai klip yang sedang diputar. */
    fun syncClipParams() {
        val c = clips.getOrNull(player.currentMediaItemIndex) ?: return
        if (player.playbackParameters.speed != c.speed) player.setPlaybackSpeed(c.speed)
        if (player.volume != c.volume) player.volume = c.volume
    }

    fun globalMs(): Long {
        val index = player.currentMediaItemIndex
        var t = 0L
        for (i in 0 until index.coerceAtMost(clips.size)) t += clips[i].effectiveMs
        val c = clips.getOrNull(index) ?: return t
        return t + (player.currentPosition.coerceAtLeast(0L) / c.speed).toLong()
    }

    fun seekToGlobal(ms: Long) {
        var remaining = ms.coerceAtLeast(0L)
        for ((i, c) in clips.withIndex()) {
            if (remaining < c.effectiveMs || i == clips.lastIndex) {
                player.seekTo(i, (remaining.coerceAtMost(c.effectiveMs) * c.speed).toLong())
                break
            }
            remaining -= c.effectiveMs
        }
        if (hasMusic && player.isPlaying) musicPlayer.seekTo(musicPositionFor(ms))
    }

    fun seekToClip(index: Int) {
        if (index in clips.indices) player.seekTo(index, 0L)
    }

    fun togglePlay() {
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_ENDED) player.seekTo(0, 0L)
            player.play()
        }
    }

    fun pause() {
        player.pause()
        musicPlayer.pause()
    }

    fun release() {
        player.release()
        musicPlayer.release()
    }

    private fun musicPositionFor(globalMs: Long): Long {
        val d = musicPlayer.duration
        return if (d > 0) globalMs % d else globalMs
    }
}
