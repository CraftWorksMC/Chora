package com.craftworks.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.craftworks.music.data.MediaData
import com.craftworks.music.data.albumList
import com.craftworks.music.providers.navidrome.NavidromeManager
import com.craftworks.music.providers.navidrome.getNavidromeAlbums
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlbumScreenViewModel : ViewModel(), ReloadableViewModel {
    private val _allAlbums = MutableStateFlow<List<MediaData.Album>>(emptyList())
    val allAlbums: StateFlow<List<MediaData.Album>> = _allAlbums.asStateFlow()

    override fun reloadData() {
        viewModelScope.launch {
            coroutineScope {
                if (NavidromeManager.getCurrentServer() != null) {
                    val allAlbumsDeferred = async { getNavidromeAlbums("recent", 20) }

                    _allAlbums.value = allAlbumsDeferred.await()
                } else {
                    _allAlbums.value = albumList.sortedBy { it.name }
                }
            }
        }
    }
}