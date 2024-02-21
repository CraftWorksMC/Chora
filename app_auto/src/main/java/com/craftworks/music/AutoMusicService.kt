package com.craftworks.music

import android.media.MediaMetadata
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat
import com.craftworks.music.data.songsList

class AutoMusicService : MediaBrowserServiceCompat() {

    private lateinit var session: MediaSessionCompat

    override fun onCreate() {
        super.onCreate()
        session = MediaSessionCompat(baseContext, "AutoMusicService").apply {
            setCallback(callback)
            setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setActions(PlaybackStateCompat.ACTION_STOP)
                    .setActions(PlaybackStateCompat.ACTION_PAUSE)
                    .setActions(PlaybackStateCompat.ACTION_PLAY)
                    .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE)
                    .setActions(PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
                    .setActions(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                    .setActions(PlaybackStateCompat.ACTION_SEEK_TO)
                    .setState(
                        PlaybackStateCompat.STATE_NONE,
                        0,
                        0f
                    )
                    .build()
            )
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
        }
        sessionToken = session.sessionToken
    }

    fun updateAutomotiveMetadata() {
        println("Called updateAutomotiveState")
        val mediaMetadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, SongHelper.currentSong.title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, SongHelper.currentSong.artist)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, SongHelper.currentSong.album)
            .putString(MediaMetadata.METADATA_KEY_ART_URI, SongHelper.currentSong.imageUrl.toString())
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, SongHelper.currentDuration)
            //.putLong(MediaMetadata.METADATA_KEY_YEAR, SongHelper.currentSong.year?.toLong()?:0)
            .build()

        session.apply {
            setMetadata(mediaMetadata)
            setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setActions(PlaybackStateCompat.ACTION_STOP)
                    .setActions(PlaybackStateCompat.ACTION_PAUSE)
                    .setActions(PlaybackStateCompat.ACTION_PLAY)
                    .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE)
                    .setActions(PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
                    .setActions(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                    .setActions(PlaybackStateCompat.ACTION_SEEK_TO)
                    .setState(
                        if (SongHelper.player.playbackState == PlaybackStateCompat.STATE_PLAYING)
                            PlaybackStateCompat.STATE_PLAYING
                        else
                            PlaybackStateCompat.STATE_PAUSED,
                        sliderPos.intValue.toLong(),
                        if (SongHelper.player.isPlaying) 1f else 0f
                    ).build()
            )
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            setCallback(callback)
        }
        println("Updated MediaSessionCompat")
    }

    private val callback = object : MediaSessionCompat.Callback() {

        override fun onPlay() {
            println("AA: onPlay")
            SongHelper.player.playWhenReady = true
            updateAutomotiveMetadata()
            session.apply {
                setPlaybackState(
                    PlaybackStateCompat.Builder().setState(
                        PlaybackStateCompat.STATE_PLAYING,
                        sliderPos.intValue.toLong(),
                        1f)
                        .build()
                )
                isActive = true
            }
            super.onPlay()
        }

        override fun onSeekTo(pos: Long) {
            println("AA: Requested Seek To $pos")
            SongHelper.isSeeking = true
            sliderPos.intValue = pos.toInt()
            SongHelper.currentPosition = pos
            SongHelper.player.seekTo(pos)
            super.onSeekTo(pos)
            SongHelper.isSeeking = false
            updateAutomotiveMetadata()
        }

        override fun onPause() {
            println("AA: Requested Pause")
            SongHelper.player.playWhenReady = false
            SongHelper.pauseStream()
            updateAutomotiveMetadata()
            session.apply {
                setPlaybackState(
                    PlaybackStateCompat.Builder().setState(
                        PlaybackStateCompat.STATE_PAUSED,
                        sliderPos.intValue.toLong(),
                        0f)
                        .build()
                )
            }
            super.onPause()
        }

        override fun onSkipToNext() {
            super.onSkipToNext()
            SongHelper.nextSong(SongHelper.currentSong)
            updateAutomotiveMetadata()
        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
            SongHelper.previousSong(SongHelper.currentSong)
            updateAutomotiveMetadata()
        }

        override fun onStop() {
            println("AA: Requested Stop")
            SongHelper.pauseStream()
            updateAutomotiveMetadata()
            super.onStop()
        }

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            super.onPlayFromMediaId(mediaId, extras)

            println("AA: onPlayFromMediaId $mediaId")
            playingSong.selectedSong = songsList[songsList.indexOfFirst { it.media.toString() == mediaId }]
            playingSong.selectedList = songsList

            SongHelper.playStream(baseContext, Uri.parse(mediaId))
            SongHelper.player.playWhenReady = true
            updateAutomotiveMetadata()
            session.apply {
                setPlaybackState(
                    PlaybackStateCompat.Builder().setState(
                        PlaybackStateCompat.STATE_PLAYING,
                        sliderPos.intValue.toLong(),
                        1f)
                        .build()
                )
                isActive = true
            }
        }
    }



    override fun onDestroy() {
        super.onDestroy()
        //session.release()
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?,
    ): BrowserRoot {
        return BrowserRoot("rootId", null)
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaItem>>) {
        val mediaItems = ArrayList<MediaItem>()
        // Add Media Items
        if (songsList.isEmpty())
            saveManager(applicationContext).loadSettings()

        for (song in songsList){
            if (song.isRadio == true) break

            val mediaMetadata = MediaMetadataCompat.Builder()
                .putString(MediaMetadata.METADATA_KEY_MEDIA_URI, song.media.toString())
                .putString(MediaMetadata.METADATA_KEY_TITLE, song.title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, song.artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, song.album)
                .putString(MediaMetadata.METADATA_KEY_ART_URI, song.imageUrl.toString())
                .putLong(MediaMetadata.METADATA_KEY_YEAR, song.year?.toLongOrNull()?: 0)
                .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, song.media.toString())
                .build()

            val mediaItem = MediaItem(mediaMetadata.description, MediaItem.FLAG_PLAYABLE)
            mediaItems.add(mediaItem)
        }
        result.sendResult(mediaItems)
    }
}