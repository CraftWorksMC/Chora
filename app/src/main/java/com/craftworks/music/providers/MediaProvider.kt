package com.craftworks.music.providers

import android.content.Context
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
import com.craftworks.music.data.model.LyricsResponse
import com.craftworks.music.data.model.MediaModel
import com.craftworks.music.data.model.MediaProviderData
import com.craftworks.music.data.model.MediaQuery
import com.craftworks.music.data.model.MusicFolder
import com.craftworks.music.data.model.PlaylistListSort
import com.craftworks.music.data.model.PlaylistRules
import com.craftworks.music.data.model.ProviderFeatures
import com.craftworks.music.data.model.ProviderInfo
import com.craftworks.music.data.model.ScrobbleEvent
import com.craftworks.music.data.model.ScrobbleMediaType
import com.craftworks.music.data.model.SearchResponse
import com.craftworks.music.data.model.SongListSort
import com.craftworks.music.data.model.TagListResponse
import com.craftworks.music.data.model.User
import com.craftworks.music.data.model.UserInfoResponse
import com.craftworks.music.providers.local.LocalMediaProvider
import com.craftworks.music.providers.subsonic.SubsonicMediaProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
abstract class MediaProvider {
    companion object {
        val serializerModule = SerializersModule {
            polymorphic(MediaProvider::class) {
                subclass(SubsonicMediaProvider::class)
                subclass(LocalMediaProvider::class)
            }
        }
    }
    lateinit var data: MediaProviderData
    abstract val featureFlags: ProviderFeatures
    abstract val supportedAlbumSort: List<AlbumListSort>
    abstract val supportedAlbumArtistSort: List<AlbumArtistListSort>
    abstract val supportedArtistSort: List<ArtistListSort>
    abstract val supportedGenreSort: List<GenreListSort>
    abstract val supportedPlaylistSort: List<PlaylistListSort>
    abstract val supportedSongSort: List<SongListSort>

    abstract fun init(context: Context)
    abstract suspend fun addToPlaylist(playlistId: String, songIds: List<String>) : Boolean
    abstract suspend fun authenticate(username: String, password: String) : AuthenticationResponse
    abstract suspend fun createFavorite(ids: List<String>, type: LibraryType) : Boolean
    abstract suspend fun createInternetRadioStation(name: String, streamUrl: String, homepageUrl: String?) : Boolean
    abstract suspend fun createPlaylist(name: String, comment: String = "", ownerId: String = "", public: Boolean = false, queryBuilderRules: PlaylistRules? = null, sync: Boolean = false) : String?
    abstract suspend fun deleteFavorite(ids: List<String>, type: LibraryType) : Boolean
    abstract suspend fun deleteInternetRadioStation(id: String): Boolean
    abstract suspend fun deletePlaylist(id: String): Boolean
    abstract suspend fun getAlbumArtistDetail(id: String): MediaModel.Artist?
    abstract suspend fun getAlbumArtistInfo(id: String, limit: Int? = null): AlbumArtistInfo?
    abstract suspend fun getAlbumArtistList(query: MediaQuery.AlbumArtistListQuery): List<MediaModel.Artist>
    abstract suspend fun getAlbumArtistListCount(query: MediaQuery.AlbumArtistListQuery): Int
    abstract suspend fun getAlbumDetail(id: String): MediaModel.Album
    abstract suspend fun getAlbumInfo(id: String): AlbumInfo
    abstract suspend fun getAlbumList(query: MediaQuery.AlbumListQuery): List<MediaModel.Album>
    abstract suspend fun getAlbumListCount(query: MediaQuery.AlbumListQuery): Int
    abstract suspend fun getAlbumRadio(albumId: String, count: Int? = null): List<MediaModel.Song>
    abstract suspend fun getArtistList(query: MediaQuery.ArtistListQuery): List<MediaModel.Artist>
    abstract suspend fun getArtistListCount(query: MediaQuery.ArtistListQuery): Int
    abstract suspend fun getArtistRadio(artistId: String, count: Int? = null): List<MediaModel.Song>
    abstract suspend fun getDownloadUrl(id: String): String
    abstract suspend fun getFolder(query: MediaQuery.FolderQuery): MediaModel.Folder
    abstract suspend fun getGenreList(query: MediaQuery.GenreListQuery): List<MediaModel.Genre>
    abstract suspend fun getImageRequest(id: String, itemType: LibraryType, size: Int? = null, baseUrl: String? = null): ImageRequest?
    abstract fun getImageUrl(id: String, itemType: LibraryType? = null, size: Int? = null): String
    abstract suspend fun getInternetRadioStations(): List<MediaModel.InternetRadioStation>
    abstract suspend fun getLyrics(songId: String): LyricsResponse
    abstract suspend fun getMusicFolderList(): List<MusicFolder>
    abstract suspend fun getPlaylistDetail(id: String): MediaModel.Playlist
    abstract suspend fun getPlaylistList(query: MediaQuery.PlaylistListQuery): List<MediaModel.Playlist>
    abstract suspend fun getPlaylistListCount(query: MediaQuery.PlaylistListQuery): Int
    abstract suspend fun getPlaylistSongList(id: String): List<MediaModel.Song>
    abstract suspend fun getPlayQueue(): GetQueueResponse
    abstract suspend fun getRandomSongList(query: MediaQuery.RandomSongListQuery): List<MediaModel.Song>
    abstract suspend fun getRoles(): List<String>
    abstract suspend fun getProviderInfo(): ProviderInfo
    abstract suspend fun getSimilarSongs(songId: String, count: Int? = null, musicFolderId: List<String>? = null): List<MediaModel.Song>
    abstract suspend fun getSongDetail(id: String): MediaModel.Song
    abstract suspend fun getSongList(query: MediaQuery.SongListQuery): List<MediaModel.Song>
    abstract suspend fun getSongListCount(query: MediaQuery.SongListQuery): Int
    abstract fun getStreamUrl(id: String, transcode: Boolean, bitrate: Int? = null, format: String? = null, mediaType: String? = null, offset: Int? = null, skipAutoTranscode: Boolean? = null): String
    abstract suspend fun getTagList(type: LibraryType, folder: String? = null, tagName: String? = null): TagListResponse
    abstract suspend fun getTopSongs(artist: String, artistId: String, limit: Int? = null, type: String? = null): List<MediaModel.Song>
    abstract suspend fun getUserInfo(id: String, username: String): UserInfoResponse
    abstract suspend fun getUserList(query: MediaQuery.UserListQuery): List<User>
    abstract suspend fun movePlaylistItem(playlistId: String, trackId: String, startingIndex: Int, endingIndex: Int)
    abstract suspend fun ping() : Boolean
    abstract suspend fun removeFromPlaylist(id: String, songIds: List<String>): Boolean
    abstract suspend fun replacePlaylist(id: String, songIds: List<String>): Boolean
    abstract suspend fun savePlayQueue(songs: List<String>, currentIndex: Int? = null, positionMs: Int? = null)
    abstract suspend fun scrobble(id: String, mediaType: ScrobbleMediaType = ScrobbleMediaType.SONG, playbackRate: Float, submission: Boolean, albumId: String? = null, event: ScrobbleEvent?, position: Int? = null)
    abstract suspend fun search(query: MediaQuery.SearchQuery): SearchResponse
    abstract suspend fun setPlaylistSongs(id: String, songIds: List<String>)
    abstract suspend fun setRating(ids: List<String>, rating: Int, type: LibraryType): Boolean
    abstract suspend fun shareItem(description: String, downloadable: Boolean, expires: Long, resourceIds: String, resourceType: String): String?
    abstract suspend fun updateInternetRadioStation(id: String, name: String, streamUrl: String, homepageUrl: String? = null): Boolean
    abstract suspend fun updatePlaylist(id: String, name: String, comment: String? = null, ownerId: String? = null, public: Boolean? = null, queryBuilderRules: PlaylistRules? = null, sync: Boolean? = null): Boolean
}