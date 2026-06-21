@file:OptIn(UnstableApi::class) package com.craftworks.music.player

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.craftworks.music.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SongHelper {
    companion object{
        suspend fun play(mediaItems: List<MediaItem.Song>, index: Int, mediaController: MediaController?) {
            if (mediaItems.isEmpty())
                return

            withContext(Dispatchers.Main) {
                mediaController?.setMediaItems(mediaItems.map { it.toMediaItem() }, index, 0)
                mediaController?.prepare()
                mediaController?.play()
            }
        }
    }
}