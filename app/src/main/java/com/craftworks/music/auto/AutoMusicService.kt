package com.craftworks.music.auto

import android.net.Uri
import android.os.Bundle
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
import com.craftworks.music.saveManager
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AutoMediaLibraryService : MediaLibraryService() {

    private lateinit var player: Player
    private var session: MediaLibrarySession? = null

    private val serviceIOScope = CoroutineScope(Dispatchers.IO)
    private val serviceMainScope = CoroutineScope(Dispatchers.Main)

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
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                .setTitle("Chora - All Songs")
                .setArtworkUri(
                    Uri.parse("android.resource://com.craftworks.music/drawable/ic_notification_icon")
                )
                .build()
        )
        .build()

    private val rootHierarchy = listOf(subrootTracklistItem)
    private val tracklist = mutableListOf<MediaItem>()

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        Log.d("AA", "onCreate: Android Auto")

        SongHelper.initPlayer(this)
        player = SongHelper.player

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

        player.repeatMode = Player.REPEAT_MODE_ALL

        session = MediaLibrarySession.Builder(this, player, LibrarySessionCallback()).setId("AutoSession").build()


        if (tracklist.isNotEmpty()) return

        saveManager(this).loadSettings()

        serviceIOScope.launch {
            tracklist.clear()

            for (song in songsList) {
                val mediaMetadata = MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .setArtworkUri(song.imageUrl)
                    .setReleaseYear(song.year?.toIntOrNull() ?: 0)
                    .setExtras(Bundle().apply {
                        putInt("duration", song.duration)
                        putString("MoreInfo", "${song.format} • ${song.bitrate}")
                        putString("NavidromeID", song.navidromeID)
                        putBoolean("isRadio", false)
                    })
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build()
                tracklist.add(MediaItem.fromUri(song.media.toString())
                    .buildUpon()
                    .setMediaId(song.media.toString())
                    .setMediaMetadata(mediaMetadata).build()
                )
            }

            Log.d("AA", "Added Songs To Android Auto!")
            session?.notifyChildrenChanged("nodeTRACKLIST", tracklist.size, null)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return session
    }

    override fun onDestroy() {
        session?.run {
            SongHelper.releasePlayer()
            release()
            session = null
        }
        super.onDestroy()
    }

    private inner class LibrarySessionCallback : MediaLibrarySession.Callback {
        @UnstableApi
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

    }
}
