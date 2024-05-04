package com.craftworks.music.player

import android.content.Intent
import android.net.Uri
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
import com.craftworks.music.data.tracklist
import com.craftworks.music.data.useNavidromeServer
import com.craftworks.music.lyrics.getLyrics
import com.craftworks.music.providers.navidrome.markNavidromeSongAsPlayed
import com.craftworks.music.saveManager
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/*
    Thanks to Yurowitz on StackOverflow for this! Used it as a template.
    https://stackoverflow.com/questions/76838126/can-i-define-a-medialibraryservice-without-an-app
*/

class ChoraMediaLibraryService : MediaLibraryService() {

    //region Vars
    lateinit var player: ExoPlayer
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

    //endregion

    override fun onCreate() {
        Log.d("AA", "onCreate: Android Auto")

        //SongHelper.initPlayer(this)
        saveManager(applicationContext).loadSettings()

        initializePlayer()

        super.onCreate()
    }

    @OptIn(UnstableApi::class)
    fun initializePlayer(){

        player = ExoPlayer.Builder(applicationContext)
            .setSeekParameters(SeekParameters.CLOSEST_SYNC)
//            .setRenderersFactory(
//                DefaultRenderersFactory(applicationContext).setExtensionRendererMode(
//                    DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER /* We prefer extensions, such as FFmpeg */
//                ))
            .setWakeMode(
                if (useNavidromeServer.value)
                    C.WAKE_MODE_NETWORK
                else
                    C.WAKE_MODE_LOCAL)
            .setHandleAudioBecomingNoisy(true)
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .build()

        player.availableCommands.buildUpon()
            .add(Player.COMMAND_SET_SHUFFLE_MODE)
            .add(Player.COMMAND_SET_REPEAT_MODE)
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
                    markNavidromeSongAsPlayed(SongHelper.currentSong)
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                super.onMediaMetadataChanged(mediaMetadata)

//                val extras = mediaMetadata.extras
//
//                SongHelper.currentSong = Song(
//                    title = mediaMetadata.title.toString(),
//                    artist = mediaMetadata.artist.toString(),
//                    duration = extras?.getInt("duration") ?: 0,
//                    imageUrl = Uri.parse(mediaMetadata.artworkUri.toString()),
//                    year = mediaMetadata.releaseYear.toString(),
//                    album = mediaMetadata.albumTitle.toString(),
//                    format = extras?.getString("MoreInfo"),
//                    navidromeID = extras?.getString("NavidromeID"),
//                    isRadio = extras?.getBoolean("isRadio")
//                )
//                println("imageURL: ${player.mediaMetadata.artworkUri}, duration: ${extras?.getInt("duration")}")
//                println(SongHelper.currentSong)

                try {
                    SongHelper.currentSong = SongHelper.currentList[SongHelper.currentList.indexOfFirst { it.media.toString() == player.currentMediaItem?.mediaId }]
                }
                catch (e: IndexOutOfBoundsException){
                    Log.d("AA", "Empty Song, Invalid Index")
                }


                if (SongHelper.currentSong.isRadio == false)
                    getLyrics()
            }

            override fun onPlayerError(error: PlaybackException) {
                error.printStackTrace()
                Log.e("PLAYER", error.stackTraceToString())
            }
        })
        player.setPlaybackSpeed(1f) //Avoid negative speed error.

        session = MediaLibrarySession.Builder(this, player, LibrarySessionCallback()).setId("AutoSession").build()

        Log.d("AA", "Initialized MediaLibraryService.")

        //player.setMediaItems(addMediaItems())
    }

    fun notifyNewSessionItems() {
        session?.connectedControllers?.forEach {
            (session as MediaLibrarySession).notifyChildrenChanged(it, "nodeTRACKLIST", SongHelper.currentTracklist.size, null)
        }
        Log.d("AA", "Notified MediaLibrarySession with new tracklist. (Length: ${SongHelper.currentTracklist.size})")
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Check if the player is not ready to play or there are no items in the media queue
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            // Stop the service
            stopSelf()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return session
    }

    private inner class LibrarySessionCallback : MediaLibrarySession.Callback {
//        @UnstableApi
//        override fun onSetMediaItems(
//            mediaSession: MediaSession,
//            controller: MediaSession.ControllerInfo,
//            mediaItems: MutableList<MediaItem>,
//            startIndex: Int,
//            startPositionMs: Long
//        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
//
//            val newItems = mutableListOf<MediaItem>()
//
//            for (item in tracklist) {
//                val newItem = item.buildUpon()
//                    .setUri(item.mediaId)
//                    .setMediaMetadata(item.mediaMetadata)
//                    .build()
//                newItems.add(newItem)
//            }
//            Log.d("AA", "onSetMediaItems: oldItems: ${mediaItems.size} |newItems: ${newItems.size}")
//            session?.connectedControllers?.forEach {
//                (session as MediaLibrarySession).notifyChildrenChanged(it, "nodeTRACKLIST", newItems.size, /* params= */ null)
//            }
//
//            return super.onSetMediaItems(
//                mediaSession,
//                controller,
//                newItems,
//                startIndex,
//                startPositionMs
//            )
//        }
//        override fun onAddMediaItems(
//            mediaSession: MediaSession,
//            controller: MediaSession.ControllerInfo,
//            mediaItems: MutableList<MediaItem>
//        ): ListenableFuture<MutableList<MediaItem>> {
//            println("ONADDMEDIAITEMS: ${mediaItems.size}")
//            val newItems = mutableListOf<MediaItem>()
//
//            for (item in mediaItems){
//                val newItem = item.buildUpon()
//                    .setUri(item.mediaId)
//                    .setMediaMetadata(item.mediaMetadata)
//                    .build()
//                newItems.add(newItem)
//            }
//
//            return super.onAddMediaItems(mediaSession, controller, newItems)
//        }

        @OptIn(UnstableApi::class)
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            Log.d("AA", "onPlaybackResumption")
            // Create a list of media items to be played
            // Create a MediaItemsWithStartPosition object with the media items and starting position

            //val index = tracklist.indexOf(mediaSession.player.currentMediaItem)
            val index = SongHelper.currentTrackIndex.intValue
            Log.d("AA", "Player Index: $index")

            val mediaItemsWithStartPosition = MediaSession.MediaItemsWithStartPosition(tracklist, index,0)
            session?.connectedControllers?.forEach {
                (session as MediaLibrarySession).notifyChildrenChanged(it, "nodeTRACKLIST", tracklist.size, /* params= */ null)
            }

            return Futures.immediateFuture(mediaItemsWithStartPosition)
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
                        "nodeTRACKLIST" -> tracklist
                        else -> rootHierarchy
                    },
                    params
                )
            )
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
                    "nodeTRACKLIST" -> tracklist.size
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