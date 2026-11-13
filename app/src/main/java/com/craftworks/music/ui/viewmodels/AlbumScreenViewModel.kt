package com.craftworks.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.craftworks.music.data.model.AlbumListSort
import com.craftworks.music.data.model.MediaQuery
import com.craftworks.music.data.model.SortOrder
import com.craftworks.music.data.repository.AlbumRepository
import com.craftworks.music.managers.DataRefreshManager
import com.craftworks.music.managers.settings.LocalDataSettingsManager
import com.craftworks.music.utils.PagingUtils
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
                localDataSettingsManager.showFavoriteAlbum
            ) { sort, sortOrder, showFavorites -> Triple(sort, sortOrder, showFavorites) }
                .distinctUntilChanged()
                .collect { (sort, sortOrder, showFavorites) ->
                    _sort.value = sort
                    _sortOrder.value = sortOrder
                    _showFavoritesOnly.value = showFavorites
                    getAlbums()
                }
        }

        viewModelScope.launch {
            DataRefreshManager.dataSourceChangedEvent.collect {
                getAlbums()
            }
        }
    }

    private var getAlbumsJob: Job? = null
    private var getMoreAlbumsJob: Job? = null
    private var nextAlbumOffset = 0
    private var isEndReached = false
    private var isLoadingMore = false

    fun getAlbums() {
        getAlbumsJob?.cancel()
        getMoreAlbumsJob?.cancel()
        isLoadingMore = false
        nextAlbumOffset = 0
        isEndReached = false
        _allAlbums.value = emptyList()

        getAlbumsJob = viewModelScope.launch {
            try {
                _isLoading.value = true
                val albums = albumRepository.getAlbums(
                    MediaQuery.AlbumListQuery(
                        _sort.value,
                        _sortOrder.value,
                        limit = PAGE_SIZE,
                        startIndex = 0,
                        favorite = if (_showFavoritesOnly.value) true else null
                    )
                )
                nextAlbumOffset = albums.size
                isEndReached = albums.size < PAGE_SIZE
                _allAlbums.value = PagingUtils.appendDistinct(emptyList(), albums) { it.mediaId }
            }
            finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun getAlbum(id: String): List<MediaItem> {
        return albumRepository.getAlbum(id) ?: emptyList()
    }

    fun getMoreAlbums(size: Int = PAGE_SIZE) {
        if (isLoadingMore || isEndReached || getAlbumsJob?.isActive == true) return

        isLoadingMore = true
        getMoreAlbumsJob = viewModelScope.launch {
            try {
                _isLoading.value = true
                val albumOffset = nextAlbumOffset
                val newAlbums = albumRepository.getAlbums(
                    MediaQuery.AlbumListQuery(
                        _sort.value,
                        _sortOrder.value,
                        limit = size,
                        startIndex = albumOffset,
                        favorite = if (_showFavoritesOnly.value) true else null
                    )
                )
                nextAlbumOffset = albumOffset + newAlbums.size
                if (newAlbums.size < size) isEndReached = true
                _allAlbums.value = PagingUtils.appendDistinct(_allAlbums.value, newAlbums) { it.mediaId }
            }
            finally {
                isLoadingMore = false
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
                        favorite = if (_showFavoritesOnly.value) true else null
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
            localDataSettingsManager.saveShowFavoriteAlbum(showFavorites)
        }
    }

    companion object {
        private const val PAGE_SIZE = 50
    }
}
