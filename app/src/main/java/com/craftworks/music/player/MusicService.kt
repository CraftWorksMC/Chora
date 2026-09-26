package com.craftworks.music.player

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
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
import androidx.media3.common.Rating
import androidx.media3.common.StarRating
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaConstants
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.craftworks.music.MainActivity
import com.craftworks.music.R
import com.craftworks.music.data.model.AlbumArtistListSort
import com.craftworks.music.data.model.AlbumListSort
import com.craftworks.music.data.model.MediaQuery
import com.craftworks.music.data.model.PlaylistListSort
import com.craftworks.music.data.model.ProviderFeature
import com.craftworks.music.data.model.Screen
import com.craftworks.music.data.model.ScrobbleEvent
import com.craftworks.music.data.model.SongListSort
import com.craftworks.music.data.model.SortOrder
import com.craftworks.music.data.model.id
import com.craftworks.music.data.repository.AlbumRepository
import com.craftworks.music.data.repository.ArtistRepository
import com.craftworks.music.data.repository.LyricsRepository
import com.craftworks.music.data.repository.PlaylistRepository
import com.craftworks.music.data.repository.RadioRepository
import com.craftworks.music.data.repository.SongRepository
import com.craftworks.music.managers.MediaProviderManager
import com.craftworks.music.managers.TranscodeManager
import com.craftworks.music.managers.settings.AppearanceSettingsManager
import com.craftworks.music.managers.settings.LocalDataSettingsManager
import com.craftworks.music.managers.settings.PlaybackSettingsManager
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import kotlin.math.pow
import kotlin.time.Duration.Companion.seconds

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

    private var scrobbleJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var _sleepTimerRemainingTime = MutableStateFlow(0)
    val sleepTimerRemainingTime: StateFlow<Int> = _sleepTimerRemainingTime.asStateFlow()

    @Inject lateinit var appearanceSettingsManager: AppearanceSettingsManager
    @Inject lateinit var playbackSettingsManager: PlaybackSettingsManager
    @Inject lateinit var transcodeManager: TranscodeManager

    @Inject lateinit var albumRepository: AlbumRepository
    @Inject lateinit var artistRepository: ArtistRepository
    @Inject lateinit var songRepository: SongRepository
    @Inject lateinit var radioRepository: RadioRepository
    @Inject lateinit var playlistRepository: PlaylistRepository
    @Inject lateinit var lyricsRepository: LyricsRepository

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

    private val albumsItem = MediaItem.Builder()
        .setMediaId("nodeALBUMS")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS)
                .setTitle("Albums")
                .build()
        )
        .build()

    private val artistsItem = MediaItem.Builder()
        .setMediaId("nodeARTISTS")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS)
                .setTitle("Artists")
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

    private var rootHierarchy = mutableListOf<MediaItem>()

    private val serviceMainScope = CoroutineScope(Dispatchers.Main)
    private val serviceIOScope = CoroutineScope(Dispatchers.IO)

    var aHomeScreenItems = mutableListOf<MediaItem>()
    var aAlbumScreenItems = mutableListOf<MediaItem>()
    var aArtistsScreenItems = mutableListOf<MediaItem>()
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
        serviceIOScope.launch {
            appearanceSettingsManager.bottomNavItemsFlow.collect { items ->
                val routeToItem = mapOf(
                    Screen.Home to homeItem,
                    Screen.Albums to albumsItem,
                    Screen.Artists to artistsItem,
                    Screen.Radios to radiosItem,
                    Screen.Playlists to playlistsItem
                )

                rootHierarchy = items
                    .filter { it.enabled }
                    .mapNotNull { routeToItem[it.screenRoute] }
                    .toMutableList()

                session?.notifyChildrenChanged("nodeROOT", 0, null)
            }
        }

        val resolvingDataSourceFactory = ResolvingDataSource.Factory(
            DefaultDataSource.Factory(this),
            object : ResolvingDataSource.Resolver {
                override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
                    val uri = dataSpec.uri
                    val scheme = dataSpec.uri.scheme

                    if (scheme != "media")
                        return dataSpec

                    val providerId = uri.authority ?: throw Exception("No provider for MediaItem")
                    val songId = uri.lastPathSegment ?: throw Exception("No ID for MediaItem")

                    val provider = MediaProviderManager.getProvider(providerId)
                    var actualStreamUrl = provider?.getStreamUrl(songId, false)
                        ?: throw Exception("Can't get streamUrl for mediaitem $songId")

                    val bitrate = runBlocking { transcodeManager.currentBitrateFlow.first() }
                    if (bitrate != "No Transcoding") {
                        val format = runBlocking { transcodeManager.currentFormatFlow.first() }

                        actualStreamUrl = provider.getStreamUrl(songId, true, bitrate.toInt(), format)
                    }

                    return dataSpec.withUri(actualStreamUrl.toUri())
                }
            }
        )

        val audioOffloadPreferences =
            TrackSelectionParameters.AudioOffloadPreferences.Builder()
                .setAudioOffloadMode(TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED)
                .build()

        player = ExoPlayer.Builder(this)
            .setSeekParameters(SeekParameters.EXACT)
            .setMediaSourceFactory(DefaultMediaSourceFactory(resolvingDataSourceFactory))
            .setWakeMode(
                if (MediaProviderManager.currentProvider.value?.featureFlags?.contains(
                        ProviderFeature.OFFLINE_PLAYBACK
                    ) ?: false)
                    C.WAKE_MODE_NETWORK
                else
                    C.WAKE_MODE_LOCAL
            )
            .setHandleAudioBecomingNoisy(true)
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .build()

        // todo: make it a setting
        player.trackSelectionParameters =
            player.trackSelectionParameters
                .buildUpon()
                .setAudioOffloadPreferences(audioOffloadPreferences)
                .build()

        player.repeatMode = Player.REPEAT_MODE_OFF
        player.shuffleModeEnabled = false

        var playerScrobbled = false

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // Apply ReplayGain
                player.volume = clamp(
                    (10f.pow(((mediaItem?.mediaMetadata?.extras?.getFloat("replayGain") ?: 0f) / 20f))),
                    0f,
                    1f
                )
                Log.d("REPLAY GAIN", "Setting ReplayGain to ${player.volume}")

                playerScrobbled = false

                super.onMediaItemTransition(mediaItem, reason)

                serviceIOScope.launch {
                    lyricsRepository.getLyrics(mediaItem?.mediaMetadata)
                    val mediaId = mediaItem?.mediaMetadata?.id ?: return@launch
                    songRepository.scrobbleSong(mediaId, 0, 1f, ScrobbleEvent.START, false)
                }

                /*
                serviceMainScope.launch {
                    playbackSettingsManager.autoPlayFlow.collect {
                        if (it && player.currentMediaItemIndex == player.mediaItemCount - 1)
                            player.addMediaItems(songRepository.getSimilarSongs(mediaItem?.mediaMetadata?.extras?.getString("navidromeID")!!, 1))
                    }
                }
                */
            }

            override fun onPlayerError(error: PlaybackException) {
                error.printStackTrace()
                Log.e("PLAYER", error.stackTraceToString())

                Toast.makeText(
                    this@ChoraMediaLibraryService,
                    PlaybackException.getErrorCodeName(error.errorCode),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        val mainActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainActivityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        session = MediaLibrarySession.Builder(this, player, LibrarySessionCallback())
            .setId("AutoSession")
            .setSessionActivity(sessionActivityPendingIntent)
            .build()

        scrobbleJob = serviceMainScope.launch {
            while (isActive) {
                val duration = player.duration
                val mediaItem = player.currentMediaItem

                if (duration > 0 && !playerScrobbled) {
                    val currentPosition = player.currentPosition
                    val progress = (currentPosition * 100 / duration).toInt()
                    val scrobblePercentage = playbackSettingsManager.scrobblePercentFlow.first() * 10

                    if (progress >= scrobblePercentage) {
                        playerScrobbled = true
                        if (mediaItem?.mediaMetadata?.mediaType != MediaMetadata.MEDIA_TYPE_RADIO_STATION) {
                            serviceIOScope.launch {
                                songRepository.scrobbleSong(mediaItem?.mediaMetadata?.id ?: "", currentPosition.toInt(), 1f,
                                    ScrobbleEvent.START , true)
                            }
                        }
                    }
                }
                delay(1000)
            }
        }

        serviceMainScope.launch {
            transcodeManager.transcodingConfigChangesFlow
                .distinctUntilChanged()
                .collect {
                    if (player.playbackState == Player.STATE_READY || player.playbackState == Player.STATE_BUFFERING) {
                        if (player.currentTimeline.isEmpty.not()) {
                            updateTranscodingDuringPlayback()
                        }
                    }
                }
        }

        Log.d("AA", "Initialized MediaLibraryService.")
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return session
    }

    private inner class LibrarySessionCallback : MediaLibrarySession.Callback {
        override fun onPostConnect(session: MediaSession, controller: MediaSession.ControllerInfo) {
            serviceIOScope.launch {
                println("ONPOSTCONNTECT MUSIC SERVICE!")

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

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>
        ): ListenableFuture<List<MediaItem>> {
            // Android Auto uses the legacy MediaController, so we need to do some very weird hacks to make it work nicely.

            // If only one item is requested, check if it belongs to a folder we've already loaded
            if (mediaItems.size == 1) {
                val requestedId = mediaItems[0].mediaId
                // Try to find the full item in the last browsed folder
                val fullItem = aFolderSongs.find { it.mediaId == requestedId }
                if (fullItem != null) {
                    val startIndex = aFolderSongs.indexOf(fullItem)
                    val folderQueue = aFolderSongs.subList(startIndex, aFolderSongs.size)
                    return Futures.immediateFuture(folderQueue)
                }

                // Not found in the current folder
                val cachedItem = aPlaylistScreenItems.find { it.mediaId == requestedId }
                    ?: aRadioScreenItems.find { it.mediaId == requestedId }
                    ?: aAlbumScreenItems.find { it.mediaId == requestedId }
                    ?: aArtistsScreenItems.find { it.mediaId == requestedId }

                if (cachedItem != null) {
                    return Futures.immediateFuture(listOf(cachedItem))
                }
            }

            return Futures.immediateFuture(mediaItems)
        }

        override fun onSetRating(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            rating: Rating
        ): ListenableFuture<SessionResult> {
            val currentItem = player.currentMediaItem
                ?: return Futures.immediateFuture(SessionResult(SessionError.ERROR_INVALID_STATE))

            val id = currentItem.mediaMetadata.id ?: ""
            val newRating = (rating as StarRating).starRating.toInt()

            runBlocking {
                songRepository.setSongRating(id, newRating)
            }

            val updatedItem = currentItem.buildUpon()
                .setMediaMetadata(
                    currentItem.mediaMetadata.buildUpon()
                        .setUserRating(rating)
                        .build()
                )
                .build()

            val index = player.currentMediaItemIndex
            if (player.currentMediaItem?.mediaMetadata?.id == id) {
                player.replaceMediaItem(index, updatedItem)
            }
            return super.onSetRating(session, controller, rating)
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
                            "nodeALBUMS" -> getAlbumScreenItems()
                            "nodeARTISTS" -> getArtistScreenItems()
                            "nodeRADIOS" -> getRadioItems()
                            "nodePLAYLISTS" -> getPlaylistItems()
                            else -> {
                                val mediaItem =
                                    aHomeScreenItems.find { it.mediaId == parentId }
                                        ?: aPlaylistScreenItems.find { it.mediaId == parentId }
                                        ?: aAlbumScreenItems.find { it.mediaId == parentId }
                                        ?: aArtistsScreenItems.find { it.mediaId == parentId }
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
                ?: aPlaylistScreenItems.find { it.mediaId == mediaId }
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
                    "nodeALBUMS" -> aAlbumScreenItems.size
                    "nodeARTISTS" -> aArtistsScreenItems.size
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
            controller: MediaSession.ControllerInfo,
            isToStream: Boolean
        ): ListenableFuture<MediaItemsWithStartPosition> {
            val settable = SettableFuture.create<MediaItemsWithStartPosition>()
            serviceMainScope.launch {
                try {
                    val playbackResumptionList = withTimeout(2.seconds) {
                        LocalDataSettingsManager(applicationContext)
                            .playbackResumptionPlaylistWithStartPosition
                            .first()
                    }
                    withContext(Dispatchers.Main) {
                        player.setMediaItems(playbackResumptionList.mediaItems)
                        player.prepare()
                        player.playWhenReady = true
                        player.seekTo(playbackResumptionList.startIndex, playbackResumptionList.startPositionMs)
                    }
                    settable.set(playbackResumptionList)
                } catch (e: Exception) {
                    Log.e("RESUMPTION", "Failed/timed out getting resumption state", e)
                    settable.setException(e)
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
                        songRepository.getSongs(MediaQuery.SongListQuery(sortBy = SongListSort.NAME, sortOrder = SortOrder.ASC, searchTerm = query, startIndex = 0)).toMutableList()
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
                    songRepository.getSongs(MediaQuery.SongListQuery(sortBy = SongListSort.NAME, sortOrder = SortOrder.ASC, searchTerm = query, startIndex = 0)).size +
                            albumRepository.getAlbums(MediaQuery.AlbumListQuery(sortBy = AlbumListSort.NAME, sortOrder = SortOrder.ASC, searchTerm = query, startIndex = 0)).size +
                            radioRepository.getRadios().fastFilter {
                                it.name.contains(query)
                            }.map { it.toMediaItem() }.size +
                            playlistRepository.getPlaylists(MediaQuery.PlaylistListQuery(sortBy = PlaylistListSort.NAME, sortOrder = SortOrder.ASC, searchTerm = query, startIndex = 0)).fastFilter {
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

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel() // Cancel any previously running timer

        if (minutes <= 0) {
            Log.d("SLEEPTIMER", "Sleep timer cancelled.")
            _sleepTimerRemainingTime.value = 0
            return
        }

        Log.d("SLEEPTIMER", "Sleep timer set for $minutes minutes.")

        sleepTimerJob = serviceMainScope.launch {
            var timeRemaining = minutes
            _sleepTimerRemainingTime.value = timeRemaining

            while (timeRemaining > 0) {
                delay(60 * 1000L)
                timeRemaining--
                _sleepTimerRemainingTime.value = timeRemaining
            }

            if (::player.isInitialized && player.isPlaying) {
                player.stop()
                Log.d("SLEEPTIMER", "Timer finished. Playback stopped.")
            }
        }
    }

    private fun updateTranscodingDuringPlayback() {
        val currentWindowIndex = player.currentMediaItemIndex
        val currentPlaybackPosition = player.currentPosition
        val wasPlaying = player.isPlaying

        val currentQueue = mutableListOf<MediaItem>()
        for (i in 0 until player.mediaItemCount) {
            currentQueue.add(player.getMediaItemAt(i))
        }

        player.setMediaItems(currentQueue, currentWindowIndex, currentPlaybackPosition)

        player.prepare()

        if (wasPlaying) {
            player.play()
        }
    }

    override fun onDestroy() {
        saveState()
        session?.release()
        scrobbleJob?.cancel()
        sleepTimerJob?.cancel()
        instance = null
        super.onDestroy()
    }

    fun saveState() {
        runBlocking {
            Log.d(
                "AA",
                "Saving state! Playlist: ${List(player.mediaItemCount) { i -> player.getMediaItemAt(i) }.map { it.mediaMetadata.title }}, current index: ${player.currentMediaItemIndex}, current position: ${player.currentPosition}"
            )

            LocalDataSettingsManager(applicationContext).setPlaybackResumption(
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
                val recentlyPlayedAlbums = async { albumRepository.getAlbums(MediaQuery.AlbumListQuery(sortBy = AlbumListSort.RECENTLY_PLAYED, sortOrder = SortOrder.DESC, limit = 6, startIndex = 0)) }.await()
                val mostPlayedAlbums = async { albumRepository.getAlbums(MediaQuery.AlbumListQuery(sortBy = AlbumListSort.PLAY_COUNT, sortOrder = SortOrder.DESC, limit = 6, startIndex = 0)) }.await()

                recentlyPlayedAlbums.forEach { album ->
                    aHomeScreenItems.add(
                        album.apply {
                            this.mediaMetadata.extras?.putString(
                                MediaConstants.EXTRAS_KEY_CONTENT_STYLE_GROUP_TITLE,
                                this@ChoraMediaLibraryService.getString(R.string.home_recently_played)
                            )
                        }
                    )
                }

                mostPlayedAlbums.forEach { album ->
                    aHomeScreenItems.add(
                        album.apply {
                            this.mediaMetadata.extras?.putString(
                                MediaConstants.EXTRAS_KEY_CONTENT_STYLE_GROUP_TITLE,
                                this@ChoraMediaLibraryService.getString(R.string.home_most_played)
                            )
                        }
                    )
                }
            }
        }
        return aHomeScreenItems
    }

    private fun getAlbumScreenItems() : MutableList<MediaItem> {
        println("GETTING ANDROID AUTO ALBUM SCREEN ITEMS")
        runBlocking {
            if (aAlbumScreenItems.isEmpty()) {
                while (true) {
                    val albums = async { albumRepository.getAlbums(MediaQuery.AlbumListQuery(sortBy = AlbumListSort.NAME, sortOrder = SortOrder.ASC, limit = 250, startIndex = aAlbumScreenItems.size)) }.await()
                    aAlbumScreenItems.addAll(albums)
                    if (albums.isEmpty()) {
                        break
                    }
                }
            }
        }
        return aAlbumScreenItems
    }

    private fun getArtistScreenItems() : MutableList<MediaItem> {
        println("GETTING ANDROID AUTO ARTIST SCREEN ITEMS")
        runBlocking {
            if (aArtistsScreenItems.isEmpty()) {
                val albums = async { artistRepository.getArtists(MediaQuery.AlbumArtistListQuery(sortBy = AlbumArtistListSort.NAME, sortOrder = SortOrder.ASC, startIndex = aArtistsScreenItems.size)) }.await()

                albums.forEach {
                    aArtistsScreenItems.add(it.toMediaItem())
                }
            }
        }
        return aArtistsScreenItems
    }

    private fun getRadioItems(): MutableList<MediaItem> {
        runBlocking {
            if (aRadioScreenItems.isEmpty()) {
                aRadioScreenItems.addAll(
                    radioRepository.getRadios().map { radio ->
                        Log.d("MediaItemTransition", radio.toString())
                        radio.toMediaItem()
                    }
                )
                Log.d("MediaItemTransition", "aRadioScreenItems: ${aRadioScreenItems.map { it.mediaMetadata }}")
            }
        }
        return aRadioScreenItems
    }

    private fun getPlaylistItems(): MutableList<MediaItem> {
        runBlocking {
            if (aPlaylistScreenItems.isEmpty()) {
                aPlaylistScreenItems.addAll(playlistRepository.getPlaylists(MediaQuery.PlaylistListQuery(sortBy = PlaylistListSort.NAME, sortOrder = SortOrder.ASC, startIndex = aPlaylistScreenItems.size)))
            }
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

                MediaMetadata.MEDIA_TYPE_ARTIST -> {
                    aFolderSongs.addAll(
                        artistRepository.getArtistAlbums(parentId)
                    )
                }

                else -> aFolderSongs.clear()
            }
        }

        return aFolderSongs
    }
    //endregion
}