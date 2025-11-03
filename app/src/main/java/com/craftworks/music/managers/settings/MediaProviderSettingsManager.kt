package com.craftworks.music.managers.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.craftworks.music.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaProviderSettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val LRCLIB_ENDPOINT = stringPreferencesKey("lrclib_endpoint")
        private val LRCLIB_LYRICS = booleanPreferencesKey("lrclib_lyrics_enabled")
    }

    val lrcLibEndpointFlow: Flow<String> = context.dataStore.data.map {
        it[LRCLIB_ENDPOINT] ?: "https://lrclib.net"
    }

    suspend fun setLrcLibEndpoint(LrcLibEndpoint: String) {
        context.dataStore.edit { preferences ->
            preferences[LRCLIB_ENDPOINT] = LrcLibEndpoint
        }
    }

    val lrcLibLyricsFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[LRCLIB_LYRICS] ?: true // Original used != false, interpreting null as true
    }

    suspend fun setUseLrcLib(useLrcLib: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LRCLIB_LYRICS] = useLrcLib
        }
    }
}