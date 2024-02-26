package com.craftworks.music.auto

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
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.craftworks.music.data.songsList
import com.craftworks.music.providers.navidrome.useNavidromeServer
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

class AutoLibraryService : MediaLibraryService() {

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

    val subroot_TracklistItem = MediaItem.Builder()
        .setMediaId("nodeTRACKLIST")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                .setTitle("Chora - All Songs")
                .setArtworkUri(
                    Uri.parse("android.resource://com.craftworks.music/drawable/ic_notification_icon")
                )
                .build()
        )
        .build()

    val rootHierarchy = listOf(subroot_TracklistItem)

    var tracklist = mutableListOf<MediaItem>()

    /** This will fetch music from the source folder (or the entire device if not specified) */
    private fun queryMusic(initial: Boolean = false) {
        serviceIOScope.launch {
            tracklist.clear()
            for (song in songsList) {
                val mediaMetadata = MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .setArtworkUri(song.imageUrl)
                    .setReleaseYear(song.year?.toIntOrNull() ?: 0)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build()
                tracklist.add(MediaItem.fromUri(song.media.toString())
                    .buildUpon()
                    .setMediaId(song.media.toString())
                    .setMediaMetadata(mediaMetadata).build()
                )
            }

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
        /** Building ExoPlayer to use FFmpeg Audio Renderer and also enable fast-seeking */
        player = ExoPlayer.Builder(applicationContext)
            .setSeekParameters(SeekParameters.CLOSEST_SYNC) /* Enabling fast seeking */
            .setRenderersFactory(
                DefaultRenderersFactory(this).setExtensionRendererMode(
                    DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER /* We prefer extensions, such as FFmpeg */
                )
            )
            .setWakeMode(
                if (useNavidromeServer.value){
                    C.WAKE_MODE_NETWORK }
                else {
                    C.WAKE_MODE_LOCAL }
            )
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
                    if (tracklist.size > 1) {
                        val index = tracklist.indexOfFirst { it.mediaId == mediaItem.mediaId }
                        player.setMediaItems(tracklist, index, 0)
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
                        //.add(CUSTOM_COM_PLAY_ITEM) //Command executed when an item is requested to play
                        //.add(CUSTOM_COM_END_SERVICE) //This one is called to end the service manually from the UI
                        //.add(CUSTOM_COM_SCAN_MUSIC) //Command use to execute a music scan
                        //.add(CUSTOM_COM_TRACKLIST_FORGET) //Used when an item is to be forgotten (swipe left)
                        .build()

                    val playerComs = super.onConnect(session, controller).availablePlayerCommands

                    return MediaSession.ConnectionResult.accept(sessionComs, playerComs)
                }

                /** Receiving some custom commands such as the command that ends the service.
                 * In order to make the player accept newly customized foreign weird commands, we have
                 * to edit the onConnect callback method like we did above */
                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    customCommand: SessionCommand,
                    args: Bundle
                ): ListenableFuture<SessionResult> {
                    /*

                    /** When the controller tries to add an item to the playlist */
                    if (customCommand == CUSTOM_COM_PLAY_ITEM) {
                        args.getString("id")?.let { mediaid ->

                            val i = tracklist.indexOfFirst { it.mediaId == mediaid }
                            //setPlaybackMode(PlayBackMode.PBM_TRACKLIST)
                            player.setMediaItems(tracklist, i, 0)

                            player.prepare()
                            player.play()
                            return Futures.immediateFuture(SessionResult(RESULT_SUCCESS))

                        }
                    }

                    /** When the controller (like the app) closes fully, we need to disconnect */
                    if (customCommand == CUSTOM_COM_END_SERVICE) {
                        session.release()
                        player.release()
                        this@AutoLibraryService.stopSelf()

                        return Futures.immediateFuture(SessionResult(RESULT_SUCCESS))
                    }

                    /** When the user changes the source folder */
                    if (customCommand == CUSTOM_COM_SCAN_MUSIC) {
                        queryMusic()
                        return Futures.immediateFuture(SessionResult(RESULT_SUCCESS))
                    }

                    */

                    return super.onCustomCommand(session, controller, customCommand, args)
                }
            }).setId("AutoSession").build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return session
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