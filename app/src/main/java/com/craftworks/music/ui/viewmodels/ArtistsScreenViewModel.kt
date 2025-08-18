package com.craftworks.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.craftworks.music.data.model.MediaData
import com.craftworks.music.data.repository.ArtistRepository
import com.craftworks.music.managers.DataRefreshManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistsScreenViewModel @Inject constructor(
    private val artistRepository: ArtistRepository
) : ViewModel() {
    private val _allArtists = MutableStateFlow<List<MediaData.Artist>>(emptyList())
    val allArtists: StateFlow<List<MediaData.Artist>> = _allArtists.asStateFlow()

    private val _selectedArtist = MutableStateFlow<MediaData.Artist?>(null)
    val selectedArtist: StateFlow<MediaData.Artist?> = _selectedArtist

    private val _artistAlbums = MutableStateFlow<List<MediaItem>>(emptyList())
    val artistAlbums: StateFlow<List<MediaItem>> = _artistAlbums.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        getArtists()

        viewModelScope.launch {
            DataRefreshManager.dataSourceChangedEvent.collect {
                getArtists()
            }
        }
    }

    fun getArtists() {
        viewModelScope.launch {
            _isLoading.value = true
            _allArtists.value = artistRepository.getArtists(ignoreCachedResponse = true)
            _isLoading.value = false
        }
    }

    suspend fun getAlbums(id: String): List<MediaItem> {
        return artistRepository.getArtistAlbums(id)
    }

    suspend fun search(query: String) {
        //_allArtists.value = artistRepository.searchArtist(query)
    }

    fun setSelectedArtist(artist: MediaData.Artist) {
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
}