package com.craftworks.music.providers

import android.content.Context
import android.net.Uri
import androidx.compose.ui.util.fastFilter
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.craftworks.music.data.MediaData
import com.craftworks.music.data.albumList
import com.craftworks.music.data.playlistList
import com.craftworks.music.data.radioList
import com.craftworks.music.data.selectedArtist
import com.craftworks.music.data.toMediaItem
import com.craftworks.music.data.toSong
import com.craftworks.music.managers.LocalProviderManager
import com.craftworks.music.managers.NavidromeManager
import com.craftworks.music.managers.SettingsManager
import com.craftworks.music.providers.local.LocalProvider
import com.craftworks.music.providers.local.localPlaylistImageGenerator
import com.craftworks.music.providers.navidrome.sendNavidromeGETRequest
import com.craftworks.music.ui.elements.dialogs.songToAddToPlaylist
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

// region Albums
suspend fun getAlbums(
    sort: String? = "alphabeticalByName",
    size: Int? = 100,
    offset: Int? = 0,
    ignoreCachedResponse: Boolean = false
): List<MediaItem> = coroutineScope {
    val deferredAlbums = mutableListOf<Deferred<List<MediaItem>>>()

    if (NavidromeManager.checkActiveServers()) {
        deferredAlbums.add(async {
            sendNavidromeGETRequest("getAlbumList.view?type=$sort&size=$size&offset=$offset&f=json", ignoreCachedResponse).filterIsInstance<MediaItem>()
        })
    }

    if (LocalProviderManager.checkActiveFolders()) {
        if (offset == 0) {
            deferredAlbums.add(async {
                LocalProvider.getInstance().getLocalAlbums(sort)
            })
        }
    }

    deferredAlbums.awaitAll().flatten()
}

suspend fun getAlbum(albumId: String, ignoreCachedResponse: Boolean = false): List<MediaItem>? = coroutineScope {
    if (albumId.startsWith("Local_")){
        LocalProvider.getInstance().getLocalAlbum(albumId)
    }
    else {
        val deferredAlbum = async {
            sendNavidromeGETRequest("getAlbum.view?id=${albumId}&f=json", ignoreCachedResponse)
                .filterIsInstance<MediaItem>()
        }
        deferredAlbum.await()
    }
}

suspend fun searchAlbum(
    query: String? = ""
) : List<MediaItem> = coroutineScope {
    val deferredAlbums = mutableListOf<Deferred<List<MediaItem>>>()

    if (NavidromeManager.checkActiveServers()) {
        deferredAlbums.add(async {
            sendNavidromeGETRequest("search3.view?query=$query&songCount=0&songOffset=0&artistCount=0&albumCount=100&f=json").filterIsInstance<MediaItem>()
        })
    }

    deferredAlbums.awaitAll().flatten()
}
//endregion

//region Songs
suspend fun getSongs(
    query: String? = "",
    songCount: Int? = 100,
    songOffset: Int? = 0
) : List<MediaItem> = coroutineScope {
    val deferredSongs = mutableListOf<Deferred<List<MediaItem>>>()

    if (LocalProviderManager.checkActiveFolders()) {
        if (songOffset == 0) {
            deferredSongs.add(async {
                if (query?.isNotBlank() == true)
                    LocalProvider.getInstance().getLocalSongs().fastFilter { it.mediaMetadata.title?.contains(query, ignoreCase = true) == true }
                else
                    LocalProvider.getInstance().getLocalSongs()
            })
        }
    }
    if (NavidromeManager.checkActiveServers()) {
        deferredSongs.add(async {
            sendNavidromeGETRequest("search3.view?query=$query&songCount=$songCount&songOffset=$songOffset&artistCount=0&albumCount=0&f=json").filterIsInstance<MediaItem>()
        })
    }

    deferredSongs.awaitAll().flatten()
}
//endregion

//region Artists
suspend fun getArtists(): List<MediaData.Artist> = coroutineScope {
    val deferredArtists = mutableListOf<Deferred<List<MediaData.Artist>>>()

    if (NavidromeManager.checkActiveServers()) {
        deferredArtists.add(async {
            sendNavidromeGETRequest("getArtists.view?size=100&f=json").filterIsInstance<MediaData.Artist>()
        })
    }
    if (LocalProviderManager.checkActiveFolders()) {
        deferredArtists.add(async {
            LocalProvider.getInstance().getLocalArtists()
        })
    }

    deferredArtists.awaitAll().flatten()
}

suspend fun getArtistDetails(
    id: String
): MediaData.Artist = coroutineScope {
    if (NavidromeManager.checkActiveServers()) {
        val deferredArtist = async {
            val details = sendNavidromeGETRequest("getArtist.view?id=$id&f=json")
                .filterIsInstance<MediaData.Artist>()
                .firstOrNull() ?: throw IllegalStateException("No artist details returned")
            val biography = sendNavidromeGETRequest("getArtistInfo.view?id=$id&f=json")
                .filterIsInstance<MediaData.Artist>()
                .firstOrNull() ?: throw IllegalStateException("No artist biography returned")

            details.copy(
                description = biography.description,
                similarArtist = biography.similarArtist,
                musicBrainzId = biography.musicBrainzId
            )
        }
        deferredArtist.await()
    } else {
        selectedArtist.copy(
            album = albumList.fastFilter { it.artist == selectedArtist.name }
        )
    }
}

suspend fun searchArtist(
    query: String? = ""
) : List<MediaData.Artist> = coroutineScope {
    val deferredArtists = mutableListOf<Deferred<List<MediaData.Artist>>>()

    if (NavidromeManager.checkActiveServers()) {
        deferredArtists.add(async {
            sendNavidromeGETRequest("search3.view?query=$query&songCount=0&songOffset=0&artistCount=100&albumCount=0&f=json").filterIsInstance<MediaData.Artist>()
        })
    }

    deferredArtists.awaitAll().flatten()
}
//endregion

//region Radios
suspend fun getRadios(context: Context, ignoreCachedResponse: Boolean = false): List<MediaData.Radio> = coroutineScope {
    val deferredRadios = mutableListOf<Deferred<List<MediaData.Radio>>>()

    if (NavidromeManager.checkActiveServers()) {
        deferredRadios.add(async {
            sendNavidromeGETRequest("getInternetRadioStations.view?f=json", ignoreCachedResponse).filterIsInstance<MediaData.Radio>()
        })
    }

    deferredRadios.add(async {
        SettingsManager(context).localRadios.first()
    })

    deferredRadios.awaitAll().flatten()
}

suspend fun createRadio(name:String, url:String, homePage:String, context: Context, addToNavidrome: Boolean) {
    if (addToNavidrome && NavidromeManager.checkActiveServers()) {
        sendNavidromeGETRequest("createInternetRadioStation.view?name=$name&streamUrl=$url&homepageUrl=$homePage", true) // Always ignore cache when adding radios
        getRadios(context, true)
    }
    else {
        radioList.add(
            MediaData.Radio(
                navidromeID = "Local",
                name = name,
                media = url,
                homePageUrl = homePage,
            )
        )
        SettingsManager(context).saveLocalRadios()
    }
}
suspend fun modifyRadio(radio: MediaData.Radio, context: Context) {
    if (NavidromeManager.checkActiveServers() && radio.navidromeID != "Local") {
        sendNavidromeGETRequest(
            "updateInternetRadioStation.view?name=${radio.name}&streamUrl=${radio.media}&homepageUrl=${radio.homePageUrl}&id=${radio.navidromeID}",
            true // Always ignore cache when modifying radios
        )
        getRadios(context, true)
    }
    else {
        radioList.remove(radio)
        radioList.add(
            MediaData.Radio(
                navidromeID = "Local",
                name = radio.name,
                media = radio.media
            )
        )
    }
}
suspend fun deleteRadio(radio: MediaData.Radio, context: Context){
    if (NavidromeManager.checkActiveServers() && radio.navidromeID != "Local") {
        sendNavidromeGETRequest("deleteInternetRadioStation.view?id=${radio.navidromeID}", true) // Always ignore cache when deleting radios
        getRadios(context, true)
    }
    else {
        radioList.remove(radio)
        SettingsManager(context).saveLocalRadios()
    }
}
//endregion

//region Playlists
suspend fun getPlaylists(context: Context, ignoreCachedResponse: Boolean = false): List<MediaItem> = coroutineScope {
    val deferredPlaylists = mutableListOf<Deferred<List<MediaItem>>>()

    if (NavidromeManager.checkActiveServers()) {
        deferredPlaylists.add(async {
            sendNavidromeGETRequest("getPlaylists.view?f=json", ignoreCachedResponse).filterIsInstance<MediaItem>()
        })
    }
    deferredPlaylists.add(
        async {
            playlistList.map {
                it.toMediaItem().buildUpon().setMediaMetadata(
                    it.toMediaItem().mediaMetadata.buildUpon()
                        .setArtworkData(localPlaylistImageGenerator(it.songs?.map { it.toMediaItem() } ?: emptyList(), context), MediaMetadata.PICTURE_TYPE_OTHER)
                        .build()
                ).build()
            }
        }
    )

    deferredPlaylists.awaitAll().flatten()
}

suspend fun getPlaylistDetails(
    id: String,
    ignoreCachedResponse: Boolean = false
): List<MediaItem>? = coroutineScope {
    val deferredPlaylist: Deferred<List<MediaItem>>
    if (NavidromeManager.checkActiveServers() && !id.startsWith("Local")) {
        deferredPlaylist = async {
            sendNavidromeGETRequest("getPlaylist.view?id=$id&f=json", ignoreCachedResponse).filterIsInstance<MediaItem>()
        }
        deferredPlaylist.await()
    } else {
        playlistList.first { it.navidromeID == id }.songs?.map { it.toMediaItem() }
    }
}

suspend fun createPlaylist(playlistName: String, addToNavidrome: Boolean, context: Context) {
    if (NavidromeManager.checkActiveServers() && addToNavidrome) {
        println("creating navidrome $playlistName")
        sendNavidromeGETRequest("createPlaylist.view?name=$playlistName&songId=${songToAddToPlaylist.value.mediaMetadata.extras?.getString("navidromeID")}", true) // Always ignore cache when creating playlists
    }
    else {
        playlistList.add(
            MediaData.Playlist(
                "Local_$playlistName",
                playlistName,
                Uri.EMPTY.toString(),
                changed = "",
                created = "",
                duration = 0,
                songCount = 0,
                songs = listOf(songToAddToPlaylist.value.toSong())
            )
        )
        SettingsManager(context).saveLocalPlaylists()
    }
}
suspend fun deletePlaylist(playlistID: String, context: Context){
    if (NavidromeManager.checkActiveServers() && !playlistID.startsWith("Local"))
        sendNavidromeGETRequest("deletePlaylist.view?id=$playlistID", true) // Always ignore cache when deleting playlists
    else{
        playlistList = playlistList.filter { it.navidromeID != playlistID }.toMutableList()
        SettingsManager(context).saveLocalPlaylists()
    }
}
suspend fun addSongToPlaylist(playlistId: String, songID: String, context: Context) {
    println("Adding song $songID to playlist $playlistId")

    if (NavidromeManager.checkActiveServers() && !playlistId.startsWith("Local"))
        sendNavidromeGETRequest("updatePlaylist.view?playlistId=${playlistId}&songIdToAdd=$songID", true) // Always ignore cache when modifying playlists
    else {
        println("Adding song to local playlist")
        val index = playlistList.indexOfFirst { it.navidromeID == playlistId }
        playlistList[index].songs = playlistList[index].songs?.plus(songToAddToPlaylist.value.toSong())

        playlistList[index].coverArt =
            localPlaylistImageGenerator(playlistList[index].songs?.map { it.toMediaItem() } ?: emptyList(), context).toString()
        SettingsManager(context).saveLocalPlaylists()
    }
}
//endregion

//region Favourites
suspend fun getFavouriteSongs() : List<MediaItem> = coroutineScope {
    val deferredSongs = mutableListOf<Deferred<List<MediaItem>>>()

    if (LocalProviderManager.checkActiveFolders()) {
        emptyList<MediaItem>()
    }
    if (NavidromeManager.checkActiveServers()) {
        deferredSongs.add(async {
            sendNavidromeGETRequest("getStarred.view?f=json", true).filterIsInstance<MediaItem>()
        })
    }

    deferredSongs.awaitAll().flatten()
}
//endregion