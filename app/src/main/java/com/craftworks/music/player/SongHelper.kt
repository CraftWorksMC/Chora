@file:OptIn(UnstableApi::class) package com.craftworks.music.player

import android.content.Context
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

        //private lateinit var notification: Notification
        //private lateinit var notificationManager: NotificationManager

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

        fun playStream(context: Context, url: Uri, isRadio: Boolean ? = false, mediaController: MediaController? = null) {

            if (currentTracklist.isEmpty()) return

            if (mediaController?.isPlaying == true){
                mediaController.stop()
            }

            //mediaController?.clearMediaItems()
            sliderPos.intValue = 0

            currentTrackIndex.intValue = currentTracklist.indexOfFirst { it.mediaId == url.toString() }
            currentSong = songsList[currentTrackIndex.intValue]

            mediaController?.setMediaItems(currentTracklist)

            ChoraMediaLibraryService().notifyNewSessionItems()

            mediaController?.prepare()
            mediaController?.playWhenReady = true

            println("Index: ${currentTrackIndex.intValue}, playlist size: ${mediaController?.mediaItemCount}")

            if (isRadio == false)
                getLyrics()
        }

        fun updateCurrentPos(mediaController: MediaController?){
            sliderPos.intValue = mediaController?.currentPosition?.toInt() ?: 0
        }
    }
}