package com.craftworks.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.craftworks.music.data.repository.AlbumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumDetailsViewModel @Inject constructor(
    private val albumRepository: AlbumRepository
) : ViewModel() {
    private val _songsInAlbum = MutableStateFlow<List<MediaItem>>(listOf())
    val songsInAlbum: StateFlow<List<MediaItem>> = _songsInAlbum.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadAlbumDetails(albumId: String) {
        viewModelScope.launch {
            val loadingJob = launch {
                delay(500)
                if (_songsInAlbum.value.isEmpty()) {
                    _isLoading.value = true
                }
            }

            try {
                _songsInAlbum.value = albumRepository.getAlbum(albumId) ?: listOf(MediaItem.EMPTY)
            } catch (e: Exception) {
                e.printStackTrace()
                _songsInAlbum.value = listOf(MediaItem.EMPTY)
            } finally {
                loadingJob.cancel()
                _isLoading.value = false
            }
        }
    }
}