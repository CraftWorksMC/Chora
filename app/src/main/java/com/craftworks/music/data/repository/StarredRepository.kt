package com.craftworks.music.data.repository

import android.util.Log
import androidx.media3.common.MediaItem // MediaItem can represent songs, albums (via browse MediaId), etc.
import com.craftworks.music.data.database.dao.AlbumDao
import com.craftworks.music.data.database.dao.ArtistDao
import com.craftworks.music.data.database.dao.SongDao
import com.craftworks.music.data.datasource.local.LocalDataSource
import com.craftworks.music.data.datasource.navidrome.NavidromeDataSource
import com.craftworks.music.managers.LocalProviderManager
import com.craftworks.music.managers.NavidromeManager
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StarredRepository @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val navidromeDataSource: NavidromeDataSource,
    private val songDao: SongDao,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao
) {

    suspend fun getStarredItems(ignoreCachedResponse: Boolean = false): List<MediaItem> = supervisorScope {
        val deferredStarred = mutableListOf<Deferred<List<MediaItem>>>()

        if (NavidromeManager.checkActiveServers())
            deferredStarred.add(async {
                try {
                    navidromeDataSource.getNavidromeStarred(ignoreCachedResponse)
                } catch (e: Exception) {
                    Log.e("StarredRepository", "Failed to fetch Navidrome starred items", e)
                    emptyList()
                }
            })

        if (LocalProviderManager.checkActiveFolders())
            deferredStarred.add(async {
                try {
                    localDataSource.getLocalStarredItems()
                } catch (e: Exception) {
                    Log.e("StarredRepository", "Failed to fetch local starred items", e)
                    emptyList()
                }
            })

        deferredStarred.awaitAll().flatten()
    }

    suspend fun starItem(itemId: String, ignoreCachedResponse: Boolean) = withContext(Dispatchers.IO) {
        if (!itemId.startsWith("Local_")) {
            try {
                val success = navidromeDataSource.starNavidromeItem(itemId, ignoreCachedResponse)
                if (success) {
                    // Update local database for immediate UI feedback
                    val starredTimestamp = Instant.now().toString()
                    // Update all tables - only one will match, others are no-ops
                    songDao.updateStarred(itemId, starredTimestamp)
                    albumDao.updateStarred(itemId, starredTimestamp)
                    artistDao.updateStarred(itemId, starredTimestamp)
                }
            } catch (e: Exception) {
                Log.e("StarredRepository", "Failed to star item $itemId", e)
            }
        } else {
            localDataSource.starLocalItem(itemId)
        }
    }

    suspend fun unStarItem(itemId: String, ignoreCachedResponse: Boolean) = withContext(Dispatchers.IO) {
        if (!itemId.startsWith("Local_")) {
            try {
                val success = navidromeDataSource.unstarNavidromeItem(itemId, ignoreCachedResponse)
                if (success) {
                    // Update local database for immediate UI feedback
                    songDao.updateStarred(itemId, null)
                    albumDao.updateStarred(itemId, null)
                    artistDao.updateStarred(itemId, null)
                }
            } catch (e: Exception) {
                Log.e("StarredRepository", "Failed to unstar item $itemId", e)
            }
        } else {
            localDataSource.unstarLocalItem(itemId)
        }
    }
}
