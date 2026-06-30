package com.craftworks.music.data.repository

import androidx.media3.common.MediaItem
import com.craftworks.music.data.model.MediaQuery
import com.craftworks.music.managers.MediaProviderManager
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Singleton

@Singleton
class PlaylistRepository {

    suspend fun getPlaylists(query: MediaQuery.PlaylistListQuery): List<MediaItem> = coroutineScope {
        MediaProviderManager.currentProvider.value?.getPlaylistList(query)?.map { it.toMediaItem() } ?: listOf()
    }

    suspend fun getPlaylistSongs(playlistId: String): List<MediaItem> = coroutineScope {
        MediaProviderManager.currentProvider.value?.getPlaylistSongList(playlistId)?.map { it.toMediaItem() } ?: listOf()
    }

    suspend fun createPlaylist(name: String, comment: String = "", songsToAdd: List<String>, public: Boolean) {
        val playlistId = MediaProviderManager.currentProvider.value?.createPlaylist(name, comment, public = public)
        if (playlistId != null) addSongsToPlaylist(playlistId, songsToAdd)
    }

    suspend fun addSongsToPlaylist(playlistId: String, songsToAdd: List<String>) {
        MediaProviderManager.currentProvider.value?.addToPlaylist(playlistId, songsToAdd)
    }

    suspend fun removeSongsFromPlaylist(playlistId: String, songsToRemove: List<String>) {
        MediaProviderManager.currentProvider.value?.removeFromPlaylist(playlistId,songsToRemove)
    }

    suspend fun deletePlaylist(playlistId: String) {
        MediaProviderManager.currentProvider.value?.deletePlaylist(playlistId)
    }
}
