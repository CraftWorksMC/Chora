package com.craftworks.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.craftworks.music.data.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SongsScreenViewModel @Inject constructor(
    private val songRepository: SongRepository
) : ViewModel() {

    private val _allSongs = MutableStateFlow<List<MediaItem>>(emptyList())
    val allSongs: StateFlow<List<MediaItem>> = _allSongs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        getSongs()
    }

    fun getSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            coroutineScope {
                _allSongs.value = songRepository.getSongs(ignoreCachedResponse = true)
            }
            _isLoading.value = false
        }
    }

    fun getMoreSongs(size: Int){
        viewModelScope.launch {
            _isLoading.value = true
            coroutineScope {
                val songOffset = _allSongs.value.size
                _allSongs.value += songRepository.getSongs(songCount = size, songOffset = songOffset)
            }
            _isLoading.value = false
        }
    }

    suspend fun search(query: String){
        _allSongs.value = songRepository.getSongs(query, 500)
    }
}