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
class AlbumScreenViewModel @Inject constructor(
    private val albumRepository: AlbumRepository
) : ViewModel() {

    private val _allAlbums = MutableStateFlow<List<MediaItem>>(emptyList())
    val allAlbums: StateFlow<List<MediaItem>> = _allAlbums.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        getAlbums()
    }

    fun getAlbums() {
        viewModelScope.launch {
            _isLoading.value = true
            coroutineScope {
                val allAlbumsDeferred = async { albumRepository.getAlbums("alphabeticalByName", 20, 0, true) }

                _allAlbums.value = allAlbumsDeferred.await().sortedByDescending {
                    it.mediaMetadata.extras?.getString("navidromeID")!!.startsWith("Local_")
                }
            }
            _isLoading.value = false
        }
    }

    suspend fun getAlbum(id: String): List<MediaItem> {
        return albumRepository.getAlbum(id) ?: emptyList()
    }

    fun getMoreAlbums(sort: String? = "alphabeticalByName" , size: Int){
        viewModelScope.launch {
            coroutineScope {
                val albumOffset = _allAlbums.value.size
                val newAlbums = albumRepository.getAlbums(sort, size, albumOffset)
                _allAlbums.value += newAlbums
            }
        }
    }

    suspend fun search(query: String){
        _allAlbums.value = albumRepository.searchAlbum(query)
    }
}