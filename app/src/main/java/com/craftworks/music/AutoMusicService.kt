package com.craftworks.music

import android.media.MediaMetadata
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.OptIn
import androidx.media.MediaBrowserServiceCompat
import androidx.media3.common.util.UnstableApi
import com.craftworks.music.data.songsList

/**
 * This class provides a MediaBrowser through a service. It exposes the media library to a browsing
 * client, through the onGetRoot and onLoadChildren methods. It also creates a MediaSession and
 * exposes it through its MediaSession.Token, which allows the client to create a MediaController
 * that connects to and send control commands to the MediaSession remotely. This is useful for
 * user interfaces that need to interact with your media session, like Android Auto. You can
 * (should) also use the same service from your app's UI, which gives a seamless playback
 * experience to the user.
 *
 *
 * To implement a MediaBrowserService, you need to:
 *
 *  *  Extend [MediaBrowserServiceCompat], implementing the media browsing
 * related methods [MediaBrowserServiceCompat.onGetRoot] and
 * [MediaBrowserServiceCompat.onLoadChildren];
 *
 *  *  In onCreate, start a new [MediaSessionCompat] and notify its parent
 * with the session"s token [MediaBrowserServiceCompat.setSessionToken];
 *
 *  *  Set a callback on the [MediaSessionCompat.setCallback].
 * The callback will receive all the user"s actions, like play, pause, etc;
 *
 *  *  Handle all the actual music playing using any method your app prefers (for example,
 * [android.media.MediaPlayer])
 *
 *  *  Update playbackState, "now playing" metadata and queue, using MediaSession proper methods
 * [MediaSessionCompat.setPlaybackState]
 * [MediaSessionCompat.setMetadata] and
 * [MediaSessionCompat.setQueue])
 *
 *  *  Declare and export the service in AndroidManifest with an intent receiver for the action
 * android.media.browse.MediaBrowserService
 *
 * To make your app compatible with Android Auto, you also need to:
 *
 *  *  Declare a meta-data tag in AndroidManifest.xml linking to a xml resource
 * with a &lt;automotiveApp&gt; root element. For a media app, this must include
 * an &lt;uses name="media"/&gt; element as a child.
 * For example, in AndroidManifest.xml:
 * &lt;meta-data android:name="com.google.android.gms.car.application"
 * android:resource="@xml/automotive_app_desc"/&gt;
 * And in res/values/automotive_app_desc.xml:
 * &lt;automotiveApp&gt;
 * &lt;uses name="media"/&gt;
 * &lt;/automotiveApp&gt;
 *
 */

class AutoMusicService : MediaBrowserServiceCompat() {

    private lateinit var session: MediaSessionCompat

    fun updateAutomotiveState() {

        val mediaMetadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, SongHelper.currentSong.title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, SongHelper.currentSong.artist)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, SongHelper.currentSong.album)
            .putString(MediaMetadata.METADATA_KEY_ART_URI, SongHelper.currentSong.imageUrl.toString())
            //.putLong(MediaMetadata.METADATA_KEY_YEAR, SongHelper.currentSong.year?.toLong()?:0)
            .build()

        val playbackState = if (SongHelper.player.playWhenReady) {
            PlaybackStateCompat.STATE_PLAYING
        } else {
            PlaybackStateCompat.STATE_PAUSED
        }

        session.apply {
            setMetadata(mediaMetadata)
            setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setState(playbackState, sliderPos.intValue.toLong(),  1f)
                    .build()
            )
            isActive = true
        }
    }

    private val callback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            super.onPlay()
            session.setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_PLAYING, sliderPos.intValue.toLong(),  1f)
                    .build()
            )
            SongHelper.player.playWhenReady = true
            updateAutomotiveState()
        }

        override fun onSeekTo(pos: Long) {
            super.onSeekTo(pos)
            sliderPos.intValue = pos.toInt()
            SongHelper.currentPosition = pos
            SongHelper.player.seekTo(pos)
        }

        override fun onPause() {
            super.onPause()
            session.setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_PAUSED, SongHelper.mediaSession.player.currentPosition,1f)
                    .build()
            )
            SongHelper.player.playWhenReady = false
            SongHelper.pauseStream()
            updateAutomotiveState()
        }

        override fun onSkipToNext() {
            super.onSkipToNext()
            SongHelper.nextSong(SongHelper.currentSong)
        }
        override fun onStop() {
            super.onStop()
            SongHelper.stopStream()
            updateAutomotiveState()
        }

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            super.onPlayFromMediaId(mediaId, extras)

            playingSong.selectedSong = songsList[songsList.indexOfFirst { it.media.toString() == mediaId }]
            playingSong.selectedList = songsList

            SongHelper.playStream(baseContext, Uri.parse(mediaId))
            updateAutomotiveState()
        }
    }

    @OptIn(UnstableApi::class) override fun onCreate() {
        super.onCreate()

        session = MediaSessionCompat(baseContext, "AutoMusicService").apply {
            setCallback(callback)
            setMetadata(MediaMetadataCompat.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, SongHelper.currentSong.title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, SongHelper.currentSong.artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, SongHelper.currentSong.album)
                .putString(MediaMetadata.METADATA_KEY_ART_URI, SongHelper.currentSong.imageUrl.toString())
                //.putLong(MediaMetadata.METADATA_KEY_YEAR, SongHelper.currentSong.year?.toLong()?:0)
                .build())
        }
        sessionToken = session.sessionToken
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