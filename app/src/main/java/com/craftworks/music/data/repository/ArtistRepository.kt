package com.craftworks.music.data.repository

import androidx.media3.common.MediaItem
import com.craftworks.music.data.datasource.local.LocalDataSource
import com.craftworks.music.data.datasource.navidrome.NavidromeDataSource
import com.craftworks.music.data.model.MediaData
import com.craftworks.music.managers.LocalProviderManager
import com.craftworks.music.managers.NavidromeManager
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArtistRepository @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val navidromeDataSource: NavidromeDataSource
) {

    suspend fun getArtists(
        sort: String? = "alphabeticalByName",
        size: Int? = 100,
        offset: Int? = 0,
        ignoreCachedResponse: Boolean = false
    ): List<MediaData.Artist> = coroutineScope {
        val deferredArtists = mutableListOf<Deferred<List<MediaData.Artist>>>()

        if (LocalProviderManager.checkActiveFolders())
            if (offset == 0)
                deferredArtists.add(async { localDataSource.getLocalArtists() })

        if (NavidromeManager.checkActiveServers()) {
            deferredArtists.add(async { navidromeDataSource.getNavidromeArtists(ignoreCachedResponse) })
        }

        deferredArtists.awaitAll().flatten()
    }

    suspend fun searchArtists(
        query: String,
        ignoreCachedResponse: Boolean = false
    ): List<MediaData.Artist> = coroutineScope {
        val deferredArtists = mutableListOf<Deferred<List<MediaData.Artist>>>()

        if (LocalProviderManager.checkActiveFolders())
            deferredArtists.add(async { localDataSource.searchLocalArtists(query) })

        if (NavidromeManager.checkActiveServers())
            deferredArtists.add(async { navidromeDataSource.searchNavidromeArtists(query, ignoreCachedResponse) })

        deferredArtists.awaitAll().flatten()
    }

    suspend fun getArtistAlbums(artistId: String, ignoreCachedResponse: Boolean = false): List<MediaItem> = coroutineScope {
        if (artistId.startsWith("Local_"))
            localDataSource.getLocalArtistAlbums(artistId)
        else
            navidromeDataSource.getNavidromeArtistAlbums(artistId, ignoreCachedResponse)
    }

    suspend fun getArtistInfo(artistId: String, ignoreCachedResponse: Boolean = false): MediaData.ArtistInfo? = coroutineScope {
        if (artistId.startsWith("Local_"))
            null
        else
            navidromeDataSource.getNavidromeArtistInfo(artistId, ignoreCachedResponse)
    }
}
