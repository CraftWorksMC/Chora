package com.craftworks.music.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.StarRating
import com.craftworks.music.data.repository.PlaylistRepository
import com.craftworks.music.data.repository.SongRepository
import com.craftworks.music.managers.DataRefreshManager
import com.craftworks.music.providers.local.localPlaylistImageGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistScreenViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val songRepository: SongRepository,
) : ViewModel() {
    private val _allPlaylists = MutableStateFlow<List<MediaItem>>(emptyList())
    val allPlaylists: StateFlow<List<MediaItem>> = _allPlaylists.asStateFlow()

    private var _selectedPlaylist = MutableStateFlow<MediaItem?>(null)
    val selectedPlaylist: StateFlow<MediaItem?> = _selectedPlaylist

    private var _selectedPlaylistSongs = MutableStateFlow<List<MediaItem>>(emptyList())
    val selectedPlaylistSongs: StateFlow<List<MediaItem>> = _selectedPlaylistSongs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadPlaylists()

        viewModelScope.launch {
            DataRefreshManager.dataSourceChangedEvent.collect {
                loadPlaylists()
            }
        }
    }

    fun loadPlaylists() {
        viewModelScope.launch {
            _isLoading.value = true
            _allPlaylists.value = playlistRepository.getPlaylists(true)
            _isLoading.value = false
        }
    }

    fun setCurrentPlaylist(playlist: MediaItem){
        _selectedPlaylistSongs.value = emptyList<MediaItem>()
        _selectedPlaylist.value = playlist
        fetchPlaylistDetails() // Fetch details when playlist is set
    }

    suspend fun updatePlaylistsImages(context: Context) {
        _allPlaylists.value = _allPlaylists.value.map {
            if (it.mediaMetadata.extras?.getString("navidromeID")?.startsWith("Local_") == true && it.mediaMetadata.artworkData == null) {
                val songs = playlistRepository.getPlaylistSongs(it.mediaMetadata.extras?.getString("navidromeID") ?: "", true)
                it.buildUpon()
                    .setMediaMetadata(
                        it.mediaMetadata.buildUpon()
                            .setArtworkData(localPlaylistImageGenerator(songs, context), MediaMetadata.PICTURE_TYPE_OTHER)
                            .build()
                    )
                    .build()
            } else {
                it
            }
        }
    }

    fun fetchPlaylistDetails() {
        if (_selectedPlaylist.value == null) return

        val playlistId = _selectedPlaylist.value?.mediaMetadata?.extras?.getString("navidromeID")
        if (playlistId == null) return

        println("Fetching playlist details for playlist ID: $playlistId")

        viewModelScope.launch {
            val loadingJob = launch {
                if (_selectedPlaylistSongs.value.isEmpty()) {
                    _isLoading.value = true
                }
            }
            loadingJob.start()
            coroutineScope {
                _selectedPlaylistSongs.value = async { playlistRepository.getPlaylistSongs(playlistId) }.await()
            }
            loadingJob.cancel()
            _isLoading.value = false
        }
    }

    fun createPlaylist(name: String, songstoAdd: String, addToNavidrome: Boolean, context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            playlistRepository.createPlaylist(name, songstoAdd, addToNavidrome)
            DataRefreshManager.notifyDataSourcesChanged()
            _isLoading.value = false
            updatePlaylistsImages(context)
            loadPlaylists()
        }
    }

    fun addSongToPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            playlistRepository.addSongToPlaylist(playlistId, songId)
            _isLoading.value = false
            loadPlaylists()
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            val index = _selectedPlaylistSongs.value.indexOfFirst { it.mediaMetadata.extras?.getString("navidromeID") == songId }
            playlistRepository.removeSongFromPlaylist(playlistId, index.toString())
            _selectedPlaylistSongs.value = _selectedPlaylistSongs.value.filter { it.mediaMetadata.extras?.getString("navidromeID") != songId }
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            playlistRepository.deletePlaylist(playlistId)
            DataRefreshManager.notifyDataSourcesChanged()
            _isLoading.value = false
            loadPlaylists()
        }
    }

    fun setSongRating(
        songId: String,
        rating: Int,
    ) {
        val song =_selectedPlaylistSongs.value.first {
            it.mediaMetadata.extras?.getString("navidromeID") == songId
        }
        val maxStars = (song.mediaMetadata.userRating as? StarRating)?.maxStars ?: 5

        val updatedSong = song.buildUpon().setMediaMetadata(
            song.mediaMetadata.buildUpon()
                .setUserRating(StarRating(maxStars, rating.toFloat()))
                .build()
        ).build()

        _selectedPlaylistSongs.value = _selectedPlaylistSongs.value.map { item ->
            if (item.mediaId == song.mediaId) updatedSong else item
        }

        viewModelScope.launch {
            songRepository.setSongRating(songId, rating)
        }
    }
}
