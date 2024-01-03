package com.craftworks.music

import android.content.Context
import android.content.Context.WIFI_SERVICE
import android.net.Uri
import android.net.wifi.WifiManager
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import com.craftworks.music.data.Song
import com.craftworks.music.lyrics.SyncedLyric
import com.craftworks.music.lyrics.getLyrics
import com.craftworks.music.lyrics.songLyrics
import com.craftworks.music.navidrome.markSongAsPlayed
import com.craftworks.music.ui.screens.useNavidromeServer
import kotlin.math.abs


class SongHelper {
    companion object{
        var isSeeking = false
        lateinit var player: ExoPlayer
        private lateinit var mediaSession: MediaSession
        private lateinit var controller: MediaController
        var currentPosition: Long = 0

        fun initPlayer(context: Context){
            player = ExoPlayer.Builder(context).build()
            mediaSession = MediaSession.Builder(context, player).build()
        }
        fun playStream(context: Context, url: Uri) {
            // Stop If It's Playing
            if (player.isPlaying){
                player.stop()
                player.clearMediaItems()
            }

            // Initialize Player With Media
            val mediaItem = MediaItem.fromUri(url)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.seekTo(currentPosition)
            player.playWhenReady = true

            // Add OnComplete Listener
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        onPlayerComplete()
                    }
                }
            })

            // Set WakeLock For Navidrome Streaming
            if (useNavidromeServer.value){
                val wifiManager = context.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
                val wifiLock: WifiManager.WifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "Navidrome Wake Lock")
                wifiLock.acquire()
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

        fun previousSong(song: Song){
            Log.d("MEDIAPLAYER", "Skipping to previous song")

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
            Log.d("MEDIAPLAYER", "Skipping to next song")

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
            println("${player.currentPosition} ; $currentPosition")
            currentPosition = sliderPos.intValue.toLong()
        }

        private fun onPlayerComplete(){
            if (abs(sliderPos.intValue - playingSong.selectedSong?.duration!!) > 1000 || playingSong.selectedSong?.isRadio == true) return
            playingSong.selectedSong?.let { nextSong(it)}
        }
    }
}