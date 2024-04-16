@file:OptIn(UnstableApi::class) package com.craftworks.music

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.craftworks.music.lyrics.getLyrics
import com.craftworks.music.providers.navidrome.markNavidromeSongAsPlayed
import com.craftworks.music.data.useNavidromeServer
import com.craftworks.music.ui.bitmap
import java.util.Calendar

class SongHelper {
    companion object{
        var isSeeking = false
        lateinit var player: ExoPlayer
        lateinit var mediaSession: MediaSession

        private lateinit var notification: Notification
        private lateinit var notificationManager: NotificationManager


        var currentPosition: Long = 0
        var currentSong by mutableStateOf<Song>(
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
            if (this::player.isInitialized || this::mediaSession.isInitialized) return

            player = ExoPlayer.Builder(context).build()
            mediaSession = MediaSession.Builder(context, player).build()

            player.availableCommands.buildUpon()
                .add(Player.COMMAND_SET_SHUFFLE_MODE)
                .add(Player.COMMAND_SET_REPEAT_MODE)
                .build()

            player.shuffleModeEnabled = false
            player.repeatMode = Player.REPEAT_MODE_OFF

            // Create a Notification Channel
            val channel = NotificationChannel(
                "Chora",
                "Media Controls",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.cancelAll()
            notificationManager.createNotificationChannel(channel)

            saveManager(context).loadSettings()
        }

        fun playStream(context: Context, url: Uri, isRadio: Boolean ? = false) {
            // Stop If It's Playing
            if (player.isPlaying){
                player.stop()
            }
            // Start From 0
            sliderPos.intValue = 0
            currentPosition = 0

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

            player.pauseAtEndOfMediaItems = false
            player.playWhenReady = true

            // Set WakeLock
            if (useNavidromeServer.value){
                player.setWakeMode(C.WAKE_MODE_NETWORK)
            }
            else {
                player.setWakeMode(C.WAKE_MODE_LOCAL)
            }

            if (isRadio == false)
                getLyrics()

            updateNotification(context)

            //region On State Changed:
            //  - Update Notification
            //  - Get Lyrics If Song Changed
            //  - Next Song On End
            player.addListener(object : Player.Listener {
                override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                    super.onMediaMetadataChanged(mediaMetadata)
                    currentSong = Song(
                        title = player.mediaMetadata.title.toString(),
                        artist = player.mediaMetadata.artist.toString(),
                        duration = player.mediaMetadata.extras?.getInt("duration") ?: 0,
                        imageUrl = Uri.parse(mediaSession.player.mediaMetadata.artworkUri.toString()),
                        year = player.mediaMetadata.releaseYear.toString(),
                        album = player.mediaMetadata.albumTitle.toString(),
                        format = player.mediaMetadata.extras?.getString("MoreInfo"),
                        navidromeID = player.mediaMetadata.extras?.getString("NavidromeID"),
                        isRadio = player.mediaMetadata.extras?.getBoolean("isRadio")
                    )
                    if (isRadio == false)
                        getLyrics()
                }

                override fun onPlaybackStateChanged(state: Int) {
                    super.onPlaybackStateChanged(state)
                    updateNotification(context)
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    super.onMediaItemTransition(mediaItem, reason)
                    println("Media Item Transition, marking song as played.")
                    markNavidromeSongAsPlayed(currentSong)
                    updateNotification(context)
                }
            })
            //endregion
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
            notificationManager.cancelAll()
            notificationManager.cancel(2)
            println("Released Player, Session and Notification! Bye")
        }

        fun previousSong(song: Song){
            player.seekToPreviousMediaItem()
            sliderPos.intValue = 0
            player.playWhenReady = true
        }

        fun nextSong(song: Song){
            player.seekToNextMediaItem()
            sliderPos.intValue = 0
            player.playWhenReady = true
        }

        fun updateCurrentPos(){
            sliderPos.intValue = player.currentPosition.toInt()
            currentPosition = sliderPos.intValue.toLong()
        }
        fun updateNotification(context: Context){
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
        }
    }
}