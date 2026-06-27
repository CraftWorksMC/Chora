package com.craftworks.music.providers

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
import com.craftworks.music.providers.navidrome.MusicFolder
import kotlinx.coroutines.flow.StateFlow

interface MediaProvider {
    val featureFlags: StateFlow<ProviderFeatures>

    suspend fun addToPlaylist(songIds: List<String>, playlistId: String) : Boolean
    suspend fun authenticate(username: String, password: String) : AuthenticationResponse
    suspend fun createFavorite(ids: List<String>, type: LibraryType) : Boolean
    suspend fun createInternetRadioStation(homepageUrl: String?, name: String, streamUrl: String) : Boolean
    suspend fun createPlaylist(name: String, comment: String = "", ownerId: String = "", public: Boolean = false, queryBuilderRules: PlaylistRules?, sync: Boolean = false) : String?
    suspend fun deleteFavorite(ids: List<String>, type: LibraryType) : Boolean
    suspend fun deleteInternetRadioStation(id: String): Boolean
    suspend fun deletePlaylist(id: String): Boolean
    suspend fun getAlbumArtistDetail(id: String): MediaItem.AlbumArtist?
    suspend fun getAlbumArtistInfo(id: String, limit: Int? = null): AlbumArtistInfoResponse?
    suspend fun getAlbumArtistList(query: MediaQuery.AlbumArtistListQuery): List<MediaItem.AlbumArtist>
    suspend fun getAlbumArtistListCount(query: MediaQuery.AlbumArtistListQuery): Int
    suspend fun getAlbumDetail(id: String): MediaItem.Album
    suspend fun getAlbumInfo(id: String): AlbumInfo
    suspend fun getAlbumList(query: MediaQuery.AlbumListQuery): List<MediaItem.Album>
    suspend fun getAlbumListCount(query: MediaQuery.AlbumListQuery): Int
    suspend fun getAlbumRadio(albumId: String, count: Int? = null): List<MediaItem.Song>
    suspend fun getArtistList(query: MediaQuery.ArtistListQuery): List<MediaItem.AlbumArtist>
    suspend fun getArtistListCount(query: MediaQuery.ArtistListQuery): Int
    suspend fun getArtistRadio(artistId: String, count: Int? = null): List<MediaItem.Song>
    suspend fun getDownloadUrl(id: String): String
    suspend fun getFolder(query: MediaQuery.FolderQuery): MediaItem.Folder
    suspend fun getGenreList(query: MediaQuery.GenreListQuery): List<MediaItem.Genre>
    suspend fun getImageRequest(id: String, itemType: LibraryType, size: Int? = null, baseUrl: String? = null): ImageRequest?
    suspend fun getImageUrl(id: String, itemType: LibraryType, size: Int? = null, baseUrl: String? = null): String?
    suspend fun getInternetRadioStations(): List<InternetRadioStation>
    suspend fun getLyrics(songId: String): LyricsResponse
    suspend fun getMusicFolderList(): List<MusicFolder>
    suspend fun getPlaylistDetail(id: String): MediaItem.Playlist
    suspend fun getPlaylistList(query: MediaQuery.PlaylistListQuery): List<MediaItem.Playlist>
    suspend fun getPlaylistListCount(query: MediaQuery.PlaylistListQuery): Int
    suspend fun getPlaylistSongList(id: String): List<MediaItem.Song>
    suspend fun getPlayQueue(): GetQueueResponse
    suspend fun getRandomSongList(query: MediaQuery.RandomSongListQuery): List<MediaItem.Song>
    suspend fun getRoles(): List<String>
    suspend fun getProviderInfo(): ProviderInfo
    suspend fun getSimilarSongs(songId: String, count: Int? = null, musicFolderId: List<String>? = null): List<MediaItem.Song>
    suspend fun getSongDetail(id: String): MediaItem.Song
    suspend fun getSongList(query: MediaQuery.SongListQuery): List<MediaItem.Song>
    suspend fun getSongListCount(query: MediaQuery.SongListQuery): Int
    suspend fun getStreamUrl(id: String, transcode: Boolean, bitrate: Int? = null, format: String? = null, mediaType: String? = null, offset: Int? = null, skipAutoTranscode: Boolean? = null): String
    suspend fun getTagList(type: LibraryType, folder: String? = null, tagName: String? = null): TagListResponse
    suspend fun getTopSongs(artist: String, artistId: String, limit: Int? = null, type: String? = null): List<MediaItem.Song>
    suspend fun getUserInfo(id: String, username: String): UserInfoResponse
    suspend fun getUserList(query: MediaQuery.UserListQuery): List<User>
    suspend fun movePlaylistItem(playlistId: String, trackId: String, startingIndex: Int, endingIndex: Int)
    suspend fun ping() : Boolean
    suspend fun removeFromPlaylist(id: String, songIds: List<String>): Boolean
    suspend fun replacePlaylist(id: String, songIds: List<String>): Boolean
    suspend fun savePlayQueue(songs: List<String>, currentIndex: Int? = null, positionMs: Int? = null)
    suspend fun scrobble(id: String, mediaType: String, playbackRate: Float, submission: Boolean, albumId: String? = null, event: String? = null, position: Int? = null)
    suspend fun search(query: MediaQuery.SearchQuery): SearchResponse
    suspend fun setPlaylistSongs(id: String, songIds: List<String>)
    suspend fun setRating(ids: List<String>, rating: Int, type: LibraryType): Boolean
    suspend fun shareItem(description: String, downloadable: Boolean, expires: Long, resourceIds: String, resourceType: String): String?
    suspend fun updateInternetRadioStation(id: String, name: String, streamUrl: String, homepageUrl: String? = null): Boolean
    suspend fun updatePlaylist(id: String, name: String, comment: String? = null, ownerId: String? = null, public: Boolean? = null, queryBuilderRules: PlaylistRules? = null, sync: Boolean? = null): Boolean
}