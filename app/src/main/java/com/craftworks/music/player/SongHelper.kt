@file:OptIn(UnstableApi::class) package com.craftworks.music.player

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SongHelper {
    companion object{
        suspend fun play(
            mediaItems: List<MediaItem>,
            index: Int,
            mediaController: MediaController?,
            shuffle: Boolean = false,
        ) {
            if (mediaItems.isEmpty())
                return

            withContext(Dispatchers.Main) {
                // Always set shuffle mode explicitly, so a previous Shuffle doesn't leak into a normal Play.
                mediaController?.shuffleModeEnabled = shuffle
                mediaController?.setMediaItems(mediaItems, index, 0)
                mediaController?.prepare()
                mediaController?.play()
            }
        }
        suspend fun shuffle(mediaItems: List<MediaItem>, mediaController: MediaController?) {
            if (mediaItems.isEmpty())
                return

            play(mediaItems, mediaItems.indices.random(), mediaController, shuffle = true)
        }
        fun enqueue(mediaItems: List<MediaItem>, mediaController: MediaController?) {
            mediaController?.addMediaItems(mediaItems)
        }
        fun playNext(mediaItems: List<MediaItem>, mediaController: MediaController?) {
            mediaController?.addMediaItems(
                mediaController.currentMediaItemIndex + 1,
                mediaItems
            )
        }
    }
}