package com.craftworks.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.craftworks.music.data.model.MediaItem
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
    private val _allArtists = MutableStateFlow<List<com.craftworks.music.data.model.MediaItem.Artist>>(emptyList())
    val allArtists: StateFlow<List<com.craftworks.music.data.model.MediaItem.Artist>> = _allArtists.asStateFlow()

    private val _selectedArtist = MutableStateFlow<com.craftworks.music.data.model.MediaItem.Artist?>(null)
    val selectedArtist: StateFlow<com.craftworks.music.data.model.MediaItem.Artist?> = _selectedArtist

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
            _allArtists.value = artistRepository.getArtists(ignoreCachedResponse = true, favoritesOnly = _showFavoritesOnly.value)
            _isLoading.value = false
        }
    }

    suspend fun getAlbum(id: String): List<MediaItem> {
        return albumRepository.getAlbum(id) ?: emptyList()
    }

//    suspend fun search(query: String) {
//        _allArtists.value = artistRepository.searchArtists(query)
//    }
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    @OptIn(FlowPreview::class)
    val searchResults: StateFlow<List<com.craftworks.music.data.model.MediaItem.Artist>> = searchQuery
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


    fun setSelectedArtist(artist: com.craftworks.music.data.model.MediaItem.Artist) {
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
                val artistAlbumsAsync = async { artistRepository.getArtistAlbums(artist.navidromeID) }
                _artistAlbums.value = artistAlbumsAsync.await()

                val artistDetails = async { artistRepository.getArtistInfo(artist.navidromeID) }.await()
                _selectedArtist.value = _selectedArtist.value?.copy(
                    description = artistDetails?.biography ?: "",
                    musicBrainzId = artistDetails?.musicBrainzId,
                    similarArtist = artistDetails?.similarArtist
                )
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