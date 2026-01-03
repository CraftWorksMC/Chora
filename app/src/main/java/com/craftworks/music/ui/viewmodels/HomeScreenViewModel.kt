package com.craftworks.music.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.craftworks.music.data.repository.AlbumRepository
import com.craftworks.music.managers.DataRefreshManager
import com.craftworks.music.managers.NavidromeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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

    // Track active load job to prevent redundant concurrent loads
    private var loadJob: Job? = null

    init {
        loadHomeScreenData()

        // Separate coroutines for each flow to prevent blocking
        viewModelScope.launch {
            try {
                DataRefreshManager.dataSourceChangedEvent.collect {
                    loadHomeScreenData()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        viewModelScope.launch {
            try {
                combine(
                    NavidromeManager.currentServerId,
                    NavidromeManager.libraries
                ) { serverId, libs -> serverId to libs }
                    .distinctUntilChanged()
                    .collect { (serverId, libs) ->
                        if (serverId != null && libs.isNotEmpty()) {
                            loadHomeScreenData()
                        }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadHomeScreenData(forceRefresh: Boolean = false) {
        // Cancel any existing load to prevent redundant concurrent loads
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                coroutineScope {
                    // Use cache by default, only bypass on explicit refresh
                    val recentlyPlayedDeferred = async { albumRepository.getAlbums("recent", 20, 0, forceRefresh) }
                    val recentDeferred = async { albumRepository.getAlbums("newest", 20, 0, forceRefresh) }
                    val mostPlayedDeferred = async { albumRepository.getAlbums("frequent", 20, 0, forceRefresh) }
                    val shuffledDeferred = async { albumRepository.getAlbums("random", 20, 0, forceRefresh) }

                    _recentlyPlayedAlbums.value = recentlyPlayedDeferred.await()
                    _recentAlbums.value = recentDeferred.await()
                    _mostPlayedAlbums.value = mostPlayedDeferred.await()
                    _shuffledAlbums.value = shuffledDeferred.await()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun getAlbumSongs(albumId: String): List<MediaItem> {
        return albumRepository.getAlbum(albumId) ?: emptyList()
    }
}
