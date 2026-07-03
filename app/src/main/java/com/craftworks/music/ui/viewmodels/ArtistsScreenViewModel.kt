package com.craftworks.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.craftworks.music.data.model.AlbumArtistListSort
import com.craftworks.music.data.model.MediaModel
import com.craftworks.music.data.model.MediaQuery
import com.craftworks.music.data.model.SortOrder
import com.craftworks.music.data.repository.AlbumRepository
import com.craftworks.music.data.repository.ArtistRepository
import com.craftworks.music.managers.DataRefreshManager
import com.craftworks.music.managers.settings.LocalDataSettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistsScreenViewModel @Inject constructor(
    private val artistRepository: ArtistRepository,
    private val albumRepository: AlbumRepository,
    private val localDataSettingsManager: LocalDataSettingsManager
) : ViewModel() {
    private val _allArtists = MutableStateFlow<List<MediaModel.Artist>>(emptyList())
    val allArtists: StateFlow<List<MediaModel.Artist>> = _allArtists.asStateFlow()

    private val _selectedArtist = MutableStateFlow<MediaModel.Artist?>(null)
    val selectedArtist: StateFlow<MediaModel.Artist?> = _selectedArtist

    private val _artistAlbums = MutableStateFlow<List<MediaItem>>(emptyList())
    val artistAlbums: StateFlow<List<MediaItem>> = _artistAlbums.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly: StateFlow<Boolean> = _showFavoritesOnly.asStateFlow()

    init {
        getArtists()
        viewModelScope.launch {
            localDataSettingsManager.showFavoriteOnly.collect { showFavorites ->
                _showFavoritesOnly.value = showFavorites
                getArtists()
            }
            DataRefreshManager.dataSourceChangedEvent.collect {
                getArtists()
            }
        }
    }

    fun getArtists() {
        viewModelScope.launch {
            _isLoading.value = true
            _allArtists.value = artistRepository.getArtists(MediaQuery.AlbumArtistListQuery(
                AlbumArtistListSort.NAME,
                SortOrder.ASC,
                startIndex = 0,
                favorite = _showFavoritesOnly.value
            ))
            _isLoading.value = false
        }
    }

    suspend fun getAlbum(id: String): List<MediaItem> {
        return albumRepository.getAlbum(id) ?: emptyList()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    @OptIn(FlowPreview::class)
    val searchResults: StateFlow<List<MediaModel.Artist>> = searchQuery
        .debounce(300L) // Adds a small delay to avoid searching on every keystroke.
        .combine(allArtists) { query, artists ->
            if (query.isBlank()) {
                emptyList()
            } else {
                artists.filter { artist ->
                    artist.name.contains(query, ignoreCase = true)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )


    fun setSelectedArtist(artist: MediaModel.Artist) {
        _selectedArtist.value = artist
        viewModelScope.launch {
            val loadingJob = launch {
                delay(1000)
                if (_artistAlbums.value.isEmpty()) {
                    _isLoading.value = true
                }
            }
            loadingJob.start()
            coroutineScope {
                val artistAlbumsAsync = async { artistRepository.getArtistAlbums(artist.id) }
                _artistAlbums.value = artistAlbumsAsync.await()

                _selectedArtist.value = async { artistRepository.getArtistDetail(artist.id) }.await()
            }

            loadingJob.cancel()
            _isLoading.value = false
        }
    }
    fun setShowFavoritesOnly(showFavorites: Boolean) {
        viewModelScope.launch {
            localDataSettingsManager.saveShowFavoriteOnly(showFavorites)
        }
    }
}