@file:OptIn(UnstableApi::class) package com.craftworks.music

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.craftworks.music.auto.AutoMediaLibraryService
import com.craftworks.music.data.Song
import com.craftworks.music.data.tracklist
import com.craftworks.music.lyrics.getLyrics

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

        var minPercentageScrobble = mutableIntStateOf(75)

        fun playStream(context: Context, url: Uri, isRadio: Boolean ? = false, mediaController: MediaController? = null) {
            if (mediaController?.isPlaying == true){
                mediaController.stop()
            }

            //mediaController?.clearMediaItems()

            // Start From 0
            sliderPos.intValue = 0

            val index = tracklist.indexOfFirst { it.mediaId == url.toString() }

            mediaController?.setMediaItems(tracklist, index, 0)

            AutoMediaLibraryService().addMediaItems()

            //mediaController?.seekToDefaultPosition(index)
            mediaController?.prepare()
            mediaController?.play()

            println("Index: $index, playlist size: ${mediaController?.mediaItemCount}, timeline items: ${mediaController?.currentTimeline?.periodCount}")

            if (isRadio == false)
                getLyrics()
        }

        fun updateCurrentPos(){
            sliderPos.intValue = AutoMediaLibraryService().player.currentPosition.toInt()
        }
    }
}