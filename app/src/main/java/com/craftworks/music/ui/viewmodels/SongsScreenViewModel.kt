package com.craftworks.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.StarRating
import com.craftworks.music.data.model.MediaQuery
import com.craftworks.music.data.model.SongListSort
import com.craftworks.music.data.model.SortOrder
import com.craftworks.music.data.model.id
import com.craftworks.music.data.repository.SongRepository
import com.craftworks.music.managers.DataRefreshManager
import com.craftworks.music.managers.settings.LocalDataSettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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

    private val _sortOrder = MutableStateFlow(SortOrder.ASC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _sort = MutableStateFlow(SongListSort.NAME)
    val sort: StateFlow<SongListSort> = _sort.asStateFlow()

    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly: StateFlow<Boolean> = _showFavoritesOnly.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                localDataSettingsManager.sortSong,
                localDataSettingsManager.sortSongOrder,
                localDataSettingsManager.showFavoriteSong
            ) { sort, sortOrder, showFavorites -> Triple(sort, sortOrder, showFavorites) }
                .distinctUntilChanged()
                .collect { (sort, sortOrder, showFavorites) ->
                    _sort.value = sort
                    _sortOrder.value = sortOrder
                    _showFavoritesOnly.value = showFavorites
                    getSongs()
                }
            DataRefreshManager.dataSourceChangedEvent.collect {
                getSongs()
            }
        }
    }

    private var getSongsJob: Job? = null
    fun getSongs() {
        getSongsJob?.cancel()

        getSongsJob = viewModelScope.launch {
            try {
                _isLoading.value = true
                _allSongs.value = songRepository.getSongs(
                    MediaQuery.SongListQuery(
                        sortBy = _sort.value,
                        sortOrder = _sortOrder.value,
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

    fun getMoreSongs(size: Int){
        viewModelScope.launch {
            _isLoading.value = true
            coroutineScope {
                val songOffset = _allSongs.value.size
                _allSongs.value += songRepository.getSongs(MediaQuery.SongListQuery(sortBy = _sort.value, sortOrder = _sortOrder.value, limit = size, startIndex = songOffset, favorite = _showFavoritesOnly.value))
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
                _searchResults.value = songRepository.getSongs(MediaQuery.SongListQuery(sortBy = _sort.value, sortOrder = _sortOrder.value, searchTerm = query, startIndex = 0))
            }
            _isLoading.value = false
        }
    }
    fun setSorting(newSort: SongListSort) {
        viewModelScope.launch {
            localDataSettingsManager.saveSortSong(newSort)
        }
    }
    fun setOrder(newSortOrder: SortOrder) {
        viewModelScope.launch {
            localDataSettingsManager.saveSortSongOrder(newSortOrder)
        }
    }
    fun setShowFavoritesOnly(showFavorites: Boolean) {
        viewModelScope.launch {
            localDataSettingsManager.saveShowFavoriteSong(showFavorites)
        }
    }

    fun setSongRating(
        songId: String,
        rating: Int,
    ) {
        val song =_allSongs.value.firstOrNull {
            it.mediaMetadata.id == songId
        } ?: _searchResults.value.first {
            it.mediaMetadata.id == songId
        }

        val maxStars = (song.mediaMetadata.userRating as? StarRating)?.maxStars ?: 5

        val updatedSong = song.buildUpon().setMediaMetadata(
            song.mediaMetadata.buildUpon()
                .setUserRating(StarRating(maxStars, rating.toFloat()))
                .build()
        ).build()

        _allSongs.value = _allSongs.value.map { item ->
            if (item.mediaId == song.mediaId) updatedSong else item
        }
        _searchResults.value = _searchResults.value.map { item ->
            if (item.mediaId == song.mediaId) updatedSong else item
        }

        viewModelScope.launch {
            songRepository.setSongRating(songId, rating)
        }
    }
}