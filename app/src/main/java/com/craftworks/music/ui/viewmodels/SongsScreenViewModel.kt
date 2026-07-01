package com.craftworks.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.craftworks.music.data.model.MediaQuery
import com.craftworks.music.data.model.SongListSort
import com.craftworks.music.data.model.SortOrder
import com.craftworks.music.data.repository.SongRepository
import com.craftworks.music.managers.DataRefreshManager
import com.craftworks.music.managers.settings.LocalDataSettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SongsScreenViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val localDataSettingsManager: LocalDataSettingsManager
) : ViewModel() {

    private val _allSongs = MutableStateFlow<List<MediaItem>>(emptyList())
    val allSongs: StateFlow<List<MediaItem>> = _allSongs.asStateFlow()

    private val _searchResults = MutableStateFlow<List<MediaItem>>(emptyList())
    val searchResults: StateFlow<List<MediaItem>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly: StateFlow<Boolean> = _showFavoritesOnly.asStateFlow()

    init {
        getSongs()
        viewModelScope.launch {
            localDataSettingsManager.showFavoriteOnly.collect { showFavorites ->
                _showFavoritesOnly.value = showFavorites
                getSongs()
            }
            DataRefreshManager.dataSourceChangedEvent.collect {
                getSongs()
            }
        }
    }

    fun getSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            coroutineScope {
                _allSongs.value = songRepository.getSongs(MediaQuery.SongListQuery(sortBy = SongListSort.RECENTLY_ADDED, sortOrder = SortOrder.ASC, startIndex = 0))
            }
            _isLoading.value = false
        }
    }

    fun getMoreSongs(size: Int){
        viewModelScope.launch {
            _isLoading.value = true
            coroutineScope {
                val songOffset = _allSongs.value.size
                _allSongs.value += songRepository.getSongs(MediaQuery.SongListQuery(sortBy = SongListSort.RECENTLY_ADDED, sortOrder = SortOrder.ASC, limit = size, startIndex = songOffset))
            }
            _isLoading.value = false
        }
    }

    fun search(query: String){
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            coroutineScope {
                _searchResults.value = songRepository.getSongs(MediaQuery.SongListQuery(sortBy = SongListSort.RECENTLY_ADDED, sortOrder = SortOrder.ASC, searchTerm = query, startIndex = 0))
            }
            _isLoading.value = false
        }
    }
    fun setShowFavoritesOnly(showFavorites: Boolean) {
        viewModelScope.launch {
            localDataSettingsManager.saveShowFavoriteOnly(showFavorites)
        }
    }
}