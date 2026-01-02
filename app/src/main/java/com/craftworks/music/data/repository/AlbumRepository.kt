package com.craftworks.music.data.repository

import android.util.Log
import androidx.media3.common.MediaItem
import com.craftworks.music.data.database.dao.AlbumDao
import com.craftworks.music.data.database.dao.SongDao
import com.craftworks.music.data.database.entity.toMediaDataAlbum
import com.craftworks.music.data.database.entity.toMediaDataSong
import com.craftworks.music.data.datasource.local.LocalDataSource
import com.craftworks.music.data.datasource.navidrome.NavidromeDataSource
import com.craftworks.music.data.model.toMediaItem
import com.craftworks.music.managers.LocalProviderManager
import com.craftworks.music.managers.NavidromeManager
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlbumRepository @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val navidromeDataSource: NavidromeDataSource,
    private val albumDao: AlbumDao,
    private val songDao: SongDao
) {
    suspend fun getAlbums(
        sort: String? = "alphabeticalByName",
        size: Int? = 100,
        offset: Int? = 0,
        ignoreCachedResponse: Boolean = false
    ): List<MediaItem> = supervisorScope {
        val deferredAlbums = mutableListOf<Deferred<List<MediaItem>>>()

        if (NavidromeManager.checkActiveServers())
            deferredAlbums.add(async {
                try {
                    navidromeDataSource.getNavidromeAlbums(sort, size, offset, ignoreCachedResponse)
                } catch (e: Exception) {
                    Log.e("AlbumRepository", "Failed to fetch Navidrome albums", e)
                    emptyList()
                }
            })

        if (LocalProviderManager.checkActiveFolders())
            if (offset == 0)
                deferredAlbums.add(async {
                    try {
                        localDataSource.getLocalAlbums(sort)
                    } catch (e: Exception) {
                        Log.e("AlbumRepository", "Failed to fetch local albums", e)
                        emptyList()
                    }
                })


        deferredAlbums.awaitAll().flatten()
    }

    suspend fun getAlbum(albumId: String, ignoreCachedResponse: Boolean = false): List<MediaItem>? = coroutineScope {
        if (albumId.startsWith("Local_")) {
            localDataSource.getLocalAlbum(albumId)
        } else {
            // Cache-first strategy: check Room database first
            if (!ignoreCachedResponse) {
                val cachedSongs = songDao.getSongsByAlbumOnce(albumId)
                if (cachedSongs.isNotEmpty()) {
                    val album = albumDao.getAlbumById(albumId)
                    return@coroutineScope listOfNotNull(album?.toMediaDataAlbum()?.toMediaItem()) +
                        cachedSongs.map { it.toMediaDataSong().toMediaItem() }
                }
            }
            navidromeDataSource.getNavidromeAlbum(albumId, ignoreCachedResponse)
        }
    }

    suspend fun searchAlbum(query: String): List<MediaItem> = supervisorScope {
        val deferredAlbums = mutableListOf<Deferred<List<MediaItem>>>()

        if (LocalProviderManager.checkActiveFolders())
            deferredAlbums.add(async {
                try {
                    localDataSource.searchLocalAlbums(query)
                } catch (e: Exception) {
                    Log.e("AlbumRepository", "Failed to search local albums", e)
                    emptyList()
                }
            })

        if (NavidromeManager.checkActiveServers())
            deferredAlbums.add(async {
                try {
                    navidromeDataSource.searchNavidromeAlbums(query)
                } catch (e: Exception) {
                    Log.e("AlbumRepository", "Failed to search Navidrome albums", e)
                    emptyList()
                }
            })

        deferredAlbums.awaitAll().flatten()
    }
}