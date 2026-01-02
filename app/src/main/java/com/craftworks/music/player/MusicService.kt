package com.craftworks.music.player

import android.app.PendingIntent
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
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
import com.craftworks.music.MainActivity
import com.craftworks.music.R
import com.craftworks.music.data.model.toMediaItem
import com.craftworks.music.data.repository.AlbumRepository
import com.craftworks.music.data.repository.ArtistRepository
import com.craftworks.music.data.repository.LyricsRepository
import com.craftworks.music.data.repository.PlaylistRepository
import com.craftworks.music.data.repository.RadioRepository
import com.craftworks.music.data.repository.SongRepository
import com.craftworks.music.data.repository.StarredRepository
import com.craftworks.music.managers.NavidromeManager
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.Collections
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

    private var scrobbleJob: Job? = null
    private var playerListener: Player.Listener? = null

    @Inject lateinit var playbackSettingsManager: PlaybackSettingsManager

    @Inject lateinit var albumRepository: AlbumRepository
    @Inject lateinit var artistRepository: ArtistRepository
    @Inject lateinit var songRepository: SongRepository
    @Inject lateinit var radioRepository: RadioRepository
    @Inject lateinit var playlistRepository: PlaylistRepository
    @Inject lateinit var lyricsRepository: LyricsRepository
    @Inject lateinit var starredRepository: StarredRepository
    @Inject lateinit var offlineMediaResolver: OfflineMediaResolver

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
                .setExtras(Bundle().apply {
                    putInt(
                        MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
                        MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
                    )
                })
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
                .setExtras(Bundle().apply {
                    putInt(
                        MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
                        MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
                    )
                })
                .build()
        )
        .build()

    private val favoritesItem = MediaItem.Builder()
        .setMediaId("nodeFAVORITES")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                .setTitle("Favorites")
                .setExtras(Bundle().apply {
                    putInt(
                        MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
                        MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM
                    )
                })
                .build()
        )
        .build()

    private val shuffleAllItem = MediaItem.Builder()
        .setMediaId("action_shuffle_all")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle("Shuffle All")
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .build()
        )
        .build()

    private val albumsItem = MediaItem.Builder()
        .setMediaId("nodeALBUMS")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS)
                .setTitle("Albums")
                .setExtras(Bundle().apply {
                    putInt(
                        MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
                        MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM
                    )
                })
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
                .setExtras(Bundle().apply {
                    putInt(
                        MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
                        MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM
                    )
                })
                .build()
        )
        .build()

    private val rootHierarchy = listOf(
        shuffleAllItem,
        homeItem,
        albumsItem,
        artistsItem,
        favoritesItem,
        playlistsItem,
        radiosItem
    )

    private val serviceMainScope = CoroutineScope(Dispatchers.Main + Job())
    private val serviceIOScope = CoroutineScope(Dispatchers.IO + Job())

    // Thread-safe synchronized lists for Android Auto browsing
    private val aHomeScreenItems: MutableList<MediaItem> = Collections.synchronizedList(mutableListOf())
    private val aRadioScreenItems: MutableList<MediaItem> = Collections.synchronizedList(mutableListOf())
    private val aPlaylistScreenItems: MutableList<MediaItem> = Collections.synchronizedList(mutableListOf())
    private val aAlbumScreenItems: MutableList<MediaItem> = Collections.synchronizedList(mutableListOf())
    private val aArtistScreenItems: MutableList<MediaItem> = Collections.synchronizedList(mutableListOf())
    private val aFavoriteScreenItems: MutableList<MediaItem> = Collections.synchronizedList(mutableListOf())
    private val aFolderSongs: MutableList<MediaItem> = Collections.synchronizedList(mutableListOf())

    //endregion

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        // Clear any stale instance before setting new one
        instance?.let { oldInstance ->
            if (oldInstance !== this) {
                Log.w("AA", "Replacing stale service instance")
            }
        }
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

        // Use AtomicBoolean for thread-safe scrobble state
        val playerScrobbled = java.util.concurrent.atomic.AtomicBoolean(false)

        playerListener = object : Player.Listener {
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

                playerScrobbled.set(false)

                super.onMediaItemTransition(mediaItem, reason)

                serviceIOScope.launch {
                    try {
                        songRepository.scrobbleSong(mediaItem?.mediaMetadata?.extras?.getString("navidromeID") ?: "", false)
                        lyricsRepository.getLyrics(mediaItem?.mediaMetadata)
                    } catch (e: Exception) {
                        Log.e("PLAYER", "Error scrobbling or fetching lyrics", e)
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                error.printStackTrace()
                Log.e("PLAYER", error.stackTraceToString())
            }
        }
        playerListener?.let { player.addListener(it) }

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
                // Guard against accessing player after release
                if (!::player.isInitialized) {
                    delay(1000)
                    continue
                }

                try {
                    val duration = player.duration
                    val mediaItem = player.currentMediaItem

                    if (duration > 0 && !playerScrobbled.get()) {
                        val currentPosition = player.currentPosition
                        // Use Double to avoid integer overflow and precision loss
                        val progress = (currentPosition.toDouble() * 100.0 / duration.toDouble()).toInt()
                        val scrobblePercentage = playbackSettingsManager.scrobblePercentFlow.first() * 10

                        if (progress >= scrobblePercentage) {
                            playerScrobbled.set(true)
                            if (NavidromeManager.checkActiveServers() &&
                                mediaItem?.mediaMetadata?.extras?.getString("navidromeID")
                                    ?.startsWith("Local") == false &&
                                mediaItem.mediaMetadata.mediaType != MediaMetadata.MEDIA_TYPE_RADIO_STATION
                            ) {
                                serviceIOScope.launch {
                                    try {
                                        songRepository.scrobbleSong(mediaItem.mediaMetadata.extras?.getString("navidromeID") ?: "", true)
                                    } catch (e: Exception) {
                                        Log.e("PLAYER", "Error submitting scrobble", e)
                                    }
                                }
                            }
                        }
                    }
                } catch (e: IllegalStateException) {
                    // Player may have been released during access
                    Log.d("PLAYER", "Player released during scrobble check")
                }
                delay(1000)
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
                try {
                    println("ONPOSTCONNTECT MUSIC SERVICE!")

                    if (session.isAutoCompanionController(controller))
                        getHomeScreenItems()

                    this@ChoraMediaLibraryService.session?.notifyChildrenChanged(
                        "nodeHOME",
                        aHomeScreenItems.size,
                        null
                    )
                } catch (e: Exception) {
                    Log.e("MusicService", "Error in onPostConnect", e)
                }
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
            // Handle empty media items list to prevent IndexOutOfBoundsException
            if (mediaItems.isEmpty()) {
                return Futures.immediateFuture(
                    MediaItemsWithStartPosition(emptyList(), 0, 0L)
                )
            }

            // Handle Shuffle All action
            if (mediaItems.firstOrNull()?.mediaId == "action_shuffle_all") {
                val shuffleFuture = SettableFuture.create<MediaItemsWithStartPosition>()
                serviceIOScope.launch {
                    try {
                        val randomSongs = songRepository.getRandomSongs(100)
                        val resolvedItems = resolveMediaItemsForPlayback(randomSongs)
                        withContext(Dispatchers.Main) {
                            player.shuffleModeEnabled = true
                        }
                        SongHelper.currentTracklist = resolvedItems.toMutableList()
                        shuffleFuture.set(MediaItemsWithStartPosition(resolvedItems, 0, 0L))
                    } catch (e: Exception) {
                        Log.e("MusicService", "Error handling shuffle all", e)
                        shuffleFuture.set(MediaItemsWithStartPosition(emptyList(), 0, 0L))
                    }
                }
                return shuffleFuture
            }

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

            val connectivityManager =
                this@ChoraMediaLibraryService.baseContext.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

            val networkCapabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)

            // Resolve offline media items asynchronously - move ALL suspend calls into the coroutine
            val resultFuture = SettableFuture.create<MediaItemsWithStartPosition>()

            serviceIOScope.launch {
                // Get bitrate inside the coroutine to avoid runBlocking
                val bitrate: String? = when {
                    networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> {
                        Log.d("NetworkCheck", "Device is on Wi-Fi")
                        playbackSettingsManager.wifiTranscodingBitrateFlow.first()
                    }
                    networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> {
                        Log.d("NetworkCheck", "Device is on Mobile Data")
                        playbackSettingsManager.mobileDataTranscodingBitrateFlow.first()
                    }
                    else -> {
                        Log.d("NetworkCheck", "Device is on another network type")
                        playbackSettingsManager.wifiTranscodingBitrateFlow.first()
                    }
                }

                val bitrateOptions = if (bitrate != null && bitrate != "No Transcoding" && bitrate.isNotEmpty()) {
                    "&maxBitRate=$bitrate&format=${playbackSettingsManager.transcodingFormatFlow.first()}"
                } else {
                    ""
                }
                try {
                    val resolvedItems = currentTracklist.map { mediaItem ->
                        val songId = mediaItem.mediaMetadata.extras?.getString("navidromeID")
                        val offlinePath = if (songId != null) {
                            offlineMediaResolver.getOfflinePath(songId)
                        } else null

                        if (offlinePath != null) {
                            // Use offline file - no transcoding needed
                            Log.d("OfflinePlayback", "Using offline file for: ${mediaItem.mediaMetadata.title}")
                            MediaItem.Builder()
                                .setMediaId(mediaItem.mediaId)
                                .setMediaMetadata(mediaItem.mediaMetadata)
                                .setUri(offlinePath)
                                .build()
                        } else {
                            // Use streaming URL with transcoding options
                            MediaItem.Builder()
                                .setMediaId(mediaItem.mediaId)
                                .setMediaMetadata(mediaItem.mediaMetadata)
                                .setUri(mediaItem.mediaId + bitrateOptions)
                                .build()
                        }
                    }

                    val result = MediaItemsWithStartPosition(
                        resolvedItems,
                        if (updatedStartIndex != -1) updatedStartIndex else startIndex,
                        startPositionMs
                    )
                    resultFuture.set(result)
                } catch (e: Exception) {
                    Log.e("MusicService", "Error resolving offline media items", e)
                    resultFuture.setException(e)
                }
            }

            return resultFuture
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
            // For root hierarchy, return immediately
            if (parentId == "nodeROOT") {
                return Futures.immediateFuture(LibraryResult.ofItemList(rootHierarchy, params))
            }

            // For all other nodes, use async operations
            val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
            val offset = page * pageSize
            val limit = pageSize.coerceIn(1, 100)

            serviceIOScope.launch {
                try {
                    val items: List<MediaItem> = when {
                        parentId == "nodeHOME" -> getHomeScreenItems()
                        parentId == "nodeALBUMS" -> getAlbumItems(limit, offset)
                        parentId == "nodeARTISTS" -> getArtistItems(limit, offset)
                        parentId == "nodeFAVORITES" -> getFavoriteItems()
                        parentId == "nodeRADIOS" -> getRadioItems()
                        parentId == "nodePLAYLISTS" -> getPlaylistItems()
                        parentId.startsWith("artist_") -> {
                            val artistId = parentId.removePrefix("artist_")
                            getArtistAlbums(artistId)
                        }
                        else -> {
                            val mediaItem =
                                aHomeScreenItems.find { it.mediaId == parentId }
                                    ?: aPlaylistScreenItems.find { it.mediaId == parentId }
                                    ?: aAlbumScreenItems.find { it.mediaId == parentId }
                                    ?: aFavoriteScreenItems.find { it.mediaId == parentId }
                            getFolderItems(
                                parentId,
                                mediaItem?.mediaMetadata?.mediaType
                                    ?: MediaMetadata.MEDIA_TYPE_ALBUM
                            )
                        }
                    }
                    future.set(LibraryResult.ofItemList(items, params))
                } catch (e: Exception) {
                    Log.e("MusicService", "Error in onGetChildren for $parentId", e)
                    future.set(LibraryResult.ofError(SessionError.ERROR_UNKNOWN))
                }
            }
            return future
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
                try {
                    Log.d("RESUMPTION", "Getting onPlaybackResumption")
                    // Use .first() instead of collectLatest to get only one emission
                    // and avoid calling settable.set() multiple times (which throws IllegalStateException)
                    val playbackResumptionList = LocalDataSettingsManager(applicationContext)
                        .playbackResumptionPlaylistWithStartPosition.first()

                    val mediaItems = playbackResumptionList.mediaItems
                    if (mediaItems.isEmpty()) {
                        Log.w("RESUMPTION", "Empty playlist, skipping resumption")
                        // Return empty result instead of failing
                        settable.set(MediaItemsWithStartPosition(emptyList(), 0, 0L))
                        return@launch
                    }

                    settable.set(playbackResumptionList)
                    Log.d("RESUMPTION", "Got mediaitems")

                    withContext(Dispatchers.Main) {
                        player.setMediaItems(mediaItems)
                        player.prepare()
                        player.playWhenReady = true

                        // Validate startIndex is within bounds to prevent IndexOutOfBoundsException
                        // Use maxOf(0, size-1) to handle edge case where size-1 could be negative
                        val safeStartIndex = playbackResumptionList.startIndex.coerceIn(0, maxOf(0, mediaItems.size - 1))
                        player.seekTo(safeStartIndex, playbackResumptionList.startPositionMs)

                        SongHelper.currentTracklist = mediaItems

                        Log.d(
                            "RESUMPTION",
                            "Set playlist: ${mediaItems.map { it.mediaMetadata.title }} at index $safeStartIndex with position ${playbackResumptionList.startPositionMs}"
                        )
                    }
                } catch (e: Exception) {
                    Log.e("RESUMPTION", "Error during playback resumption", e)
                    settable.set(MediaItemsWithStartPosition(emptyList(), 0, 0L))
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
            val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

            serviceIOScope.launch {
                try {
                    val results = mutableListOf<MediaItem>()

                    // Albums first - grouped
                    albumRepository.searchAlbum(query).forEach { album ->
                        results.add(album.withGroupTitle("Albums"))
                    }

                    // Artists second - grouped
                    artistRepository.getArtists("alphabeticalByName", 50, 0)
                        .filter { it.name.contains(query, ignoreCase = true) }
                        .forEach { artist ->
                            results.add(
                                MediaItem.Builder()
                                    .setMediaId("artist_${artist.navidromeID}")
                                    .setMediaMetadata(
                                        MediaMetadata.Builder()
                                            .setTitle(artist.name)
                                            .setArtist(artist.name)
                                            .setArtworkUri(Uri.parse(artist.artistImageUrl ?: ""))
                                            .setIsBrowsable(true)
                                            .setIsPlayable(false)
                                            .setMediaType(MediaMetadata.MEDIA_TYPE_ARTIST)
                                            .setExtras(Bundle().apply {
                                                putString(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_GROUP_TITLE, "Artists")
                                            })
                                            .build()
                                    )
                                    .build()
                            )
                        }

                    // Songs third - grouped
                    val songs = songRepository.getSongs(query).take(20)
                    songs.forEach { song ->
                        results.add(song.withGroupTitle("Songs"))
                    }

                    // Playlists fourth - grouped
                    playlistRepository.getPlaylists().fastFilter {
                        it.mediaMetadata.title?.contains(query, ignoreCase = true) == true
                    }.forEach { playlist ->
                        results.add(playlist.withGroupTitle("Playlists"))
                    }

                    // Store songs for playback
                    SongHelper.currentTracklist = songs.toMutableList()

                    future.set(LibraryResult.ofItemList(results, LibraryParams.Builder().build()))
                } catch (e: Exception) {
                    Log.e("MusicService", "Error in onGetSearchResult", e)
                    future.set(LibraryResult.ofError(SessionError.ERROR_UNKNOWN))
                }
            }
            return future
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<Void>> {
            Log.d("AA", "onSearch: $query")

            serviceIOScope.launch {
                try {
                    val totalCount = songRepository.getSongs(query).size +
                            albumRepository.searchAlbum(query).size +
                            artistRepository.getArtists("alphabeticalByName", 50, 0)
                                .filter { it.name.contains(query, ignoreCase = true) }.size +
                            playlistRepository.getPlaylists().fastFilter {
                                it.mediaMetadata.title?.contains(query, ignoreCase = true) == true
                            }.size

                    session.notifySearchResultChanged(
                        browser,
                        query,
                        totalCount,
                        LibraryParams.Builder().build()
                    )
                } catch (e: Exception) {
                    Log.e("MusicService", "Error in onSearch", e)
                }
            }

            return Futures.immediateFuture(LibraryResult.ofVoid())
        }
    }


    override fun onDestroy() {
        saveState()
        scrobbleJob?.cancel()
        scrobbleJob = null
        // Cancel coroutine scopes to prevent memory leaks and orphaned coroutines
        (serviceMainScope.coroutineContext[Job])?.cancel()
        (serviceIOScope.coroutineContext[Job])?.cancel()
        session?.release()
        session = null
        if (::player.isInitialized) {
            playerListener?.let { player.removeListener(it) }
            playerListener = null
            player.release()
        }
        instance = null
        super.onDestroy()
    }

    fun saveState() {
        if (!::player.isInitialized) {
            Log.w("AA", "Cannot save state - player not initialized")
            return
        }

        try {
            // Capture player state on the current thread before async save
            val mediaItemCount = player.mediaItemCount
            val mediaItems = List(mediaItemCount) { i -> player.getMediaItemAt(i) }
            val currentIndex = player.currentMediaItemIndex
            val currentPosition = player.currentPosition

            Log.d(
                "AA",
                "Saving state! Playlist: ${mediaItems.map { it.mediaMetadata.title }}, current index: $currentIndex, current position: $currentPosition"
            )

            // Use runBlocking with timeout to prevent ANR, but this is still called from main
            // Note: This is acceptable in onDestroy since we need to complete before the service dies
            runBlocking {
                kotlinx.coroutines.withTimeout(5000L) {
                    LocalDataSettingsManager(applicationContext).setPlaybackResumption(
                        mediaItems,
                        currentIndex,
                        currentPosition
                    )
                }
            }
        } catch (e: IllegalStateException) {
            Log.e("AA", "Error saving state - player may be released", e)
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Log.e("AA", "Timeout saving playback state", e)
        }
    }

    //region getChildren - All suspend functions for async Android Auto browsing

    private suspend fun getHomeScreenItems(): List<MediaItem> = coroutineScope {
        Log.d("AA", "Getting home screen items")
        if (aHomeScreenItems.isEmpty()) {
            // Fetch all sections concurrently
            val recentlyPlayed = async { albumRepository.getAlbums("recent", 6) }
            val recentlyAdded = async { albumRepository.getAlbums("newest", 6) }
            val mostPlayed = async { albumRepository.getAlbums("frequent", 6) }
            val randomAlbums = async { albumRepository.getAlbums("random", 6) }

            recentlyPlayed.await().forEach { album ->
                aHomeScreenItems.add(album.withGroupTitle(getString(R.string.recently_played)))
            }

            recentlyAdded.await().forEach { album ->
                aHomeScreenItems.add(album.withGroupTitle(getString(R.string.recently_added)))
            }

            mostPlayed.await().forEach { album ->
                aHomeScreenItems.add(album.withGroupTitle(getString(R.string.most_played)))
            }

            randomAlbums.await().forEach { album ->
                aHomeScreenItems.add(album.withGroupTitle(getString(R.string.random_songs)))
            }
        }
        aHomeScreenItems
    }

    private suspend fun getAlbumItems(limit: Int, offset: Int): List<MediaItem> {
        Log.d("AA", "Getting album items: limit=$limit, offset=$offset")
        val albums = albumRepository.getAlbums("alphabeticalByName", limit, offset)
        aAlbumScreenItems.clear()
        aAlbumScreenItems.addAll(albums)
        return albums
    }

    private suspend fun getArtistItems(limit: Int, offset: Int): List<MediaItem> {
        Log.d("AA", "Getting artist items: limit=$limit, offset=$offset")
        val artists = artistRepository.getArtists("alphabeticalByName", limit, offset)
        aArtistScreenItems.clear()
        aArtistScreenItems.addAll(
            artists.map { artist ->
                MediaItem.Builder()
                    .setMediaId("artist_${artist.navidromeID}")
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(artist.name)
                            .setArtist(artist.name)
                            .setArtworkUri(Uri.parse(artist.artistImageUrl ?: ""))
                            .setIsBrowsable(true)
                            .setIsPlayable(false)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_ARTIST)
                            .setExtras(Bundle().apply {
                                putString("navidromeID", artist.navidromeID)
                                putInt("albumCount", artist.albumCount ?: 0)
                            })
                            .build()
                    )
                    .build()
            }
        )
        return aArtistScreenItems
    }

    private suspend fun getArtistAlbums(artistId: String): List<MediaItem> {
        Log.d("AA", "Getting albums for artist: $artistId")
        return artistRepository.getArtistAlbums(artistId)
    }

    private suspend fun getFavoriteItems(): List<MediaItem> {
        Log.d("AA", "Getting favorite items")
        aFavoriteScreenItems.clear()
        val starred = starredRepository.getStarredItems()
        aFavoriteScreenItems.addAll(starred)
        return starred
    }

    private suspend fun getRadioItems(): List<MediaItem> {
        Log.d("AA", "Getting radio items")
        aRadioScreenItems.clear()
        aRadioScreenItems.addAll(
            radioRepository.getRadios().map { radio ->
                radio.toMediaItem()
            }
        )
        SongHelper.currentTracklist = aRadioScreenItems
        return aRadioScreenItems
    }

    private suspend fun getPlaylistItems(): List<MediaItem> {
        Log.d("AA", "Getting playlist items")
        if (aPlaylistScreenItems.isEmpty()) {
            aPlaylistScreenItems.addAll(playlistRepository.getPlaylists())
        }
        return aPlaylistScreenItems
    }

    private suspend fun getFolderItems(parentId: String, type: Int): List<MediaItem> {
        Log.d("AA", "Getting folder items: $parentId, type=$type")
        aFolderSongs.clear()
        when (type) {
            MediaMetadata.MEDIA_TYPE_ALBUM -> {
                val albumSongs = albumRepository.getAlbum(parentId)
                aFolderSongs.addAll(
                    if (albumSongs != null && albumSongs.size > 1) albumSongs.subList(1, albumSongs.size) else emptyList()
                )
            }
            MediaMetadata.MEDIA_TYPE_PLAYLIST -> {
                aFolderSongs.addAll(playlistRepository.getPlaylistSongs(parentId))
            }
            else -> aFolderSongs.clear()
        }
        SongHelper.currentTracklist = aFolderSongs
        return aFolderSongs
    }

    //endregion

    //region Helper functions

    private fun MediaItem.withGroupTitle(title: String): MediaItem {
        return MediaItem.Builder()
            .setMediaId(this.mediaId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .populate(this.mediaMetadata)
                    .setExtras(Bundle().apply {
                        this@withGroupTitle.mediaMetadata.extras?.let { putAll(it) }
                        putString(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_GROUP_TITLE, title)
                    })
                    .build()
            )
            .build()
    }

    private suspend fun resolveMediaItemsForPlayback(mediaItems: List<MediaItem>): List<MediaItem> {
        val connectivityManager = baseContext.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)

        val bitrate: String? = when {
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> {
                playbackSettingsManager.wifiTranscodingBitrateFlow.first()
            }
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> {
                playbackSettingsManager.mobileDataTranscodingBitrateFlow.first()
            }
            else -> {
                playbackSettingsManager.wifiTranscodingBitrateFlow.first()
            }
        }

        val bitrateOptions = if (bitrate != null && bitrate != "No Transcoding" && bitrate.isNotEmpty()) {
            "&maxBitRate=$bitrate&format=${playbackSettingsManager.transcodingFormatFlow.first()}"
        } else {
            ""
        }

        return mediaItems.map { mediaItem ->
            val songId = mediaItem.mediaMetadata.extras?.getString("navidromeID")
            val offlinePath = if (songId != null) {
                offlineMediaResolver.getOfflinePath(songId)
            } else null

            if (offlinePath != null) {
                MediaItem.Builder()
                    .setMediaId(mediaItem.mediaId)
                    .setMediaMetadata(mediaItem.mediaMetadata)
                    .setUri(offlinePath)
                    .build()
            } else {
                MediaItem.Builder()
                    .setMediaId(mediaItem.mediaId)
                    .setMediaMetadata(mediaItem.mediaMetadata)
                    .setUri(mediaItem.mediaId + bitrateOptions)
                    .build()
            }
        }
    }

    //endregion
}