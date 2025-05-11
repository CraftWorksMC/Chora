package com.craftworks.music.player

import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.ui.util.fastFilter
import androidx.core.math.MathUtils.clamp
import androidx.core.net.toUri
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
import com.craftworks.music.data.MediaData
import com.craftworks.music.lyrics.LyricsManager
import com.craftworks.music.managers.LocalProviderManager
import com.craftworks.music.managers.NavidromeManager
import com.craftworks.music.managers.SettingsManager
import com.craftworks.music.providers.getAlbum
import com.craftworks.music.providers.getAlbums
import com.craftworks.music.providers.getPlaylistDetails
import com.craftworks.music.providers.getPlaylists
import com.craftworks.music.providers.getRadios
import com.craftworks.music.providers.getSongs
import com.craftworks.music.providers.navidrome.markNavidromeSongAsPlayed
import com.craftworks.music.providers.searchAlbum
import com.craftworks.music.ui.viewmodels.GlobalViewModels
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.math.pow

/*
    Thanks to Yurowitz on StackOverflow for this! Used it as a template.
    https://stackoverflow.com/questions/76838126/can-i-define-a-medialibraryservice-without-an-app
*/

@UnstableApi
class ChoraMediaLibraryService : MediaLibraryService() {
    //region Vars
    lateinit var player: Player
    var session: MediaLibrarySession? = null

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

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)

                // Apply ReplayGain
                if (mediaItem?.mediaMetadata?.extras?.getFloat("replayGain") != null) {
                    player.volume = clamp(
                        (10f.pow(
                            ((mediaItem.mediaMetadata.extras?.getFloat("replayGain") ?: 0f) / 20f)
                        )), 0f, 1f
                    )
                    Log.d("REPLAY GAIN", "Setting ReplayGain to ${player.volume}")
                }

                serviceMainScope.launch {
                    if (NavidromeManager.checkActiveServers() && !SongHelper.currentSong.navidromeID.startsWith(
                            "Local"
                        )
                    ) {
                        async {
                            markNavidromeSongAsPlayed(
                                SongHelper.currentSong.navidromeID,
                                player.currentPosition.toFloat(),
                                player.duration.toFloat()
                            )
                        }
                    }
                }

                serviceIOScope.launch {
                    if (mediaItem?.mediaMetadata?.mediaType != MediaMetadata.MEDIA_TYPE_RADIO_STATION)
                        async { LyricsManager.getLyrics(mediaItem?.mediaMetadata) }
                }
//                    mediaItem?.toSong()?.let {
//                        SongHelper.currentSong = it
//                    }
            }

            override fun onPlayerError(error: PlaybackException) {
                error.printStackTrace()
                Log.e("PLAYER", error.stackTraceToString())
            }
        })

        session = MediaLibrarySession.Builder(this, player, LibrarySessionCallback())
            .setId("AutoSession")
            .build()

        Log.d("AA", "Initialized MediaLibraryService.")
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return session
    }

    private inner class LibrarySessionCallback : MediaLibrarySession.Callback {
        override fun onPostConnect(session: MediaSession, controller: MediaSession.ControllerInfo) {
            serviceIOScope.launch {
                println("ONPOSTCONNTECT MUSIC SERVICE!")
                NavidromeManager.init(this@ChoraMediaLibraryService)
                LocalProviderManager.init(this@ChoraMediaLibraryService)

                if (session.isAutoCompanionController(controller))
                    getHomeScreenItems()

                GlobalViewModels.refreshAll()

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
            val updatedStartIndex = SongHelper.currentTracklist.indexOfFirst { it.mediaId == mediaItems[0].mediaId }

            val currentTracklist =
                if (updatedStartIndex != -1) {
                    SongHelper.currentTracklist
                }
                else {
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
                SettingsManager(applicationContext).playbackResumptionPlaylistWithStartPosition.collectLatest {
                    settable.set(it)
                    Log.d("RESUMPTION", "Got mediaitems")
                    withContext(Dispatchers.Main) {
                        player.setMediaItems(it.mediaItems)
                        player.prepare()
                        player.playWhenReady = true

                        player.seekTo(it.startIndex, it.startPositionMs)

                        SongHelper.currentTracklist = it.mediaItems

                        Log.d(
                            "RESUMPTION",
                            "Set playlist: ${it.mediaItems.map { it.mediaMetadata.title }} at index ${it.startIndex} with position ${it.startPositionMs}"
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
                        SongHelper.currentTracklist = getSongs(query).toMutableList()
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
                    getSongs(query).size +
                            searchAlbum(query).size +
                            getRadios(this@ChoraMediaLibraryService).fastFilter {
                                it.name.contains(
                                    query
                                )
                            }.size +
                            getPlaylists().fastFilter { it.mediaMetadata.title?.contains(query) == true }.size
                },
                LibraryParams.Builder().build()
            )

            return Futures.immediateFuture(LibraryResult.ofVoid())
        }
    }

    override fun onDestroy() {
        saveState()

        instance = null
        super.onDestroy()
    }

    fun saveState() {
        runBlocking {
            Log.d(
                "AA",
                "Saving state! Playlist: ${SongHelper.currentTracklist.map { it.mediaMetadata.title }}, current index: ${player.currentMediaItemIndex}, current position: ${player.currentPosition}"
            )

            SettingsManager(applicationContext).setPlaybackResumption(
                SongHelper.currentTracklist,
                player.currentMediaItemIndex,
                player.currentPosition
            )
        }
    }

    //region Convert to media items
    private fun radioToMediaItem(radio: MediaData.Radio): MediaItem {
        val mediaMetadata =
            MediaMetadata.Builder()
                .setTitle(radio.name)
                .setArtist(radio.name)
                .setArtworkUri(
                    ("android.resource://com.craftworks.music/" + R.drawable.radioplaceholder).toUri()
                )
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
                .setExtras(Bundle().apply {
                    putString("navidromeID", radio.navidromeID)
                    putBoolean("isRadio", true)
                }).build()

        return MediaItem.Builder()
            .setMediaId(radio.media)
            .setUri(radio.media)
            .setMediaMetadata(mediaMetadata)
            .build()
    }

    private fun playlistToMediaItem(playlist: MediaData.Playlist): MediaItem {
        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(playlist.name)
            .setSubtitle(playlist.comment)
            .setArtworkUri(playlist.coverArt?.toUri())
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
            .setExtras(Bundle().apply {
                putString("navidromeID", playlist.navidromeID)
                putInt("Duration", playlist.duration)
            })
            .build()

        Log.d("AA", "Added ${mediaMetadata.title} to recently played albums")
        return MediaItem.Builder()
            .setMediaId(playlist.navidromeID)
            .setMediaMetadata(mediaMetadata)
            .build()
    }
    //endregion

    //region getChildren
    private fun getHomeScreenItems(): MutableList<MediaItem> {
        println("GETTING ANDROID AUTO SCREEN ITEMS")
        runBlocking {
            if (aHomeScreenItems.isEmpty()) {
                val recentlyPlayedAlbums = async { getAlbums("recent", 6) }.await()
                val mostPlayedAlbums = async { getAlbums("frequent", 6) }.await()

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
                getRadios(baseContext).forEach { radio ->
                    aRadioScreenItems.add(radioToMediaItem(radio))
                }
            }
            SongHelper.currentTracklist = aRadioScreenItems
        }
        return aRadioScreenItems
    }

    private fun getPlaylistItems(): MutableList<MediaItem> {
        runBlocking {
            if (aPlaylistScreenItems.isEmpty()) {
                getPlaylists()
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
                    val albumSongs = getAlbum(parentId)
                    aFolderSongs.addAll(
                        albumSongs?.subList(1, albumSongs.size) ?: emptyList()
                    )
                }

                MediaMetadata.MEDIA_TYPE_PLAYLIST -> {
                    val playlistSongs = getPlaylistDetails(parentId)
                    aFolderSongs.addAll(
                        playlistSongs?.subList(1, playlistSongs.size) ?: emptyList()
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