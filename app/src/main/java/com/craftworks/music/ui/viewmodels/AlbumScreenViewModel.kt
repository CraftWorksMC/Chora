package com.craftworks.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.craftworks.music.data.model.AlbumListSort
import com.craftworks.music.data.model.MediaQuery
import com.craftworks.music.data.model.SortOrder
import com.craftworks.music.data.repository.AlbumRepository
import com.craftworks.music.managers.settings.LocalDataSettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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

    private val _sortOrder = MutableStateFlow(SortOrder.ASC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _sort = MutableStateFlow(AlbumListSort.NAME)
    val sort: StateFlow<AlbumListSort> = _sort.asStateFlow()

    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly: StateFlow<Boolean> = _showFavoritesOnly.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                localDataSettingsManager.sortAlbum,
                localDataSettingsManager.sortAlbumOrder,
                localDataSettingsManager.showFavoriteOnly
            ) { sort, sortOrder, showFavorites -> Triple(sort, sortOrder, showFavorites) }
                .distinctUntilChanged()
                .collect { (sort, sortOrder, showFavorites) ->
                    _sort.value = sort
                    _sortOrder.value = sortOrder
                    _showFavoritesOnly.value = showFavorites
                    getAlbums()
                }
        }
    }

    private var getAlbumsJob: Job? = null
    fun getAlbums() {
        getAlbumsJob?.cancel()
        _allAlbums.value = emptyList()

        getAlbumsJob = viewModelScope.launch {
            try {
                _isLoading.value = true
                _allAlbums.value = albumRepository.getAlbums(
                    MediaQuery.AlbumListQuery(
                        _sort.value,
                        _sortOrder.value,
                        limit = 50,
                        startIndex = 0,
                        favorite = _showFavoritesOnly.value
                    )
                )
            }
            finally {
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
            try {
                _isLoading.value = true
                val albumOffset = _allAlbums.value.size
                val newAlbums = albumRepository.getAlbums(
                    MediaQuery.AlbumListQuery(
                        _sort.value,
                        _sortOrder.value,
                        limit = size,
                        startIndex = albumOffset,
                        favorite = _showFavoritesOnly.value
                    )
                )
                _allAlbums.value += newAlbums
            }
            finally {
                _isLoading.value = false
            }
        }
    }

    fun search(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _searchResults.value = albumRepository.getAlbums(
                    MediaQuery.AlbumListQuery(
                        _sort.value,
                        _sortOrder.value,
                        searchTerm = query,
                        startIndex = 0,
                        favorite = _showFavoritesOnly.value
                    )
                )
            }
            finally {
                _isLoading.value = false
            }
        }
    }

    fun setSorting(newSort: AlbumListSort) {
        viewModelScope.launch {
            localDataSettingsManager.saveSortAlbum(newSort)
        }
    }
    fun setOrder(newSortOrder: SortOrder) {
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