package com.craftworks.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.craftworks.music.data.model.SortOrder
import com.craftworks.music.data.repository.AlbumRepository
import com.craftworks.music.managers.DataRefreshManager
import com.craftworks.music.managers.settings.LocalDataSettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumScreenViewModel @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val localDataSettingsManager: LocalDataSettingsManager
) : ViewModel() {

    private val _allAlbums = MutableStateFlow<List<MediaItem>>(emptyList())
    val allAlbums: StateFlow<List<MediaItem>> = _allAlbums.asStateFlow()

    private val _searchResults = MutableStateFlow<List<MediaItem>>(emptyList())
    val searchResults: StateFlow<List<MediaItem>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.ALPHABETICAL)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()
    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly: StateFlow<Boolean> = _showFavoritesOnly.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                localDataSettingsManager.sortAlbumOrder,
                localDataSettingsManager.showFavoriteOnly
            ) { sortOrder, showFavorites -> sortOrder to showFavorites }
                .collect { (sortOrder, showFavorites) ->
                    _sortOrder.value = sortOrder
                    _showFavoritesOnly.value = showFavorites
                    getAlbums()
                }
        }
        viewModelScope.launch {
            localDataSettingsManager.sortAlbumOrder.collect { sortOrder ->
                _sortOrder.value = sortOrder
                getAlbums()
            }
        }
    }

    private var getAlbumsJob: Job? = null
    fun getAlbums() {
        _allAlbums.value = emptyList()

        getAlbumsJob?.cancel()

        getAlbumsJob = viewModelScope.launch {
            _isLoading.value = true

            try {
                coroutineScope {
                    val allAlbumsDeferred = async { albumRepository.getAlbums(_sortOrder.value.key, 50, 0, true, _showFavoritesOnly.value) }

                    _allAlbums.value = allAlbumsDeferred.await().sortedByDescending {
                        it.mediaMetadata.extras?.getString("navidromeID")!!.startsWith("Local_")
                    }
                }
                _isLoading.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun getAlbum(id: String): List<MediaItem> {
        return albumRepository.getAlbum(id) ?: emptyList()
    }

    fun getMoreAlbums(size: Int){
        println("GETTING MORE ALBUMS")
        viewModelScope.launch {
            coroutineScope {
                val albumOffset = _allAlbums.value.size
                val newAlbums = albumRepository.getAlbums(_sortOrder.value.key, size, albumOffset, favoritesOnly=_showFavoritesOnly.value)
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
        viewModelScope.launch {
            localDataSettingsManager.saveSortAlbumOrder(newSortOrder)
        }
    }
    fun setShowFavoritesOnly(showFavorites: Boolean) {
        viewModelScope.launch {
            localDataSettingsManager.saveShowFavoriteOnly(showFavorites)
        }
    }
}