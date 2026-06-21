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

    fun addToPlaylist(songIds: List<String>, playlistId: String) : Boolean
    fun authenticate(username: String, password: String) : AuthenticationResponse
    fun createFavorite(ids: List<String>, type: LibraryType) : Boolean
    fun createInternetRadioStation(homepageUrl: String?, name: String, streamUrl: String) : Boolean
    fun createPlaylist(name: String, comment: String = "", ownerId: String = "", public: Boolean = false, queryBuilderRules: PlaylistRules?, sync: Boolean = false) : String?
    fun deleteFavorite(ids: List<String>, type: LibraryType) : Boolean
    fun deleteInternetRadioStation(id: String): Boolean
    fun deletePlaylist(id: String): Boolean
    fun getAlbumArtistDetail(id: String): MediaItem.AlbumArtist?
    fun getAlbumArtistInfo(id: String, limit: Int? = null): AlbumArtistInfoResponse?
    fun getAlbumArtistList(query: MediaQuery.AlbumArtistListQuery): List<MediaItem.AlbumArtist>
    fun getAlbumArtistListCount(query: MediaQuery.AlbumArtistListQuery): Int
    fun getAlbumDetail(id: String): MediaItem.Album
    fun getAlbumInfo(id: String): AlbumInfo
    fun getAlbumList(query: MediaQuery.AlbumListQuery): List<MediaItem.Album>
    fun getAlbumListCount(query: MediaQuery.AlbumListQuery): Int
    fun getAlbumRadio(albumId: String, count: Int? = null): List<MediaItem.Song>
    fun getArtistList(query: MediaQuery.ArtistListQuery): List<MediaItem.AlbumArtist>
    fun getArtistListCount(query: MediaQuery.ArtistListQuery): Int
    fun getArtistRadio(artistId: String, count: Int? = null): List<MediaItem.Song>
    fun getDownloadUrl(id: String): String
    fun getFolder(query: MediaQuery.FolderQuery): MediaItem.Folder
    fun getGenreList(query: MediaQuery.GenreListQuery): List<MediaItem.Genre>
    fun getImageRequest(id: String, itemType: LibraryType, size: Int? = null, baseUrl: String? = null): ImageRequest?
    fun getImageUrl(id: String, itemType: LibraryType, size: Int? = null, baseUrl: String? = null): String?
    fun getInternetRadioStations(): List<InternetRadioStation>
    fun getLyrics(songId: String): LyricsResponse
    fun getMusicFolderList(): List<MusicFolder>
    fun getPlaylistDetail(id: String): MediaItem.Playlist
    fun getPlaylistList(query: MediaQuery.PlaylistListQuery): List<MediaItem.Playlist>
    fun getPlaylistListCount(query: MediaQuery.PlaylistListQuery): Int
    fun getPlaylistSongList(id: String): List<MediaItem.Song>
    fun getPlayQueue(): GetQueueResponse
    fun getRandomSongList(query: MediaQuery.RandomSongListQuery): List<MediaItem.Song>
    fun getRoles(): List<String>
    fun getProviderInfo(): ProviderInfo
    fun getSimilarSongs(songId: String, count: Int? = null, musicFolderId: List<String>? = null): List<MediaItem.Song>
    fun getSongDetail(id: String): MediaItem.Song
    fun getSongList(query: MediaQuery.SongListQuery): List<MediaItem.Song>
    fun getSongListCount(query: MediaQuery.SongListQuery): Int
    fun getStreamUrl(id: String, transcode: Boolean, bitrate: Int? = null, format: String? = null, mediaType: String? = null, offset: Int? = null, skipAutoTranscode: Boolean? = null): String
    fun getTagList(type: LibraryType, folder: String? = null, tagName: String? = null): TagListResponse
    fun getTopSongs(artist: String, artistId: String, limit: Int? = null, type: String? = null): List<MediaItem.Song>
    fun getUserInfo(id: String, username: String): UserInfoResponse
    fun getUserList(query: MediaQuery.UserListQuery): List<User>
    fun movePlaylistItem(playlistId: String, trackId: String, startingIndex: Int, endingIndex: Int)
    fun ping() : Boolean
    fun removeFromPlaylist(id: String, songIds: List<String>): Boolean
    fun replacePlaylist(id: String, songIds: List<String>): Boolean
    fun savePlayQueue(songs: List<String>, currentIndex: Int? = null, positionMs: Int? = null)
    fun scrobble(id: String, mediaType: String, playbackRate: Float, submission: Boolean, albumId: String? = null, event: String? = null, position: Int? = null)
    fun search(query: MediaQuery.SearchQuery): SearchResponse
    fun setPlaylistSongs(id: String, songIds: List<String>)
    fun setRating(ids: List<String>, rating: Int, type: LibraryType): Boolean
    fun shareItem(description: String, downloadable: Boolean, expires: Long, resourceIds: String, resourceType: String): String?
    fun updateInternetRadioStation(id: String, name: String, streamUrl: String, homepageUrl: String? = null): Boolean
    fun updatePlaylist(id: String, name: String, comment: String? = null, ownerId: String? = null, public: Boolean? = null, queryBuilderRules: PlaylistRules? = null, sync: Boolean? = null): Boolean
}