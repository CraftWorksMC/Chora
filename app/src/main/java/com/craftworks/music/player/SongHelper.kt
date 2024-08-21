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
import com.craftworks.music.data.MediaData
import com.craftworks.music.sliderPos

class SongHelper {
    companion object{
        var isSeeking = false

        var currentSong by mutableStateOf(
            MediaData.Song(
                "id",
                "parent",
                false,
                "",
                "",
                "",
                0,
                0,
                "",
                "",
                0,
                "contentType",
                "suffix",
                0,
                0,
                "path",
                0,
                0,
                "",
                "",
                "",
                "type",
                false,
                "",
                0,
                "",
                "",
                "mediaType",
                "",
                listOf(),
                null,
                2,
                0,
                false,
                "media",
            )
        )

        var currentList:List<MediaData.Song> = emptyList()
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
                    .setArtworkUri(Uri.parse(song.imageUrl))
                    .setReleaseYear(song.year)
                    .setExtras(Bundle().apply {
                        putInt("duration", song.duration)
                        //putString("MoreInfo", "${song.format} • ${song.bitrate}")
                        putString("NavidromeID", song.navidromeID)
                        putBoolean("isRadio", song.isRadio ?: false)
                        putString("format", song.format)
                        putInt("bitrate", song.bitrate ?: 0)
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

                mediaController?.setMediaItems(currentTracklist, currentTrackIndex, 0)
                mediaController?.seekToDefaultPosition(currentTrackIndex)
                mediaController?.prepare()
                mediaController?.play()

                println("Index: ${currentTrackIndex}, playlist size: ${mediaController?.mediaItemCount}")
            }
            else {
                val radioMetadata = MediaMetadata.Builder()
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .setTitle(currentSong.title)
                    .setArtist(currentSong.artist)
                    .setAlbumTitle(currentSong.album)
                    .setArtworkUri(Uri.parse(currentSong.imageUrl))
                    .setReleaseYear(currentSong.year)
                    .setExtras(Bundle().apply {
                        putInt("duration", currentSong.duration)
                        //putString("MoreInfo", "${currentSong.format} • ${currentSong.bitrate}")
                        putString("NavidromeID", currentSong.navidromeID)
                        putBoolean("isRadio", true)
                        putString("format", currentSong.format)
                        putInt("bitrate", currentSong.bitrate ?: 0)
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