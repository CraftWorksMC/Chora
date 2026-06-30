package com.craftworks.music.data.repository

import androidx.media3.common.MediaItem
import com.craftworks.music.data.model.MediaQuery
import com.craftworks.music.managers.MediaProviderManager
import kotlinx.coroutines.coroutineScope
import javax.inject.Singleton

@Singleton
class AlbumRepository {
    suspend fun getAlbums(
        query: MediaQuery.AlbumListQuery
    ): List<MediaItem> = coroutineScope {
        MediaProviderManager.currentProvider.value?.getAlbumList(query)?.map { it.toMediaItem() } ?: listOf()
    }

    suspend fun getAlbum(albumId: String): List<MediaItem>? = coroutineScope {
        val album = MediaProviderManager.currentProvider.value?.getAlbumDetail(albumId)
        if (album != null) listOf(album.toMediaItem()) + (album.songs?.map { it.toMediaItem() } ?: listOf()) else null
    }
}