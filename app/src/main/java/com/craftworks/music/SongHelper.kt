@file:OptIn(UnstableApi::class) package com.craftworks.music

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.craftworks.music.data.Song
import com.craftworks.music.lyrics.SyncedLyric
import com.craftworks.music.lyrics.getLyrics
import com.craftworks.music.lyrics.songLyrics
import com.craftworks.music.providers.navidrome.markSongAsPlayed
import com.craftworks.music.providers.navidrome.useNavidromeServer
import com.craftworks.music.ui.bitmap
import kotlin.math.abs

class SongHelper {
    companion object{
        var isSeeking = false
        lateinit var player: ExoPlayer
        private lateinit var mediaSession: MediaSession

        private lateinit var notification: Notification
        private lateinit var notificationManager: NotificationManager

        var currentPosition: Long = 0
        var currentDuration: Long = 0
        var currentSong by mutableStateOf<Song>(
            Song(
                title = "",
                artist = "",
                duration = 0,
                imageUrl = Uri.EMPTY,
                dateAdded = "",
                year = "",
                album = ""))

        fun initPlayer(context: Context) {
            // Do NOT Re-Initialize Player and MediaSession
            // Because this function gets called when re-focusing the app
            if (this::player.isInitialized || this::mediaSession.isInitialized) return

            player = ExoPlayer.Builder(context).build()
            mediaSession = MediaSession.Builder(context, player).build()

            player.shuffleModeEnabled = false
            player.repeatMode = Player.REPEAT_MODE_OFF

            // Create a Notification Channel
            val channel = NotificationChannel(
                "Chora",
                "Chora Notifications",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            saveManager(context).loadSettings()
        }

        fun playStream(context: Context, url: Uri) {
            // Stop If It's Playing
            if (player.isPlaying){
                player.stop()
                player.clearMediaItems()
            }

            val index = playingSong.selectedList.indexOfFirst { it.media == url }

            for (song in playingSong.selectedList){
                if (song.isRadio == true) break

                val mediaMetadata = MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .setArtworkUri(song.imageUrl)
                    .setReleaseYear(song.year?.toIntOrNull() ?: 0)
                    .setSubtitle("${song.format} • ${song.bitrate}") // Hack for more info
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
            player.pauseAtEndOfMediaItems = true
            player.shuffleModeEnabled = false
            player.playWhenReady = true

            // Set WakeLock
            if (useNavidromeServer.value){
                player.setWakeMode(C.WAKE_MODE_NETWORK)
            }
            else {
                player.setWakeMode(C.WAKE_MODE_LOCAL)
            }

            // Get First Song Lyrics
            songLyrics.SongLyrics = "Getting Lyrics... \n No Lyrics Found"
            SyncedLyric.clear()
            getLyrics()

            // On State Changed:
            //  - Update Notification
            //  - Get Lyrics If Song Changed
            //  - Next Song On End
            player.addListener(object : Player.Listener {
                override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                    super.onMediaMetadataChanged(mediaMetadata)
                    currentSong = Song(
                        title = player.mediaMetadata.title.toString(),
                        artist = player.mediaMetadata.artist.toString(),
                        duration = player.duration.toInt(),
                        imageUrl = Uri.parse(mediaSession.player.mediaMetadata.artworkUri.toString()),
                        year = player.mediaMetadata.releaseYear.toString(),
                        album = player.mediaMetadata.albumTitle.toString(),
                        format = player.mediaMetadata.subtitle.toString()
                    )
                    if (player.duration > 0)
                        currentDuration = player.duration

                    // this will do until i finish it
                    playingSong.selectedSong = currentSong

                    println("Player Changed Song!")
                    songLyrics.SongLyrics = "Getting Lyrics... \n No Lyrics Found"
                    SyncedLyric.clear()
                    getLyrics()
                }
                override fun onPlaybackStateChanged(state: Int) {
                    // Update Notification
                    notification = NotificationCompat.Builder(context, "Chora")
                        .setSmallIcon(R.drawable.ic_notification_icon)
                        .setContentTitle(player.mediaMetadata.title)
                        .setContentText(player.mediaMetadata.artist)
                        .setLargeIcon(bitmap.value)
                        .setStyle(androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mediaSession.sessionCompatToken))
                        //.setOngoing(true) // Don't dismiss it
                        .setContentIntent(PendingIntent.getActivity( // Open app on notification click
                            context,
                            0,
                            Intent(context.applicationContext,MainActivity::class.java),
                            PendingIntent.FLAG_IMMUTABLE))
                        .build()
                    notificationManager.notify(2, notification)

                    if (state == Player.STATE_ENDED){
                        onPlayerComplete()
                    }
                }
            })
        }

        fun pauseStream(){
            player.let {
                currentPosition = it.currentPosition
                it.playWhenReady = false
                it.pause()
            }
        }


        fun stopStream(){
            player.stop()
            player.clearMediaItems()
            currentPosition = 0
        }

        /*
        fun releasePlayer(){
            player.release()
            mediaSession.release()
        }
        */

        fun previousSong(song: Song){
            player.seekToPreviousMediaItem()
            sliderPos.intValue = 0
            markSongAsPlayed(song)
        }

        fun nextSong(song: Song){
            player.seekToNextMediaItem()
            sliderPos.intValue = 0
            markSongAsPlayed(song)
        }

        fun updateCurrentPos(){
            sliderPos.intValue = player.currentPosition.toInt()
            currentPosition = sliderPos.intValue.toLong()
        }

        private fun onPlayerComplete(){
            if (abs(sliderPos.intValue - currentDuration) > 1000 || currentSong.isRadio == true) return
            nextSong(currentSong)
        }
    }
}