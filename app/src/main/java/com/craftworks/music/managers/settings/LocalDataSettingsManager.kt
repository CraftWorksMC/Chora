package com.craftworks.music.managers.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import com.craftworks.music.data.model.SortOrder
import com.craftworks.music.data.model.toSong
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
class LocalDataSettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val LOCAL_RADIOS = stringPreferencesKey("radios_list")
        private val LOCAL_PLAYLISTS = stringPreferencesKey("playlists_list")

        private val MEDIA_RESUMPTION_PLAYLIST = stringPreferencesKey("media_resumption_playlist")
        private val MEDIA_RESUMPTION_INDEX = intPreferencesKey("media_resumption_index")
        private val MEDIA_RESUMPTION_TIME = longPreferencesKey("media_resumption_timestamp")

        private val SORT_ALBUM_ORDER = stringPreferencesKey("sort_album_order")
        private val SHOW_FAVORITES_ONLY = booleanPreferencesKey("show_favorites_only")
    }

    val localRadios: Flow<MutableList<com.craftworks.music.data.model.MediaModel.Radio>> =
        context.dataStore.data.map { preferences ->
            Json.decodeFromString<List<com.craftworks.music.data.model.MediaModel.Radio>>(preferences[LOCAL_RADIOS] ?: "[]")
                .toMutableList()
        }

    suspend fun saveLocalRadios(radios: List<com.craftworks.music.data.model.MediaModel.Radio>) {
        withContext(NonCancellable) {
            val radiosListJson =
                Json.encodeToString(radios.filter { it.navidromeID.startsWith("Local_") })
            context.dataStore.edit { preferences ->
                preferences[LOCAL_RADIOS] = radiosListJson
            }
        }
    }

    val localPlaylists: Flow<MutableList<com.craftworks.music.data.model.MediaModel.Playlist>> =
        context.dataStore.data.map { preferences ->
            Json.decodeFromString<List<com.craftworks.music.data.model.MediaModel.Playlist>>(preferences[LOCAL_PLAYLISTS] ?: "[]")
                .toMutableList()
        }

    suspend fun saveLocalPlaylists(playlists: List<com.craftworks.music.data.model.MediaModel.Playlist>) {
        withContext(NonCancellable) {
            val playlistJson =
                Json.encodeToString(playlists.filter { it.navidromeID.startsWith("Local_") })
            context.dataStore.edit { preferences ->
                preferences[LOCAL_PLAYLISTS] = playlistJson
            }
        }
    }

    @UnstableApi
    suspend fun setPlaybackResumption(playlist: List<MediaItem>, currentPos: Int, currentTime: Long) {
        withContext(NonCancellable) {
            context.dataStore.edit { preferences ->
                preferences[MEDIA_RESUMPTION_PLAYLIST] =
                    Json.encodeToString(playlist.map { it.toSong() })
                preferences[MEDIA_RESUMPTION_INDEX] = currentPos
                preferences[MEDIA_RESUMPTION_TIME] = currentTime
            }
        }
    }

    @UnstableApi
    val playbackResumptionPlaylistWithStartPosition: Flow<MediaSession.MediaItemsWithStartPosition> = context.dataStore.data.map { preferences ->
        withContext(NonCancellable) {
            MediaSession.MediaItemsWithStartPosition(
                Json.decodeFromString<List<com.craftworks.music.data.model.MediaModel.Song>>(
                    preferences[MEDIA_RESUMPTION_PLAYLIST] ?: "[]"
                ).map { it.toMediaItem() },
                preferences[MEDIA_RESUMPTION_INDEX] ?: 0,
                preferences[MEDIA_RESUMPTION_TIME] ?: 0L
            )
        }
    }

    val sortAlbumOrder: Flow<SortOrder> =
        context.dataStore.data.map { preferences ->
            SortOrder.entries.find { it.key == preferences[SORT_ALBUM_ORDER] } ?: SortOrder.ALPHABETICAL
        }

    val showFavoriteOnly: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[SHOW_FAVORITES_ONLY] ?: false
        }

    suspend fun saveSortAlbumOrder(sortOrder: SortOrder) {
        withContext(NonCancellable) {
            context.dataStore.edit { preferences ->
                preferences[SORT_ALBUM_ORDER] = sortOrder.key
            }
        }
    }

    suspend fun saveShowFavoriteOnly(showFavorites: Boolean) {
        withContext(NonCancellable) {
            context.dataStore.edit { preferences ->
                preferences[SHOW_FAVORITES_ONLY] = showFavorites;
            }
        }
    }
}