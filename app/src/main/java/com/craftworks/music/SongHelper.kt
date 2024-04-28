@file:OptIn(UnstableApi::class) package com.craftworks.music

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.session.MediaController
import com.craftworks.music.auto.AutoMediaLibraryService
import com.craftworks.music.data.Song
import com.craftworks.music.lyrics.getLyrics

class SongHelper {
    companion object{
        var isSeeking = false
        lateinit var player: ExoPlayer

        //private lateinit var notification: Notification
        //private lateinit var notificationManager: NotificationManager


        var currentPosition: Long = 0
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

        fun initPlayer(context: Context) {
            // Do NOT Re-Initialize Player and MediaSession
            // Because this function gets called when re-focusing the app
            //if (this::player.isInitialized) return

            player = ExoPlayer.Builder(context)
                .setSeekParameters(SeekParameters.CLOSEST_SYNC)
                .setRenderersFactory(
                    DefaultRenderersFactory(context).setExtensionRendererMode(
                        DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER /* We prefer extensions, such as FFmpeg */
                    ))
                .setHandleAudioBecomingNoisy(true)
                .setAudioAttributes(AudioAttributes.DEFAULT, true)
                .build()

            Log.setLogLevel(Log.LOG_LEVEL_ERROR)

            //saveManager(context).loadSettings()

        }

        fun playStream(context: Context, url: Uri, isRadio: Boolean ? = false, mediaController: MediaController? = null) {
            if (mediaController?.isPlaying == true){
                mediaController.stop()
            }

            // Start From 0
            sliderPos.intValue = 0
            currentPosition = 0

            /*
            if (isRadio == false){
                // Add Media Items
                player.clearMediaItems()
                val index = currentList.indexOfFirst { it.media == url }
                for (song in currentList){
                    if (song.isRadio == true) break

                    val mediaMetadata = MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .setArtworkUri(song.imageUrl)
                        .setReleaseYear(song.year?.toIntOrNull() ?: 0)
                        .setExtras(Bundle().apply {
                            putInt("duration", song.duration)
                            putString("MoreInfo", "${song.format} • ${song.bitrate}")
                            putString("NavidromeID", song.navidromeID)
                            putBoolean("isRadio", false)
                        })
                        .build()

                    val mediaItem = MediaItem.Builder()
                        .setUri(song.media)
                        .setMediaMetadata(mediaMetadata)
                        .setMimeType(song.format)
                        .build()

                    player.addMediaItem(mediaItem)
                }

                mediaSession.player = player

                player.prepare()
                player.seekTo(index, currentPosition)
            }
            else {
                player.clearMediaItems()

                val mediaMetadata = MediaMetadata.Builder()
                    .setArtist(currentSong.artist)
                    .setReleaseYear(Calendar.getInstance().get(Calendar.YEAR))
                    .setArtworkUri(currentSong.imageUrl)
                    .setExtras(Bundle().apply {
                        putString("MoreInfo", "${currentSong.format} • ${currentSong.bitrate}")
                        putBoolean("isRadio", true)
                    })
                    .build()

                val mediaItem = MediaItem.Builder()
                    .setUri(url)
                    .setMediaMetadata(mediaMetadata)
                    .build()

                player.setMediaItem(mediaItem)
                player.prepare()
                player.seekTo(0)
            }
            */

            val index = currentList.indexOfFirst { it.media == url }
            val mediaItems = mutableListOf<MediaItem>()
//            mediaItems.clear()
//            mediaController?.clearMediaItems()
//
//            for (song in currentList) {
//                val mediaMetadata = MediaMetadata.Builder()
//                    .setTitle(song.title)
//                    .setArtist(song.artist)
//                    .setAlbumTitle(song.album)
//                    .setArtworkUri(song.imageUrl)
//                    .setReleaseYear(song.year?.toIntOrNull() ?: 0)
//                    .setExtras(Bundle().apply {
//                        putInt("duration", song.duration)
//                        putString("MoreInfo", "${song.format} • ${song.bitrate}")
//                        putString("NavidromeID", song.navidromeID)
//                        putBoolean("isRadio", false)
//                    })
//                    .setIsBrowsable(false)
//                    .setIsPlayable(true)
//                    .build()
//                mediaItems.add(
//                    MediaItem.fromUri(song.media.toString())
//                    .buildUpon()
//                    .setMediaId(song.media.toString())
//                    .setMediaMetadata(mediaMetadata).build()
//                )
//            }
//
//            mediaController?.setMediaItems(mediaItems)
            mediaController?.setMediaItems(AutoMediaLibraryService().addMediaItems(true), index, 0)
            println("Index: $index, playlist size: ${mediaController?.mediaItemCount}")
            mediaController?.prepare()
            mediaController?.playWhenReady = true

            //mediaController?.seekToDefaultPosition(index)
            println("Started Playback hopefully.")

            // Set WakeLock
//            if (useNavidromeServer.value){
//                AutoMediaLibraryService().player.setWakeMode(C.WAKE_MODE_NETWORK)
//            }
//            else {
//                AutoMediaLibraryService().player.setWakeMode(C.WAKE_MODE_LOCAL)
//            }

            if (isRadio == false)
                getLyrics()

            //updateNotification(context)
        }

        fun pauseStream(){
            player.let {
                currentPosition = it.currentPosition
                it.playWhenReady = false
                it.pause()
            }
        }

        fun updateCurrentPos(){
            sliderPos.intValue = player.currentPosition.toInt()
            currentPosition = sliderPos.intValue.toLong()
        }
    }
}