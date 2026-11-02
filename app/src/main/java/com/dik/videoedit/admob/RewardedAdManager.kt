package com.dik.videoedit.admob

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.dik.videoedit.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface AdState {
    data object Idle : AdState
    data object Loading : AdState
    data object Ready : AdState
    data class Failed(val message: String) : AdState
}

/**
 * Membungkus Rewarded Ad. Iklan HANYA dimuat saat pengguna membuka dialog premium,
 * tidak pernah saat aplikasi dibuka, dan reward HANYA diberikan lewat callback reward.
 */
class RewardedAdManager(private val context: Context) {
    private var ad: RewardedAd? = null
    private val _state = MutableStateFlow<AdState>(AdState.Idle)
    val state: StateFlow<AdState> = _state.asStateFlow()

    /** Harus dipanggil dari main thread. */
    fun load() {
        if (_state.value == AdState.Loading) return
        if (ad != null) {
            _state.value = AdState.Ready
            return
        }
        if (!isOnline()) {
            _state.value = AdState.Failed("Tidak ada koneksi internet. Sambungkan internet lalu coba lagi.")
            return
        }
        _state.value = AdState.Loading
        RewardedAd.load(
            context,
            BuildConfig.REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(loaded: RewardedAd) {
                    ad = loaded
                    _state.value = AdState.Ready
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    ad = null
                    _state.value = AdState.Failed("Iklan belum tersedia (kode ${error.code}). Coba lagi sebentar lagi.")
                }
            },
        )
    }

    /**
     * Menampilkan iklan. [onReward] dipanggil HANYA ketika pengguna benar-benar mendapat reward.
     * [onClosed] dipanggil ketika iklan ditutup atau gagal ditampilkan.
     */
    fun show(activity: Activity, onReward: () -> Unit, onClosed: () -> Unit) {
        val current = ad
        if (current == null) {
            load()
            onClosed()
            return
        }
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                ad = null
                _state.value = AdState.Idle
                onClosed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                ad = null
                _state.value = AdState.Failed("Iklan gagal ditampilkan. Coba lagi.")
                onClosed()
            }
        }
        current.show(activity) { _ -> onReward() }
    }

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
