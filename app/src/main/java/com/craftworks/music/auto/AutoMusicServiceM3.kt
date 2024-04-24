package com.craftworks.music.auto

import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.craftworks.music.SongHelper
import com.craftworks.music.data.songsList
import com.craftworks.music.data.useNavidromeServer
import com.craftworks.music.providers.local.getSongsOnDevice
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

    private val subrootTracklistItem = MediaItem.Builder()
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

    val rootHierarchy = listOf(subrootTracklistItem)

    var tracklist = mutableListOf<MediaItem>()

    /** This will fetch music from the source folder (or the entire device if not specified) */
    private fun queryMusic(initial: Boolean = false) {
        serviceIOScope.launch {
            tracklist.clear()
            if (!useNavidromeServer.value){
                getSongsOnDevice(baseContext)
            }
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

            Log.d("AA", "Added Songs To Android Auto!")
            session?.notifyChildrenChanged("nodeTRACKLIST", tracklist.size, null)
        }
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        Log.d("AA", "onCreate: Android Auto")
        /** Building ExoPlayer to use FFmpeg Audio Renderer and also enable fast-seeking */
//        player = ExoPlayer.Builder(applicationContext)
//            .setSeekParameters(SeekParameters.CLOSEST_SYNC) /* Enabling fast seeking */
//            .setRenderersFactory(
//                DefaultRenderersFactory(this).setExtensionRendererMode(
//                    DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER /* We prefer extensions, such as FFmpeg */
//                )
//            )
//            .setWakeMode(
//                if (useNavidromeServer.value){
//                    C.WAKE_MODE_NETWORK }
//                else {
//                    C.WAKE_MODE_LOCAL }
//            )
//            .setHandleAudioBecomingNoisy(true) /* Prevent annoying noise when changing devices */
//            .setAudioAttributes(AudioAttributes.DEFAULT, true)
//            .build()
//
//        player.repeatMode = Player.REPEAT_MODE_ALL
        player = SongHelper.player

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