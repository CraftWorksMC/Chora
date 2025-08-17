package com.craftworks.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.craftworks.music.data.repository.AlbumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val albumRepository: AlbumRepository
) : ViewModel() {
    private val _recentlyPlayedAlbums = MutableStateFlow<List<MediaItem>>(emptyList())
    val recentlyPlayedAlbums: StateFlow<List<MediaItem>> = _recentlyPlayedAlbums.asStateFlow()

    private val _recentAlbums = MutableStateFlow<List<MediaItem>>(emptyList())
    val recentAlbums: StateFlow<List<MediaItem>> = _recentAlbums.asStateFlow()

    private val _mostPlayedAlbums = MutableStateFlow<List<MediaItem>>(emptyList())
    val mostPlayedAlbums: StateFlow<List<MediaItem>> = _mostPlayedAlbums.asStateFlow()

    private val _shuffledAlbums = MutableStateFlow<List<MediaItem>>(emptyList())
    val shuffledAlbums: StateFlow<List<MediaItem>> = _shuffledAlbums.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadHomeScreenData()
    }

    fun loadHomeScreenData() {
        viewModelScope.launch {
            _isLoading.value = true // Set loading to true
            coroutineScope {
                val recentlyPlayedDeferred = async { albumRepository.getAlbums("recent", 20, 0, true) }
                val recentDeferred = async { albumRepository.getAlbums("newest", 20, 0, true) }
                val mostPlayedDeferred = async { albumRepository.getAlbums("frequent", 20, 0, true) }
                val shuffledDeferred = async { albumRepository.getAlbums("random", 20, 0, true) }

                _recentlyPlayedAlbums.value = recentlyPlayedDeferred.await()
                _recentAlbums.value = recentDeferred.await()
                _mostPlayedAlbums.value = mostPlayedDeferred.await()
                _shuffledAlbums.value = shuffledDeferred.await()
            }
            _isLoading.value = false
        }
    }

    suspend fun getAlbumSongs(albumId: String): List<MediaItem> {
        return albumRepository.getAlbum(albumId) ?: emptyList()
    }
}