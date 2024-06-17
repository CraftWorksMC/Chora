package com.craftworks.music.player

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.craftworks.music.data.MediaData
import com.craftworks.music.data.songsList
import com.craftworks.music.data.useNavidromeServer
import com.craftworks.music.lyrics.requestLyrics
import com.craftworks.music.providers.navidrome.markNavidromeSongAsPlayed
import com.craftworks.music.saveManager
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/*
    Thanks to Yurowitz on StackOverflow for this! Used it as a template.
    https://stackoverflow.com/questions/76838126/can-i-define-a-medialibraryservice-without-an-app
*/

class ChoraMediaLibraryService : MediaLibraryService() {

    //region Vars
    lateinit var player: Player
    var session: MediaLibrarySession? = null

    private val rootItem = MediaItem.Builder()
        .setMediaId("nodeROOT")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setIsBrowsable(false)
                .setIsPlayable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                .setTitle("MyMusicAppRootWhichIsNotVisibleToControllers")
                .build()
        )
        .build()

    private val subrootTracklistItem = MediaItem.Builder()
        .setMediaId("nodeTRACKLIST")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .setTitle("Chora - All Songs")
                .setArtworkUri(
                    Uri.parse("android.resource://com.craftworks.music/drawable/ic_notification_icon")
                )
                .build()
        )
        .build()
    private val rootHierarchy = listOf(subrootTracklistItem)

    private val serviceMainScope = CoroutineScope(Dispatchers.Main)
    private val serviceIOScope = CoroutineScope(Dispatchers.IO)

    var androidAutoAllSongsTracklist = mutableListOf<MediaItem>()

    //endregion

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        Log.d("AA", "onCreate: Android Auto")

        saveManager(this).loadPreferences()
        saveManager(this).loadBottomNavItems()

        serviceIOScope.launch {
            saveManager(this@ChoraMediaLibraryService).loadSettings()
        }
        initializePlayer()
    }

    @OptIn(UnstableApi::class)
    fun initializePlayer(){

        player = ExoPlayer.Builder(this)
            .setSeekParameters(SeekParameters.CLOSEST_SYNC)
            .setWakeMode(
                if (useNavidromeServer.value)
                    C.WAKE_MODE_NETWORK
                else
                    C.WAKE_MODE_LOCAL)
            .setHandleAudioBecomingNoisy(true)
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .build()

        player.repeatMode = Player.REPEAT_MODE_OFF
        player.shuffleModeEnabled = false

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)

//                //Controlling Android Auto queue here intelligently
//                if (mediaItem != null && player.mediaItemCount == 1) {
//                    if (tracklist.size > 1) {
//                        val index = tracklist.indexOfFirst { it.mediaId == mediaItem.mediaId }
//                        player.setMediaItems(tracklist, index, 0)
//                    }
//                }

                if (useNavidromeServer.value)
                    serviceMainScope.launch { markNavidromeSongAsPlayed(SongHelper.currentSong) }

                serviceIOScope.launch {
                    val song = MediaData.Song(
                        title = mediaItem?.mediaMetadata?.title.toString(),
                        artist = mediaItem?.mediaMetadata?.artist.toString(),
                        duration = mediaItem?.mediaMetadata?.extras?.getInt("duration") ?: 0,
                        imageUrl = mediaItem?.mediaMetadata?.artworkUri.toString(),
                        year = mediaItem?.mediaMetadata?.releaseYear,
                        album = mediaItem?.mediaMetadata?.albumTitle.toString(),
                        format = mediaItem?.mediaMetadata?.extras?.getString("Format").toString(),
                        bitrate = mediaItem?.mediaMetadata?.extras?.getInt("Bitrate"),
                        navidromeID = mediaItem?.mediaMetadata?.extras?.getString("NavidromeID").toString(),
                        isRadio = mediaItem?.mediaMetadata?.extras?.getBoolean("isRadio"),
                        albumId = "",
                        bpm = 0,
                        contentType = "music",
                        dateAdded = "",
                        timesPlayed = 0,
                        genre = "",
                        parent = "",
                        sortName = "",
                        comment = "",
                        lastPlayed = "",
                        isVideo = false,
                        genres = listOf(),
                        isDir = false,
                        mediaType = "song",
                        path = "",
                        size = 0,
                        type = "song"
                    )
                    SongHelper.currentSong = song

                    if (SongHelper.currentSong.isRadio == false)
                        requestLyrics()
                }
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                super.onMediaMetadataChanged(mediaMetadata)

//                if (SongHelper.currentSong.title == mediaMetadata.title.toString() &&
//                    SongHelper.currentSong.artist == mediaMetadata.artist.toString())
//                    return //Don't update metadata if song is already the same
//
//                serviceIOScope.launch {
//                    val song = Song(
//                        title = mediaMetadata.title.toString(),
//                        artist = mediaMetadata.artist.toString(),
//                        duration = mediaMetadata.extras?.getInt("duration") ?: 0,
//                        imageUrl = Uri.parse(mediaMetadata.artworkUri.toString()),
//                        year = mediaMetadata.releaseYear.toString(),
//                        album = mediaMetadata.albumTitle.toString(),
//                        format = mediaMetadata.extras?.getString("MoreInfo"),
//                        navidromeID = mediaMetadata.extras?.getString("NavidromeID"),
//                        isRadio = mediaMetadata.extras?.getBoolean("isRadio"))
//                    SongHelper.currentSong = song
//                }
//
//                if (SongHelper.currentSong.isRadio == false)
//                    getLyrics()
            }

            override fun onPlayerError(error: PlaybackException) {
                error.printStackTrace()
                Log.e("PLAYER", error.stackTraceToString())
            }
        })

        // Launch MainActivity when clicking on the notification.
          //val intent = Intent(applicationContext, MainActivity::class.java)
          //val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        session = MediaLibrarySession.Builder(this, player, LibrarySessionCallback())
            .setId("AutoSession")
          //.setSessionActivity(pendingIntent)
            .build()

        // Add all songs to android auto.
        // For now it will only contain an ordered list of all the songs.
        synchronized(songsList){
            for (song in songsList){
                val mediaMetadata = MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .setArtworkUri(Uri.parse(song.imageUrl))
                    .setReleaseYear(song.year)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setExtras(Bundle().apply {
                        putString("Format", song.format)
                        song.bitrate?.let { putInt("Bitrate", it) }
                        putString("navidromeID", song.navidromeID)
                        putInt("Duration", song.duration)
                    })
                    .build()
                val mediaItem = MediaItem.Builder()
                    .setUri(song.media)
                    .setMediaId(song.media.toString())
                    .setMediaMetadata(mediaMetadata)
                    .build()

                androidAutoAllSongsTracklist.add(mediaItem)
            }
        }
        androidAutoAllSongsTracklist = androidAutoAllSongsTracklist.sortedBy { it.mediaMetadata.title.toString() }.toMutableList()
        SongHelper.currentTracklist = androidAutoAllSongsTracklist

        session?.notifyChildrenChanged("nodeTRACKLIST", androidAutoAllSongsTracklist.size, null)

        Log.d("AA", "Initialized MediaLibraryService.")
    }

/*    fun notifyNewSessionItems() {
        session?.connectedControllers?.forEach {
            (session as MediaLibrarySession).notifyChildrenChanged(it, "nodeTRACKLIST", SongHelper.currentTracklist.size, null)
        }
        Log.d("AA", "Notified MediaLibrarySession with new tracklist. (Length: ${SongHelper.currentTracklist.size})")
    }*/

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Check if the player is not ready to play or there are no items in the media queue
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return session
    }

    private inner class LibrarySessionCallback : MediaLibrarySession.Callback {

        @OptIn(UnstableApi::class)
        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            // We need to use URI from requestMetaData because of https://github.com/androidx/media/issues/282
            val updatedMediaItems: List<MediaItem> =
                SongHelper.currentTracklist.map { mediaItem ->
                    MediaItem.Builder()
                        .setMediaId(mediaItem.mediaId)
                        .setMediaMetadata(mediaItem.mediaMetadata)
                        .setUri(mediaItem.mediaId)
                        .build()
                }
            val updatedStartIndex =
                if (SongHelper.currentSong.isRadio == false)
                    updatedMediaItems.indexOfFirst { it.mediaId == mediaItems[0].mediaId }
                else {
                    0
                }

            Log.d("AA", "updatedStartIndex: $updatedStartIndex")

            return super.onSetMediaItems(mediaSession, controller, updatedMediaItems, updatedStartIndex, startPositionMs)
        }


        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession, browser: MediaSession.ControllerInfo,
            parentId: String, page: Int, pageSize: Int, params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {

            return Futures.immediateFuture(
                LibraryResult.ofItemList(
                    when (parentId) {
                        "nodeROOT" -> rootHierarchy
                        "nodeTRACKLIST" -> androidAutoAllSongsTracklist
                        else -> rootHierarchy
                    },
                    params
                )
            )
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val mediaItem = androidAutoAllSongsTracklist.find { it.mediaId == mediaId }
                ?: return Futures.immediateFuture(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))

            return Futures.immediateFuture(LibraryResult.ofItem(mediaItem, LibraryParams.Builder().build()))
        }

        override fun onSubscribe(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<Void>> {
            session.notifyChildrenChanged(
                parentId,
                when (parentId) {
                    "nodeROOT" -> 2
                    "nodeTRACKLIST" -> androidAutoAllSongsTracklist.size
                    else -> 0
                },
                params
            )
            return Futures.immediateFuture(LibraryResult.ofVoid()) //super.onSubscribe(session, browser, parentId, params)
        }
    }

    override fun onDestroy() {
        session?.run {
            player.release()
            release()
            session = null
        }
        super.onDestroy()
    }
}