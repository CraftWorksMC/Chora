package com.craftworks.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.craftworks.music.providers.getAlbums
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeScreenViewModel : ViewModel(), ReloadableViewModel {
    private val _recentlyPlayedAlbums = MutableStateFlow<List<MediaItem>>(emptyList())
    val recentlyPlayedAlbums: StateFlow<List<MediaItem>> = _recentlyPlayedAlbums.asStateFlow()

    private val _recentAlbums = MutableStateFlow<List<MediaItem>>(emptyList())
    val recentAlbums: StateFlow<List<MediaItem>> = _recentAlbums.asStateFlow()

    private val _mostPlayedAlbums = MutableStateFlow<List<MediaItem>>(emptyList())
    val mostPlayedAlbums: StateFlow<List<MediaItem>> = _mostPlayedAlbums.asStateFlow()

    private val _shuffledAlbums = MutableStateFlow<List<MediaItem>>(emptyList())
    val shuffledAlbums: StateFlow<List<MediaItem>> = _shuffledAlbums.asStateFlow()

    override fun reloadData() {
        viewModelScope.launch {
            coroutineScope {
                val recentlyPlayedDeferred = async { getAlbums("recent", 20, 0, true) }
                val recentDeferred = async { getAlbums("newest", 20, 0, true) }
                val mostPlayedDeferred = async { getAlbums("frequent", 20, 0, true) }
                val shuffledDeferred = async { getAlbums("random", 20, 0, true) }

                _recentlyPlayedAlbums.value = recentlyPlayedDeferred.await()
                _recentAlbums.value = recentDeferred.await()
                _mostPlayedAlbums.value = mostPlayedDeferred.await()
                _shuffledAlbums.value = shuffledDeferred.await()
            }
        }
    }
}