package com.craftworks.music.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.craftworks.music.providers.getFavouriteSongs
import com.craftworks.music.providers.getPlaylistDetails
import com.craftworks.music.providers.getPlaylists
import com.craftworks.music.providers.local.localPlaylistImageGenerator
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlaylistScreenViewModel : ViewModel() {
    private val _allPlaylists = MutableStateFlow<List<MediaItem>>(emptyList())
    val allPlaylists: StateFlow<List<MediaItem>> = _allPlaylists.asStateFlow()

    private var _selectedPlaylist = MutableStateFlow<MediaItem?>(null)
    val selectedPlaylist: StateFlow<MediaItem?> = _selectedPlaylist

    private var _selectedPlaylistSongs = MutableStateFlow<List<MediaItem>>(emptyList())
    val selectedPlaylistSongs: StateFlow<List<MediaItem>> = _selectedPlaylistSongs.asStateFlow()

    fun setCurrentPlaylist(playlist: MediaItem){
        _selectedPlaylistSongs.value = emptyList<MediaItem>()
        _selectedPlaylist.value = playlist
    }

    suspend fun updatePlaylistsImages(context: Context) {
        _allPlaylists.value = _allPlaylists.value.map {
            if (it.mediaMetadata.extras?.getString("navidromeID")?.startsWith("Local") == true) {
                val songs = getPlaylistDetails(it.mediaMetadata.extras?.getString("navidromeID") ?: "", true)
                it.buildUpon()
                    .setMediaMetadata(
                        it.mediaMetadata.buildUpon()
                            .setArtworkData(localPlaylistImageGenerator(songs ?: emptyList(), context), MediaMetadata.PICTURE_TYPE_OTHER)
                            .build()
                    )
                    .build()
            } else {
                it
            }
        }
    }


    fun reloadData(context: Context) {
        viewModelScope.launch {
            coroutineScope {
                val allPlaylistsDeferred = async { getPlaylists(context, true) }

                _allPlaylists.value = allPlaylistsDeferred.await()
            }
        }
    }

    fun fetchPlaylistDetails() {
        //if (!NavidromeManager.checkActiveServers()) return

        if (_selectedPlaylist.value == null) return

        println("Fetching playlist details for playlist ID: ${_selectedPlaylist.value?.mediaMetadata?.extras?.getString("navidromeID")}")

        viewModelScope.launch {
            coroutineScope {
                if (_selectedPlaylist.value?.mediaMetadata?.extras?.getString("navidromeID") == "favourites") {
                    val favouriteSongs = async { getFavouriteSongs() }
                    _selectedPlaylistSongs.value = favouriteSongs.await()
                }
                else {
                    val selectedPlaylistDeferred = async { getPlaylistDetails(_selectedPlaylist.value?.mediaMetadata?.extras?.getString("navidromeID") ?: "") }

                    _selectedPlaylistSongs.value = selectedPlaylistDeferred.await() ?: emptyList()
                }
            }
        }
    }
}