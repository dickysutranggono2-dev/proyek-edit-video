package com.dik.videoedit

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.dik.videoedit.admob.PremiumManager
import com.dik.videoedit.admob.RewardedAdManager
import com.dik.videoedit.storage.ProjectRepository
import com.dik.videoedit.storage.SettingsStore
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Penyedia dependensi sederhana (tanpa library DI). */
class AppContainer(val context: Context) {
    /** Scope untuk pekerjaan yang harus selesai walau layar sudah ditutup (mis. autosave). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val projects = ProjectRepository(context)
    val settings = SettingsStore(context)
    val premium = PremiumManager(settings)
    val ads = RewardedAdManager(context)

    /** SDK iklan diinisialisasi di thread latar; TIDAK ada iklan yang dimuat/ditampilkan di sini. */
    fun initAds() {
        Thread { runCatching { MobileAds.initialize(context) } }.start()
    }
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer belum disediakan")
}
