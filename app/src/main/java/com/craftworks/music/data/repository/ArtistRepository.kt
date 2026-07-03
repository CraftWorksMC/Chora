package com.craftworks.music.data.repository

import androidx.media3.common.MediaItem
import com.craftworks.music.data.model.AlbumListSort
import com.craftworks.music.data.model.MediaModel
import com.craftworks.music.data.model.MediaQuery
import com.craftworks.music.data.model.SortOrder
import com.craftworks.music.managers.MediaProviderManager
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArtistRepository @Inject constructor() {

    suspend fun getArtists(
        query: MediaQuery.AlbumArtistListQuery
    ): List<MediaModel.Artist> = coroutineScope {
        MediaProviderManager.currentProvider.value?.getAlbumArtistList(query) ?: listOf()
    }

    suspend fun getArtistAlbums(artistId: String): List<MediaItem> = coroutineScope {
        MediaProviderManager.currentProvider.value?.getAlbumList(MediaQuery.AlbumListQuery(
            AlbumListSort.NAME,
            SortOrder.ASC,
            startIndex = 0,
            artistIds = listOf(artistId)
        ))?.map { it.toMediaItem() } ?: listOf()
    }

    suspend fun getArtistDetail(artistId: String): MediaModel.Artist? = coroutineScope {
        MediaProviderManager.currentProvider.value?.getAlbumArtistDetail(artistId)
    }
}
