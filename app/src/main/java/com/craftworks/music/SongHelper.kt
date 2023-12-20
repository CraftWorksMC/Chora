package com.craftworks.music

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.craftworks.music.data.Song
import com.craftworks.music.lyrics.SyncedLyric
import com.craftworks.music.lyrics.getLyrics
import com.craftworks.music.lyrics.songLyrics
import com.craftworks.music.navidrome.markSongAsPlayed
import kotlin.math.abs

class SongHelper {
    companion object{
        var mediaPlayer: MediaPlayer? = null
        var currentPosition = 0
        var isSeeking = false

        fun playStream(context: Context, url: Uri) {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    mediaPlayer?.stop()
                    mediaPlayer?.reset()
                }
            }
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, url)
                prepareAsync()
            }
            mediaPlayer?.setOnPreparedListener { mediaPlayer ->
                mediaPlayer.seekTo(currentPosition)
                mediaPlayer.start()
            }
            mediaPlayer?.setOnCompletionListener { _ ->
                onPlayerComplete()
            }
        }

        fun pauseStream(){
            mediaPlayer?.let {
                currentPosition = it.currentPosition
                it.pause()
            }
        }

        fun stopStream(){
            mediaPlayer?.stop()
            mediaPlayer?.reset()
            currentPosition = 0
        }

        fun releasePlayer(){
            mediaPlayer?.reset()
            mediaPlayer?.release()
            mediaPlayer = null
            currentPosition = 0
        }

        fun previousSong(song: Song){
            Log.d("MEDIAPLAYER", "Skipping to previous song")

            val currentSongIndex = songsList.indexOfFirst{it.media == playingSong.selectedSong?.media}

            if (repeatSong.value){
                mediaPlayer?.seekTo(0)
                mediaPlayer?.start()
            }

            if (shuffleSongs.value
                && songsList.size > 0){
                playingSong.selectedSong = songsList[(0..songsList.size - 1).random()]
            }
            // Play Previous only if there is actually a song behind, and not shuffling or repeating.
            if ( (currentSongIndex - 1) >= 0
                && !repeatSong.value
                && !shuffleSongs.value){
                playingSong.selectedSong = songsList[currentSongIndex - 1]
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

            val currentSongIndex = songsList.indexOfFirst{it.media == playingSong.selectedSong?.media}

            if (repeatSong.value){
                mediaPlayer?.seekTo(0)
                mediaPlayer?.start()
            }
            if (shuffleSongs.value
                && songsList.size > 0)
                playingSong.selectedSong = songsList[(0..songsList.size - 1).random()]
            if (currentSongIndex < songsList.size-1
                && !repeatSong.value
                && !shuffleSongs.value)
                playingSong.selectedSong = songsList[currentSongIndex + 1]
            stopStream()
            sliderPos.intValue = 0
            songLyrics.SongLyrics = "Getting Lyrics... \n No Lyrics Found"
            SyncedLyric.clear()
            getLyrics()
            markSongAsPlayed(song)
        }

        fun updateCurrentPos(){
            sliderPos.intValue = mediaPlayer!!.currentPosition
            currentPosition = sliderPos.intValue
        }

        private fun onPlayerComplete(){
            if (abs(sliderPos.intValue - playingSong.selectedSong?.duration!!) > 1000 || playingSong.selectedSong?.isRadio == true) return
            playingSong.selectedSong?.let { nextSong(it)}
        }
    }
}