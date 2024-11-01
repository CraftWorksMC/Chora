@file:Suppress("SpellCheckingInspection")

package com.craftworks.music.ui.viewmodels

import androidx.compose.ui.util.fastFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.craftworks.music.data.MediaData
import com.craftworks.music.data.albumList
import com.craftworks.music.data.artistList
import com.craftworks.music.managers.NavidromeManager
import com.craftworks.music.providers.navidrome.getNavidromeArtistBiography
import com.craftworks.music.providers.navidrome.getNavidromeArtistDetails
import com.craftworks.music.providers.navidrome.getNavidromeArtists
import com.craftworks.music.providers.navidrome.searchNavidromeArtists
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Suppress("SpellCheckingInspection")
class ArtistsScreenViewModel : ViewModel(), ReloadableViewModel {
    private val _allArtists = MutableStateFlow<List<MediaData.Artist>>(emptyList())
    val allArtists: StateFlow<List<MediaData.Artist>> = _allArtists.asStateFlow()

    private val _selectedArtist = MutableStateFlow<MediaData.Artist?>(null)
    val selectedArtist: StateFlow<MediaData.Artist?> = _selectedArtist

    override fun reloadData() {
        viewModelScope.launch {
            coroutineScope {
                if (NavidromeManager.checkActiveServers()) {
                    val allArtistsDeferred = async { getNavidromeArtists() }

                    _allArtists.value = allArtistsDeferred.await()
                } else {
                    _allArtists.value = artistList
                }
            }
        }
    }

    suspend fun search(query: String) {
        if (NavidromeManager.checkActiveServers()) {
            _allArtists.value = searchNavidromeArtists(query)
        } else {
            _allArtists.value =
                artistList.fastFilter { it.name.lowercase().contains(query.lowercase()) }
        }
    }

    fun fetchArtistDetails(artistId: String) {
        viewModelScope.launch {
            if (NavidromeManager.getCurrentServer() != null) {
                // Fetch artist details and biography concurrently
                val detailsDeferred = async { getNavidromeArtistDetails(artistId) }
                val biographyDeferred = async { getNavidromeArtistBiography(artistId) }

                // Wait for both to complete
                val details = detailsDeferred.await()
                val biography = biographyDeferred.await()

                // Update the state with the combined data
                _selectedArtist.value = details.copy(
                    description = biography.description,
                    similarArtist = biography.similarArtist
                )
            } else {
                _selectedArtist.value = com.craftworks.music.data.selectedArtist.copy(
                    album = albumList.fastFilter { it.artist == com.craftworks.music.data.selectedArtist.name }
                )
            }
            println("${_selectedArtist.value} + ${com.craftworks.music.data.selectedArtist}")

            //com.craftworks.music.data.selectedArtist = _selectedArtist.value ?: com.craftworks.music.data.selectedArtist
        }
    }
}