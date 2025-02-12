package com.craftworks.music.player

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.core.math.MathUtils.clamp
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.RequestMetadata
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
import androidx.media3.session.SessionError
import com.craftworks.music.R
import com.craftworks.music.data.MediaData
import com.craftworks.music.data.ReplayGain
import com.craftworks.music.lyrics.LyricsManager
import com.craftworks.music.managers.LocalProviderManager
import com.craftworks.music.managers.NavidromeManager
import com.craftworks.music.providers.getAlbum
import com.craftworks.music.providers.getAlbums
import com.craftworks.music.providers.getPlaylistDetails
import com.craftworks.music.providers.getPlaylists
import com.craftworks.music.providers.getRadios
import com.craftworks.music.providers.navidrome.markNavidromeSongAsPlayed
import com.craftworks.music.ui.viewmodels.GlobalViewModels
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
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
                    putInt(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE, MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM)
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

        Log.d("AA", "onCreate: Android Auto")

        if (session == null)
            initializePlayer()
        else
            Log.d("AA", "MediaSession already initialized, not recreating")
    }

    @OptIn(UnstableApi::class)
    fun initializePlayer(){

        player = ExoPlayer.Builder(this)
            .setSeekParameters(SeekParameters.EXACT)
            .setWakeMode(
                if (NavidromeManager.checkActiveServers())
                    C.WAKE_MODE_NETWORK
                else
                    C.WAKE_MODE_LOCAL)
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
                    player.volume = clamp((10f.pow(((mediaItem.mediaMetadata.extras?.getFloat("replayGain") ?: 0f) / 20f))), 0f, 1f)
                    Log.d("REPLAY GAIN", "Setting ReplayGain to ${player.volume}")
                }

                serviceIOScope.launch {
                    if (NavidromeManager.checkActiveServers() && !SongHelper.currentSong.navidromeID.startsWith("Local"))
                        async { markNavidromeSongAsPlayed(SongHelper.currentSong.navidromeID) }

                    if (SongHelper.currentSong.isRadio == false)
                        async { LyricsManager.getLyrics() }

                    SongHelper.currentSong = mediaItemToSong(mediaItem)
                }
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

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Check if the player is not ready to play or there are no items in the media queue
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
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

                this@ChoraMediaLibraryService.session?.notifyChildrenChanged("nodeHOME", aHomeScreenItems.size, null)
            }
            super.onPostConnect(session, controller)
        }

        @OptIn(UnstableApi::class)
        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            // We need to use URI from requestMetaData because of https://github.com/androidx/media/issues/282
            val currentTracklist =
                if (SongHelper.currentTracklist.find { it.mediaId == mediaItems[0].mediaId } != null)
                    SongHelper.currentTracklist
                else {
                    SongHelper.currentTracklist = mediaItems
                    mediaItems
                }

            val updatedMediaItems: List<MediaItem> =
                currentTracklist.map { mediaItem ->
                    MediaItem.Builder()
                        .setMediaId(mediaItem.mediaId)
                        .setMediaMetadata(mediaItem.mediaMetadata)
                        .setUri(mediaItem.mediaId)
                        .setRequestMetadata(mediaItem.requestMetadata)
                        .build()
                }
            val updatedStartIndex =
                if (SongHelper.currentSong.isRadio == false)
                    updatedMediaItems.indexOfFirst { it.mediaId == mediaItems[0].mediaId }
                else {
                    0
                }

            Log.d("AA", "updatedStartIndex: $updatedStartIndex")

            return super.onSetMediaItems(mediaSession, controller, updatedMediaItems, updatedStartIndex, startPositionMs)
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
                    val items = when {
                        parentId == "nodeROOT" -> rootHierarchy
                        parentId == "nodeHOME" -> getHomeScreenItems()
                        parentId == "nodeRADIOS" -> getRadioItems()
                        parentId == "nodePLAYLISTS" -> getPlaylistItems()
                        parentId.startsWith("folder_") -> getFolderItems(parentId)
                        else -> emptyList()
                    }
                    LibraryResult.ofItemList(items, params)
                } catch (e: Exception) {
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

            return Futures.immediateFuture(LibraryResult.ofItem(mediaItem, LibraryParams.Builder().build()))
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
    }

    override fun onDestroy() {
        session?.run {
            player.release()
            release()
            session = null
        }
        super.onDestroy()
    }

    //region Convert to media items
    private fun albumToMediaItem(album: MediaData.Album): MediaItem {
        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(album.title)
            .setArtist(album.artist)
            .setAlbumTitle(album.album)
            .setAlbumArtist(album.artist)
            .setArtworkUri(Uri.parse(album.coverArt))
            .setReleaseYear(album.year)
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .setMediaType(MediaMetadata.MEDIA_TYPE_ALBUM)
            .setExtras(Bundle().apply {
                putString("navidromeID", album.navidromeID)
                putInt("Duration", album.duration)
            })
            .build()

        Log.d("AA", "Added ${mediaMetadata.title} to recently played albums")
        return MediaItem.Builder()
            .setMediaId("folder_album_" + album.navidromeID)
            .setMediaMetadata(mediaMetadata)
            .build()
    }

    private fun songToMediaItem(song: MediaData.Song): MediaItem {
        val mediaMetadata =
            MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist)
                .setAlbumTitle(song.album)
                .setArtworkUri(Uri.parse(song.imageUrl))
                .setReleaseYear(song.year)
                .setIsBrowsable(false).setIsPlayable(true)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .setExtras(Bundle().apply {
                    putString("navidromeID", song.navidromeID)
                    putInt("duration", song.duration)
                    putString("format", song.format)
                    song.bitrate?.let { putInt("bitrate", it) }
                    putBoolean("isRadio", song.isRadio == true)
                    if (song.replayGain?.trackGain != null)
                        putFloat("replayGain", song.replayGain.trackGain)
                }).build()

        val requestMetadata = RequestMetadata.Builder().setMediaUri(Uri.parse(song.media)).build()

        Log.d("AA", "Added ${mediaMetadata.title} to album")
        return MediaItem.Builder()
            .setMediaId(song.media.toString())
            .setUri(Uri.parse(song.media))
            .setMediaMetadata(mediaMetadata)
            .setRequestMetadata(requestMetadata)
            .build()
    }
    fun mediaItemToSong(mediaItem: MediaItem?): MediaData.Song {
        val mediaMetadata = mediaItem?.mediaMetadata
        val extras = mediaMetadata?.extras

        return MediaData.Song(
            navidromeID = extras?.getString("navidromeID") ?: "",
            title = mediaMetadata?.title.toString(),
            artist = mediaMetadata?.artist.toString(),
            album = mediaMetadata?.albumTitle.toString(),
            imageUrl = mediaMetadata?.artworkUri.toString(),
            year = mediaMetadata?.releaseYear ?: 0,
            duration = extras?.getInt("duration") ?: 0,
            format = extras?.getString("format") ?: "",
            bitrate = extras?.getInt("bitrate"),
            media = mediaItem?.mediaId.toString(),
            replayGain = ReplayGain(
                trackGain = extras?.getFloat("replayGain") ?: 0f
            ),
            isRadio = extras?.getBoolean("isRadio"),
            path = "",
            parent = "",
            dateAdded = "",
            bpm = 0,
            albumId = ""
        )
    }

    private fun radioToMediaItem(radio: MediaData.Radio): MediaItem {
        val mediaMetadata =
            MediaMetadata.Builder()
                .setTitle(radio.name)
                .setArtworkData(
                    drawableToByteArray(R.drawable.radioplaceholder),
                    MediaMetadata.PICTURE_TYPE_FRONT_COVER
                )
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .setExtras(Bundle().apply {
                    putString("navidromeID", radio.navidromeID)
                    putBoolean("isRadio", true)
                }).build()

        Log.d("AA", "Added ${mediaMetadata.title} to radio list")
        return MediaItem.Builder()
            .setMediaId(radio.media)
            .setUri(radio.media)
            .setMediaMetadata(mediaMetadata)
            .build()
    }
    private fun drawableToByteArray(drawableId: Int): ByteArray {
        val drawable = ContextCompat.getDrawable(applicationContext, drawableId)
        val bitmap = when (drawable) {
            is BitmapDrawable -> drawable.bitmap
            else -> {
                val bitmap = Bitmap.createBitmap(
                    drawable!!.intrinsicWidth,
                    drawable.intrinsicHeight,
                    Bitmap.Config.RGB_565
                )
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            }
        }

        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, stream)
        return stream.toByteArray()
    }

    private fun playlistToMediaItem(playlist: MediaData.Playlist): MediaItem {
        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(playlist.name)
            .setSubtitle(playlist.comment)
            .setArtworkUri(Uri.parse(playlist.coverArt))
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
            .setMediaId("folder_playlist_" + playlist.navidromeID)
            .setMediaMetadata(mediaMetadata)
            .build()
    }
    //endregion

    //region getChildren
    private fun getHomeScreenItems() : MutableList<MediaItem> {
        println("GETTING ANDROID AUTO SCREEN ITEMS")
        serviceIOScope.launch {
            if (aHomeScreenItems.isEmpty()) {
                val recentlyPlayedAlbums = async { getAlbums("recent", 6) }.await()
                val mostPlayedAlbums = async { getAlbums("frequent", 6) }.await()

                recentlyPlayedAlbums.forEach { album ->
                    val mediaItem = albumToMediaItem(album).apply {
                        this.mediaMetadata.extras?.putString(
                            MediaConstants.EXTRAS_KEY_CONTENT_STYLE_GROUP_TITLE,
                            //this@ChoraMediaLibraryService.getString(R.string.recently_played)
                            "Recently Played"
                        )
                    }
                    aHomeScreenItems.add(mediaItem)
                }

                mostPlayedAlbums.forEach { album ->
                    val mediaItem = albumToMediaItem(album).apply {
                        this.mediaMetadata.extras?.putString(
                            MediaConstants.EXTRAS_KEY_CONTENT_STYLE_GROUP_TITLE,
                            this@ChoraMediaLibraryService.getString(R.string.most_played)
                        )
                    }
                    aHomeScreenItems.add(mediaItem)
                }
            }
        }
        return aHomeScreenItems
    }

    private fun getRadioItems() : MutableList<MediaItem> {
        serviceIOScope.launch {
            if (aRadioScreenItems.isEmpty()) {
                getRadios(baseContext).forEach { radio ->
                    aRadioScreenItems.add(radioToMediaItem(radio))
                }
            }
            SongHelper.currentTracklist = aRadioScreenItems
        }
        return aRadioScreenItems
    }

    private fun getPlaylistItems() : MutableList<MediaItem> {
        serviceIOScope.launch {
            if (aPlaylistScreenItems.isEmpty()) {
                val playlists = async { getPlaylists() }.await()
                playlists.forEach { playlist ->
                    aPlaylistScreenItems.add(playlistToMediaItem(playlist))
                }
            }
            SongHelper.currentTracklist = aPlaylistScreenItems
        }
        return aPlaylistScreenItems
    }

    private fun getFolderItems(parentId: String) : MutableList<MediaItem> {
        serviceIOScope.launch {
            aFolderSongs.clear()
            val folderId = parentId.removePrefix("folder_")
            when {
                folderId.startsWith("album_") -> {
                    val albumId = folderId.removePrefix("album_")
                    aFolderSongs.addAll(
                        getAlbum(albumId)?.songs?.map { songToMediaItem(it) } ?: emptyList()
                    )
                }
                folderId.startsWith("playlist_") -> {
                    val playlistId = folderId.removePrefix("playlist_")
                    getPlaylistDetails(playlistId)?.songs?.forEach {
                        aFolderSongs.add(songToMediaItem(it))
                    }
                }
                else -> aFolderSongs.clear()
            }
            SongHelper.currentTracklist = aFolderSongs
        }

        return aFolderSongs
    }
    //endregion
}