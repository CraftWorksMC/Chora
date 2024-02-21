package com.craftworks.music.auto

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.craftworks.music.R
import com.craftworks.music.data.songsList
import com.craftworks.music.playingSong
import com.craftworks.music.saveManager
import java.io.IOException


class AutoMusicServiceNew : MediaBrowserServiceCompat(), OnCompletionListener,
    OnAudioFocusChangeListener {
    private var mMediaPlayer: MediaPlayer? = null
    private var mMediaSessionCompat: MediaSessionCompat? = null
    private val mNoisyReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (mMediaPlayer != null && mMediaPlayer!!.isPlaying) {
                mMediaPlayer!!.pause()
            }
        }
    }
    private val mMediaSessionCallback: MediaSessionCompat.Callback =
        object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                super.onPlay()
                if (!successfullyRetrievedAudioFocus()) {
                    return
                }
                mMediaSessionCompat!!.isActive = true
                setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING)
                mMediaPlayer!!.start()
            }

            override fun onPause() {
                super.onPause()
                if (mMediaPlayer!!.isPlaying) {
                    mMediaPlayer!!.pause()
                    setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED)
                }
            }

            override fun onPlayFromMediaId(mediaId: String, extras: Bundle) {
                super.onPlayFromMediaId(mediaId, extras)
                mMediaPlayer?.setDataSource(applicationContext, Uri.parse(mediaId))
                mMediaPlayer?.prepare()
                mMediaPlayer?.start()
                try {
                    //val afd = resources.openRawResourceFd(Integer.valueOf(mediaId)) ?: return
                    try {

                        /*mMediaPlayer!!.setDataSource(
                            afd.fileDescriptor,
                            afd.startOffset,
                            afd.length
                        )*/
                    } catch (e: IllegalStateException) {
                        mMediaPlayer!!.release()
                        initMediaPlayer()
                    }
                    initMediaSessionMetadata()
                } catch (e: IOException) {
                    return
                }
                try {
                    mMediaPlayer!!.prepare()
                } catch (e: Exception) {
                }

                println("AA: onPlayFromMediaId $mediaId")
                playingSong.selectedSong = songsList[songsList.indexOfFirst { it.media.toString() == mediaId }]
                playingSong.selectedList = songsList

                //SongHelper.playStream(baseContext, Uri.parse(mediaId))
                initMediaSessionMetadata()
                //Work with extras here if you want
            }

            override fun onSeekTo(pos: Long) {
                super.onSeekTo(pos)
            }
        }

    override fun onCreate() {
        super.onCreate()
        initMediaPlayer()
        initMediaSession()
        initNoisyReceiver()
    }

    private fun initNoisyReceiver() {
        //Handles headphones coming unplugged. cannot be done through a manifest receiver
        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(mNoisyReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        audioManager.abandonAudioFocus(this)
        unregisterReceiver(mNoisyReceiver)
        mMediaSessionCompat!!.release()
        NotificationManagerCompat.from(this).cancel(1)
    }

    private fun initMediaPlayer() {
        mMediaPlayer = MediaPlayer()
        mMediaPlayer!!.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
        mMediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
        mMediaPlayer!!.setVolume(1.0f, 1.0f)
    }

    private fun initMediaSession() {
        val mediaButtonReceiver = ComponentName(
            applicationContext,
            MediaButtonReceiver::class.java
        )
        mMediaSessionCompat =
            MediaSessionCompat(applicationContext, "Tag", mediaButtonReceiver, null)
        mMediaSessionCompat!!.setCallback(mMediaSessionCallback)
        mMediaSessionCompat!!.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
        mediaButtonIntent.setClass(this, MediaButtonReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent,
            PendingIntent.FLAG_IMMUTABLE)
        mMediaSessionCompat!!.setMediaButtonReceiver(pendingIntent)
        setSessionToken(mMediaSessionCompat!!.sessionToken)
    }

    private fun setMediaPlaybackState(state: Int) {
        val playbackstateBuilder = PlaybackStateCompat.Builder()
        if (state == PlaybackStateCompat.STATE_PLAYING) {
            playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_PAUSE)
        } else {
            playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_PLAY)
        }
        playbackstateBuilder.setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0f)
        mMediaSessionCompat!!.setPlaybackState(playbackstateBuilder.build())
    }

    private fun initMediaSessionMetadata() {
        val metadataBuilder = MediaMetadataCompat.Builder()
        //Notification icon in card
        metadataBuilder.putBitmap(
            MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, BitmapFactory.decodeResource(
                resources, R.mipmap.ic_launcher
            )
        )
        metadataBuilder.putBitmap(
            MediaMetadataCompat.METADATA_KEY_ALBUM_ART, BitmapFactory.decodeResource(
                resources, R.mipmap.ic_launcher
            )
        )

        //lock screen icon for pre lollipop
        metadataBuilder.putBitmap(
            MediaMetadataCompat.METADATA_KEY_ART, BitmapFactory.decodeResource(
                resources, R.mipmap.ic_launcher
            )
        )
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, "Display Title")
        metadataBuilder.putString(
            MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE,
            "Display Subtitle"
        )
        metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, 1)
        metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, 1)
        mMediaSessionCompat!!.setMetadata(metadataBuilder.build())
    }

    private fun successfullyRetrievedAudioFocus(): Boolean {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val result = audioManager.requestAudioFocus(
            this,
            AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN
        )
        return result == AudioManager.AUDIOFOCUS_GAIN
    }

    //Not important for general audio service, required for class
    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?,
    ): BrowserRoot {
        Log.d("MEDIA_BROWSER_SERVICE_COMPAT","onGetRoot() called");
        return BrowserRoot("rootId", null)
    }

    //Not important for general audio service, required for class
    override fun onLoadChildren(
        parentId: String,
        result: Result<List<MediaBrowserCompat.MediaItem>>,
    ) {
        val mediaItems = ArrayList<MediaBrowserCompat.MediaItem>()
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

            val mediaItem = MediaBrowserCompat.MediaItem(
                mediaMetadata.description,
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
            )
            mediaItems.add(mediaItem)
        }
        result.sendResult(mediaItems)
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                if (mMediaPlayer!!.isPlaying) {
                    mMediaPlayer!!.stop()
                }
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                mMediaPlayer!!.pause()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (mMediaPlayer != null) {
                    mMediaPlayer!!.setVolume(0.3f, 0.3f)
                }
            }

            AudioManager.AUDIOFOCUS_GAIN -> {
                if (mMediaPlayer != null) {
                    if (!mMediaPlayer!!.isPlaying) {
                        mMediaPlayer!!.start()
                    }
                    mMediaPlayer!!.setVolume(1.0f, 1.0f)
                }
            }
        }
    }

    override fun onCompletion(mediaPlayer: MediaPlayer) {
        if (mMediaPlayer != null) {
            mMediaPlayer!!.release()
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mMediaSessionCompat, intent)
        return super.onStartCommand(intent, flags, startId)
    }
}