package com.dik.videoedit.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dik.videoedit.AppContainer
import com.dik.videoedit.storage.AppSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val c: AppContainer) : ViewModel() {
    val settings: StateFlow<AppSettings> =
        c.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    fun setQuality(v: Int) { viewModelScope.launch { c.settings.setDefaultQuality(v) } }
    fun setTheme(v: String) { viewModelScope.launch { c.settings.setTheme(v) } }
    fun setNotifications(v: Boolean) { viewModelScope.launch { c.settings.setNotifications(v) } }
}
