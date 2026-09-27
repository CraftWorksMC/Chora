package com.craftworks.music.data.providers.media.subsonic

import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Query

interface SubsonicService {

    @GET("rest/ping.view")
    suspend fun ping(): SubsonicResponse

    @GET("rest/createInternetRadioStation.view")
    suspend fun createInternetRadioStation(
        @Query("streamUrl") streamUrl: String,
        @Query("name") name: String,
        @Query("homepageUrl") homepageUrl: String? = null,
    ): SubsonicResponse

    @GET("rest/createPlaylist.view")
    suspend fun createPlaylist(
        @Query("playlistId") playlistId: String? = null,
        @Query("name") name: String? = null,
        @Query("songId") songIds: List<String>? = null,
    ): SubsonicResponse

    @GET("rest/getAlbum.view")
    suspend fun getAlbum(@Query id: String): SubsonicResponse

    @GET("rest/getAlbumList2.view")
    suspend fun getAlbumList(
        @Query("type") type: String,
        @Query("size") size: Int? = 10,
        @Query("offset") offset: Int? = 0,
        @Query("fromYear") fromYear: Int? = null,
        @Query("toYear") toYear: Int? = null,
        @Query("genre") genre: String? = null,
        @Query("musicFolderId") musicFolderId: List<Int>? = null,
    ): SubsonicResponse

    @GET("rest/getArtist.view")
    suspend fun getArtist(
        @Query("id") id: String,
    ): SubsonicResponse

    @GET("rest/getArtistInfo.view")
    suspend fun getArtistInfo(
        @Query("id") id: String,
        @Query("count") count: Int? = 20,
        @Query("includeNotPresent") includeNotPresent: Boolean = false
    ): SubsonicResponse

    @GET("rest/getArtists.view")
    suspend fun getArtists(
        @Query("musicFolderId") musicFolderId: List<Int>? = null,
    ): SubsonicResponse

    @GET("rest/getInternetRadioStations.view")
    suspend fun getInternetRadioStations(): SubsonicResponse

    @GET("rest/getLyricsBySongId.view")
    suspend fun getLyricsBySongId(
        @Query("id") id: String,
        @Query("enhanced") enhanced: Boolean? = null,
    ): SubsonicResponse

    @GET("rest/getUser.view")
    suspend fun authenticate(
        @Query("username") username: String
    ): SubsonicResponse

    @GET("rest/getMusicFolders.view")
    suspend fun getMusicFolderList(): SubsonicResponse

    @GET("rest/getPlaylist.view")
    suspend fun getPlaylist(
        @Query("id") id: String? = null,
    ): SubsonicResponse

    @GET("rest/getPlaylists.view")
    suspend fun getPlaylists(
        @Query("username") username: String? = null,
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

    @GET("rest/scrobble.view")
    suspend fun scrobble(
        @Query("id") id: String,
        @Query("time") time: Int? = 0,
        @Query("submission") submission: Boolean? = true
    )

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

    @GET("rest/updatePlaylist.view")
    suspend fun updatePlaylist(
        @Query("playlistId") playlistId: String,
        @Query("name") name: String? = null,
        @Query("comment") comment: String? = null,
        @Query("public") public: Boolean? = null,
        @Query("songIdToAdd") songIdToAdd: List<String>? = null,
        @Query("songIndexToRemove") songIndexToRemove: List<Int>? = null,
    ): SubsonicResponse

    @GET("rest/deletePlaylist.view")
    suspend fun deletePlaylist(
        @Query("id") id: String,
    ): SubsonicResponse

    @GET("rest/updateInternetRadioStation.view")
    suspend fun updateInternetRadioStation(
        @Query("id") id: String,
        @Query("streamUrl") streamUrl: String,
        @Query("name") name: String,
        @Query("homepageUrl") homepageUrl: String? = null,
    ): SubsonicResponse

    @GET("rest/deleteInternetRadioStation.view")
    suspend fun deleteInternetRadioStation(
        @Query("id") id: String,
    ): SubsonicResponse

    @GET("rest/getSong.view")
    suspend fun getSong(
        @Query("id") id: String,
    ): SubsonicResponse

    @GET("rest/getGenres.view")
    suspend fun getGenres(): SubsonicResponse

    @GET("rest/getRandomSongs.view")
    suspend fun getRandomSongs(
        @Query("size") size: Int? = 10,
        @Query("genre") genre: String? = null,
        @Query("fromYear") fromYear: Int? = null,
        @Query("toYear") toYear: Int? = null,
        @Query("musicFolderId") musicFolderId: List<Int>? = null,
    ): SubsonicResponse

    @GET("rest/getTopSongs.view")
    suspend fun getTopSongs(
        @Query("artist") artist: String,
        @Query("count") count: Int? = 50,
    ): SubsonicResponse

    @GET("rest/getSimilarSongs2.view")
    suspend fun getSimilarSongs2(
        @Query("id") id: String,
        @Query("count") count: Int? = 50,
    ): SubsonicResponse

    @GET("rest/star.view")
    suspend fun star(
        @Query("id") id: List<String>? = null,
        @Query("albumId") albumId: List<String>? = null,
        @Query("artistId") artistId: List<String>? = null,
    ): SubsonicResponse

    @GET("rest/unstar.view")
    suspend fun unstar(
        @Query("id") id: List<String>? = null,
        @Query("albumId") albumId: List<String>? = null,
        @Query("artistId") artistId: List<String>? = null,
    ): SubsonicResponse

    @GET("rest/setRating.view")
    suspend fun setRating(
        @Query("id") id: String,
        @Query("rating") rating: Int,
    ): SubsonicResponse

    @GET("rest/getLyrics.view")
    suspend fun getLyrics(
        @Query("artist") artist: String? = null,
        @Query("title") title: String? = null,
    ): SubsonicResponse

    @GET("rest/getLyricsBySongId.view")
    suspend fun getLyricsBySongId(
        @Query("id") id: String,
    ): SubsonicResponse

    @GET("rest/getAlbumInfo2.view")
    suspend fun getAlbumInfo2(
        @Query("id") id: String,
    ): SubsonicResponse

    @GET("rest/getMusicDirectory.view")
    suspend fun getMusicDirectory(
        @Query("id") id: String,
    ): SubsonicResponse

    @GET("rest/getPlayQueue.view")
    suspend fun getPlayQueue(): SubsonicResponse

    @GET("rest/savePlayQueue.view")
    suspend fun savePlayQueue(
        @Query("id") id: List<String>,
        @Query("current") current: String? = null,
        @Query("position") position: Long? = null,
    ): SubsonicResponse

    @GET("rest/getUser.view")
    suspend fun getUser(
        @Query("username") username: String,
    ): SubsonicResponse

    @GET("rest/getUsers.view")
    suspend fun getUsers(): SubsonicResponse

    @GET("rest/createShare.view")
    suspend fun createShare(
        @Query("id") id: List<String>,
        @Query("description") description: String? = null,
        @Query("expires") expires: Long? = null,
    ): SubsonicResponse
}