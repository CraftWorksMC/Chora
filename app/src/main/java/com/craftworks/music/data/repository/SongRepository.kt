package com.craftworks.music.data.repository

import android.util.Log
import androidx.media3.common.MediaItem
import com.craftworks.music.data.datasource.local.LocalDataSource
import com.craftworks.music.data.datasource.navidrome.NavidromeDataSource
import com.craftworks.music.managers.LocalProviderManager
import com.craftworks.music.managers.NavidromeManager
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
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
    ): List<MediaItem> = supervisorScope {
        val deferredSongs = mutableListOf<Deferred<List<MediaItem>>>()

        if (LocalProviderManager.checkActiveFolders())
            if (query.isNullOrEmpty() && songOffset == 0)
                deferredSongs.add(async {
                    try {
                        localDataSource.getLocalSongs()
                    } catch (e: Exception) {
                        Log.e("SongRepository", "Failed to fetch local songs", e)
                        emptyList()
                    }
                })

        if (NavidromeManager.checkActiveServers())
            deferredSongs.add(async {
                try {
                    navidromeDataSource.getNavidromeSongs(query, songCount, songOffset, ignoreCachedResponse)
                } catch (e: Exception) {
                    Log.e("SongRepository", "Failed to fetch Navidrome songs", e)
                    emptyList()
                }
            })

        deferredSongs.awaitAll().flatten()
    }

    suspend fun getSong(songId: String, ignoreCachedResponse: Boolean = false): MediaItem? = supervisorScope {
        if (songId.startsWith("Local_"))
            async { localDataSource.getLocalSong(songId) }.await()
        else
            async { navidromeDataSource.getNavidromeSong(songId, ignoreCachedResponse) }.await()
    }

    suspend fun searchSongs(query: String, ignoreCachedResponse: Boolean = false): List<MediaItem> = supervisorScope {
        val deferredSongs = mutableListOf<Deferred<List<MediaItem>>>()

        if (LocalProviderManager.checkActiveFolders())
            deferredSongs.add(async {
                try {
                    localDataSource.searchLocalSongs(query)
                } catch (e: Exception) {
                    Log.e("SongRepository", "Failed to search local songs", e)
                    emptyList()
                }
            })

        if (NavidromeManager.checkActiveServers())
            deferredSongs.add(async {
                try {
                    navidromeDataSource.getNavidromeSongs(query, ignoreCachedResponse = ignoreCachedResponse)
                } catch (e: Exception) {
                    Log.e("SongRepository", "Failed to search Navidrome songs", e)
                    emptyList()
                }
            })

        deferredSongs.awaitAll().flatten()
    }

    suspend fun scrobbleSong(songId: String, submission: Boolean) {
        if (songId.startsWith("Local_"))
            return

        navidromeDataSource.scrobbleSong(songId, submission)
    }

    suspend fun getRandomSongs(
        size: Int = 50,
        ignoreCachedResponse: Boolean = true
    ): List<MediaItem> = supervisorScope {
        val deferredSongs = mutableListOf<Deferred<List<MediaItem>>>()

        if (LocalProviderManager.checkActiveFolders())
            deferredSongs.add(async {
                try {
                    localDataSource.getLocalSongs().shuffled().take(size / 2)
                } catch (e: Exception) {
                    Log.e("SongRepository", "Failed to get random local songs", e)
                    emptyList()
                }
            })

        if (NavidromeManager.checkActiveServers())
            deferredSongs.add(async {
                try {
                    navidromeDataSource.getRandomSongs(size, ignoreCachedResponse)
                } catch (e: Exception) {
                    Log.e("SongRepository", "Failed to get random Navidrome songs", e)
                    emptyList()
                }
            })

        deferredSongs.awaitAll().flatten().shuffled()
    }
}
