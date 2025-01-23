package com.craftworks.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.craftworks.music.data.MediaData
import com.craftworks.music.managers.NavidromeManager
import com.craftworks.music.providers.getPlaylistDetails
import com.craftworks.music.providers.getPlaylists
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlaylistScreenViewModel : ViewModel(), ReloadableViewModel {
    private val _allPlaylists = MutableStateFlow<List<MediaData.Playlist>>(emptyList())
    val allPlaylists: StateFlow<List<MediaData.Playlist>> = _allPlaylists.asStateFlow()

    private var _selectedPlaylist = MutableStateFlow<MediaData.Playlist?>(null)
    var selectedPlaylist: StateFlow<MediaData.Playlist?> = _selectedPlaylist

    fun setCurrentPlaylist(playlist: MediaData.Playlist){
        _selectedPlaylist.value = playlist
    }

    override fun reloadData() {
        viewModelScope.launch {
            coroutineScope {
                val allPlaylistsDeferred = async { getPlaylists() }

                _allPlaylists.value = allPlaylistsDeferred.await()
            }
        }
    }

    fun addLocalPlaylists(playlist: List<MediaData.Playlist>) {
        _allPlaylists.value += playlist
    }

    fun fetchPlaylistDetails() {
        if (!NavidromeManager.checkActiveServers()) return

        viewModelScope.launch {
            coroutineScope {
                val selectedPlaylistDeferred = async { getPlaylistDetails(_selectedPlaylist.value?.navidromeID.toString()) }

                _selectedPlaylist.value = _selectedPlaylist.value?.copy(
                    songs = selectedPlaylistDeferred.await()?.songs,
                    coverArt = selectedPlaylistDeferred.await()?.coverArt
                )
            }
        }
    }
}