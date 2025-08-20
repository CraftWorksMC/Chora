package com.craftworks.music.data.datasource.navidrome

import androidx.media3.common.MediaItem
import com.craftworks.music.data.model.MediaData
import com.craftworks.music.providers.navidrome.sendNavidromeGETRequest // Corrected import
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NavidromeDataSource @Inject constructor() {

    // Albums
    suspend fun getNavidromeAlbums(
        sort: String? = "alphabeticalByName",
        size: Int? = 100,
        offset: Int? = 0,
        ignoreCachedResponse: Boolean = false
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        sendNavidromeGETRequest(
            "getAlbumList.view?type=$sort&size=$size&offset=$offset&f=json",
            ignoreCachedResponse
        ).filterIsInstance<MediaItem>()
    }

    suspend fun getNavidromeAlbum(
        albumId: String, ignoreCachedResponse: Boolean = false
    ): List<MediaItem>? = withContext(Dispatchers.IO) {
        sendNavidromeGETRequest(
            "getAlbum.view?id=${albumId}&f=json",
            ignoreCachedResponse
        ).filterIsInstance<MediaItem>()
    }

    suspend fun searchNavidromeAlbums(
        query: String? = "", ignoreCachedResponse: Boolean = false
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        sendNavidromeGETRequest(
            "search3.view?query=$query&songCount=0&songOffset=0&artistCount=0&albumCount=100&f=json",
            ignoreCachedResponse
        ).filterIsInstance<MediaItem>()
    }

    // Songs
    suspend fun getNavidromeSongs(
        query: String? = "",
        songCount: Int = 100,
        songOffset: Int = 0,
        ignoreCachedResponse: Boolean = false
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        sendNavidromeGETRequest(
            "search3.view?query=$query&songCount=$songCount&songOffset=$songOffset&artistCount=0&albumCount=0&f=json",
            ignoreCachedResponse
        ).filterIsInstance<MediaItem>()
    }

    suspend fun getNavidromeSong(
        songId: String, ignoreCachedResponse: Boolean = false
    ): MediaItem? = withContext(Dispatchers.IO) {
        sendNavidromeGETRequest(
            "getSong.view?id=$songId&f=json",
            ignoreCachedResponse
        ).filterIsInstance<MediaItem>().firstOrNull()
    }


    // Artists
    suspend fun getNavidromeArtists(
        ignoreCachedResponse: Boolean = false
    ): List<MediaData.Artist> = withContext(Dispatchers.IO) {
        sendNavidromeGETRequest(
            "getArtists.view?f=json",
            ignoreCachedResponse
        ).filterIsInstance<MediaData.Artist>()
    }

    suspend fun getNavidromeArtistAlbums(
        artistId: String, ignoreCachedResponse: Boolean = false
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        sendNavidromeGETRequest(
            "getArtist.view?id=$artistId&f=json",
            ignoreCachedResponse
        ).filterIsInstance<MediaItem>()
    }

    suspend fun getNavidromeArtistInfo(
        artistId: String, ignoreCachedResponse: Boolean = false
    ): MediaData.ArtistInfo? = withContext(Dispatchers.IO) {
        sendNavidromeGETRequest(
            "getArtistInfo.view?id=$artistId&f=json",
            ignoreCachedResponse
        ).filterIsInstance<MediaData.ArtistInfo>().firstOrNull()
    }

    suspend fun searchNavidromeArtists(
        query: String? = "", ignoreCachedResponse: Boolean = false
    ): List<MediaData.Artist> = withContext(Dispatchers.IO) {
        if (query.isNullOrBlank()) getNavidromeArtists()
        else sendNavidromeGETRequest(
            "search3.view?query=$query&artistCount=100&albumCount=0&songCount=0&f=json",
            ignoreCachedResponse
        ).filterIsInstance<MediaData.Artist>()
    }

    // Playlists
    suspend fun getNavidromePlaylists(
        ignoreCachedResponse: Boolean = false
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        sendNavidromeGETRequest(
            "getPlaylists.view?f=json",
            ignoreCachedResponse
        ).filterIsInstance<MediaItem>()
    }

    suspend fun getNavidromePlaylist(
        playlistId: String, ignoreCachedResponse: Boolean = false
    ): List<MediaItem>? = withContext(Dispatchers.IO) {
        sendNavidromeGETRequest(
            "getPlaylist.view?id=$playlistId&f=json",
            ignoreCachedResponse
        ).filterIsInstance<MediaItem>()
    }

    suspend fun createNavidromePlaylist(
        name: String, songIds: List<String>? = null, ignoreCachedResponse: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        var endpoint = "createPlaylist.view?name=$name"
        songIds?.forEach { songId -> endpoint += "&songId=$songId" }
        val response = sendNavidromeGETRequest(endpoint, ignoreCachedResponse)
        response.isNotEmpty()
    }

    suspend fun addSongToNavidromePlaylist(
        playlistId: String, songId: String, ignoreCachedResponse: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        val response = sendNavidromeGETRequest(
            "updatePlaylist.view?playlistId=$playlistId&songIdToAdd=$songId",
            ignoreCachedResponse
        )
        response.isNotEmpty()
    }

    suspend fun removeSongFromNavidromePlaylist(
        playlistId: String, songIndexToRemove: Int, ignoreCachedResponse: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        val response = sendNavidromeGETRequest(
            "updatePlaylist.view?playlistId=$playlistId&songIndexToRemove=$songIndexToRemove",
            ignoreCachedResponse
        )
        response.isNotEmpty()
    }

    suspend fun deleteNavidromePlaylist(
        playlistId: String, ignoreCachedResponse: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        val response = sendNavidromeGETRequest(
            "deletePlaylist.view?id=$playlistId",
            ignoreCachedResponse
        )
        response.isNotEmpty()
    }

    // Radios
    suspend fun getNavidromeRadios(
        ignoreCachedResponse: Boolean = false
    ): List<MediaData.Radio> = withContext(Dispatchers.IO) {
        sendNavidromeGETRequest(
            "getInternetRadioStations.view?f=json",
            ignoreCachedResponse
        ).filterIsInstance<MediaData.Radio>()
    }

    suspend fun createNavidromeRadio(
        name: String, url: String, homePageUrl: String? = null
    ) = withContext(Dispatchers.IO) {
        sendNavidromeGETRequest(
            "createInternetRadioStation.view?name=$name&streamUrl=$url&homepageUrl=$homePageUrl",
            true
        )
    }

    suspend fun updateNavidromeRadio(
        radioId: String, name: String, url: String, homePageUrl: String? = null
    ) = withContext(Dispatchers.IO) {
        sendNavidromeGETRequest(
            "updateInternetRadioStation.view?name=$name&streamUrl=$url&homepageUrl=$homePageUrl&id=$radioId",
            true
        )
    }

    suspend fun deleteNavidromeRadio(
        radioId: String
    ) = withContext(Dispatchers.IO) {
        sendNavidromeGETRequest(
            "deleteInternetRadioStation.view?id=$radioId",
            true
        )
    }


    // Lyrics
    suspend fun getNavidromePlainLyrics(
        songId: String, ignoreCachedResponse: Boolean = false
    ): MediaData.PlainLyrics? = withContext(Dispatchers.IO) {
        sendNavidromeGETRequest(
            "getLyrics.view?id=$songId",
            ignoreCachedResponse
        ).filterIsInstance<MediaData.PlainLyrics>().firstOrNull()
    }

    suspend fun getNavidromeSyncedLyrics(
        songId: String, ignoreCachedResponse: Boolean = false
    ): MediaData.StructuredLyrics? = withContext(Dispatchers.IO) {
        sendNavidromeGETRequest(
            "getLyrics.view?id=$songId",
            ignoreCachedResponse
        ).filterIsInstance<MediaData.StructuredLyrics>().firstOrNull()
    }

    //TODO: Navidrome Starred Items (with UI updates)
    suspend fun getNavidromeStarred(ignoreCachedResponse: Boolean = false): List<MediaItem> =
        withContext(Dispatchers.IO) {
            emptyList()
        }

    suspend fun starNavidromeItem(itemId: String, ignoreCachedResponse: Boolean = false): Boolean =
        withContext(Dispatchers.IO) {
            false
        }

    suspend fun unstarNavidromeItem(
        itemId: String, ignoreCachedResponse: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        false
    }
}