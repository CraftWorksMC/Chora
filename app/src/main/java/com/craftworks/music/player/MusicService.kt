package com.craftworks.music.player

import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.ui.util.fastFilter
import androidx.core.math.MathUtils.clamp
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
import androidx.media3.session.MediaConstants
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.SessionError
import com.craftworks.music.R
import com.craftworks.music.data.model.toMediaItem
import com.craftworks.music.data.repository.AlbumRepository
import com.craftworks.music.data.repository.PlaylistRepository
import com.craftworks.music.data.repository.RadioRepository
import com.craftworks.music.data.repository.SongRepository
import com.craftworks.music.lyrics.LyricsManager
import com.craftworks.music.managers.NavidromeManager
import com.craftworks.music.managers.SettingsManager
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.pow

/*
    Thanks to Yurowitz on StackOverflow for this! Used it as a template.
    https://stackoverflow.com/questions/76838126/can-i-define-a-medialibraryservice-without-an-app
*/

@UnstableApi
@AndroidEntryPoint
class ChoraMediaLibraryService : MediaLibraryService() {
    //region Vars
    lateinit var player: Player
    var session: MediaLibrarySession? = null

    @Inject lateinit var settingsManager: SettingsManager

    @Inject lateinit var albumRepository: AlbumRepository
    @Inject lateinit var songRepository: SongRepository
    @Inject lateinit var radioRepository: RadioRepository
    @Inject lateinit var playlistRepository: PlaylistRepository

    companion object {
        private var instance: ChoraMediaLibraryService? = null

        fun getInstance(): ChoraMediaLibraryService? {
            return instance
        }
    }

    private val rootItem = MediaItem.Builder()
        .setMediaId("nodeROOT")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setIsBrowsable(false)
                .setIsPlayable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                .build()
        )
        .build()

    private val homeItem = MediaItem.Builder()
        .setMediaId("nodeHOME")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS)
                .setTitle("Home")
                .setExtras(Bundle().apply {
                    putInt(
                        MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
                        MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM
                    )
                })
                .build()
        )
        .build()

    private val radiosItem = MediaItem.Builder()
        .setMediaId("nodeRADIOS")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_RADIO_STATIONS)
                .setTitle("Radios")
                .build()
        )
        .build()

    private val playlistsItem = MediaItem.Builder()
        .setMediaId("nodePLAYLISTS")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS)
                .setTitle("Playlists")
                .build()
        )
        .build()

    private val rootHierarchy = listOf(homeItem, radiosItem, playlistsItem)

    private val serviceMainScope = CoroutineScope(Dispatchers.Main)
    private val serviceIOScope = CoroutineScope(Dispatchers.IO)

    var aHomeScreenItems = mutableListOf<MediaItem>()
    var aRadioScreenItems = mutableListOf<MediaItem>()
    var aPlaylistScreenItems = mutableListOf<MediaItem>()

    var aFolderSongs = mutableListOf<MediaItem>()

    //endregion

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        instance = this

        Log.d("AA", "onCreate Android Auto")

        if (session == null)
            initializePlayer()
        else
            Log.d("AA", "MediaSession already initialized, not recreating")
    }

    @OptIn(UnstableApi::class)
    fun initializePlayer() {
        player = ExoPlayer.Builder(this)
            .setSeekParameters(SeekParameters.EXACT)
            .setWakeMode(
                if (NavidromeManager.checkActiveServers())
                    C.WAKE_MODE_NETWORK
                else
                    C.WAKE_MODE_LOCAL
            )
            .setHandleAudioBecomingNoisy(true)
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .build()

        player.repeatMode = Player.REPEAT_MODE_OFF
        player.shuffleModeEnabled = false

        var playerScrobbled: Boolean = false

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // Apply ReplayGain
                if (mediaItem?.mediaMetadata?.extras?.getFloat("replayGain") != null) {
                    player.volume = clamp(
                        (10f.pow(
                            ((mediaItem.mediaMetadata.extras?.getFloat("replayGain") ?: 0f) / 20f)
                        )), 0f, 1f
                    )
                    Log.d("REPLAY GAIN", "Setting ReplayGain to ${player.volume}")
                }

                playerScrobbled = false;

                super.onMediaItemTransition(mediaItem, reason)
                serviceIOScope.launch { LyricsManager.getLyrics(mediaItem?.mediaMetadata) }
            }

            override fun onPlayerError(error: PlaybackException) {
                error.printStackTrace()
                Log.e("PLAYER", error.stackTraceToString())
            }
        })

        session = MediaLibrarySession.Builder(this, player, LibrarySessionCallback())
            .setId("AutoSession")
            .build()

        /*
        // Thread to check if we should scrobble.
        Thread {
            val defaultThread = CoroutineScope(Dispatchers.Main)
            while (true) {
                defaultThread.launch {
                    val duration = player.duration
                    val mediaItem = player.currentMediaItem

                    println("Scrobble duration: $duration")
                    if (duration > 0 && !playerScrobbled) {
                        val currentPosition = player.currentPosition
                        val progress = (currentPosition * 100 / duration).toInt()

                        println("Scrobble Percentage: $progress")

                        if (progress >= SongHelper.minPercentageScrobble.intValue) {
                            playerScrobbled = true
                            if (NavidromeManager.checkActiveServers() &&
                                mediaItem?.mediaMetadata?.extras?.getString("navidromeID")?.startsWith("Local") == false &&
                                mediaItem.mediaMetadata.mediaType != MediaMetadata.MEDIA_TYPE_RADIO_STATION
                            ) {
                                serviceMainScope.launch {
                                    sendNavidromeGETRequest("scrobble.view?id=${mediaItem.mediaMetadata.extras?.getString("navidromeID")}&submission=true", true) // Never cache scrobbles
                                }
                            }
                        }
                    }
                }
                Thread.sleep(10_000) // sleep 10 seconds
            }
        }.start()
        */

        Log.d("AA", "Initialized MediaLibraryService.")
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return session
    }

    private inner class LibrarySessionCallback : MediaLibrarySession.Callback {
        override fun onPostConnect(session: MediaSession, controller: MediaSession.ControllerInfo) {
            serviceIOScope.launch {
                println("ONPOSTCONNTECT MUSIC SERVICE!")
                //NavidromeManager.init(this@ChoraMediaLibraryService)
                //LocalProviderManager.init(this@ChoraMediaLibraryService)

                if (session.isAutoCompanionController(controller))
                    getHomeScreenItems()

                this@ChoraMediaLibraryService.session?.notifyChildrenChanged(
                    "nodeHOME",
                    aHomeScreenItems.size,
                    null
                )
            }
            super.onPostConnect(session, controller)
        }

        @OptIn(UnstableApi::class)
        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<MediaItemsWithStartPosition> {
            // We need to use URI from requestMetaData because of https://github.com/androidx/media/issues/282
            val updatedStartIndex =
                SongHelper.currentTracklist.indexOfFirst { it.mediaId == mediaItems[0].mediaId }

            val currentTracklist =
                if (updatedStartIndex != -1) {
                    SongHelper.currentTracklist
                } else {
                    SongHelper.currentTracklist = mediaItems.toMutableList()
                    mediaItems
                }

            val updatedMediaItems: List<MediaItem> =
                currentTracklist.map { mediaItem ->
                    MediaItem.Builder()
                        .setMediaId(mediaItem.mediaId)
                        .setMediaMetadata(mediaItem.mediaMetadata)
                        .setUri(mediaItem.mediaId)
                        .build()
                }

            return super.onSetMediaItems(
                mediaSession,
                controller,
                updatedMediaItems,
                updatedStartIndex,
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

        @OptIn(UnstableApi::class)
        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            return Futures.immediateFuture(
                try {
                    LibraryResult.ofItemList(
                        when (parentId) {
                            "nodeROOT" -> rootHierarchy
                            "nodeHOME" -> getHomeScreenItems()
                            "nodeRADIOS" -> getRadioItems()
                            "nodePLAYLISTS" -> getPlaylistItems()
                            else -> {
                                val mediaItem =
                                    aHomeScreenItems.find { it.mediaId == parentId }
                                        ?: aPlaylistScreenItems.find { it.mediaId == parentId }
                                getFolderItems(
                                    parentId,
                                    mediaItem?.mediaMetadata?.mediaType
                                        ?: MediaMetadata.MEDIA_TYPE_ALBUM
                                )
                            }
                        }, params)
                } catch (_: Exception) {
                    LibraryResult.ofError(SessionError.ERROR_UNKNOWN)
                }
            )
        }


        @OptIn(UnstableApi::class)
        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val mediaItem = aFolderSongs.find { it.mediaId == mediaId }
                ?: aRadioScreenItems.find { it.mediaId == mediaId }
                ?: return Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))

            return Futures.immediateFuture(
                LibraryResult.ofItem(
                    mediaItem,
                    LibraryParams.Builder().build()
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
                    "nodeHOME" -> aHomeScreenItems.size
                    "nodeRADIOS" -> aRadioScreenItems.size
                    "nodePLAYLISTS" -> aPlaylistScreenItems.size
                    else -> 0
                },
                params
            )

            return Futures.immediateFuture(LibraryResult.ofVoid())
        }

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaItemsWithStartPosition> {
            val settable = SettableFuture.create<MediaItemsWithStartPosition>()
            serviceMainScope.launch {
                Log.d("RESUMPTION", "Getting onPlaybackResumption")
                SettingsManager(applicationContext).playbackResumptionPlaylistWithStartPosition.collectLatest { playbackResumptionList ->
                    settable.set(playbackResumptionList)
                    Log.d("RESUMPTION", "Got mediaitems")
                    withContext(Dispatchers.Main) {
                        player.setMediaItems(playbackResumptionList.mediaItems)
                        player.prepare()
                        player.playWhenReady = true

                        player.seekTo(playbackResumptionList.startIndex, playbackResumptionList.startPositionMs)

                        SongHelper.currentTracklist = playbackResumptionList.mediaItems

                        Log.d(
                            "RESUMPTION",
                            "Set playlist: ${playbackResumptionList.mediaItems.map { it.mediaMetadata.title }} at index ${playbackResumptionList.startIndex} with position ${playbackResumptionList.startPositionMs}"
                        )
                    }
                }
            }
            return settable
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            return Futures.immediateFuture(
                LibraryResult.ofItemList(
                    runBlocking {
                        SongHelper.currentTracklist = songRepository.getSongs(query).toMutableList()
                        SongHelper.currentTracklist
                    },
                    LibraryParams.Builder().build()
                )
            )
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<Void>> {
            println("onSearch!!!")

            session.notifySearchResultChanged(
                browser,
                query,
                runBlocking {
                    songRepository.getSongs(query).size +
                            albumRepository.searchAlbum(query).size +
                            radioRepository.getRadios().fastFilter {
                                it.name.contains(
                                    query
                                )
                            }.size +
                            playlistRepository.getPlaylists().fastFilter {
                                it.mediaMetadata.title?.contains(
                                    query
                                ) == true
                            }.size
                },
                LibraryParams.Builder().build()
            )

            return Futures.immediateFuture(LibraryResult.ofVoid())
        }
    }


    override fun onDestroy() {
        saveState()
        session?.release()
        instance = null
        super.onDestroy()
    }

    fun saveState() {
        runBlocking {
            Log.d(
                "AA",
                "Saving state! Playlist: ${List(player.mediaItemCount) { i -> player.getMediaItemAt(i) }.map { it.mediaMetadata.title }}, current index: ${player.currentMediaItemIndex}, current position: ${player.currentPosition}"
            )

            SettingsManager(applicationContext).setPlaybackResumption(
                List(player.mediaItemCount) { i ->
                    player.getMediaItemAt(i)
                },
                player.currentMediaItemIndex,
                player.currentPosition
            )
        }
    }

    //region getChildren
    private fun getHomeScreenItems(): MutableList<MediaItem> {
        println("GETTING ANDROID AUTO SCREEN ITEMS")
        runBlocking {
            if (aHomeScreenItems.isEmpty()) {
                val recentlyPlayedAlbums = async { albumRepository.getAlbums("recent", 6) }.await()
                val mostPlayedAlbums = async { albumRepository.getAlbums("frequent", 6) }.await()

                recentlyPlayedAlbums.forEach { album ->
                    aHomeScreenItems.add(
                        album.apply {
                            this.mediaMetadata.extras?.putString(
                                MediaConstants.EXTRAS_KEY_CONTENT_STYLE_GROUP_TITLE,
                                this@ChoraMediaLibraryService.getString(R.string.recently_played)
                            )
                        }
                    )
                }

                mostPlayedAlbums.forEach { album ->
                    aHomeScreenItems.add(
                        album.apply {
                            this.mediaMetadata.extras?.putString(
                                MediaConstants.EXTRAS_KEY_CONTENT_STYLE_GROUP_TITLE,
                                this@ChoraMediaLibraryService.getString(R.string.most_played)
                            )
                        }
                    )
                }
            }
        }
        return aHomeScreenItems
    }

    private fun getRadioItems(): MutableList<MediaItem> {
        runBlocking {
            if (aRadioScreenItems.isEmpty()) {
                radioRepository.getRadios().forEach { radio ->
                    aRadioScreenItems.add(radio.toMediaItem())
                }
            }
            SongHelper.currentTracklist = aRadioScreenItems
        }
        return aRadioScreenItems
    }

    private fun getPlaylistItems(): MutableList<MediaItem> {
        runBlocking {
            if (aPlaylistScreenItems.isEmpty()) {
                aPlaylistScreenItems.addAll(playlistRepository.getPlaylists())
            }
            SongHelper.currentTracklist = aPlaylistScreenItems
        }
        return aPlaylistScreenItems
    }

    private fun getFolderItems(parentId: String, type: Int): MutableList<MediaItem> {
        runBlocking {
            aFolderSongs.clear()
            when (type) {
                MediaMetadata.MEDIA_TYPE_ALBUM -> {
                    val albumSongs = async { albumRepository.getAlbum(parentId) }.await()
                    aFolderSongs.addAll(
                        albumSongs?.subList(1, albumSongs.size) ?: emptyList()
                    )
                }

                MediaMetadata.MEDIA_TYPE_PLAYLIST -> {
                    aFolderSongs.addAll(
                        playlistRepository.getPlaylistSongs(parentId)
                    )
                }

                else -> aFolderSongs.clear()
            }
            SongHelper.currentTracklist = aFolderSongs
        }

        return aFolderSongs
    }
    //endregion
}