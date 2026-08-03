package com.craftworks.music.providers.navidrome

import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Headers
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Query
import de.jensklingenberg.ktorfit.http.Tag

interface NavidromeService {
    @Headers("Content-Type: application/json")
    @POST("auth/login")
    suspend fun authenticate(@Body body: NavidromeLoginRequest, @Tag isPublic: Boolean = true): NavidromeLoginResponse
    @GET("api/album")
    suspend fun getAlbumList(
        @Query("_end") end: Int? = null,
        @Query("_order") order: String? = null,
        @Query("_start") start: Int? = null,
        @Query("_sort") sort: String? = null,
        @Query("album_id") albumId: String? = null,
        @Query("artist_id") artistId: List<String>? = null,
        @Query compilation: Boolean? = null,
        @Query("genre_id") genreId: List<String>? = null,
        @Query("has_rating") hasRating: Boolean? = null,
        @Query id: String? = null,
        @Query("library_id") libraryId : List<String>? = null,
        @Query name: String? = null,
        @Query("recently_added") recentlyAdded: Boolean? = null,
        @Query("recently_played") recentlyPlayed: Boolean? = null,
        @Query starred: Boolean? = null,
        @Query year: Int? = null
    ): List<NavidromeAlbum>
    @GET("api/artist")
    suspend fun getAlbumArtistList(
        @Query("_end") end: Int? = null,
        @Query("_order") order: String? = null,
        @Query("_start") start: Int? = null,
        @Query("_sort") sort: String? = null,
        @Query("genre_id") genreId: List<String>? = null,
        @Query("library_id") libraryId : List<String>? = null,
        @Query missing: Boolean? = null,
        @Query name: String? = null,
        @Query role: String? = null,
        @Query starred: Boolean? = null,
    ): List<NavidromeArtist>
}