package com.dik.videoedit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.dik.videoedit.navigation.AppNavHost
import com.dik.videoedit.storage.AppSettings
import com.dik.videoedit.ui.theme.VideoEditTheme

/** Hanya menyalakan tema + navigasi; seluruh logika ada di paket masing-masing. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as VideoEditApp).container
        setContent {
            val settings by container.settings.settings.collectAsState(initial = AppSettings())
            CompositionLocalProvider(LocalAppContainer provides container) {
                VideoEditTheme(settings.theme) { AppNavHost() }
            }
        }
    }
}
