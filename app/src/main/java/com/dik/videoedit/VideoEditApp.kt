package com.dik.videoedit

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.memory.MemoryCache

class VideoEditApp : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        com.dik.videoedit.i18n.L10n.init(this)
        container = AppContainer(this)
        container.initAds()
    }

    /** Coil dengan decoder frame video + cache memori kecil (ramah HP kelas bawah). */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.12).build() }
            .crossfade(false)
            .build()
}
