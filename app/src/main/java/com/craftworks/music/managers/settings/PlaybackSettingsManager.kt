package com.craftworks.music.managers.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.craftworks.music.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackSettingsManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private val TRANSCODING_BITRATE_WIFI_KEY = stringPreferencesKey("transcoding_bitrate_wifi")
        private val TRANSCODING_BITRATE_DATA_KEY = stringPreferencesKey("transcoding_bitrate_data")
        private val TRANSCODING_FORMAT_KEY = stringPreferencesKey("transcoding_format")
        private val SCROBBLE_PERCENT_KEY = intPreferencesKey("scrobble_percent")
        private val QUEUE_ADD_TO_BOTTOM_KEY = booleanPreferencesKey("queue_add_to_bottom")
    }

    val wifiTranscodingBitrateFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[TRANSCODING_BITRATE_WIFI_KEY] ?: "No Transcoding"
    }

    suspend fun setWifiTranscodingBitrate(bitrate: String) {
        context.dataStore.edit { preferences ->
            preferences[TRANSCODING_BITRATE_WIFI_KEY] = bitrate
        }
    }

    val mobileDataTranscodingBitrateFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[TRANSCODING_BITRATE_DATA_KEY] ?: "No Transcoding"
    }

    suspend fun setMobileDataTranscodingBitrate(bitrate: String) {
        context.dataStore.edit { preferences ->
            preferences[TRANSCODING_BITRATE_DATA_KEY] = bitrate
        }
    }

    val transcodingFormatFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[TRANSCODING_FORMAT_KEY] ?: "opus"
    }

    suspend fun setTranscodingFormat(format: String) {
        context.dataStore.edit { preferences ->
            preferences[TRANSCODING_FORMAT_KEY] = format
        }
    }

    val scrobblePercentFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[SCROBBLE_PERCENT_KEY] ?: 7
    }

    suspend fun setScrobblePercent(scrobblePercent: Int) {
        context.dataStore.edit { preferences ->
            preferences[SCROBBLE_PERCENT_KEY] = scrobblePercent
        }
    }

    // Queue position: true = add to bottom (end), false = add to top (after current)
    val queueAddToBottomFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[QUEUE_ADD_TO_BOTTOM_KEY] ?: true // Default: add to bottom
    }

    suspend fun setQueueAddToBottom(addToBottom: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[QUEUE_ADD_TO_BOTTOM_KEY] = addToBottom
        }
    }
}