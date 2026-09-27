package com.craftworks.music.managers.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.craftworks.music.data.model.LyricSource
import com.craftworks.music.data.model.LyricsProvider
import com.craftworks.music.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaProviderSettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val LRCLIB_ENDPOINT = stringPreferencesKey("lrclib_endpoint")
        private val LRCLIB_LYRICS = booleanPreferencesKey("lrclib_lyrics_enabled")
        private val NETEASE_LYRICS = booleanPreferencesKey("netease_lyrics_enabled")
        private val LYRIC_PROVIDERS = stringPreferencesKey("lyric_providers")
    }

    val lrcLibEndpointFlow: Flow<String> = context.dataStore.data.map {
        it[LRCLIB_ENDPOINT] ?: "https://lrclib.net"
    }

    suspend fun setLrcLibEndpoint(LrcLibEndpoint: String) {
        withContext(NonCancellable) {
            context.dataStore.edit { preferences ->
                preferences[LRCLIB_ENDPOINT] = LrcLibEndpoint
            }
        }
    }

    val lrcLibLyricsFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[LRCLIB_LYRICS] ?: true
    }

    val netEaseLyricsFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[NETEASE_LYRICS] ?: false
    }

    val lyricProvidersFlow: Flow<List<LyricsProvider>> = context.dataStore.data.map { preferences ->
        val defaultValue = listOf(
            LyricsProvider(LyricSource.MEDIA_PROVIDER, true),
            LyricsProvider(LyricSource.LRCLIB, true),
            LyricsProvider(LyricSource.BINI_LYRICS, false),
            LyricsProvider(LyricSource.UNISON, true),
            LyricsProvider(LyricSource.NETEASE, false),
        )

        val jsonString = preferences[LYRIC_PROVIDERS] ?: return@map defaultValue

        try {
            Json.decodeFromString<List<LyricsProvider>>(jsonString)
        } catch (e: Exception) {
            defaultValue
        }
    }

    suspend fun setLyricProviders(providers: List<LyricsProvider>) {
        withContext(NonCancellable) {
            context.dataStore.edit { preferences ->
                preferences[LYRIC_PROVIDERS] = Json.encodeToString(providers)
            }
        }
    }
}