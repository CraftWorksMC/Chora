@file:OptIn(UnstableApi::class) package com.craftworks.music.player

import androidx.annotation.OptIn
import androidx.compose.runtime.mutableIntStateOf
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SongHelper {
    companion object{
        var currentTracklist = mutableListOf<MediaItem>()

        suspend fun play(mediaItems: List<MediaItem>, index: Int, mediaController: MediaController?) {
            if (mediaItems.isEmpty())
                return

            currentTracklist = mediaItems.toMutableList()
            withContext(Dispatchers.Main) {
                mediaController?.setMediaItems(mediaItems, index, 0)
                mediaController?.prepare()
                mediaController?.play()
            }
        }
    }
}