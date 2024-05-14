package com.craftworks.music.player

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
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
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

class MusicPlayerService : MediaLibraryService() {

    lateinit var player: Player
    private var session: MediaLibrarySession? = null

    private val serviceIOScope = CoroutineScope(Dispatchers.IO)
    private val serviceMainScope = CoroutineScope(Dispatchers.Main)

    /** This is the root item that is parent to our playlist.
     *  It is necessary to have a parent item otherwise there is no "library" */
    val rootItem = MediaItem.Builder()
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

    private val subrootTracklistitem = MediaItem.Builder()
        .setMediaId("nodeTRACKLIST")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                .setTitle("Tracklist")
                .setArtworkUri(
                    Uri.parse("android.resource://mpappc/drawable/ic_tracklist")
                )
                .build()
        )
        .build()

    private val subrootPlaylistitem = MediaItem.Builder()
        .setMediaId("nodePLAYLIST")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                .setTitle("Playlist")
                .setArtworkUri(
                    Uri.parse("android.resource://mpappc/drawable/ic_tracklist")
                )
                .build()
        )
        .build()

    val rootHierarchy = listOf(subrootTracklistitem, subrootPlaylistitem)


    var tracklist = mutableListOf<MediaItem>()
    var playlist = mutableListOf<MediaItem>()

    var latestSearchResults = mutableListOf<MediaItem>()

    /** This will fetch music from the source folder (or the entire device if not specified) */
    private fun queryMusic(initial: Boolean = false) {
        serviceIOScope.launch {

            saveManager(applicationContext).loadSettings()

            if (initial) {
                serviceMainScope.launch {
                    player.setMediaItems(tracklist)
                }
            }

            session?.notifyChildrenChanged("nodeTRACKLIST", tracklist.size, null)
        }
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

//        restorePlaylist(applicationContext) {
//            playlist.clear()
//            playlist.addAll(it)
//
//            session?.notifyChildrenChanged(nodePLAYLIST, playlist.size, null)
//        }

        /** Building ExoPlayer to use FFmpeg Audio Renderer and also enable fast-seeking */
        player = ExoPlayer.Builder(applicationContext)
            .setSeekParameters(SeekParameters.CLOSEST_SYNC) /* Enabling fast seeking */
            .setRenderersFactory(
                DefaultRenderersFactory(this).setExtensionRendererMode(
                    DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER /* We prefer extensions, such as FFmpeg */
                )
            )
            .setWakeMode(C.WAKE_MODE_LOCAL) /* Prevent the service from being killed during playback */
            .setHandleAudioBecomingNoisy(true) /* Prevent annoying noise when changing devices */
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .build()

        player.repeatMode = Player.REPEAT_MODE_ALL

        //Fetching music when the service starts
        queryMusic(true)

        /** Listening to some player events */
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)

                //Controlling Android Auto queue here intelligently
                if (mediaItem != null && player.mediaItemCount == 1) {
                    val playlistfootprint =
                        mediaItem.mediaMetadata.extras?.getBoolean("isplaylist", false) == true

                    if (playlistfootprint && playlist.size > 1) {
                        val index = playlist.indexOfFirst { it.mediaId == mediaItem.mediaId }
                        player.setMediaItems(playlist, index, 0)
                        //setPlaybackMode(PlayBackMode.PBM_PLAYLIST)
                    }

                    if (!playlistfootprint && tracklist.size > 1) {
                        val index = tracklist.indexOfFirst { it.mediaId == mediaItem.mediaId }
                        player.setMediaItems(tracklist, index, 0)
                        //setPlaybackMode(PlayBackMode.PBM_TRACKLIST)
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                error.printStackTrace()

                Log.e("PLAYER", error.stackTraceToString())
            }
        })

        /** Creating our MediaLibrarySession which is an advanced extension of a MediaSession */
        session = MediaLibrarySession
            .Builder(this, player, object : MediaLibrarySession.Callback {
                override fun onGetItem(
                    session: MediaLibrarySession,
                    browser: MediaSession.ControllerInfo,
                    mediaId: String
                ): ListenableFuture<LibraryResult<MediaItem>> {
                    return super.onGetItem(session, browser, mediaId)
                }

                override fun onSetMediaItems(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    mediaItems: MutableList<MediaItem>,
                    startIndex: Int,
                    startPositionMs: Long
                ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                    val newItems = mediaItems.map {
                        it.buildUpon().setUri(it.mediaId).build()
                    }.toMutableList()

                    return super.onSetMediaItems(
                        mediaSession,
                        controller,
                        newItems,
                        startIndex,
                        startPositionMs
                    )
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
                                "nodePLAYLIST" -> playlist
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
                            "nodePLAYLIST" -> playlist.size
                            else -> 0
                        },
                        params
                    )
                    return Futures.immediateFuture(LibraryResult.ofVoid()) //super.onSubscribe(session, browser, parentId, params)
                }

                /** In order to end the service from our media browser side (UI side), we receive
                 * our own custom command (which is [CUSTOM_COM_END_SERVICE]). However, the session
                 * is not designed to accept foreign weird commands. So we edit the onConnect callback method
                 * to make sure it accepts it.
                 */
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    val sessionComs = super.onConnect(session, controller).availableSessionCommands
                        .buildUpon()
//                        .add(CUSTOM_COM_PLAY_ITEM) //Command executed when an item is requested to play
//                        .add(CUSTOM_COM_END_SERVICE) //This one is called to end the service manually from the UI
//                        .add(CUSTOM_COM_PLAYLIST_ADD) //Command used when adding items to playlist
//                        .add(CUSTOM_COM_PLAYLIST_REMOVE) //Command used when removing items from playlist
//                        .add(CUSTOM_COM_PLAYLIST_CLEAR) //Command used when clearing all items from playlist
//                        .add(CUSTOM_COM_SCAN_MUSIC) //Command use to execute a music scan
//                        .add(CUSTOM_COM_TRACKLIST_FORGET) //Used when an item is to be forgotten (swipe left)
                        .build()

                    val playerComs = super.onConnect(session, controller).availablePlayerCommands

                    return MediaSession.ConnectionResult.accept(sessionComs, playerComs)
                }

                //region CUSTOM COMMANDS
/*
                *//** Receiving some custom commands such as the command that ends the service.
                 * In order to make the player accept newly customized foreign weird commands, we have
                 * to edit the onConnect callback method like we did above *//*
                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    customCommand: SessionCommand,
                    args: Bundle
                ): ListenableFuture<SessionResult> {

                    *//** When the controller tries to add an item to the playlist *//*
                    if (customCommand == CUSTOM_COM_PLAY_ITEM) {
                        args.getString("id")?.let { mediaid ->
                            if (args.getBoolean("playlist", false)) {
                                val i = playlist.indexOfFirst { it.mediaId == mediaid }
                                //setPlaybackMode(PlayBackMode.PBM_PLAYLIST)
                                player.setMediaItems(playlist, i, 0)
                            } else {
                                val i = tracklist.indexOfFirst { it.mediaId == mediaid }
                                //setPlaybackMode(PlayBackMode.PBM_TRACKLIST)
                                player.setMediaItems(tracklist, i, 0)
                            }

                            player.prepare()
                            player.play()
                            return Futures.immediateFuture(SessionResult(RESULT_SUCCESS))
                        }
                    }

                    *//** When the controller (like the app) closes fully, we need to disconnect *//*
                    if (customCommand == CUSTOM_COM_END_SERVICE) {
                        session.release()
                        player.release()
                        this@MusicPlayerService.stopSelf()

                        return Futures.immediateFuture(SessionResult(RESULT_SUCCESS))
                    }

                    *//** When the user changes the source folder *//*
                    if (customCommand == CUSTOM_COM_SCAN_MUSIC) {
                        queryMusic()

                        return Futures.immediateFuture(SessionResult(RESULT_SUCCESS))
                    }

                    *//** When the controller tries to add an item to the playlist *//*
                    if (customCommand == CUSTOM_COM_PLAYLIST_ADD) {
                        args.getString("id")?.let { mediaid ->
                            tracklist.firstOrNull { it.mediaId == mediaid }?.let { itemToAdd ->

                                //itemToAdd.playlistFootprint(true)

                                playlist.add(itemToAdd)

                                serviceIOScope.launch {
                                    *//** notifying UI-end that the playlist has been modified *//*
                                    this@MusicPlayerService.session?.apply {
                                        notifyChildrenChanged(
                                            controller,
                                            "nodePLAYLIST",
                                            playlist.size,
                                            null
                                        )
                                    }

                                    *//** Saving the playlist to memory as it is now *//*
                                    //snapshotPlaylist(playlist)
                                }

                                return Futures.immediateFuture(SessionResult(RESULT_SUCCESS))
                            }
                        }
                    }

                    *//** When the controller tries to remove an item from the playlist *//*
                    if (customCommand == CUSTOM_COM_PLAYLIST_REMOVE) {
                        args.getString("id")?.let { mediaid ->
                            playlist.firstOrNull { it.mediaId == mediaid }?.let { itemToRemove ->
                                playlist.remove(itemToRemove)

                                serviceIOScope.launch {
                                    *//** notifying UI-end that the playlist has been modified *//*
                                    this@MusicPlayerService.session?.apply {
                                        notifyChildrenChanged(
                                            controller,
                                            "nodePLAYLIST",
                                            playlist.size,
                                            null
                                        )
                                    }

                                    *//** Saving the playlist to memory as it is now *//*
                                    //snapshotPlaylist(playlist)
                                }

                                return Futures.immediateFuture(SessionResult(RESULT_SUCCESS))
                            }
                        }
                    }

                    *//** When the controller tries to clear the playlist *//*
                    if (customCommand == CUSTOM_COM_PLAYLIST_CLEAR) {
                        playlist.clear()
                        this@MusicPlayerService.session?.apply {
                            notifyChildrenChanged(
                                controller,
                                "nodePLAYLIST",
                                0,
                                null
                            )
                        }

                        *//** Saving the playlist to memory as it is now *//*
                        //snapshotPlaylist(playlist)

                        return Futures.immediateFuture(SessionResult(RESULT_SUCCESS))
                    }

                    return super.onCustomCommand(session, controller, customCommand, args)
                } endregion */
                //endregion

            }).build()

    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return session
    }


    override fun onDestroy() {

        //snapshotPlaylist(playlist)

        session?.run {
            player.release()
            release()
            session = null
        }
        super.onDestroy()
    }
}