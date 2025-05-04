package com.craftworks.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.craftworks.music.providers.getSongs
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SongsScreenViewModel : ViewModel(), ReloadableViewModel {
    private val _allSongs = MutableStateFlow<List<MediaItem>>(emptyList())
    val allSongs: StateFlow<List<MediaItem>> = _allSongs.asStateFlow()

    override fun reloadData() {
        viewModelScope.launch {
            coroutineScope {
                _allSongs.value = getSongs()
            }
        }
    }
    fun getMoreSongs(size: Int){
        viewModelScope.launch {
            val songOffset = _allSongs.value.size
            _allSongs.value += getSongs(songCount = size, songOffset = songOffset)
        }
    }

    suspend fun search(query: String){
        _allSongs.value = getSongs(query, 500)
    }
}