package com.craftworks.music.providers.local

import android.content.Context
import android.provider.MediaStore
import android.util.Log
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
import com.craftworks.music.providers.MediaProvider

class LocalMediaProvider(var providerData: LocalProviderData) : MediaProvider() {
    companion object {
        const val TAG = "LOCAL_PROVIDER"
        const val ALBUM_ART_PATH = "content://media/external/audio/albumart"

        private val ALBUM_SORT_BINDING = mapOf(
            AlbumListSort.NAME to "alphabeticalByName %f",
            AlbumListSort.RANDOM to "RANDOM()"
        )
    }

    override val featureFlags: ProviderFeatures =
        ProviderFeatures.OFFLINE_PLAYBACK +
        ProviderFeatures.SELECT_MULTIPLE_MUSIC_FOLDERS

    override val supportedAlbumSort: List<AlbumListSort> = listOf(
        AlbumListSort.NAME,
    )
    override val supportedAlbumArtistSort: List<AlbumArtistListSort> = listOf(
        AlbumArtistListSort.NAME,
    )
    override val supportedArtistSort: List<ArtistListSort> = listOf(
        ArtistListSort.NAME,
    )
    override val supportedGenreSort: List<GenreListSort> = listOf(GenreListSort.NAME)
    override val supportedPlaylistSort: List<PlaylistListSort> = listOf(PlaylistListSort.NAME)
    override val supportedSongSort: List<SongListSort> = listOf(SongListSort.NAME)

    private lateinit var appContext: Context
    override fun init(context: Context) {
        appContext = context
    }

    override suspend fun addToPlaylist(
        playlistId: String,
        songIds: List<String>
    ): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun authenticate(
        username: String,
        password: String
    ): AuthenticationResponse {
        return AuthenticationResponse(credential = password, username = username)
    }

    override suspend fun createFavorite(
        ids: List<String>,
        type: LibraryType
    ): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun createInternetRadioStation(
        name: String,
        streamUrl: String,
        homepageUrl: String?
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

    override suspend fun getAlbumArtistDetail(id: String): MediaModel.AlbumArtist? {
        TODO("Not yet implemented")
    }

    override suspend fun getAlbumArtistInfo(
        id: String,
        limit: Int?
    ): AlbumArtistInfo? {
        TODO("Not yet implemented")
    }

    override suspend fun getAlbumArtistList(query: MediaQuery.AlbumArtistListQuery): List<MediaModel.AlbumArtist> {
        TODO("Not yet implemented")
    }

    override suspend fun getAlbumArtistListCount(query: MediaQuery.AlbumArtistListQuery): Int {
        TODO("Not yet implemented")
    }

    override suspend fun getAlbumDetail(id: String): MediaModel.Album {
        TODO("Not yet implemented")
    }

    override suspend fun getAlbumInfo(id: String): AlbumInfo {
        TODO("Not yet implemented")
    }

    override suspend fun getAlbumList(query: MediaQuery.AlbumListQuery): List<MediaModel.Album> {
        Log.d(TAG, "Getting All Albums")

        val sortOrder = ALBUM_SORT_BINDING[query.sortBy]?.format(query.sortOrder.name)
            ?: "alphabeticalByName ASC"

        val contentResolver = appContext.contentResolver
        val uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST
        )

        // Get all folders first
        val folders = data.libraries.filter { it.second }.map { it.first.name }
        val albumIdsInFolders = if (folders.isNotEmpty()) {
            LocalUtils.getAlbumIdsInFolders(appContext, folders)
        } else {
            emptySet()
        }

        val selection = if (albumIdsInFolders.isNotEmpty()) {
            "${MediaStore.Audio.Albums._ID} IN (${albumIdsInFolders.joinToString(",")})"
        } else {
            null
        }

        val cursor = contentResolver.query(
            uri,
            projection,
            selection,
            null,
            sortOrder
        )
        if (cursor == null) return emptyList()

        return LocalNormalizer.cursorToAlbums(appContext, cursor)
    }

    override suspend fun getAlbumListCount(query: MediaQuery.AlbumListQuery): Int {
        TODO("Not yet implemented")
    }

    override suspend fun getAlbumRadio(
        albumId: String,
        count: Int?
    ): List<MediaModel.Song> {
        TODO("Not yet implemented")
    }

    override suspend fun getArtistList(query: MediaQuery.ArtistListQuery): List<MediaModel.Artist> {
        TODO("Not yet implemented")
    }

    override suspend fun getArtistListCount(query: MediaQuery.ArtistListQuery): Int {
        TODO("Not yet implemented")
    }

    override suspend fun getArtistRadio(
        artistId: String,
        count: Int?
    ): List<MediaModel.Song> {
        TODO("Not yet implemented")
    }

    override suspend fun getDownloadUrl(id: String): String {
        TODO("Not yet implemented")
    }

    override suspend fun getFolder(query: MediaQuery.FolderQuery): MediaModel.Folder {
        TODO("Not yet implemented")
    }

    override suspend fun getGenreList(query: MediaQuery.GenreListQuery): List<MediaModel.Genre> {
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

    override fun getImageUrl(
        id: String,
        itemType: LibraryType,
        size: Int?,
        baseUrl: String?
    ): String {
        TODO("Not yet implemented")
    }

    override suspend fun getInternetRadioStations(): List<MediaModel.InternetRadioStation> {
        TODO("Not yet implemented")
    }

    override suspend fun getLyrics(songId: String): LyricsResponse {
        TODO("Not yet implemented")
    }

    override suspend fun getMusicFolderList(): List<MusicFolder> {
        TODO("Not yet implemented")
    }

    override suspend fun getPlaylistDetail(id: String): MediaModel.Playlist {
        TODO("Not yet implemented")
    }

    override suspend fun getPlaylistList(query: MediaQuery.PlaylistListQuery): List<MediaModel.Playlist> {
        TODO("Not yet implemented")
    }

    override suspend fun getPlaylistListCount(query: MediaQuery.PlaylistListQuery): Int {
        TODO("Not yet implemented")
    }

    override suspend fun getPlaylistSongList(id: String): List<MediaModel.Song> {
        TODO("Not yet implemented")
    }

    override suspend fun getPlayQueue(): GetQueueResponse {
        TODO("Not yet implemented")
    }

    override suspend fun getRandomSongList(query: MediaQuery.RandomSongListQuery): List<MediaModel.Song> {
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
    ): List<MediaModel.Song> {
        TODO("Not yet implemented")
    }

    override suspend fun getSongDetail(id: String): MediaModel.Song {
        TODO("Not yet implemented")
    }

    override suspend fun getSongList(query: MediaQuery.SongListQuery): List<MediaModel.Song> {
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
    ): List<MediaModel.Song> {
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
        mediaType: ScrobbleMediaType,
        playbackRate: Float,
        submission: Boolean,
        albumId: String?,
        event: ScrobbleEvent?,
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