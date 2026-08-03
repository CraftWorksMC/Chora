package com.craftworks.music.providers.navidrome

import com.craftworks.music.R
import com.craftworks.music.data.model.AlbumListSort
import com.craftworks.music.data.model.MediaModel
import com.craftworks.music.data.model.MediaQuery
import com.craftworks.music.providers.subsonic.SubsonicMediaProvider
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
        private val ALBUM_SORT_BINDING =
            mapOf(
                AlbumListSort.ALBUM_ARTIST to "album_artist",
                AlbumListSort.ARTIST to "artist",
                AlbumListSort.DURATION to "duration",
                AlbumListSort.EXPLICIT_STATUS to "explicitStatus",
                AlbumListSort.FAVORITED to "starred_at",
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
    }

    override val providerIcon: Int
        get() = R.drawable.s_m_navidrome
    override val providerName: Int
        get() = R.string.Source_Navidrome

    @Transient
    override val supportedAlbumSort: List<AlbumListSort> = listOf(
        AlbumListSort.ALBUM_ARTIST,
        AlbumListSort.ARTIST,
        AlbumListSort.DURATION,
        AlbumListSort.EXPLICIT_STATUS,
        AlbumListSort.FAVORITED,
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
                    val newToken = service.authenticate(
                        NavidromeLoginRequest(
                            username = providerData.username,
                            password = providerData.password
                        )).token

                    request.headers["X-ND-Authorization"] = "Bearer $newToken"

                    val retriedCall = execute(request)

                    if (retriedCall.response.status != HttpStatusCode.Unauthorized) _token = newToken

                    return@intercept retriedCall
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
}