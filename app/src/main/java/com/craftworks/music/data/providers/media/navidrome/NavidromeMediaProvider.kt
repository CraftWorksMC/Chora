package com.craftworks.music.data.providers.media.navidrome

import com.craftworks.music.R
import com.craftworks.music.data.model.AlbumArtistListSort
import com.craftworks.music.data.model.AlbumListSort
import com.craftworks.music.data.model.ArtistListSort
import com.craftworks.music.data.model.MediaModel
import com.craftworks.music.data.model.MediaQuery
import com.craftworks.music.data.model.SongListSort
import com.craftworks.music.data.providers.media.subsonic.SubsonicMediaProvider
import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.plugins.plugin
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.AttributeKey
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@Serializable
@SerialName("navidrome")
class NavidromeMediaProvider : SubsonicMediaProvider() {

    companion object {
        private val ALBUM_ARTIST_SORT_BINDING =
            mapOf(
                AlbumArtistListSort.ALBUM_COUNT to "albumCount",
                AlbumArtistListSort.FAVORITE to "starred_at",
                AlbumArtistListSort.NAME to "name",
                AlbumArtistListSort.PLAY_COUNT to "playCount",
                AlbumArtistListSort.RATING to "rating",
                AlbumArtistListSort.SONG_COUNT to "songCount",
            )
        private val ALBUM_SORT_BINDING =
            mapOf(
                AlbumListSort.ALBUM_ARTIST to "album_artist",
                AlbumListSort.ARTIST to "artist",
                AlbumListSort.DURATION to "duration",
                AlbumListSort.EXPLICIT_STATUS to "explicitStatus",
                AlbumListSort.FAVORITE to "starred_at",
                AlbumListSort.NAME to "name",
                AlbumListSort.PLAY_COUNT to "play_count",
                AlbumListSort.RANDOM to "random",
                AlbumListSort.RATING to "rating",
                AlbumListSort.RECENTLY_ADDED to "recently_added",
                AlbumListSort.RECENTLY_PLAYED to "play_date",
                AlbumListSort.RELEASE_DATE to "release_date",
                AlbumListSort.SONG_COUNT to "songCount",
                AlbumListSort.SORT_NAME to "name",
                AlbumListSort.YEAR to "max_year",
            )
        private val ARTIST_SORT_BINDING =
            mapOf(
                ArtistListSort.ALBUM_COUNT to "albumCount",
                ArtistListSort.FAVORITE to "starred_at",
                ArtistListSort.NAME to "name",
                ArtistListSort.PLAY_COUNT to "playCount",
                ArtistListSort.RATING to "rating",
                ArtistListSort.SONG_COUNT to "songCount",
            )
        private val SONG_SORT_BINDING =
            mapOf(
                SongListSort.ALBUM to "album",
                SongListSort.ALBUM_ARTIST to "order_album_artist_name",
                SongListSort.ARTIST to "artist",
                SongListSort.BPM to "bpm",
                SongListSort.CHANNELS to "channels",
                SongListSort.COMMENT to "comment",
                SongListSort.DURATION to "duration",
                SongListSort.EXPLICIT_STATUS to "explicitStatus",
                SongListSort.FAVORITE to "starred_at",
                SongListSort.GENRE to "genre",
                SongListSort.ID to "id",
                SongListSort.NAME to "title",
                SongListSort.PLAY_COUNT to "playCount",
                SongListSort.RANDOM to "random",
                SongListSort.RATING to "rating",
                SongListSort.RECENTLY_ADDED to "createdAt",
                SongListSort.RECENTLY_PLAYED to "playDate",
                SongListSort.SORT_NAME to "title",
                SongListSort.YEAR to "year",
            )
    }

    override val providerIcon: Int
        get() = R.drawable.s_m_navidrome
    override val providerName: Int
        get() = R.string.source_navidrome

    @Transient
    override val supportedAlbumSort: List<AlbumListSort> = listOf(
        AlbumListSort.ALBUM_ARTIST,
        AlbumListSort.ARTIST,
        AlbumListSort.DURATION,
        AlbumListSort.EXPLICIT_STATUS,
        AlbumListSort.FAVORITE,
        AlbumListSort.NAME,
        AlbumListSort.PLAY_COUNT,
        AlbumListSort.RANDOM,
        AlbumListSort.RATING,
        AlbumListSort.RECENTLY_ADDED,
        AlbumListSort.RECENTLY_PLAYED,
        AlbumListSort.RELEASE_DATE,
        AlbumListSort.SONG_COUNT,
        AlbumListSort.YEAR,
    )

    @Contextual
    private val isPublicKey = AttributeKey<Boolean>("isPublic")

    @Transient
    private var _token: String? = null
    @Transient
    private val authMutex = Mutex()

    private val ktorfit: Ktorfit by lazy {
        val ktorClient = HttpClient(OkHttp) {
            install(createClientPlugin("NavidromeAuthHeaders") {
                onRequest { request, _ ->
                    if (request.attributes.getOrNull(isPublicKey) ?: false) return@onRequest
                    request.headers.append("X-ND-Authorization", "Bearer $_token")
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
        }.apply {
            plugin(HttpSend).intercept { request ->
                val originalCall = execute(request)

                if (originalCall.response.status == HttpStatusCode.Unauthorized && !(request.attributes.getOrNull(isPublicKey) ?: false)) {

                    val tokenBeforeRefresh = _token

                    val newToken = authMutex.withLock {
                        if (_token != tokenBeforeRefresh) {
                            _token
                        } else service.authenticate(
                            NavidromeLoginRequest(
                                username = providerData.username,
                                password = providerData.password
                            )
                        ).token.also {
                            _token = it
                        }
                    }

                    request.headers["X-ND-Authorization"] = "Bearer $newToken"

                    return@intercept execute(request)
                }

                originalCall
            }
        }

        Ktorfit.Builder()
            .baseUrl(if (providerData.url.endsWith("/")) providerData.url else providerData.url + "/")
            .httpClient(ktorClient)
            .build()
    }

    private val service: NavidromeService by lazy { ktorfit.createNavidromeService() }

    override suspend fun getAlbumArtistList(query: MediaQuery.AlbumArtistListQuery): List<MediaModel.Artist> {
        return service.getAlbumArtistList(
            end = query.startIndex + (query.limit ?: 0),
            order = query.sortOrder.name,
            start = query.startIndex,
            sort = ALBUM_ARTIST_SORT_BINDING[query.sortBy],
            libraryId = query.musicFolderId ?: data.libraries.filter { it.second }.map { it.first.id },
            name = query.searchTerm,
            role = "albumartist",
            starred = query.favorite,
        ).map { it.toMediaModel(id) }
    }
    override suspend fun getAlbumList(query: MediaQuery.AlbumListQuery): List<MediaModel.Album> {
        return service.getAlbumList(
            end = query.startIndex + (query.limit ?: 0),
            order = query.sortOrder.name,
            start = query.startIndex,
            sort = ALBUM_SORT_BINDING[query.sortBy],
            artistId = query.artistIds,
            compilation = query.compilation,
            genreId = query.genreIds,
            hasRating = query.hasRating,
            libraryId = query.musicFolderId ?: data.libraries.filter { it.second }.map { it.first.id },
            name = query.searchTerm,
            starred = query.favorite,
            year = query.maxYear ?: query.minYear
        ).map { it.toMediaModel(id) }
    }

    override suspend fun getArtistList(query: MediaQuery.ArtistListQuery): List<MediaModel.Artist> {
        return service.getAlbumArtistList(
            end = query.startIndex + (query.limit ?: 0),
            order = query.sortOrder.name,
            start = query.startIndex,
            sort = ARTIST_SORT_BINDING[query.sortBy],
            libraryId = query.musicFolderId ?: data.libraries.filter { it.second }.map { it.first.id },
            name = query.searchTerm,
            starred = query.favorite,
        ).map { it.toMediaModel(id) }
    }

    override suspend fun getSongList(query: MediaQuery.SongListQuery): List<MediaModel.Song> {
        return service.getSongList(
            end = query.startIndex + (query.limit ?: 50),
            order = query.sortOrder.name,
            start = query.startIndex,
            sort = SONG_SORT_BINDING[query.sortBy],
            albumId = query.albumIds,
            genreId = query.genreIds,
            artistsId = query.artistIds,
            hasRating = query.hasRating,
            libraryId = query.musicFolderId ?: data.libraries.filter { it.second }.map { it.first.id },
            starred = query.favorite,
            title = query.searchTerm,
            year = query.maxYear ?: query.minYear,
            missing = false
        ).map { it.toMediaModel(id) }
    }
}