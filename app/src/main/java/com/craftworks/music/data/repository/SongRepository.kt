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
class SongRepository @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val navidromeDataSource: NavidromeDataSource
) {

    suspend fun getSongs(
        query: String? = "",
        songCount: Int = 100, 
        songOffset: Int = 0,
        ignoreCachedResponse: Boolean = false
    ): List<MediaItem> = coroutineScope {
        val deferredSongs = mutableListOf<Deferred<List<MediaItem>>>()

        if (LocalProviderManager.checkActiveFolders())
            if (query.isNullOrEmpty() && songOffset == 0)
                deferredSongs.add(async { localDataSource.getLocalSongs() })

        if (NavidromeManager.checkActiveServers())
            deferredSongs.add(async {
                navidromeDataSource.getNavidromeSongs(query, songCount, songOffset, ignoreCachedResponse)
            })

        deferredSongs.awaitAll().flatten()
    }

    suspend fun getSong(songId: String, ignoreCachedResponse: Boolean = false): MediaItem? = coroutineScope {
        if (songId.startsWith("Local_"))
            localDataSource.getLocalSong(songId)
        else
            navidromeDataSource.getNavidromeSong(songId, ignoreCachedResponse)
    }

    suspend fun searchSongs(query: String, ignoreCachedResponse: Boolean = false): List<MediaItem> = coroutineScope {
        val deferredSongs = mutableListOf<Deferred<List<MediaItem>>>()

        if (LocalProviderManager.checkActiveFolders())
            deferredSongs.add(async { localDataSource.searchLocalSongs(query) })

        if (NavidromeManager.checkActiveServers())
            deferredSongs.add(async { navidromeDataSource.getNavidromeSongs(query, ignoreCachedResponse = ignoreCachedResponse) })

        deferredSongs.awaitAll().flatten()
    }

    suspend fun scrobbleSong(songId: String, submission: Boolean) {
        if (songId.startsWith("Local_"))
            return

        navidromeDataSource.scrobbleSong(songId, submission)
    }
}
