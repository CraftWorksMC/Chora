package com.craftworks.music.providers.subsonic

import android.annotation.SuppressLint
import android.util.Log
import com.craftworks.music.data.model.AlbumArtistInfoResponse
import com.craftworks.music.data.model.AlbumInfo
import com.craftworks.music.data.model.AuthenticationResponse
import com.craftworks.music.data.model.GetQueueResponse
import com.craftworks.music.data.model.ImageRequest
import com.craftworks.music.data.model.InternetRadioStation
import com.craftworks.music.data.model.LibraryType
import com.craftworks.music.data.model.LyricsResponse
import com.craftworks.music.data.model.MediaItem
import com.craftworks.music.data.model.MediaQuery
import com.craftworks.music.data.model.PlaylistRules
import com.craftworks.music.data.model.ProviderFeatures
import com.craftworks.music.data.model.ProviderInfo
import com.craftworks.music.data.model.SearchResponse
import com.craftworks.music.data.model.TagListResponse
import com.craftworks.music.data.model.User
import com.craftworks.music.data.model.UserInfoResponse
import com.craftworks.music.providers.MediaProvider
import com.craftworks.music.providers.navidrome.MusicFolder
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class SubsonicMediaProvider(var serverInfo: SubsonicServerInfo) : MediaProvider {
    private val _featureFlags: MutableStateFlow<ProviderFeatures> = MutableStateFlow(
        ProviderFeatures.REPORT_PLAYBACK +
                ProviderFeatures.DOWNLOADS +
                ProviderFeatures.FAVORITES
    )
    override val featureFlags: StateFlow<ProviderFeatures> = _featureFlags.asStateFlow()

    companion object {
        fun md5Hash(input: String): String {
            val md = MessageDigest.getInstance("MD5")
            val hashBytes = md.digest(input.toByteArray())
            return hashBytes.joinToString("") { "%02x".format(it) }
        }

        fun generateSalt(length: Int): String {
            val allowedChars = ('a'..'z') + ('A'..'Z') + ('0'..'9')
            return (1..length).map { allowedChars.random() }.joinToString("")
        }
    }

    private val retrofit: Retrofit by lazy {
        var builder = Retrofit.Builder();
        var okBuilder = OkHttpClient.Builder();

        okBuilder.addInterceptor {
            it.proceed(it.request().newBuilder()
                .url(it.request().url.newBuilder()
                    .addQueryParameter("u", serverInfo.username)
                    .addQueryParameter("t", _token)
                    .addQueryParameter("s", _salt)
                    .addQueryParameter("v", "1.32.0")
                    .addQueryParameter("c", "Chora")
                    .addQueryParameter("f", "json")
                .build())
                .build())
        }

        if (serverInfo.allowSelfSignedCert) {
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
            val sslContext = SSLContext.getInstance("SSL").apply {
                init(null, trustAllCerts, SecureRandom())
            }
            val sslSocketFactory: SSLSocketFactory = sslContext.socketFactory

            okBuilder
                    .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                    .hostnameVerifier { _, _ -> true }
        }

        builder.baseUrl(serverInfo.url)
        .client(okBuilder.build())
        .build()
    }
    private val json = Json { ignoreUnknownKeys = true }
    private val client: OkHttpClient by lazy {
        HttpClient(OkHttp) {
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

    private val insecureClient: HttpClient by lazy { buildInsecureClient() }
    private fun buildInsecureClient(): HttpClient {

        return HttpClient(OkHttp.create {
            config {
                val sslContext = javax.net.ssl.SSLContext.getInstance("SSL")
                sslContext.init(null, trustAllCerts, SecureRandom())
                sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                hostnameVerifier { _, _ -> true }
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

    private var _salt: String? = null;
    private var _token: String? = null;
    private suspend fun getRequest(
        endpoint: String,
        musicFolderIds: List<Int>? = null,
        ignoreCachedResponse: Boolean = false
    ): SubsonicApiResponse = withContext(Dispatchers.IO) {
        if (serverInfo == null || _salt == null || _token == null) return@withContext SubsonicApiResponse(HttpStatusCode(0, ""), "", null);


        val url = URLBuilder("${serverInfo!!.url}/rest/${endpoint.replace(" ", "%20")}").apply {
            parameters.append("u", serverInfo.username)
            parameters.append("t", _token!!)
            parameters.append("s", _salt!!)
            musicFolderIds?.forEach { parameters.append("musicFolderId", it.toString()) }
            parameters.append("v", "1.16.1")
            parameters.append("c", "Chora")
            parameters.append("f", "json")
        }.build()

        val activeClient = if (serverInfo!!.allowSelfSignedCert) insecureClient else client

        try {
            val res = activeClient.get(url) {
                // Force network request if ignoreCachedResponse is true
                if (ignoreCachedResponse) {
                    headers {
                        append("Cache-Control", "no-cache")
                    }
                }
            }

            val body = res.bodyAsText()

            return@withContext SubsonicApiResponse(res.status, body, res.headers)

        } catch (e: Exception) {
            Log.e("NAVIDROME", "Network error for URL: $url", e)
        }

        SubsonicApiResponse(HttpStatusCode(0, ""), "", null)
    }

    override fun addToPlaylist(
        songIds: List<String>,
        playlistId: String
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun authenticate(
        username: String,
        password: String
    ): AuthenticationResponse {
        if (serverInfo == null) return AuthenticationResponse("", username="");
        _salt = generateSalt(8)
        _token = md5Hash(serverInfo?.password + _salt)
        TODO("Finish auth implementation")
    }

    override fun createFavorite(
        ids: List<String>,
        type: LibraryType
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun createInternetRadioStation(
        homepageUrl: String?,
        name: String,
        streamUrl: String
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun createPlaylist(
        name: String,
        comment: String,
        ownerId: String,
        public: Boolean,
        queryBuilderRules: PlaylistRules?,
        sync: Boolean
    ): String? {
        TODO("Not yet implemented")
    }

    override fun deleteFavorite(
        ids: List<String>,
        type: LibraryType
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun deleteInternetRadioStation(id: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun deletePlaylist(id: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun getAlbumArtistDetail(id: String): MediaItem.AlbumArtist? {
        TODO("Not yet implemented")
    }

    override fun getAlbumArtistInfo(
        id: String,
        limit: Int?
    ): AlbumArtistInfoResponse? {
        TODO("Not yet implemented")
    }

    override fun getAlbumArtistList(query: MediaQuery.AlbumArtistListQuery): List<MediaItem.AlbumArtist> {
        TODO("Not yet implemented")
    }

    override fun getAlbumArtistListCount(query: MediaQuery.AlbumArtistListQuery): Int {
        TODO("Not yet implemented")
    }

    override fun getAlbumDetail(id: String): MediaItem.Album {
        TODO("Not yet implemented")
    }

    override fun getAlbumInfo(id: String): AlbumInfo {
        TODO("Not yet implemented")
    }

    override fun getAlbumList(query: MediaQuery.AlbumListQuery): List<MediaItem.Album> {
        TODO("Not yet implemented")
    }

    override fun getAlbumListCount(query: MediaQuery.AlbumListQuery): Int {
        TODO("Not yet implemented")
    }

    override fun getAlbumRadio(
        albumId: String,
        count: Int?
    ): List<MediaItem.Song> {
        TODO("Not yet implemented")
    }

    override fun getArtistList(query: MediaQuery.ArtistListQuery): List<MediaItem.AlbumArtist> {
        TODO("Not yet implemented")
    }

    override fun getArtistListCount(query: MediaQuery.ArtistListQuery): Int {
        TODO("Not yet implemented")
    }

    override fun getArtistRadio(
        artistId: String,
        count: Int?
    ): List<MediaItem.Song> {
        TODO("Not yet implemented")
    }

    override fun getDownloadUrl(id: String): String {
        TODO("Not yet implemented")
    }

    override fun getFolder(query: MediaQuery.FolderQuery): MediaItem.Folder {
        TODO("Not yet implemented")
    }

    override fun getGenreList(query: MediaQuery.GenreListQuery): List<MediaItem.Genre> {
        TODO("Not yet implemented")
    }

    override fun getImageRequest(
        id: String,
        itemType: LibraryType,
        size: Int?,
        baseUrl: String?
    ): ImageRequest? {
        TODO("Not yet implemented")
    }

    override fun getImageUrl(
        id: String,
        itemType: LibraryType,
        size: Int?,
        baseUrl: String?
    ): String? {
        TODO("Not yet implemented")
    }

    override fun getInternetRadioStations(): List<InternetRadioStation> {
        TODO("Not yet implemented")
    }

    override fun getLyrics(songId: String): LyricsResponse {
        TODO("Not yet implemented")
    }

    override fun getMusicFolderList(): List<MusicFolder> {
        TODO("Not yet implemented")
    }

    override fun getPlaylistDetail(id: String): MediaItem.Playlist {
        TODO("Not yet implemented")
    }

    override fun getPlaylistList(query: MediaQuery.PlaylistListQuery): List<MediaItem.Playlist> {
        TODO("Not yet implemented")
    }

    override fun getPlaylistListCount(query: MediaQuery.PlaylistListQuery): Int {
        TODO("Not yet implemented")
    }

    override fun getPlaylistSongList(id: String): List<MediaItem.Song> {
        TODO("Not yet implemented")
    }

    override fun getPlayQueue(): GetQueueResponse {
        TODO("Not yet implemented")
    }

    override fun getRandomSongList(query: MediaQuery.RandomSongListQuery): List<MediaItem.Song> {
        TODO("Not yet implemented")
    }

    override fun getRoles(): List<String> {
        TODO("Not yet implemented")
    }

    override fun getProviderInfo(): ProviderInfo {
        TODO("Not yet implemented")
    }

    override fun getSimilarSongs(
        songId: String,
        count: Int?,
        musicFolderId: List<String>?
    ): List<MediaItem.Song> {
        TODO("Not yet implemented")
    }

    override fun getSongDetail(id: String): MediaItem.Song {
        TODO("Not yet implemented")
    }

    override fun getSongList(query: MediaQuery.SongListQuery): List<MediaItem.Song> {
        TODO("Not yet implemented")
    }

    override fun getSongListCount(query: MediaQuery.SongListQuery): Int {
        TODO("Not yet implemented")
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
        TODO("Not yet implemented")
    }

    override fun getTagList(
        type: LibraryType,
        folder: String?,
        tagName: String?
    ): TagListResponse {
        TODO("Not yet implemented")
    }

    override fun getTopSongs(
        artist: String,
        artistId: String,
        limit: Int?,
        type: String?
    ): List<MediaItem.Song> {
        TODO("Not yet implemented")
    }

    override fun getUserInfo(
        id: String,
        username: String
    ): UserInfoResponse {
        TODO("Not yet implemented")
    }

    override fun getUserList(query: MediaQuery.UserListQuery): List<User> {
        TODO("Not yet implemented")
    }

    override fun movePlaylistItem(
        playlistId: String,
        trackId: String,
        startingIndex: Int,
        endingIndex: Int
    ) {
        TODO("Not yet implemented")
    }

    override fun ping(): Boolean {
        TODO("Not yet implemented")
    }

    override fun removeFromPlaylist(
        id: String,
        songIds: List<String>
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun replacePlaylist(
        id: String,
        songIds: List<String>
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun savePlayQueue(
        songs: List<String>,
        currentIndex: Int?,
        positionMs: Int?
    ) {
        TODO("Not yet implemented")
    }

    override fun scrobble(
        id: String,
        mediaType: String,
        playbackRate: Float,
        submission: Boolean,
        albumId: String?,
        event: String?,
        position: Int?
    ) {
        TODO("Not yet implemented")
    }

    override fun search(query: MediaQuery.SearchQuery): SearchResponse {
        TODO("Not yet implemented")
    }

    override fun setPlaylistSongs(
        id: String,
        songIds: List<String>
    ) {
        TODO("Not yet implemented")
    }

    override fun setRating(
        ids: List<String>,
        rating: Int,
        type: LibraryType
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun shareItem(
        description: String,
        downloadable: Boolean,
        expires: Long,
        resourceIds: String,
        resourceType: String
    ): String? {
        TODO("Not yet implemented")
    }

    override fun updateInternetRadioStation(
        id: String,
        name: String,
        streamUrl: String,
        homepageUrl: String?
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun updatePlaylist(
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