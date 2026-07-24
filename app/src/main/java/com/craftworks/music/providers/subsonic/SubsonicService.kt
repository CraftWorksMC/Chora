package com.craftworks.music.providers.subsonic

import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Query

interface SubsonicService {
    @GET("rest/getUser.view")
    suspend fun authenticate(
        @Query("username") username: String
    ): SubsonicResponse

    @GET("rest/getMusicFolders.view")
    suspend fun getMusicFolderList(): SubsonicResponse

    @GET("rest/getAlbum.view")
    suspend fun getAlbum(@Query id: String): SubsonicResponse

    @GET("rest/getAlbumList.view")
    suspend fun getAlbumList(
        @Query("type") type: String,
        @Query("size") size: Int? = 10,
        @Query("offset") offset: Int? = 0,
        @Query("fromYear") fromYear: Int? = null,
        @Query("toYear") toYear: Int? = null,
        @Query("genre") genre: String? = null,
        @Query("musicFolderId") musicFolderId: List<Int>? = null,
    ): SubsonicResponse

    @GET("rest/getSongsByGenre.view")
    suspend fun getSongsByGenre(
        @Query("genre") genre: String,
        @Query("count") count: Int? = 10,
        @Query("offset") offset: Int? = 0,
        @Query("musicFolderId") musicFolderId: List<Int>? = null,
    ): SubsonicResponse

    @GET("rest/getStarred.view")
    suspend fun getStarred(
        @Query("musicFolderId") musicFolderId: List<Int>? = null,
    ): SubsonicResponse

    @GET("rest/search3.view")
    suspend fun search3(
        @Query("query") query: String,
        @Query("artistCount") artistCount: Int = 20,
        @Query("artistOffset") artistOffset: Int = 0,
        @Query("albumCount") albumCount: Int = 20,
        @Query("albumOffset") albumOffset: Int = 0,
        @Query("songCount") songCount: Int = 20,
        @Query("songOffset") songOffset: Int = 0,
        @Query("musicFolderId") musicFolderId: List<Int>? = null,
    ): SubsonicResponse

    @GET("rest/scrobble.view")
    suspend fun scrobble(
        @Query("id") id: String,
        @Query("time") time: Int? = 0,
        @Query("submission") submission: Boolean? = true
    )
}