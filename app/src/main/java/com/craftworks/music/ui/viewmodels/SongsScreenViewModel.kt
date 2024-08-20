package com.craftworks.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.craftworks.music.data.MediaData
import com.craftworks.music.data.songsList
import com.craftworks.music.providers.navidrome.NavidromeManager
import com.craftworks.music.providers.navidrome.getNavidromeSongs
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SongsScreenViewModel : ViewModel(), ReloadableViewModel {
    private val _allSongs = MutableStateFlow<List<MediaData.Song>>(emptyList())
    val allSongs: StateFlow<List<MediaData.Song>> = _allSongs.asStateFlow()

    override fun reloadData() {
        viewModelScope.launch {
            coroutineScope {
                if (NavidromeManager.getCurrentServer() != null) {
                    val allSongsDeferred = async { getNavidromeSongs() }

                    _allSongs.value = allSongsDeferred.await()
                } else {
                    _allSongs.value = songsList.sortedBy { it.title }
                }
            }
        }
    }
}