package com.craftworks.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.craftworks.music.data.model.SortOrder
import com.craftworks.music.data.repository.AlbumRepository
import com.craftworks.music.managers.DataRefreshManager
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

    private val _searchResults = MutableStateFlow<List<MediaItem>>(emptyList())
    val searchResults: StateFlow<List<MediaItem>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.ALPHABETICAL)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    init {
        getAlbums()

        viewModelScope.launch {
            DataRefreshManager.dataSourceChangedEvent.collect {
                getAlbums()
            }
        }
    }

    fun getAlbums() {
        _allAlbums.value = emptyList()

        viewModelScope.launch {
            _isLoading.value = true
            coroutineScope {
                val allAlbumsDeferred = async { albumRepository.getAlbums(_sortOrder.value.key, 20, 0, true) }

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

    fun getMoreAlbums(size: Int){
        viewModelScope.launch {
            coroutineScope {
                val albumOffset = _allAlbums.value.size
                val newAlbums = albumRepository.getAlbums(_sortOrder.value.key, size, albumOffset)
                _allAlbums.value += newAlbums
            }
        }
    }

    fun search(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            coroutineScope {
                _searchResults.value = albumRepository.searchAlbum(query)
            }
            _isLoading.value = false
        }
    }

    fun setSorting(newSortOrder: SortOrder) {
        _sortOrder.value = newSortOrder
        getAlbums()
    }
}