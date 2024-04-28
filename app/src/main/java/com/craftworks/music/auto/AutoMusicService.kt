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
import com.craftworks.music.SongHelper
import com.craftworks.music.data.Song
import com.craftworks.music.data.songsList
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

class AutoMediaLibraryService : MediaLibraryService() {

    //region Vars
    lateinit var player: ExoPlayer

    private var session: MediaLibrarySession? = null

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
    val tracklist = mutableListOf<MediaItem>()

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
            .setRenderersFactory(
                DefaultRenderersFactory(applicationContext).setExtensionRendererMode(
                    DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER /* We prefer extensions, such as FFmpeg */
                ))
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

        player.repeatMode = Player.REPEAT_MODE_ALL
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

        session = MediaLibrarySession.Builder(this, player, LibrarySessionCallback()).setId("AutoSession").build()

        Log.d("AA", "Initialized Player: $player")

        player.setMediaItems(addMediaItems(songsList))
    }

    fun addMediaItems(currentList:List<Song>): List<MediaItem> {
        tracklist.clear()
        for (song in currentList) {
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
            tracklist.add(
                MediaItem.Builder()
                    .setMediaMetadata(mediaMetadata)
                    .setMediaId(song.media.toString())
                    .setUri(song.media.toString())
                    .build()
            )
        }
        println("tracklist size: ${tracklist.size}")
        println("tracklist items: $tracklist")
        Log.d("AA", "Added Songs To Android Auto!")

        session?.connectedControllers?.forEach {
            (session as MediaLibrarySession).notifyChildrenChanged(it, "nodeTRACKLIST", tracklist.size, /* params= */ null)
        }

        return tracklist
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return session
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
                it.buildUpon()
                    .setUri(it.mediaId)
                    .setMediaMetadata(it.mediaMetadata)
                    .build()
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

    override fun onDestroy() {
        session?.run {
            //SongHelper.releasePlayer()
            release()
            session = null
        }
        super.onDestroy()
    }
}