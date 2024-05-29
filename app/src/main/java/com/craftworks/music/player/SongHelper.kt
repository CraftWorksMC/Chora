@file:OptIn(UnstableApi::class) package com.craftworks.music.player

import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.craftworks.music.data.Song
import com.craftworks.music.data.songsList
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
        var currentTracklist = mutableListOf<MediaItem>()

        var minPercentageScrobble = mutableIntStateOf(75)

        fun playStream(url: Uri, isRadio: Boolean ? = false, mediaController: MediaController? = null) {
            if (mediaController?.isConnected == false) return

            currentTracklist.clear()

            for (song in currentList){
                val mediaMetadata = MediaMetadata.Builder()
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .setArtworkUri(song.imageUrl)
                    .setReleaseYear(song.year?.toIntOrNull() ?: 0)
                    .setExtras(Bundle().apply {
                        putInt("duration", song.duration)
                        putString("MoreInfo", "${song.format} • ${song.bitrate}")
                        putString("NavidromeID", song.navidromeID)
                        putBoolean("isRadio", song.isRadio ?: false)
                    })
                    .build()
                val mediaItem = MediaItem.Builder()
                    .setMediaId(song.media.toString())
                    .setUri(song.media.toString())
                    .setMediaMetadata(mediaMetadata)
                    .build()

                currentTracklist.add(mediaItem)
            }

            sliderPos.intValue = 0

            if (isRadio == false){
                val currentTrackIndex = currentTracklist.indexOfFirst { it.mediaId == url.toString() }
                currentSong = songsList.sortedBy { it.title }[currentTrackIndex]

                mediaController?.setMediaItems(currentTracklist)
                mediaController?.seekToDefaultPosition(currentTrackIndex)
                mediaController?.prepare()
                mediaController?.play()

                println("Index: ${currentTrackIndex}, playlist size: ${mediaController?.mediaItemCount}")

                //getLyrics()
            }
            else {
                val radioMetadata = MediaMetadata.Builder()
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .setTitle(currentSong.title)
                    .setArtist(currentSong.artist)
                    .setAlbumTitle(currentSong.album)
                    .setArtworkUri(currentSong.imageUrl)
                    .setReleaseYear(currentSong.year?.toInt())
                    .setExtras(Bundle().apply {
                        putInt("duration", currentSong.duration)
                        putString("MoreInfo", "${currentSong.format} • ${currentSong.bitrate}")
                        putString("NavidromeID", currentSong.navidromeID)
                        putBoolean("isRadio", true)
                    })
                    .build()
                val radioItem = MediaItem.Builder()
                    .setUri(url)
                    .setMediaId(url.toString())
                    .setMediaMetadata(radioMetadata)
                    .build()

                currentTracklist = mutableListOf(radioItem)

                mediaController?.setMediaItem(radioItem)
                mediaController?.seekToDefaultPosition()
                mediaController?.prepare()
                mediaController?.play()

                println("listening to radio")
            }
        }
    }
}