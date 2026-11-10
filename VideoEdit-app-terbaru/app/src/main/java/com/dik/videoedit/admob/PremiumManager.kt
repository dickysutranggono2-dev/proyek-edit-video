package com.dik.videoedit.admob

import com.dik.videoedit.storage.SettingsStore
import kotlinx.coroutines.flow.Flow

/**
 * Satu iklan reward membuka SEMUA fitur premium selama [DURATION_MS].
 * Akses hanya diberikan lewat [grant], yang dipanggil dari callback reward AdMob.
 */
class PremiumManager(private val settings: SettingsStore) {
    val until: Flow<Long> = settings.premiumUntil

    suspend fun grant() {
        settings.setPremiumUntil(System.currentTimeMillis() + DURATION_MS)
    }

    companion object {
        const val DURATION_MS = 30 * 60 * 1000L
    }
}
