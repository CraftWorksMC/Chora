package com.craftworks.music.data.providers.media.subsonic

import android.content.Context
import com.craftworks.music.BuildConfig
import com.craftworks.music.R
import com.craftworks.music.data.model.AlbumArtistDetailResponse
import com.craftworks.music.data.model.AlbumArtistInfo
import com.craftworks.music.data.model.AlbumArtistListSort
import com.craftworks.music.data.model.AlbumInfo
import com.craftworks.music.data.model.AlbumListSort
import com.craftworks.music.data.model.ArtistListSort
import com.craftworks.music.data.model.AuthenticationResponse
import com.craftworks.music.data.model.GenreListSort
import com.craftworks.music.data.model.GetQueueResponse
import com.craftworks.music.data.model.ImageRequest
import com.craftworks.music.data.model.LibraryType
import com.craftworks.music.data.model.Lyrics
import com.craftworks.music.data.model.MediaModel
import com.craftworks.music.data.model.MediaQuery
import com.craftworks.music.data.model.MusicFolder
import com.craftworks.music.data.model.PlaylistListSort
import com.craftworks.music.data.model.PlaylistRules
import com.craftworks.music.data.model.ProviderFeatures
import com.craftworks.music.data.model.ProviderInfo
import com.craftworks.music.data.model.ProviderType
import com.craftworks.music.data.model.ScrobbleEvent
import com.craftworks.music.data.model.ScrobbleMediaType
import com.craftworks.music.data.model.SearchResponse
import com.craftworks.music.data.model.SongListSort
import com.craftworks.music.data.model.TagListResponse
import com.craftworks.music.data.model.User
import com.craftworks.music.data.model.UserInfoResponse
import com.craftworks.music.data.providers.media.MediaProvider
import com.craftworks.music.utils.PagingUtils
import com.craftworks.music.utils.StringUtils
import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.time.Year
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@Serializable
@SerialName("subsonic")
open class SubsonicMediaProvider : MediaProvider() {
    lateinit var providerData: SubsonicProviderData
    companion object {
        private val ALBUM_ARTIST_SORT_BINDING = mapOf<AlbumArtistListSort, (MediaModel.Artist) -> Comparable<*>?>(
            AlbumArtistListSort.ALBUM_COUNT to { it.albumCount },
            AlbumArtistListSort.FAVORITE to { it.userFavorite },
            AlbumArtistListSort.NAME to { it.name },
            AlbumArtistListSort.RATING to { it.userRating },
        )
        private val PLAYLIST_SORT_BINDING = mapOf<PlaylistListSort, (SubsonicPlaylist) -> Comparable<*>?>(
            PlaylistListSort.DURATION to { it.duration },
            PlaylistListSort.NAME to { it.name },
            PlaylistListSort.SONG_COUNT to { it.songCount },
            PlaylistListSort.UPDATED_AT to { it.changed },
        )
    }
    override val providerIcon: Int
        get() = R.drawable.s_m_opensubsonic
    override val providerMonochromeIcon: Boolean
        get() = false

    override val providerName: Int
        get() = R.string.source_open_subsonic

    @Transient
    private val _featureFlags: ProviderFeatures =
                ProviderFeatures.REPORT_PLAYBACK +
                ProviderFeatures.DOWNLOADS +
                ProviderFeatures.FAVORITES +
                ProviderFeatures.INTERNET_RADIO +
                ProviderFeatures.PLAYLIST

    @Transient
    override val featureFlags: ProviderFeatures = _featureFlags
    @Transient
    override val supportedAlbumSort: List<AlbumListSort> = listOf(
        AlbumListSort.ALBUM_ARTIST,
        AlbumListSort.PLAY_COUNT,
        AlbumListSort.NAME,
        AlbumListSort.RANDOM,
        AlbumListSort.RECENTLY_ADDED,
        AlbumListSort.RECENTLY_PLAYED,
    )
    @Transient
    override val supportedAlbumArtistSort: List<AlbumArtistListSort> = listOf(
        AlbumArtistListSort.ALBUM_COUNT,
        AlbumArtistListSort.FAVORITE,
        AlbumArtistListSort.NAME,
        AlbumArtistListSort.RATING,
    )
    @Transient
    override val supportedArtistSort: List<ArtistListSort> = listOf(
        ArtistListSort.ALBUM_COUNT,
        ArtistListSort.FAVORITE,
        ArtistListSort.NAME,
        ArtistListSort.RATING,
    )
    @Transient
    override val supportedGenreSort: List<GenreListSort> = listOf(GenreListSort.NAME)
    @Transient
    override val supportedPlaylistSort: List<PlaylistListSort> = listOf(
        PlaylistListSort.DURATION,
        PlaylistListSort.NAME,
        PlaylistListSort.SONG_COUNT,
        PlaylistListSort.UPDATED_AT,
    )
    @Transient
    override val supportedSongSort: List<SongListSort> = listOf(SongListSort.NAME)

    @Transient
    protected val choraVersion: String = BuildConfig.VERSION_NAME

    private val ktorfit: Ktorfit by lazy {
        val ktorClient = HttpClient(OkHttp) {
            install(createClientPlugin("SubsonicAuthParams") {
                onRequest { request, _ ->
                    val salt = StringUtils.generateSalt(8)
                    val token = StringUtils.md5Hash(providerData.password + salt)

                    request.url.parameters.apply {
                        append("u", providerData.username)
                        append("t", token)
                        append("s", salt)
                        append("v", choraVersion)
                        append("c", "Chora")
                        append("f", "json")
                    }
                }
            })

            install(Logging) {
                logger = Logger.SIMPLE
                level = LogLevel.INFO
            }

            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }

            engine {
                config {
                    if (providerData.allowSelfSignedCert) {
                        val trustAllCerts = arrayOf<TrustManager>(
                            object : X509TrustManager {
                                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                            }
                        )
                        val sslContext = SSLContext.getInstance("SSL").apply {
                            init(null, trustAllCerts, SecureRandom())
                        }

                        sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                        hostnameVerifier { _, _ -> true }
                    }
                }
            }
        }

        Ktorfit.Builder()
            .baseUrl(if (providerData.url.endsWith("/")) providerData.url else providerData.url + "/")
            .httpClient(ktorClient)
            .build()
    }

    private val service: SubsonicService by lazy { ktorfit.createSubsonicService() }

    @Transient
    private var _salt: String? = null
    @Transient
    private var _token: String? = null

    override fun init(context: Context) {
        _salt = StringUtils.generateSalt(8)
        _token = StringUtils.md5Hash(providerData.password + _salt)
    }

    override suspend fun addToPlaylist(
        playlistId: String,
        songIds: List<String>
    ): Boolean {
        return service.updatePlaylist(playlistId, songIdToAdd = songIds).subsonicResponse.status == "ok"
    }

    override suspend fun authenticate(
        username: String,
        password: String
    ): AuthenticationResponse {
        providerData.username = username
        providerData.password = password

        _salt = StringUtils.generateSalt(8)
        _token = StringUtils.md5Hash(providerData.password + _salt)

        val res = try {
            service.ping()
        } catch (e: Exception) {
            throw Exception("Failed to ping provider", e)
        }

        return AuthenticationResponse(
            isAdmin = res.subsonicResponse.user?.adminRole ?: false,
            providerType = when (res.subsonicResponse.type) {
                "navidrome" -> ProviderType.NAVIDROME
                else -> ProviderType.SUBSONIC
            }
        )
    }

    override suspend fun createFavorite(
        ids: List<String>,
        type: LibraryType
    ): Boolean {
        val res = when (type) {
            LibraryType.SONG -> {
                service.star(ids, null, null)
            }

            LibraryType.ALBUM -> {
                service.star(null, ids, null)
            }

            LibraryType.ARTIST -> {
                service.star(null, null, ids)
            }

            else -> return false
        }

        return res.subsonicResponse.status == "ok"
    }

    override suspend fun createInternetRadioStation(
        name: String,
        streamUrl: String,
        homepageUrl: String?
    ): Boolean {
        return service.createInternetRadioStation(streamUrl, name, homepageUrl).subsonicResponse.status == "ok"
    }

    override suspend fun createPlaylist(
        name: String,
        comment: String,
        ownerId: String,
        public: Boolean,
        queryBuilderRules: PlaylistRules?,
        sync: Boolean
    ): String? {
        return service.createPlaylist(name = name).subsonicResponse.playlist?.id
    }

    override suspend fun deleteFavorite(
        ids: List<String>,
        type: LibraryType
    ): Boolean {
        val res = when (type) {
            LibraryType.SONG -> {
                service.unstar(ids, null, null)
            }

            LibraryType.ALBUM -> {
                service.unstar(null, ids, null)
            }

            LibraryType.ARTIST -> {
                service.unstar(null, null, ids)
            }

            else -> return false
        }

        return res.subsonicResponse.status == "ok"
    }

    override suspend fun deleteInternetRadioStation(id: String): Boolean {
        return service.deleteInternetRadioStation(id).subsonicResponse.status == "ok"
    }

    override suspend fun deletePlaylist(id: String): Boolean {
        return service.deletePlaylist(id).subsonicResponse.status == "ok"
    }

    override suspend fun getAlbumArtistDetail(id: String): AlbumArtistDetailResponse {
        val res = service.getArtist(id).subsonicResponse.artist
        return AlbumArtistDetailResponse(res?.toMediaModel(this.id), res?.album?.map { it.toMediaModel(this.id) })
    }

    override suspend fun getAlbumArtistInfo(
        id: String,
        limit: Int?
    ): AlbumArtistInfo? {
        return service.getArtistInfo(id = id, count = limit).subsonicResponse.artistInfo?.toArtistInfo(this.id)
    }

    override suspend fun getAlbumArtistList(query: MediaQuery.AlbumArtistListQuery): List<MediaModel.Artist> {
        var artists = (service.getArtists(
            musicFolderId = query.musicFolderId?.map { it.toInt() } ?: data.libraries.filter { it.second }.map { it.first.id.toInt() },
        ).subsonicResponse.artists?.index ?: emptyList())
            .flatMap { it.artist }
            .map { it.toMediaModel(id) }

        if (query.searchTerm != null) {
            artists = artists.filter { it.name.contains(query.searchTerm, true) }
        }

        if (query.favorite != null) {
            artists = artists.filter { it.userFavorite == query.favorite }
        }

        return PagingUtils.sortAndPaginate(
            artists,
            sortBy = ALBUM_ARTIST_SORT_BINDING[query.sortBy] ?: { it.name },
            sortOrder = query.sortOrder
        )
    }

    override suspend fun getAlbumDetail(id: String): MediaModel.Album {
        try {
            return service.getAlbum(id).subsonicResponse.album!!.toMediaModel(this.id)
        }
        catch (e: Exception) {
            throw Exception("Failed to get album", e)
        }
    }

    override suspend fun getAlbumInfo(id: String): AlbumInfo {
        TODO("Not yet implemented")
    }

    override suspend fun getAlbumList(query: MediaQuery.AlbumListQuery): List<MediaModel.Album> {
        println("GETTING ALBUM LIST")
        if (!query.searchTerm.isNullOrBlank()) {
            val res = try {
                service.search3(
                    albumCount = query.limit ?: 20,
                    albumOffset = query.startIndex,
                    artistCount = 0,
                    artistOffset = 0,
                    musicFolderId = query.musicFolderId?.map { it.toInt() } ?: data.libraries.filter { it.second }.map { it.first.id.toInt() },
                    query = query.searchTerm,
                    songCount = 0,
                    songOffset = 0
                )
            } catch (e: Exception) {
                throw Exception("Failed to get album list", e)
            }

            return res.subsonicResponse.searchResult3?.album?.map { it.toMediaModel(this.id) } ?: emptyList()
        }

        val currentYear = Year.now().value
        var fromYear: Int? = null
        var toYear: Int? = null

        if (query.minYear != null) {
            fromYear = query.minYear
            toYear = currentYear
        }

        if (query.maxYear != null) {
            toYear = query.maxYear
            if (query.minYear == null) {
                fromYear = 0
            }
        }

        var type = when (query.sortBy) {
            AlbumListSort.ALBUM_ARTIST -> "alphabeticalByArtist"
            AlbumListSort.NAME -> "alphabeticalByName"
            AlbumListSort.RECENTLY_PLAYED -> "recent"
            AlbumListSort.RECENTLY_ADDED -> "newest"
            AlbumListSort.PLAY_COUNT -> "frequent"
            AlbumListSort.RANDOM -> "random"
            else -> "alphabeticalByName"
        }

        if (query.favorite == true)
            type = "starred"

        val res = try {
            service.getAlbumList(
                type = type,
                size = query.limit,
                offset = query.startIndex,
                fromYear = fromYear,
                toYear = toYear,
                genre = query.genreIds?.firstOrNull(),
                musicFolderId = query.musicFolderId?.map { it.toInt() } ?: data.libraries.filter { it.second }.map { it.first.id.toInt() },
            )
        } catch (e: Exception) {
            throw Exception("Failed to get album list", e)
        }

        return res.subsonicResponse.albumList2?.album?.map { it.toMediaModel(this.id) } ?: emptyList()
    }

    override suspend fun getAlbumRadio(
        albumId: String,
        count: Int?
    ): List<MediaModel.Song> {
        TODO("Not yet implemented")
    }

    override suspend fun getArtistList(query: MediaQuery.ArtistListQuery): List<MediaModel.Artist> {
        TODO("Not yet implemented")
    }

    override suspend fun getArtistRadio(
        artistId: String,
        count: Int?
    ): List<MediaModel.Song> {
        TODO("Not yet implemented")
    }

    override suspend fun getDownloadUrl(id: String): String {
        TODO("Not yet implemented")
    }

    override suspend fun getFolder(query: MediaQuery.FolderQuery): MediaModel.Folder {
        TODO("Not yet implemented")
    }

    override suspend fun getGenreList(query: MediaQuery.GenreListQuery): List<MediaModel.Genre> {
        TODO("Not yet implemented")
    }

    override suspend fun getImageRequest(
        id: String,
        itemType: LibraryType,
        size: Int?,
        baseUrl: String?
    ): ImageRequest? {
        TODO("Not yet implemented")
    }

    override fun getImageUrl(
        id: String,
        itemType: LibraryType?,
        size: Int?,
    ): String {
        val urlBuilder = providerData.url.toHttpUrlOrNull()
            ?.newBuilder()
            ?.addPathSegments("rest/getCoverArt.view")
            ?.addQueryParameter("id", id)

        if (size != null) {
            urlBuilder?.addQueryParameter("size", size.toString())
        }

        urlBuilder?.addQueryParameter("u", providerData.username ?: "")
        urlBuilder?.addQueryParameter("t", _token ?: "")
        urlBuilder?.addQueryParameter("s", _salt ?: "")
        urlBuilder?.addQueryParameter("c", "Chora")
        urlBuilder?.addQueryParameter("v", choraVersion)
        return urlBuilder?.build()?.toString() ?: ""
    }

    override suspend fun getInternetRadioStations(): List<MediaModel.InternetRadioStation> {
        return service.getInternetRadioStations()
            .subsonicResponse.internetRadioStations?.internetRadioStation
            ?.map { it.toMediaModel(id) } ?: emptyList()
    }

    override suspend fun getLyrics(songId: String): List<Lyrics> {
        return service.getLyricsBySongId(songId, true)
            .subsonicResponse.lyricsList?.structuredLyrics
            ?.filter { it.kind == "main" || it.kind == null }
            ?.map { it.toLyrics() }.orEmpty()
    }

    override suspend fun getMusicFolderList(): List<MusicFolder> {
        val res = try {
            service.getMusicFolderList()
        } catch (e: Exception) {
            throw Exception("Failed to get music folders", e)
        }

        return res.subsonicResponse.musicFolders?.musicFolder?.map {
            MusicFolder(
                id = it.id.toString(),
                name = it.name
            )
        } ?: emptyList()
    }

    override suspend fun getPlaylistDetail(id: String): MediaModel.Playlist {
        TODO("Not yet implemented")
    }

    override suspend fun getPlaylistList(query: MediaQuery.PlaylistListQuery): List<MediaModel.Playlist> {
        var playlists = service.getPlaylists().subsonicResponse.playlists?.playlist ?: emptyList()

        if (query.searchTerm != null) {
            playlists = playlists.filter { it.name.contains(query.searchTerm, true) }
        }

        return PagingUtils.sortAndPaginate(
            playlists,
            query.limit,
            query.startIndex,
            true,
            PLAYLIST_SORT_BINDING[query.sortBy] ?: { it.name },
            query.sortOrder
        ).map { it.toMediaModel(id) }
    }

    override suspend fun getPlaylistSongList(id: String): List<MediaModel.Song> {
        return service.getPlaylist(id).subsonicResponse.playlist?.entry?.map { it.toMediaModel(this.id) } ?: emptyList()
    }

    override suspend fun getPlayQueue(): GetQueueResponse {
        TODO("Not yet implemented")
    }

    override suspend fun getRandomSongList(query: MediaQuery.RandomSongListQuery): List<MediaModel.Song> {
        TODO("Not yet implemented")
    }

    override suspend fun getRoles(): List<String> {
        TODO("Not yet implemented")
    }

    override suspend fun getProviderInfo(): ProviderInfo {
        TODO("Not yet implemented")
    }

    override suspend fun getSimilarSongs(
        songId: String,
        count: Int?,
        musicFolderId: List<String>?
    ): List<MediaModel.Song> {
        TODO("Not yet implemented")
    }

    override suspend fun getSongDetail(id: String): MediaModel.Song {
        TODO("Not yet implemented")
    }

    override suspend fun getSongList(query: MediaQuery.SongListQuery): List<MediaModel.Song> {
        if (query.searchTerm != null) {
            return service.search3(
                albumCount = 0,
                albumOffset = 0,
                artistCount = 0,
                artistOffset = 0,
                musicFolderId = query.musicFolderId?.map { it.toInt() } ?: data.libraries.filter { it.second }.map { it.first.id.toInt() },
                query = query.searchTerm,
                songCount = query.limit ?: 20,
                songOffset = query.startIndex
            ).subsonicResponse.searchResult3?.song?.map { it.toMediaModel(this.id) } ?: emptyList()
        }
        if (query.genreIds?.any()?:false) {
            return service.getSongsByGenre(
                count = query.limit,
                genre = query.genreIds[0],
                musicFolderId = query.musicFolderId?.map { it.toInt() } ?: data.libraries.filter { it.second }.map { it.first.id.toInt() },
                offset = query.startIndex
            ).subsonicResponse.songsByGenre?.song?.map { it.toMediaModel(this.id) } ?: emptyList()
        }
        if (query.favorite?:false) {
            return service.getStarred(
                musicFolderId = query.musicFolderId?.map { it.toInt() } ?: data.libraries.filter { it.second }.map { it.first.id.toInt() },
            ).subsonicResponse.starred?.song?.map { it.toMediaModel(this.id) } ?: emptyList()
        }

        val artistsIds = mutableListOf<String>()
        if (query.albumArtistIds != null) artistsIds.addAll(query.albumArtistIds)
        if (query.artistIds != null) artistsIds.addAll(query.artistIds)

        if (artistsIds.isNotEmpty()) {
            TODO("Not yet implemented")
        }

        return service.search3(
            albumCount = 0,
            albumOffset = 0,
            artistCount = 0,
            artistOffset = 0,
            musicFolderId = query.musicFolderId?.map { it.toInt() } ?: data.libraries.filter { it.second }.map { it.first.id.toInt() },
            query = query.searchTerm?:"",
            songCount = query.limit ?: 20,
            songOffset = query.startIndex
        ).subsonicResponse.searchResult3?.song?.map { it.toMediaModel(this.id) } ?: emptyList()
    }

    override fun getStreamUrl(
        id: String,
        transcode: Boolean,
        bitrate: Int?,
        format: String?,
        mediaType: String?,
        offset: Int?,
        skipAutoTranscode: Boolean?
    ): String {
        val urlBuilder = providerData.url.toHttpUrlOrNull()
            ?.newBuilder()
            ?.addPathSegments("rest/stream.view")
            ?.addQueryParameter("id", id)

        urlBuilder?.addQueryParameter("u", providerData.username ?: "")
        urlBuilder?.addQueryParameter("t", _token ?: "")
        urlBuilder?.addQueryParameter("s", _salt ?: "")
        urlBuilder?.addQueryParameter("c", "Chora")
        urlBuilder?.addQueryParameter("v", choraVersion)

        return urlBuilder?.build()?.toString() ?: ""
    }

    override suspend fun getTagList(
        type: LibraryType,
        folder: String?,
        tagName: String?
    ): TagListResponse {
        TODO("Not yet implemented")
    }

    override suspend fun getTopSongs(
        artist: String,
        artistId: String,
        limit: Int?,
        type: String?
    ): List<MediaModel.Song> {
        TODO("Not yet implemented")
    }

    override suspend fun getUserInfo(
        id: String,
        username: String
    ): UserInfoResponse {
        TODO("Not yet implemented")
    }

    override suspend fun getUserList(query: MediaQuery.UserListQuery): List<User> {
        TODO("Not yet implemented")
    }

    override suspend fun movePlaylistItem(
        playlistId: String,
        trackId: String,
        startingIndex: Int,
        endingIndex: Int
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun ping(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun removeFromPlaylist(
        id: String,
        songIds: List<String>
    ): Boolean {
        val playlist = service.getPlaylist(id).subsonicResponse.playlist
        return service.updatePlaylist(id, songIndexToRemove =
            songIds.map { playlist?.entry?.indexOfFirst { song -> song.id == it } ?: -1 }
                .filter { it != -1 }
        ).subsonicResponse.status == "ok"
    }

    override suspend fun replacePlaylist(
        id: String,
        songIds: List<String>
    ): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun savePlayQueue(
        songs: List<String>,
        currentIndex: Int?,
        positionMs: Int?
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun scrobble(
        id: String,
        mediaType: ScrobbleMediaType,
        playbackRate: Float,
        submission: Boolean,
        albumId: String?,
        event: ScrobbleEvent?,
        position: Int?
    ) {
        try {
            service.scrobble(id, position, submission)
        }
        catch (e: Exception) {
            throw Exception("Failed to scrobble", e)
        }
    }

    override suspend fun search(query: MediaQuery.SearchQuery): SearchResponse {
        TODO("Not yet implemented")
    }

    override suspend fun setPlaylistSongs(
        id: String,
        songIds: List<String>
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun setRating(
        ids: List<String>,
        rating: Int,
        type: LibraryType
    ): Boolean {
        return ids.all {
            service.setRating(it, rating).subsonicResponse.status == "ok"
        }
    }

    override suspend fun shareItem(
        description: String,
        downloadable: Boolean,
        expires: Long,
        resourceIds: String,
        resourceType: String
    ): String? {
        TODO("Not yet implemented")
    }

    override suspend fun updateInternetRadioStation(
        id: String,
        name: String,
        streamUrl: String,
        homepageUrl: String?
    ): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun updatePlaylist(
        id: String,
        name: String,
        comment: String?,
        ownerId: String?,
        public: Boolean?,
        queryBuilderRules: PlaylistRules?,
        sync: Boolean?
    ): Boolean {
        TODO("Not yet implemented")
    }
}