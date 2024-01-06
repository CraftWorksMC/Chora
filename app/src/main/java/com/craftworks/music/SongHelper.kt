@file:OptIn(UnstableApi::class) package com.craftworks.music

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.C.WAKE_MODE_LOCAL
import androidx.media3.common.C.WAKE_MODE_NETWORK
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.craftworks.music.data.Song
import com.craftworks.music.lyrics.SyncedLyric
import com.craftworks.music.lyrics.getLyrics
import com.craftworks.music.lyrics.songLyrics
import com.craftworks.music.navidrome.markSongAsPlayed
import com.craftworks.music.ui.bitmap
import com.craftworks.music.ui.screens.useNavidromeServer
import kotlin.math.abs


class SongHelper {
    companion object{
        var isSeeking = false
        lateinit var player: ExoPlayer
        private lateinit var mediaSession: MediaSession

        private lateinit var notification: Notification
        private lateinit var notificationManager: NotificationManager

        var currentPosition: Long = 0
        fun initPlayer(context: Context){
            // Do NOT Re-Initialize Player and MediaSession
            // Because this function gets called when re-focusing the app
            if (this::player.isInitialized || this::mediaSession.isInitialized) return

            player = ExoPlayer.Builder(context).build()
            mediaSession = MediaSession.Builder(context, player).build()

            // FINALLY Implement the new Media Controls Notification

            // Create a Notification Channel
            val channel = NotificationChannel(
                    "Chora",
                    "Chora Notifications",
                    NotificationManager.IMPORTANCE_LOW
            )
            notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            /*  Do not create the notification when the app opens. [SUBJECT TO CHANGE]

            // Create a notification
            notification = NotificationCompat.Builder(context, "Chora")
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setContentTitle(playingSong.selectedSong?.title)
                .setContentText(playingSong.selectedSong?.artist)
                .setOngoing(true) // Don't dismiss it
                .setStyle(androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mediaSession.sessionCompatToken))
                .build()
            // Actually Create The Notification
            notificationManager.notify(2, notification)

            */
        }
        fun playStream(context: Context, url: Uri) {
            // Stop If It's Playing
            if (player.isPlaying){
                player.stop()
                player.clearMediaItems()
            }

            mediaSession.player = player

            // Initialize Player With Media
            val mediaItem = MediaItem.fromUri(url)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.seekTo(currentPosition)
            player.playWhenReady = true

            notification = NotificationCompat.Builder(context, "Chora")
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setContentTitle(mediaItem.mediaMetadata.title)
                .setContentText(mediaItem.mediaMetadata.artist)
                .setLargeIcon(bitmap.value)
                .setStyle(androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mediaSession.sessionCompatToken))
                .setOngoing(true) // Don't dismiss it
                .build()
            notificationManager.notify(2, notification)

            // Add OnComplete Listener
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        onPlayerComplete()
                    }
                }
            })

            // Set WakeLock
            if (useNavidromeServer.value){
                player.setWakeMode(WAKE_MODE_NETWORK)
            }
            else {
                player.setWakeMode(WAKE_MODE_LOCAL)
            }
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

        fun releasePlayer(){
            player.release()
            mediaSession.release()
        }

        fun previousSong(song: Song){
            val currentSongIndex = playingSong.selectedList.indexOfFirst{it.media == playingSong.selectedSong?.media}

            if (repeatSong.value){
                player.seekTo(0)
                player.play()
            }

            if (shuffleSongs.value && playingSong.selectedList.isNotEmpty()){
                playingSong.selectedSong = playingSong.selectedList[(0..playingSong.selectedList.size - 1).random()]
            }
            // Play Previous only if there is actually a song behind, and not shuffling or repeating.
            if ( (currentSongIndex - 1) >= 0
                && !repeatSong.value
                && !shuffleSongs.value){
                playingSong.selectedSong = playingSong.selectedList[currentSongIndex - 1]
            }
            stopStream()
            sliderPos.intValue = 0
            songLyrics.SongLyrics = "Getting Lyrics... \n No Lyrics Found"
            SyncedLyric.clear()
            getLyrics()
            markSongAsPlayed(song)
        }

        fun nextSong(song: Song){
            val currentSongIndex = playingSong.selectedList.indexOfFirst{it.media == playingSong.selectedSong?.media}

            if (repeatSong.value){
                player.seekTo(0)
                player.play()
            }
            if (shuffleSongs.value && playingSong.selectedList.isNotEmpty())
                playingSong.selectedSong = playingSong.selectedList[(0..playingSong.selectedList.size - 1).random()]
            if (currentSongIndex < playingSong.selectedList.size-1
                && !repeatSong.value
                && !shuffleSongs.value)
                playingSong.selectedSong = playingSong.selectedList[currentSongIndex + 1]
            stopStream()
            sliderPos.intValue = 0
            songLyrics.SongLyrics = "Getting Lyrics... \n No Lyrics Found"
            SyncedLyric.clear()
            getLyrics()
            markSongAsPlayed(song)
        }

        fun updateCurrentPos(){
            sliderPos.intValue = player.currentPosition.toInt()
            currentPosition = sliderPos.intValue.toLong()
        }

        private fun onPlayerComplete(){
            if (abs(sliderPos.intValue - playingSong.selectedSong?.duration!!) > 1000 || playingSong.selectedSong?.isRadio == true) return
            playingSong.selectedSong?.let { nextSong(it)}
        }
    }
}