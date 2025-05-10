package com.craftworks.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.craftworks.music.data.MediaData
import com.craftworks.music.providers.getArtistDetails
import com.craftworks.music.providers.getArtists
import com.craftworks.music.providers.searchArtist
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ArtistsScreenViewModel : ViewModel(), ReloadableViewModel {
    private val _allArtists = MutableStateFlow<List<MediaData.Artist>>(emptyList())
    val allArtists: StateFlow<List<MediaData.Artist>> = _allArtists.asStateFlow()

    private val _selectedArtist = MutableStateFlow<MediaData.Artist?>(null)
    val selectedArtist: StateFlow<MediaData.Artist?> = _selectedArtist

    override fun reloadData() {
        viewModelScope.launch {
            coroutineScope {
                _allArtists.value = getArtists()
            }
        }
    }

    suspend fun search(query: String) {
        _allArtists.value = searchArtist(query)
    }

    fun setSelectedArtist(artist: MediaData.Artist) {
        _selectedArtist.value = artist
        runBlocking {
            val detailedArtist = async { getArtistDetails(artist.navidromeID) }
            _selectedArtist.value = detailedArtist.await()
            println("New Artist: ${_selectedArtist.value} \n Selected Artist: ${com.craftworks.music.data.selectedArtist}")
        }
    }
}