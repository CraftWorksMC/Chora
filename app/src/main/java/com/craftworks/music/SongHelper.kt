@file:OptIn(UnstableApi::class) package com.craftworks.music

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.core.app.NotificationCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
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

            //Load Settings (Once)!
            saveManager(context).loadSettings()
        }
        @Composable
        fun PlayStream(context: Context, url: Uri) {
            // Stop If It's Playing
            if (player.isPlaying){
                player.stop()
                player.clearMediaItems()
            }
                for (song in playingSong.selectedList){
                    val mediaItem = song.media?.let { MediaItem.fromUri(it) }
                    if (mediaItem != null) {
                        player.addMediaItem(mediaItem)
                    }
                }
            val index = playingSong.selectedList.indexOfFirst { it.media == url }
            println("${player.currentMediaItem?.localConfiguration?.uri} ; ${playingSong.selectedList.indexOfFirst { it.media == player.currentMediaItem?.localConfiguration?.uri }} ; ${playingSong.selectedList.size}")

            mediaSession.player = player

            // Initialize Player With Media
            //val mediaItem = MediaItem.fromUri(url)

            //player.setMediaItem(mediaItem)
            player.prepare()
            player.seekTo(index, currentPosition)
            //player.seekTo(currentPosition)
            player.playWhenReady = true

            try {
                //playingSong.selectedSong = playingSong.selectedList[playingSong.selectedList.indexOfFirst { it.title == player.mediaMetadata.title && it.artist == player.mediaMetadata.artist }]
            }
            catch (e: java.lang.IndexOutOfBoundsException){
                println("$e !!!")
            }

            // Set WakeLock
            if (useNavidromeServer.value){
                player.setWakeMode(C.WAKE_MODE_NETWORK)
            }
            else {
                player.setWakeMode(C.WAKE_MODE_LOCAL)
            }

            // Add OnComplete Listener
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        onPlayerComplete()
                    }

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

                    try {
                        //playingSong.selectedSong = playingSong.selectedList[playingSong.selectedList.indexOfFirst { it.title == player.mediaMetadata.title && it.artist == player.mediaMetadata.artist }]
                    }
                    catch (e: java.lang.IndexOutOfBoundsException){
                        println("$e !!!")
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
            if (!player.isLoading)
                sliderPos.intValue = player.currentPosition.toInt()

            currentPosition = sliderPos.intValue.toLong()
        }

        private fun onPlayerComplete(){
            if (abs(sliderPos.intValue - playingSong.selectedSong?.duration!!) > 1000 || playingSong.selectedSong?.isRadio == true) return
            playingSong.selectedSong?.let { nextSong(it)}
        }


        class NotificationActionReceiver : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if ("ACTION_SKIP_FORWARD" == intent.action) {
                    playingSong.selectedSong?.let { nextSong(it)}
                }
            }
        }

    }
}