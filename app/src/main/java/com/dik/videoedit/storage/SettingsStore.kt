package com.dik.videoedit.storage

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "videoedit_settings")

data class AppSettings(
    /** Sisi terpendek hasil export: 480, 720, atau 1080. */
    val defaultQuality: Int = 720,
    /** "light", "dark", atau "system". */
    val theme: String = "light",
    val notifications: Boolean = true,
)

class SettingsStore(private val context: Context) {
    private val kQuality = intPreferencesKey("default_quality")
    private val kTheme = stringPreferencesKey("theme")
    private val kNotif = booleanPreferencesKey("notifications")
    private val kPremiumUntil = longPreferencesKey("premium_until")

    val settings: Flow<AppSettings> = context.dataStore.data.map { it.toSettings() }

    val premiumUntil: Flow<Long> = context.dataStore.data.map { it[kPremiumUntil] ?: 0L }

    private fun Preferences.toSettings() = AppSettings(
        defaultQuality = this[kQuality] ?: 720,
        theme = this[kTheme] ?: "light",
        notifications = this[kNotif] ?: true,
    )

    suspend fun setDefaultQuality(v: Int) { context.dataStore.edit { it[kQuality] = v } }
    suspend fun setTheme(v: String) { context.dataStore.edit { it[kTheme] = v } }
    suspend fun setNotifications(v: Boolean) { context.dataStore.edit { it[kNotif] = v } }
    suspend fun setPremiumUntil(v: Long) { context.dataStore.edit { it[kPremiumUntil] = v } }
}
