package com.craftworks.music.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.craftworks.music.data.model.MediaQuery
import com.craftworks.music.data.model.PlaylistListSort
import com.craftworks.music.data.model.SortOrder
import com.craftworks.music.data.repository.PlaylistRepository
import com.craftworks.music.data.repository.StarredRepository
import com.craftworks.music.managers.DataRefreshManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistScreenViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository
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
            _allPlaylists.value = playlistRepository.getPlaylists(
                MediaQuery.PlaylistListQuery(
                    PlaylistListSort.NAME,
                    SortOrder.ASC,
                    startIndex = 0
                )
            )
            _isLoading.value = false
        }
    }

    fun setCurrentPlaylist(playlist: MediaItem){
        _selectedPlaylistSongs.value = emptyList<MediaItem>()
        _selectedPlaylist.value = playlist
        fetchPlaylistDetails() // Fetch details when playlist is set
    }

    fun fetchPlaylistDetails() {
        if (_selectedPlaylist.value == null) return

        val playlistId = _selectedPlaylist.value?.mediaMetadata?.extras?.getString("id") ?: return

        println("Fetching playlist details for playlist ID: $playlistId")

        viewModelScope.launch {
            val loadingJob = launch {
                delay(1000)
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

    fun createPlaylist(name: String, songstoAdd: List<String>, context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            playlistRepository.createPlaylist(name, "", songstoAdd, false)
            DataRefreshManager.notifyDataSourcesChanged()
            _isLoading.value = false
            loadPlaylists()
        }
    }

    fun addSongsToPlaylist(playlistId: String, songIds: List<String>) {
        viewModelScope.launch {
            _isLoading.value = true
            playlistRepository.addSongsToPlaylist(playlistId, songIds)
            _isLoading.value = false
            loadPlaylists()
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
}
