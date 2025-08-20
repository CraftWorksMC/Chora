package com.craftworks.music.data.repository

import androidx.media3.common.MediaItem
import com.craftworks.music.data.datasource.local.LocalDataSource
import com.craftworks.music.data.datasource.navidrome.NavidromeDataSource
import com.craftworks.music.managers.LocalProviderManager
import com.craftworks.music.managers.NavidromeManager
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val navidromeDataSource: NavidromeDataSource
) {

    suspend fun getPlaylists(ignoreCachedResponse: Boolean = false): List<MediaItem> = coroutineScope {
        val deferredPlaylists = mutableListOf<Deferred<List<MediaItem>>>()

        if (NavidromeManager.checkActiveServers())
            deferredPlaylists.add(async { navidromeDataSource.getNavidromePlaylists(ignoreCachedResponse) })

        if (LocalProviderManager.checkActiveFolders())
            deferredPlaylists.add(async { localDataSource.getLocalPlaylists() })

        deferredPlaylists.awaitAll().flatten()
    }

    suspend fun getPlaylistSongs(playlistId: String, ignoreCachedResponse: Boolean = false): List<MediaItem> = coroutineScope {
        if (playlistId.startsWith("Local_")){
            localDataSource.getLocalPlaylistSongs(playlistId)
        } else {
            navidromeDataSource.getNavidromePlaylist(playlistId, ignoreCachedResponse) ?: emptyList()
        }
    }
}
