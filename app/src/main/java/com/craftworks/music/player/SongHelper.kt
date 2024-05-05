@file:OptIn(UnstableApi::class) package com.craftworks.music.player

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.craftworks.music.data.Song
import com.craftworks.music.data.songsList
import com.craftworks.music.lyrics.getLyrics
import com.craftworks.music.sliderPos

class SongHelper {
    companion object{
        var isSeeking = false

        var currentSong by mutableStateOf(
            Song(
                title = "",
                artist = "",
                duration = 0,
                imageUrl = Uri.EMPTY,
                dateAdded = "",
                year = "",
                album = ""))

        var currentList:List<Song> = emptyList()
        var currentTracklist:List<MediaItem> = emptyList()

        var currentTrackIndex = mutableIntStateOf(0)

        var minPercentageScrobble = mutableIntStateOf(75)

        fun playStream(url: Uri, isRadio: Boolean ? = false, mediaController: MediaController? = null) {
            if (currentTracklist.isEmpty() || mediaController?.isConnected == false) return

            sliderPos.intValue = 0

            currentTrackIndex.intValue = currentTracklist.indexOfFirst { it.mediaId == url.toString() }
            currentSong = songsList.sortedBy { it.title }[currentTrackIndex.intValue]

            mediaController?.setMediaItems(currentTracklist, currentTrackIndex.intValue, 0)
            ChoraMediaLibraryService().notifyNewSessionItems()
            mediaController?.prepare()
            mediaController?.play()

            println("Index: ${currentTrackIndex.intValue}, playlist size: ${mediaController?.mediaItemCount}")

            if (isRadio == false)
                getLyrics()
        }
    }
}