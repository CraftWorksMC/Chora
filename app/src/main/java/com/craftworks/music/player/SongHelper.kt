@file:OptIn(UnstableApi::class) package com.craftworks.music.player

import androidx.annotation.OptIn
import androidx.compose.runtime.mutableIntStateOf
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController

class SongHelper {
    companion object{
        var currentTracklist = mutableListOf<MediaItem>()

        fun play(mediaItems: List<MediaItem>, index: Int, mediaController: MediaController?) {
            currentTracklist = mediaItems.toMutableList()
            mediaController?.setMediaItems(currentTracklist, index, 0)
            mediaController?.prepare()
            mediaController?.play()
        }
    }
}