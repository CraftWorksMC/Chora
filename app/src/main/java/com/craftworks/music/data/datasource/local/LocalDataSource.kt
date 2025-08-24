package com.craftworks.music.data.datasource.local

import androidx.media3.common.MediaItem
import com.craftworks.music.data.model.MediaData
import com.craftworks.music.data.model.toMediaItem
import com.craftworks.music.data.model.toSong
import com.craftworks.music.managers.SettingsManager
import com.craftworks.music.providers.local.LocalProvider
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalDataSource @Inject constructor(
    private val localProvider: LocalProvider,
    private val settingsManager: SettingsManager,
) {

    suspend fun getLocalAlbums(sort: String?): List<MediaItem> {
        return localProvider.getLocalAlbums(sort)
    }

    suspend fun searchLocalAlbums(query: String): List<MediaItem> {
        if (query.isBlank()) return emptyList()
        return localProvider.getLocalAlbums().filter {
            it.mediaMetadata.title?.contains(query, ignoreCase = true) == true ||
                    it.mediaMetadata.artist?.contains(query, ignoreCase = true) == true ||
                    it.mediaMetadata.albumTitle?.contains(query, ignoreCase = true) == true
        }
    }

    fun getLocalAlbum(albumId: String): List<MediaItem>? {
        return localProvider.getLocalAlbum(albumId)
    }

    fun getLocalSongs(): List<MediaItem> {
        return localProvider.getLocalSongs()
    }

    suspend fun searchLocalSongs(query: String): List<MediaItem> {
        if (query.isBlank()) return getLocalSongs()
        return localProvider.getLocalSongs().filter {
            it.mediaMetadata.title?.contains(query, ignoreCase = true) == true ||
                    it.mediaMetadata.artist?.contains(query, ignoreCase = true) == true ||
                    it.mediaMetadata.albumTitle?.contains(query, ignoreCase = true) == true
        }
    }

    fun getLocalSong(songId: String): MediaItem? {
        return localProvider.getLocalSongs().find { it.mediaMetadata.extras?.getString("navidromeID") == songId }
    }

    fun getLocalArtists(): List<MediaData.Artist> {
        return localProvider.getLocalArtists()
    }

    suspend fun getLocalArtistAlbums(artist: String): List<MediaItem> {
        return localProvider.getAlbumsByArtistId(artist)
    }

    fun searchLocalArtists(query: String): List<MediaData.Artist> {
        if (query.isBlank()) return getLocalArtists()
        return localProvider.getLocalArtists().filter {
            it.name.contains(query, ignoreCase = true)
        }
    }

    // Playlists
    suspend fun getLocalPlaylists(): List<MediaItem> {
        return settingsManager.localPlaylists.first().map { it.toMediaItem() }
    }

    suspend fun getLocalPlaylistSongs(playlistId: String): List<MediaItem> {
        val playlist = settingsManager.localPlaylists.first().find { it.navidromeID == playlistId }
        println("Found playlist: $playlist")
        return playlist?.songs?.map { it.toMediaItem() } ?: emptyList()
    }

    suspend fun createLocalPlaylist(
        playlistName: String,
        initialSongId: String?
    ): Boolean {
        val initialSong = getLocalSong(initialSongId ?: "")?.toSong()

        val newPlaylist = MediaData.Playlist(
            navidromeID = "Local_${UUID.randomUUID()}",
            name = playlistName,
            comment = "",
            owner = settingsManager.usernameFlow.first(),
            public = false,
            songCount = if (initialSong == null) 0 else 1,
            coverArt = initialSong?.imageUrl,
            duration = initialSong?.duration ?: 0,
            created = "",
            changed = "",
            songs = if (initialSong != null) mutableListOf(initialSong) else mutableListOf()
        )

        val currentPlaylistsFromStore = settingsManager.localPlaylists.first().toMutableList()
        currentPlaylistsFromStore.add(newPlaylist)

        settingsManager.saveLocalPlaylists(currentPlaylistsFromStore)
        return true
    }

    suspend fun addSongToLocalPlaylist(
        playlistId: String,
        songId: String
    ): Boolean {
        val song = getLocalSong(songId)?.toSong() ?: return false
        val currentPlaylistsFromStore = settingsManager.localPlaylists.first().toMutableList()
        val playlistIndex = currentPlaylistsFromStore.indexOfFirst { it.navidromeID == playlistId }
        if (playlistIndex == -1) return false

        println("Found playlist to modify: ${currentPlaylistsFromStore[playlistIndex]}")

        val playlistToModify = currentPlaylistsFromStore[playlistIndex]

        val updatedSongs = (playlistToModify.songs ?: emptyList()).toMutableList()

        if (updatedSongs.any { it.navidromeID == song.navidromeID })
            return false

        updatedSongs.add(song)

        val updatedPlaylist = playlistToModify.copy(songs = updatedSongs)
        currentPlaylistsFromStore[playlistIndex] = updatedPlaylist

        println("modified playlist: ${currentPlaylistsFromStore[playlistIndex]}")

        settingsManager.saveLocalPlaylists(currentPlaylistsFromStore)
        return true
    }

    suspend fun removeSongFromLocalPlaylist(
        playlistId: String,
        songId: String
    ): Boolean {
        val currentPlaylistsFromStore = settingsManager.localPlaylists.first().toMutableList()
        val playlistToModify = currentPlaylistsFromStore.find { it.navidromeID == playlistId }
            ?: return false

        val songRemoved = playlistToModify.songs?.removeAll { it.navidromeID == songId }

        if (songRemoved == true) {
            settingsManager.saveLocalPlaylists(currentPlaylistsFromStore)
            return true
        }
        return false
    }

    suspend fun deleteLocalPlaylist(playlistId: String): Boolean {
        val currentPlaylistsFromStore = settingsManager.localPlaylists.first().toMutableList()
        val removed = currentPlaylistsFromStore.removeAll { it.navidromeID == playlistId }

        if (removed) {
            settingsManager.saveLocalPlaylists(currentPlaylistsFromStore)
        }
        return removed
    }

    // Radios
    suspend fun getLocalRadios(): List<MediaData.Radio> {
        return settingsManager.localRadios.first()
    }

    suspend fun createLocalRadio(
        name: String,
        url: String,
        homePageUrl: String?
    ): Boolean {
        val newRadio = MediaData.Radio(
            name = name,
            media = url,
            homePageUrl = homePageUrl ?: "",
            navidromeID = "Local_${UUID.randomUUID()}"
        )
        val currentRadiosFromStore = settingsManager.localRadios.first().toMutableList()
        currentRadiosFromStore.add(newRadio)

        settingsManager.saveLocalRadios(currentRadiosFromStore)
        return true
    }

    suspend fun updateLocalRadio(radio: MediaData.Radio): Boolean {
        val currentRadiosFromStore = settingsManager.localRadios.first().toMutableList()
        val index = currentRadiosFromStore.indexOfFirst { it.navidromeID == radio.navidromeID }
        if (index != -1) {
            currentRadiosFromStore[index] = radio

            settingsManager.saveLocalRadios(currentRadiosFromStore)
            return true
        }
        return false
    }

    suspend fun deleteLocalRadio(id: String): Boolean {
        val currentRadiosFromStore = settingsManager.localRadios.first().toMutableList()
        val removed = currentRadiosFromStore.removeAll { it.navidromeID == id }
        if (removed) {
            settingsManager.saveLocalRadios(currentRadiosFromStore)
        }
        return removed
    }

    //TODO: Local Starred Items with DB
    fun getLocalStarredItems(): List<MediaItem> {
        return emptyList()
    }

    fun starLocalItem(itemId: String): Boolean {
        return false
    }

    fun unstarLocalItem(itemId: String): Boolean {
        return false
    }
}
