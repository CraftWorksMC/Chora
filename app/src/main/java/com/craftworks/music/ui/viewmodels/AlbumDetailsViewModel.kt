package com.craftworks.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.craftworks.music.data.model.LibraryType
import com.craftworks.music.data.model.MediaItem
import com.craftworks.music.managers.MediaProviderManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumDetailsViewModel @Inject constructor(
    private val mediaProviderManager: MediaProviderManager,
) : ViewModel() {
    val provider = mediaProviderManager.currentProvider.value;
    private val _albumDetails = MutableStateFlow<MediaItem.Album?>(null)
    val albumDetails: StateFlow<MediaItem.Album?> = _albumDetails.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadAlbumDetails(albumId: String) {
        viewModelScope.launch {
            val loadingJob = launch {
                delay(500)
                if (_albumDetails.value == null) {
                    _isLoading.value = true
                }
            }

            _albumDetails.value = provider?.getAlbumDetail(albumId)

            loadingJob.cancel()
            _isLoading.value = false
        }
    }

    fun starAlbum(id: String) {
        viewModelScope.launch {
            provider?.createFavorite(listOf(id),LibraryType.ALBUM)
        }
    }
    fun unstarAlbum(id: String) {
        viewModelScope.launch {
            provider?.deleteFavorite(listOf(id),LibraryType.ALBUM)
        }
    }
}