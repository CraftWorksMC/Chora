package com.craftworks.music.data.repository

import androidx.media3.common.MediaItem
import com.craftworks.music.data.model.MediaQuery
import com.craftworks.music.data.model.ScrobbleEvent
import com.craftworks.music.managers.MediaProviderManager
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongRepository @Inject constructor() {

    suspend fun getSongs(query: MediaQuery.SongListQuery): List<MediaItem> = coroutineScope {
        MediaProviderManager.currentProvider.value?.getSongList(query)?.map { it.toMediaItem() } ?: listOf()
    }

    suspend fun getSong(songId: String): MediaItem? = coroutineScope {
        MediaProviderManager.currentProvider.value?.getSongDetail(songId)?.toMediaItem()
    }

    suspend fun getSimilarSongs(songId: String, count: Int) : List<MediaItem> = coroutineScope {
        MediaProviderManager.currentProvider.value?.getSimilarSongs(songId, count)?.map { it.toMediaItem() } ?: listOf()
    }

    suspend fun scrobbleSong(songId: String, position: Int, playbackRate: Float, event: ScrobbleEvent?, submission: Boolean) {
        MediaProviderManager.currentProvider.value?.scrobble(
            id=songId,
            position = position,
            playbackRate = playbackRate,
            event = event,
            submission = submission
        )
    }
}
