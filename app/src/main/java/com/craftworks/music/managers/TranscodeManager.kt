package com.craftworks.music.managers

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.craftworks.music.managers.settings.PlaybackSettingsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranscodeManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playbackSettingsManager: PlaybackSettingsManager
) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val currentBitrateFlow: Flow<String> = combine(
        playbackSettingsManager.wifiTranscodingBitrateFlow,
        playbackSettingsManager.mobileDataTranscodingBitrateFlow
    ) { wifiBitrate, mobileBitrate ->
        if (isUsingMobileData()) mobileBitrate else wifiBitrate
    }

    val currentFormatFlow: Flow<String> = playbackSettingsManager.transcodingFormatFlow

    val transcodingConfigChangesFlow: Flow<String> = combine(
        currentFormatFlow,
        playbackSettingsManager.wifiTranscodingBitrateFlow,
        playbackSettingsManager.mobileDataTranscodingBitrateFlow
    ) { format, wifi, mobile -> "$format-$wifi-$mobile" }

    fun isUsingMobileData(): Boolean {
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
    }
}