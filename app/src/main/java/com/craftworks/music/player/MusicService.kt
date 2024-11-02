package com.craftworks.music.player

import android.content.Intent
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
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaConstants
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import com.craftworks.music.data.MediaData
import com.craftworks.music.lyrics.LyricsManager
import com.craftworks.music.managers.NavidromeManager
import com.craftworks.music.providers.navidrome.getNavidromeAlbumSongs
import com.craftworks.music.providers.navidrome.getNavidromeAlbums
import com.craftworks.music.providers.navidrome.markNavidromeSongAsPlayed
import com.craftworks.music.ui.screens.selectedAlbum
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.internal.toImmutableList

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
                .setTitle("MyMusicAppRootWhichIsNotVisibleToControllers")
                .build()
        )
        .build()

    private val homeItem = MediaItem.Builder()
        .setMediaId("nodeHOME")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                .setTitle("Home")
                .setArtworkUri(
                    Uri.parse("android.resource://com.craftworks.music/drawable/ic_home_icon")
                )
                .build()
        )
        .build()

    private val recentlyPlayedItem = MediaItem.Builder()
        .setMediaId("nodeRECENTLYPLAYED")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS)
                .setTitle("Recently Played Albums")
                .setArtworkUri(
                    Uri.parse("android.resource://com.craftworks.music/drawable/ic_notification_icon")
                )
                .setExtras(Bundle().apply {
                    putInt(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE, MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_GRID_ITEM)
                })
                .build()
        )
        .build()

    private val mostPlayedItem = MediaItem.Builder()
        .setMediaId("nodeMOSTPLAYED")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS)
                .setTitle("Most Played Albums")
                .setArtworkUri(
                    Uri.parse("android.resource://com.craftworks.music/drawable/ic_notification_icon")
                )
                .setExtras(Bundle().apply {
                    putInt(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE, MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_GRID_ITEM)
                })
                .build()
        )
        .build()

    private val rootHierarchy = listOf(homeItem)

    private val serviceMainScope = CoroutineScope(Dispatchers.Main)
    private val serviceIOScope = CoroutineScope(Dispatchers.IO)

    var aRecentlyPlayed = mutableListOf<MediaItem>()
    var aMostPlayed = mutableListOf<MediaItem>()
    var aAlbumSongs = mutableListOf<MediaItem>()

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

                if (NavidromeManager.checkActiveServers())
                    serviceMainScope.launch { markNavidromeSongAsPlayed(SongHelper.currentSong) }

                serviceIOScope.launch {
                    val song = MediaData.Song(
                        title = mediaItem?.mediaMetadata?.title.toString(),
                        artist = mediaItem?.mediaMetadata?.artist.toString(),
                        duration = mediaItem?.mediaMetadata?.extras?.getInt("duration") ?: 0,
                        imageUrl = mediaItem?.mediaMetadata?.artworkUri.toString(),
                        year = mediaItem?.mediaMetadata?.releaseYear,
                        album = mediaItem?.mediaMetadata?.albumTitle.toString(),
                        format = mediaItem?.mediaMetadata?.extras?.getString("format").toString(),
                        bitrate = mediaItem?.mediaMetadata?.extras?.getInt("bitrate"),
                        navidromeID = mediaItem?.mediaMetadata?.extras?.getString("NavidromeID").toString(),
                        isRadio = mediaItem?.mediaMetadata?.extras?.getBoolean("isRadio"),
                        albumId = "",
                        bpm = 0,
                        contentType = "music",
                        dateAdded = "",
                        timesPlayed = 0,
                        genre = "",
                        parent = "",
                        sortName = "",
                        comment = "",
                        lastPlayed = "",
                        isVideo = false,
                        genres = listOf(),
                        isDir = false,
                        mediaType = "song",
                        path = "",
                        size = 0,
                        type = "song"
                    )
                    SongHelper.currentSong = song

                    if (SongHelper.currentSong.isRadio == false)
                        LyricsManager.getLyrics()
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

        // Add recently played albums
        serviceIOScope.launch {

            NavidromeManager.init(this@ChoraMediaLibraryService)

            if (NavidromeManager.checkActiveServers()){
                val recentlyPlayedAlbumsAsync = async { getNavidromeAlbums("recent", 20) }
                val mostPlayedAlbumsAsync = async { getNavidromeAlbums("frequent", 20) }

                val recentlyPlayedAlbums = recentlyPlayedAlbumsAsync.await()
                val mostPlayedAlbums = mostPlayedAlbumsAsync.await()

                Log.d("AA", "Got ${recentlyPlayedAlbums.size} recently played albums and ${mostPlayedAlbums.size} most played!")

                for (album in recentlyPlayedAlbums)
                    aRecentlyPlayed.add(albumToMediaItem(album))

                for (album in mostPlayedAlbums)
                    aMostPlayed.add(albumToMediaItem(album))
            }

            Log.d("AA", "Notified AA of new tracks")

            session?.notifyChildrenChanged("nodeHOME", 2, null)
            session?.notifyChildrenChanged("nodeRECENTLYPLAYED", aRecentlyPlayed.size, null)
            session?.notifyChildrenChanged("nodeMOSTPLAYED", aMostPlayed.size, null)
        }


//        synchronized(songsList){
//            for (song in songsList){
//                val mediaMetadata = MediaMetadata.Builder()
//                    .setTitle(song.title)
//                    .setArtist(song.artist)
//                    .setAlbumTitle(song.album)
//                    .setArtworkUri(Uri.parse(song.imageUrl))
//                    .setReleaseYear(song.year)
//                    .setIsBrowsable(false)
//                    .setIsPlayable(true)
//                    .setExtras(Bundle().apply {
//                        putString("Format", song.contentType)
//                        song.bitrate?.let { putInt("Bitrate", it) }
//                        putString("navidromeID", song.navidromeID)
//                        putInt("Duration", song.duration)
//                    })
//                    .build()
//                val mediaItem = MediaItem.Builder()
//                    .setUri(song.media)
//                    .setMediaId(song.media.toString())
//                    .setMediaMetadata(mediaMetadata)
//                    .build()
//
//                androidAutoRecentlyListenedTracklist.add(mediaItem)
//            }
//        }

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

        @OptIn(UnstableApi::class)
        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            // We need to use URI from requestMetaData because of https://github.com/androidx/media/issues/282
            val updatedMediaItems: List<MediaItem> =
                SongHelper.currentTracklist.map { mediaItem ->
                    MediaItem.Builder()
                        .setMediaId(mediaItem.mediaId)
                        .setMediaMetadata(mediaItem.mediaMetadata)
                        .setUri(mediaItem.mediaId)
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

        var isGettingChildren = false
        @OptIn(UnstableApi::class)
        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {

            val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

            serviceIOScope.launch {
                try {
                    val items = withContext(Dispatchers.IO) {
                        when {
                            parentId == "nodeROOT" -> rootHierarchy
                            parentId == "nodeRECENTLYPLAYED" -> aRecentlyPlayed
                            parentId == "nodeMOSTPLAYED" -> aMostPlayed
                            parentId == "nodeHOME" -> listOf(recentlyPlayedItem, mostPlayedItem) // Return both folders for Home
                            parentId.startsWith("album_") -> {
                                // Extract album ID from parentId (e.g., "album_<albumId>")
                                val albumId = parentId.removePrefix("album_")

                                // Use a lock to prevent concurrent loading of the same album
                                if (!isGettingChildren) {
                                    isGettingChildren = true
                                    // Load album songs only if albumId is different
                                    getNavidromeAlbumSongs(albumId)
                                    aAlbumSongs = selectedAlbum?.songs?.map { track ->
                                        songToMediaItem(track)
                                    }?.toMutableList() ?: mutableListOf()
                                    isGettingChildren = false
                                } else {
                                    // If another coroutine is loading, wait until it completes
                                    emptyList<MediaItem>()
                                }

                                SongHelper.currentTracklist = aAlbumSongs

                                // Use cached album songs from aAlbumSongs
                                aAlbumSongs.ifEmpty {
                                    selectedAlbum?.songs?.map { track ->
                                        songToMediaItem(track)
                                    }?.toImmutableList() ?: emptyList()
                                }
                            }
                            else -> emptyList()
                        }
                    }
                    future.set(LibraryResult.ofItemList(items, params))
                } catch (e: Exception) {
                    future.setException(e)
                }
            }

            // Return the ListenableFuture
            return future
        }


        @OptIn(UnstableApi::class)
        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val mediaItem =
                aMostPlayed.find { it.mediaId == mediaId }
                ?: aRecentlyPlayed.find { it.mediaId == mediaId }
                ?: aAlbumSongs.find { it.mediaId == mediaId }
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
                    "nodeHOME" -> 2
                    "nodeMOSTPLAYED" -> aMostPlayed.size
                    "nodeRECENTLYPLAYED" -> aRecentlyPlayed.size
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
}

@OptIn(UnstableApi::class)
fun albumToMediaItem(album: MediaData.Album): MediaItem {
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
            putInt(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE, MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM)
        })
        .build()

    Log.d("AA", "Added ${mediaMetadata.title} to recently played albums")
    return MediaItem.Builder()
        .setMediaId("album_" + album.navidromeID)
        .setMediaMetadata(mediaMetadata)
        .build()
}

fun songToMediaItem(song: MediaData.Song): MediaItem {
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
                putBoolean("isRadio", false)
            }).build()

    Log.d("AA", "Added ${mediaMetadata.title} to album")
    return MediaItem.Builder()
        .setMediaId(song.media.toString())
        .setUri(song.media)
        .setMediaMetadata(mediaMetadata).build()
}