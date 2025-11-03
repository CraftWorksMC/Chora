package com.craftworks.music.managers.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import com.craftworks.music.data.model.MediaData
import com.craftworks.music.data.model.toMediaItem
import com.craftworks.music.data.model.toSong
import com.craftworks.music.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalDataSettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val LOCAL_RADIOS = stringPreferencesKey("radios_list")
        private val LOCAL_PLAYLISTS = stringPreferencesKey("playlists_list")

        private val MEDIA_RESUMPTION_PLAYLIST = stringPreferencesKey("media_resumption_playlist")
        private val MEDIA_RESUMPTION_INDEX = intPreferencesKey("media_resumption_index")
        private val MEDIA_RESUMPTION_TIME = longPreferencesKey("media_resumption_timestamp")
    }

    val localRadios: Flow<MutableList<MediaData.Radio>> =
        context.dataStore.data.map { preferences ->
            Json.decodeFromString<List<MediaData.Radio>>(preferences[LOCAL_RADIOS] ?: "[]")
                .toMutableList()
        }

    suspend fun saveLocalRadios(radios: List<MediaData.Radio>) {
        val radiosListJson =
            Json.encodeToString(radios.filter { it.navidromeID.startsWith("Local_") })
        context.dataStore.edit { preferences ->
            preferences[LOCAL_RADIOS] = radiosListJson
        }
    }

    val localPlaylists: Flow<MutableList<MediaData.Playlist>> =
        context.dataStore.data.map { preferences ->
            Json.decodeFromString<List<MediaData.Playlist>>(preferences[LOCAL_PLAYLISTS] ?: "[]")
                .toMutableList()
        }

    suspend fun saveLocalPlaylists(playlists: List<MediaData.Playlist>) {
        val playlistJson =
            Json.encodeToString(playlists.filter { it.navidromeID.startsWith("Local_") })
        context.dataStore.edit { preferences ->
            preferences[LOCAL_PLAYLISTS] = playlistJson
        }
    }

    @UnstableApi
    suspend fun setPlaybackResumption(playlist: List<MediaItem>, currentPos: Int, currentTime: Long) {
        println(Json.encodeToString(playlist.map { it.toSong() })) // Keeping original debug print
        context.dataStore.edit { preferences ->
            preferences[MEDIA_RESUMPTION_PLAYLIST] = Json.encodeToString(playlist.map { it.toSong() })
            preferences[MEDIA_RESUMPTION_INDEX] = currentPos
            preferences[MEDIA_RESUMPTION_TIME] = currentTime
        }
    }

    @UnstableApi
    val playbackResumptionPlaylistWithStartPosition: Flow<MediaSession.MediaItemsWithStartPosition> = context.dataStore.data.map { preferences ->
        println(preferences[MEDIA_RESUMPTION_PLAYLIST]) // Keeping original debug print
        MediaSession.MediaItemsWithStartPosition(
            Json.decodeFromString<List<MediaData.Song>>(preferences[MEDIA_RESUMPTION_PLAYLIST] ?: "[]").map {it.toMediaItem()},
            preferences[MEDIA_RESUMPTION_INDEX] ?: 0,
            preferences[MEDIA_RESUMPTION_TIME] ?: 0L
        )
    }
}