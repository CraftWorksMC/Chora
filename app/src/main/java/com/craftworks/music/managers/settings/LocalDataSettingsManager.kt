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
import com.craftworks.music.data.model.AlbumArtistListSort
import com.craftworks.music.data.model.AlbumListSort
import com.craftworks.music.data.model.MediaModel
import com.craftworks.music.data.model.ProviderType
import com.craftworks.music.data.model.SongListSort
import com.craftworks.music.data.model.SortOrder
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

        private val SORT_ALBUM = stringPreferencesKey("sort_album")
        private val SORT_ALBUM_ORDER = stringPreferencesKey("sort_album_order")
        private val SHOW_FAVORITES_ALBUM = booleanPreferencesKey("show_favorites_album")
        private val SORT_ARTIST = stringPreferencesKey("sort_artist")
        private val SORT_ARTIST_ORDER = stringPreferencesKey("sort_artist_order")
        private val SHOW_FAVORITES_ARTIST = booleanPreferencesKey("show_favorites_artist")
        private val SORT_SONG = stringPreferencesKey("sort_song")
        private val SORT_SONG_ORDER = stringPreferencesKey("sort_song_order")
        private val SHOW_FAVORITES_SONG = booleanPreferencesKey("show_favorites_song")
    }
/*
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
*/
    @UnstableApi
    suspend fun setPlaybackResumption(playlist: List<MediaItem>, currentPos: Int, currentTime: Long) {
        withContext(NonCancellable) {
            context.dataStore.edit { preferences ->
                preferences[MEDIA_RESUMPTION_PLAYLIST] =
                    Json.encodeToString(playlist.map {
                        val song = MediaModel.Song(
                            id = it.mediaMetadata.extras?.getString("id") ?: "",
                            providerId = it.mediaMetadata.extras?.getString("providerId") ?: "",
                            providerType = ProviderType.valueOf(it.mediaMetadata.extras?.getString("providerType") ?: ""),
                            albumArtistName = it.mediaMetadata.artist.toString(),
                            albumId = it.mediaMetadata.extras?.getString("albumId") ?: "",
                            artistName = it.mediaMetadata.artist.toString(),
                            discNumber = it.mediaMetadata.discNumber ?: 0,
                            durationMs = it.mediaMetadata.durationMs?.toInt() ?: 0,
                            name = it.mediaMetadata.title.toString(),
                            trackNumber = it.mediaMetadata.trackNumber ?: 0,
                            userFavorite = it.mediaMetadata.extras?.getBoolean("userFavorite") ?: false,
                        )
                        song
                    })
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

    val sortAlbum: Flow<AlbumListSort> =
        context.dataStore.data.map { preferences ->
            AlbumListSort.entries.find { it.name == preferences[SORT_ALBUM] } ?: AlbumListSort.NAME
        }

    val sortAlbumOrder: Flow<SortOrder> =
        context.dataStore.data.map { preferences ->
            SortOrder.entries.find { it.name == preferences[SORT_ALBUM_ORDER] } ?: SortOrder.ASC
        }

    val showFavoriteAlbum: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[SHOW_FAVORITES_ALBUM] ?: false
        }

    val sortArtist: Flow<AlbumArtistListSort> =
        context.dataStore.data.map { preferences ->
            AlbumArtistListSort.entries.find { it.name == preferences[SORT_ARTIST] } ?: AlbumArtistListSort.NAME
        }

    val sortArtistOrder: Flow<SortOrder> =
        context.dataStore.data.map { preferences ->
            SortOrder.entries.find { it.name == preferences[SORT_ARTIST_ORDER] } ?: SortOrder.ASC
        }

    val showFavoriteArtist: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[SHOW_FAVORITES_ARTIST] ?: false
        }

    val sortSong: Flow<SongListSort> =
        context.dataStore.data.map { preferences ->
            SongListSort.entries.find { it.name == preferences[SORT_SONG] } ?: SongListSort.NAME
        }

    val sortSongOrder: Flow<SortOrder> =
        context.dataStore.data.map { preferences ->
            SortOrder.entries.find { it.name == preferences[SORT_SONG_ORDER] } ?: SortOrder.ASC
        }

    val showFavoriteSong: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[SHOW_FAVORITES_SONG] ?: false
        }

    suspend fun saveSortAlbum(sort: AlbumListSort) {
        withContext(NonCancellable) {
            context.dataStore.edit { preferences ->
                preferences[SORT_ALBUM] = sort.name
            }
        }
    }
    suspend fun saveSortAlbumOrder(sortOrder: SortOrder) {
        withContext(NonCancellable) {
            context.dataStore.edit { preferences ->
                preferences[SORT_ALBUM_ORDER] = sortOrder.name
            }
        }
    }

    suspend fun saveShowFavoriteAlbum(showFavorites: Boolean) {
        withContext(NonCancellable) {
            context.dataStore.edit { preferences ->
                preferences[SHOW_FAVORITES_ALBUM] = showFavorites;
            }
        }
    }

    suspend fun saveSortArtist(sort: AlbumArtistListSort) {
        withContext(NonCancellable) {
            context.dataStore.edit { preferences ->
                preferences[SORT_ARTIST] = sort.name
            }
        }
    }
    suspend fun saveSortArtistOrder(sortOrder: SortOrder) {
        withContext(NonCancellable) {
            context.dataStore.edit { preferences ->
                preferences[SORT_ARTIST_ORDER] = sortOrder.name
            }
        }
    }

    suspend fun saveShowFavoriteArtist(showFavorites: Boolean) {
        withContext(NonCancellable) {
            context.dataStore.edit { preferences ->
                preferences[SHOW_FAVORITES_ARTIST] = showFavorites;
            }
        }
    }

    suspend fun saveSortSong(sort: SongListSort) {
        withContext(NonCancellable) {
            context.dataStore.edit { preferences ->
                preferences[SORT_SONG] = sort.name
            }
        }
    }
    suspend fun saveSortSongOrder(sortOrder: SortOrder) {
        withContext(NonCancellable) {
            context.dataStore.edit { preferences ->
                preferences[SORT_SONG_ORDER] = sortOrder.name
            }
        }
    }

    suspend fun saveShowFavoriteSong(showFavorites: Boolean) {
        withContext(NonCancellable) {
            context.dataStore.edit { preferences ->
                preferences[SHOW_FAVORITES_SONG] = showFavorites;
            }
        }
    }
}