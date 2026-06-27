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
import retrofit2.awaitResponse
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
        suspend fun md5Hash(input: String): String {
            val md = MessageDigest.getInstance("MD5")
            val hashBytes = md.digest(input.toByteArray())
            return hashBytes.joinToString("") { "%02x".format(it) }
        }

        suspend fun generateSalt(length: Int): String {
            val allowedChars = ('a'..'z') + ('A'..'Z') + ('0'..'9')
            return (1..length).map { allowedChars.random() }.joinToString("")
        }
    }

    private val retrofit: Retrofit by lazy {
        var builder = Retrofit.Builder();
        var okBuilder = OkHttpClient.Builder();

        okBuilder.addInterceptor {
            if (_salt == null) {
                val delimiter = serverInfo.credentials.indexOf(':')
                _salt = serverInfo.credentials.substring(0, delimiter)
                _token = serverInfo.credentials.substring(delimiter + 1)
            }
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
    private val service: SubsonicService by lazy {retrofit.create(SubsonicService::class.java)}

    private var _salt: String? = null;
    private var _token: String? = null;

    override suspend fun addToPlaylist(
        songIds: List<String>,
        playlistId: String
    ): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun authenticate(
        username: String,
        password: String
    ): AuthenticationResponse {
        serverInfo.username = username
        _salt = generateSalt(8)
        _token = md5Hash(password + _salt)

        val res = service.authenticate(username).awaitResponse();
        val body = res.body();

        if (res.isSuccessful) return AuthenticationResponse(
            credential = "$_salt:$_token",
            isAdmin = body?.user?.adminRole ?: false,
            userId = body?.user?.username,
            username = username
        )

        throw Exception("Failed to login")
    }

    override suspend fun createFavorite(
        ids: List<String>,
        type: LibraryType
    ): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun createInternetRadioStation(
        homepageUrl: String?,
        name: String,
        streamUrl: String
    ): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun createPlaylist(
        name: String,
        comment: String,
        ownerId: String,
        public: Boolean,
        queryBuilderRules: PlaylistRules?,
        sync: Boolean
    ): String? {
        TODO("Not yet implemented")
    }

    override suspend fun deleteFavorite(
        ids: List<String>,
        type: LibraryType
    ): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun deleteInternetRadioStation(id: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun deletePlaylist(id: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun getAlbumArtistDetail(id: String): MediaItem.AlbumArtist? {
        TODO("Not yet implemented")
    }

    override suspend fun getAlbumArtistInfo(
        id: String,
        limit: Int?
    ): AlbumArtistInfoResponse? {
        TODO("Not yet implemented")
    }

    override suspend fun getAlbumArtistList(query: MediaQuery.AlbumArtistListQuery): List<MediaItem.AlbumArtist> {
        TODO("Not yet implemented")
    }

    override suspend fun getAlbumArtistListCount(query: MediaQuery.AlbumArtistListQuery): Int {
        TODO("Not yet implemented")
    }

    override suspend fun getAlbumDetail(id: String): MediaItem.Album {
        TODO("Not yet implemented")
    }

    override suspend fun getAlbumInfo(id: String): AlbumInfo {
        TODO("Not yet implemented")
    }

    override suspend fun getAlbumList(query: MediaQuery.AlbumListQuery): List<MediaItem.Album> {
        TODO("Not yet implemented")
    }

    override suspend fun getAlbumListCount(query: MediaQuery.AlbumListQuery): Int {
        TODO("Not yet implemented")
    }

    override suspend fun getAlbumRadio(
        albumId: String,
        count: Int?
    ): List<MediaItem.Song> {
        TODO("Not yet implemented")
    }

    override suspend fun getArtistList(query: MediaQuery.ArtistListQuery): List<MediaItem.AlbumArtist> {
        TODO("Not yet implemented")
    }

    override suspend fun getArtistListCount(query: MediaQuery.ArtistListQuery): Int {
        TODO("Not yet implemented")
    }

    override suspend fun getArtistRadio(
        artistId: String,
        count: Int?
    ): List<MediaItem.Song> {
        TODO("Not yet implemented")
    }

    override suspend fun getDownloadUrl(id: String): String {
        TODO("Not yet implemented")
    }

    override suspend fun getFolder(query: MediaQuery.FolderQuery): MediaItem.Folder {
        TODO("Not yet implemented")
    }

    override suspend fun getGenreList(query: MediaQuery.GenreListQuery): List<MediaItem.Genre> {
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

    override suspend fun getImageUrl(
        id: String,
        itemType: LibraryType,
        size: Int?,
        baseUrl: String?
    ): String? {
        TODO("Not yet implemented")
    }

    override suspend fun getInternetRadioStations(): List<InternetRadioStation> {
        TODO("Not yet implemented")
    }

    override suspend fun getLyrics(songId: String): LyricsResponse {
        TODO("Not yet implemented")
    }

    override suspend fun getMusicFolderList(): List<MusicFolder> {
        TODO("Not yet implemented")
    }

    override suspend fun getPlaylistDetail(id: String): MediaItem.Playlist {
        TODO("Not yet implemented")
    }

    override suspend fun getPlaylistList(query: MediaQuery.PlaylistListQuery): List<MediaItem.Playlist> {
        TODO("Not yet implemented")
    }

    override suspend fun getPlaylistListCount(query: MediaQuery.PlaylistListQuery): Int {
        TODO("Not yet implemented")
    }

    override suspend fun getPlaylistSongList(id: String): List<MediaItem.Song> {
        TODO("Not yet implemented")
    }

    override suspend fun getPlayQueue(): GetQueueResponse {
        TODO("Not yet implemented")
    }

    override suspend fun getRandomSongList(query: MediaQuery.RandomSongListQuery): List<MediaItem.Song> {
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
    ): List<MediaItem.Song> {
        TODO("Not yet implemented")
    }

    override suspend fun getSongDetail(id: String): MediaItem.Song {
        TODO("Not yet implemented")
    }

    override suspend fun getSongList(query: MediaQuery.SongListQuery): List<MediaItem.Song> {
        TODO("Not yet implemented")
    }

    override suspend fun getSongListCount(query: MediaQuery.SongListQuery): Int {
        TODO("Not yet implemented")
    }

    override suspend fun getStreamUrl(
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
    ): List<MediaItem.Song> {
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
        TODO("Not yet implemented")
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
        mediaType: String,
        playbackRate: Float,
        submission: Boolean,
        albumId: String?,
        event: String?,
        position: Int?
    ) {
        TODO("Not yet implemented")
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
        TODO("Not yet implemented")
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