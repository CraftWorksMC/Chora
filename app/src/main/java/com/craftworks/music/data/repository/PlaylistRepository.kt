package com.craftworks.music.data.repository

import android.util.Log
import androidx.media3.common.MediaItem
import com.craftworks.music.data.datasource.local.LocalDataSource
import com.craftworks.music.data.datasource.navidrome.NavidromeDataSource
import com.craftworks.music.managers.NavidromeManager
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val navidromeDataSource: NavidromeDataSource
) {

    suspend fun getPlaylists(ignoreCachedResponse: Boolean = false): List<MediaItem> = supervisorScope {
        val deferredPlaylists = mutableListOf<Deferred<List<MediaItem>>>()

        if (NavidromeManager.checkActiveServers())
            deferredPlaylists.add(async {
                try {
                    navidromeDataSource.getNavidromePlaylists(ignoreCachedResponse)
                } catch (e: Exception) {
                    Log.e("PlaylistRepository", "Failed to fetch Navidrome playlists", e)
                    emptyList()
                }
            })

        //if (LocalProviderManager.checkActiveFolders())
        deferredPlaylists.add(async {
            try {
                localDataSource.getLocalPlaylists()
            } catch (e: Exception) {
                Log.e("PlaylistRepository", "Failed to fetch local playlists", e)
                emptyList()
            }
        })

        deferredPlaylists.awaitAll().flatten()
    }

    suspend fun getPlaylistSongs(playlistId: String, ignoreCachedResponse: Boolean = false): List<MediaItem> = supervisorScope {
        if (playlistId.startsWith("Local_")){
            async { localDataSource.getLocalPlaylistSongs(playlistId) }.await()
        } else {
            async { navidromeDataSource.getNavidromePlaylist(playlistId, ignoreCachedResponse) ?: emptyList() }.await()
        }
    }

    suspend fun createPlaylist(name: String, songsToAdd: String, addToNavidrome: Boolean) {
        if (NavidromeManager.checkActiveServers() && addToNavidrome) {
            navidromeDataSource.createNavidromePlaylist(name, listOf(songsToAdd), true)
        }
        else {
            localDataSource.createLocalPlaylist(name, songsToAdd)
        }
    }

    suspend fun addSongToPlaylist(playlistId: String, songID: String) {
        if (playlistId.startsWith("Local_")){
            localDataSource.addSongToLocalPlaylist(playlistId, songID)
        } else {
            navidromeDataSource.addSongToNavidromePlaylist(playlistId, songID, true)
        }
    }

    suspend fun deletePlaylist(playlistId: String) {
        if (playlistId.startsWith("Local_")){
            localDataSource.deleteLocalPlaylist(playlistId)
        } else {
            navidromeDataSource.deleteNavidromePlaylist(playlistId, true)
        }
    }
}
