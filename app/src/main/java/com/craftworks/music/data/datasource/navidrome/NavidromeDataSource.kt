package com.craftworks.music.data.datasource.navidrome

import android.annotation.SuppressLint
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.craftworks.music.data.NavidromeLibrary
import com.craftworks.music.data.model.Lyric
import com.craftworks.music.data.model.MediaData
import com.craftworks.music.data.model.toLyric
import com.craftworks.music.data.model.toLyrics
import com.craftworks.music.managers.NavidromeManager
import com.craftworks.music.providers.navidrome.parseNavidromeAlbumJSON
import com.craftworks.music.providers.navidrome.parseNavidromeAlbumListJSON
import com.craftworks.music.providers.navidrome.parseNavidromeArtistAlbumsJSON
import com.craftworks.music.providers.navidrome.parseNavidromeArtistBiographyJSON
import com.craftworks.music.providers.navidrome.parseNavidromeArtistsJSON
import com.craftworks.music.providers.navidrome.parseNavidromeFavouritesJSON
import com.craftworks.music.providers.navidrome.parseNavidromeLibrariesJSON
import com.craftworks.music.providers.navidrome.parseNavidromePlainLyricsJSON
import com.craftworks.music.providers.navidrome.parseNavidromePlaylistJSON
import com.craftworks.music.providers.navidrome.parseNavidromePlaylistsJSON
import com.craftworks.music.providers.navidrome.parseNavidromeRadioJSON
import com.craftworks.music.providers.navidrome.parseNavidromeSearch3JSON
import com.craftworks.music.providers.navidrome.parseNavidromeStatus
import com.craftworks.music.providers.navidrome.parseNavidromeSyncedLyricsJSON
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.security.cert.X509Certificate
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@Singleton
class NavidromeDataSource @Inject constructor() {
    private val json = Json { ignoreUnknownKeys = true }

    private val client: HttpClient by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(json)
            }
            install(HttpCache)
            install(Logging) {
                level = LogLevel.ALL
                logger = Logger.SIMPLE
            }
        }
    }

    private fun md5Hash(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val hashBytes = md.digest(input.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun generateSalt(length: Int): String {
        val allowedChars = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..length).map { allowedChars.random() }.joinToString("")
    }

    private fun buildInsecureClient(): HttpClient {
        val trustAllCerts = arrayOf<TrustManager>(
            @SuppressLint("CustomX509TrustManager")
            object : X509TrustManager {
                @SuppressLint("TrustAllX509TrustManager")
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                @SuppressLint("TrustAllX509TrustManager")
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
        )

        return HttpClient(CIO.create {
            https {
                this.trustManager = trustAllCerts[0]
            }
        }) {
            install(ContentNegotiation) {
                json(json)
            }
            install(HttpCache)
            install(Logging) {
                level = LogLevel.ALL
                logger = Logger.SIMPLE
            }
        }
    }

    private suspend fun getRequest(
        endpoint: String,
        musicFolderIds: List<Int>? = null,
        ignoreCachedResponse: Boolean = false
    ): List<Any> = withContext(Dispatchers.IO) {
        val server = NavidromeManager.getCurrentServer() ?: throw IllegalArgumentException("No active Navidrome server")
        val salt = generateSalt(8)
        val token = md5Hash(server.password + salt)
        var url = "${server.url}/rest/$endpoint&u=${server.username}&t=$token&s=$salt&v=1.16.1&c=Chora"

        // Append musicFolderId parameters if provided
        musicFolderIds?.forEach { folderId ->
            url += "&musicFolderId=$folderId"
        }

        NavidromeManager.setSyncingStatus(true)

        val activeClient = if (server.allowSelfSignedCert == true) buildInsecureClient() else client
        val parsedData = mutableListOf<Any>()

        try {
            val response: HttpResponse = activeClient.get(url) {
                // Force network request if ignoreCachedResponse is true
                if (ignoreCachedResponse) {
                    headers {
                        append("Cache-Control", "no-cache")
                    }
                }
            }

            if (response.status != HttpStatusCode.OK) {
                Log.w("NAVIDROME", "HTTP ${response.status} for URL: $url")
                return@withContext emptyList<Any>()
            }
            val responseContent = response.bodyAsText()

            when {
                endpoint.startsWith("ping")         -> parsedData.addAll(parseNavidromeStatus(responseContent))
                endpoint.startsWith("getMusicFolders") -> parsedData.addAll(parseNavidromeLibrariesJSON(responseContent))

                endpoint.startsWith("search3")      -> parsedData.addAll(parseNavidromeSearch3JSON(responseContent, server.url, server.username, server.password))

                endpoint.startsWith("getAlbumList") -> parsedData.addAll(parseNavidromeAlbumListJSON(responseContent, server.url, server.username, server.password))
                endpoint.startsWith("getAlbum.")    -> parsedData.addAll(parseNavidromeAlbumJSON(responseContent, server.url, server.username, server.password)) // Note: getAlbum.view takes an album ID, typically not musicFolderId

                endpoint.startsWith("getArtists")   -> parsedData.addAll(parseNavidromeArtistsJSON(responseContent))
                endpoint.startsWith("getArtist.")   -> parsedData.addAll(parseNavidromeArtistAlbumsJSON(responseContent, server.url, server.username, server.password))
                endpoint.startsWith("getArtistInfo")-> parsedData.addAll(listOf(parseNavidromeArtistBiographyJSON(responseContent)))

                endpoint.startsWith("getPlaylists") -> parsedData.addAll(parseNavidromePlaylistsJSON(responseContent, server.url, server.username, server.password))
                endpoint.startsWith("getPlaylist.") -> parsedData.addAll(parseNavidromePlaylistJSON(responseContent, server.url, server.username, server.password))

                endpoint.startsWith("getInternetRadioStations") -> parsedData.addAll(parseNavidromeRadioJSON(responseContent))

                endpoint.startsWith("getLyrics.") -> parsedData.addAll(listOf(parseNavidromePlainLyricsJSON(responseContent)))
                endpoint.startsWith("getLyricsBySongId.") -> parsedData.addAll(parseNavidromeSyncedLyricsJSON(responseContent))

                endpoint.startsWith("getStarred") -> { parsedData.addAll(parseNavidromeFavouritesJSON(responseContent, server.url, server.username, server.password)) }

                endpoint.startsWith("star") -> { NavidromeManager.setSyncingStatus(false) }
                endpoint.startsWith("unstar") -> { NavidromeManager.setSyncingStatus(false) }
            }
        } catch (e: Exception) {
            Log.e("NAVIDROME", "Network error for URL: $url", e)
        } finally {
            NavidromeManager.setSyncingStatus(false)
        }

        parsedData
    }

    suspend fun pingNavidromeServer(): List<String> = withContext(Dispatchers.IO) {
        getRequest("ping.view?f=json").filterIsInstance<String>()
    }

    suspend fun getNavidromeLibraries(): List<NavidromeLibrary> = withContext(Dispatchers.IO) {
        getRequest("getMusicFolders.view?f=json").filterIsInstance<NavidromeLibrary>()
    }

    // Albums
    suspend fun getNavidromeAlbums(
        sort: String? = "alphabeticalByName",
        size: Int? = 100,
        offset: Int? = 0,
        ignoreCachedResponse: Boolean = false,
        musicFolderIds: List<Int>? = NavidromeManager.getEnabledLibraryIdsForCurrentServer()
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        getRequest(
            "getAlbumList.view?type=$sort&size=$size&offset=$offset&f=json",
            musicFolderIds,
            ignoreCachedResponse
        ).filterIsInstance<MediaItem>()
    }

    suspend fun getNavidromeAlbum(
        albumId: String, ignoreCachedResponse: Boolean = false
    ): List<MediaItem>? = withContext(Dispatchers.IO) {
        getRequest(
            "getAlbum.view?id=${albumId}&f=json",
            null,
            ignoreCachedResponse
        ).filterIsInstance<MediaItem>()
    }

    suspend fun searchNavidromeAlbums(
        query: String? = "",
        ignoreCachedResponse: Boolean = false,
        musicFolderIds: List<Int>? = NavidromeManager.getEnabledLibraryIdsForCurrentServer(),
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        getRequest(
            "search3.view?query=$query&songCount=0&songOffset=0&artistCount=0&albumCount=100&f=json",
            musicFolderIds,
            ignoreCachedResponse
        ).filterIsInstance<MediaItem>()
    }

    // Songs
    suspend fun getNavidromeSongs(
        query: String? = "",
        songCount: Int = 100,
        songOffset: Int = 0,
        ignoreCachedResponse: Boolean = false,
        musicFolderIds: List<Int>? = NavidromeManager.getEnabledLibraryIdsForCurrentServer(),
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        getRequest(
            "search3.view?query=$query&songCount=$songCount&songOffset=$songOffset&artistCount=0&albumCount=0&f=json",
            musicFolderIds,
            ignoreCachedResponse
        ).filterIsInstance<MediaItem>()
    }

    suspend fun getNavidromeSong(
        songId: String, ignoreCachedResponse: Boolean = false
    ): MediaItem? = withContext(Dispatchers.IO) {
        getRequest(
            "getSong.view?id=$songId&f=json",
            null,
            ignoreCachedResponse
        ).filterIsInstance<MediaItem>().firstOrNull()
    }

    suspend fun scrobbleSong(songId: String, submission: Boolean) = withContext(Dispatchers.IO) {
        getRequest(
            "scrobble.view?id=$songId&submission=$submission",
            null,
            true
        )
    }

    // Artists
    suspend fun getNavidromeArtists(
        ignoreCachedResponse: Boolean = false,
        musicFolderIds: List<Int>? = NavidromeManager.getEnabledLibraryIdsForCurrentServer(),
    ): List<MediaData.Artist> = withContext(Dispatchers.IO) {
        getRequest(
            "getArtists.view?f=json",
            musicFolderIds,
            ignoreCachedResponse
        ).filterIsInstance<MediaData.Artist>()
    }

    suspend fun getNavidromeArtistAlbums(
        artistId: String, ignoreCachedResponse: Boolean = false
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        getRequest(
            "getArtist.view?id=$artistId&f=json",
            null,
            ignoreCachedResponse
        ).filterIsInstance<MediaItem>()
    }

    suspend fun getNavidromeArtistInfo(
        artistId: String, ignoreCachedResponse: Boolean = false
    ): MediaData.ArtistInfo? = withContext(Dispatchers.IO) {
        getRequest(
            "getArtistInfo.view?id=$artistId&f=json",
            null,
            ignoreCachedResponse
        ).filterIsInstance<MediaData.ArtistInfo>().firstOrNull()
    }

    suspend fun searchNavidromeArtists(
        query: String? = "",
        ignoreCachedResponse: Boolean = false,
        musicFolderIds: List<Int>? = NavidromeManager.getEnabledLibraryIdsForCurrentServer(),
    ): List<MediaData.Artist> = withContext(Dispatchers.IO) {
        if (query.isNullOrBlank()) getNavidromeArtists(musicFolderIds = musicFolderIds, ignoreCachedResponse = ignoreCachedResponse)
        else getRequest(
            "search3.view?query=$query&artistCount=100&albumCount=0&songCount=0&f=json",
            musicFolderIds,
            ignoreCachedResponse
        ).filterIsInstance<MediaData.Artist>()
    }

    // Playlists
    suspend fun getNavidromePlaylists(
        ignoreCachedResponse: Boolean = false,
        musicFolderIds: List<Int>? = NavidromeManager.getEnabledLibraryIdsForCurrentServer(),
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        getRequest(
            "getPlaylists.view?f=json",
            musicFolderIds,
            ignoreCachedResponse
        ).filterIsInstance<MediaItem>()
    }

    suspend fun getNavidromePlaylist(
        playlistId: String, ignoreCachedResponse: Boolean = false
    ): List<MediaItem>? = withContext(Dispatchers.IO) {
        getRequest(
            "getPlaylist.view?id=$playlistId&f=json",
            null,
            ignoreCachedResponse
        ).filterIsInstance<MediaItem>()
    }

    suspend fun createNavidromePlaylist(
        name: String, songIds: List<String>? = null, ignoreCachedResponse: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        var endpoint = "createPlaylist.view?name=$name"
        songIds?.forEach { songId -> endpoint += "&songId=$songId" }
        val response = getRequest(endpoint, null, ignoreCachedResponse)
        response.isNotEmpty()
    }

    suspend fun addSongToNavidromePlaylist(
        playlistId: String, songId: String, ignoreCachedResponse: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        val response = getRequest(
            "updatePlaylist.view?playlistId=$playlistId&songIdToAdd=$songId",
            null,
            ignoreCachedResponse
        )
        response.isNotEmpty()
    }

    suspend fun removeSongFromNavidromePlaylist(
        playlistId: String, songIndexToRemove: Int, ignoreCachedResponse: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        val response = getRequest(
            "updatePlaylist.view?playlistId=$playlistId&songIndexToRemove=$songIndexToRemove",
            null,
            ignoreCachedResponse
        )
        response.isNotEmpty()
    }

    suspend fun deleteNavidromePlaylist(
        playlistId: String, ignoreCachedResponse: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        val response = getRequest(
            "deletePlaylist.view?id=$playlistId",
            null,
            ignoreCachedResponse
        )
        response.isNotEmpty()
    }

    // Radios
    suspend fun getNavidromeRadios(
        ignoreCachedResponse: Boolean = false
    ): List<MediaData.Radio> = withContext(Dispatchers.IO) {
        getRequest(
            "getInternetRadioStations.view?f=json",
            null,
            ignoreCachedResponse
        ).filterIsInstance<MediaData.Radio>()
    }

    suspend fun createNavidromeRadio(
        name: String, url: String, homePageUrl: String? = null
    ) = withContext(Dispatchers.IO) {
        getRequest(
            "createInternetRadioStation.view?name=$name&streamUrl=$url&homepageUrl=$homePageUrl",
            null,
            true
        )
    }

    suspend fun updateNavidromeRadio(
        radioId: String, name: String, url: String, homePageUrl: String? = null
    ) = withContext(Dispatchers.IO) {
        getRequest(
            "updateInternetRadioStation.view?name=$name&streamUrl=$url&homepageUrl=$homePageUrl&id=$radioId",
            null,
            true
        )
    }

    suspend fun deleteNavidromeRadio(
        radioId: String
    ) = withContext(Dispatchers.IO) {
        getRequest(
            "deleteInternetRadioStation.view?id=$radioId",
            null,
            true
        )
    }

    // Lyrics
    suspend fun getNavidromePlainLyrics(
        metadata: MediaMetadata?, ignoreCachedResponse: Boolean = false
    ): List<Lyric> = withContext(Dispatchers.IO) {
        getRequest("getLyrics.view?artist=${metadata?.artist}&title=${metadata?.title}&f=json", null).filterIsInstance<MediaData.PlainLyrics>().getOrNull(0)?.toLyric()?.takeIf { it.content.isNotEmpty() }?.let { listOf(it) } ?: emptyList()
    }

    suspend fun getNavidromeSyncedLyrics(
        songId: String, ignoreCachedResponse: Boolean = false
    ): List<Lyric> = withContext(Dispatchers.IO) {
        getRequest("getLyricsBySongId.view?id=${songId}&f=json", null, ignoreCachedResponse)
            .filterIsInstance<MediaData.StructuredLyrics>().flatMap { it.toLyrics() }
    }

    // Starred Items
    suspend fun getNavidromeStarred(
        ignoreCachedResponse: Boolean = false,
        musicFolderIds: List<Int>? = NavidromeManager.getEnabledLibraryIdsForCurrentServer(),
        ): List<MediaItem> = withContext(Dispatchers.IO) {
        getRequest(
            "getStarred.view?f=json",
            musicFolderIds,
            ignoreCachedResponse
        ).filterIsInstance<MediaItem>()
    }

    suspend fun starNavidromeItem(itemId: String, ignoreCachedResponse: Boolean = false): Boolean =
        withContext(Dispatchers.IO) {
            getRequest("star.view?id=$itemId", null, ignoreCachedResponse)
            true
        }

    suspend fun unstarNavidromeItem(
        itemId: String, ignoreCachedResponse: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        getRequest("unstar.view?id=$itemId", null, ignoreCachedResponse)
        true
    }
}