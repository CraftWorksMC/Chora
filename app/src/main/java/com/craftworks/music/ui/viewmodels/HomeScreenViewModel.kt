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

class HomeScreenViewModel : ViewModel(), ReloadableViewModel {
    private val _recentlyPlayedAlbums = MutableStateFlow<List<MediaData.Album>>(emptyList())
    val recentlyPlayedAlbums: StateFlow<List<MediaData.Album>> = _recentlyPlayedAlbums.asStateFlow()

    private val _recentAlbums = MutableStateFlow<List<MediaData.Album>>(emptyList())
    val recentAlbums: StateFlow<List<MediaData.Album>> = _recentAlbums.asStateFlow()

    private val _mostPlayedAlbums = MutableStateFlow<List<MediaData.Album>>(emptyList())
    val mostPlayedAlbums: StateFlow<List<MediaData.Album>> = _mostPlayedAlbums.asStateFlow()

    private val _shuffledAlbums = MutableStateFlow<List<MediaData.Album>>(emptyList())
    val shuffledAlbums: StateFlow<List<MediaData.Album>> = _shuffledAlbums.asStateFlow()

    override fun reloadData() {
        viewModelScope.launch {
            coroutineScope {
                if (NavidromeManager.getCurrentServer() != null) {
                    val recentlyPlayedDeferred = async { getNavidromeAlbums("recent", 20) }
                    val recentDeferred = async { getNavidromeAlbums("newest", 20) }
                    val mostPlayedDeferred = async { getNavidromeAlbums("frequent", 20) }
                    val shuffledDeferred = async { getNavidromeAlbums("random", 20) }

                    _recentlyPlayedAlbums.value = recentlyPlayedDeferred.await()
                    _recentAlbums.value = recentDeferred.await()
                    _mostPlayedAlbums.value = mostPlayedDeferred.await()
                    _shuffledAlbums.value = shuffledDeferred.await()
                } else {
                    _recentlyPlayedAlbums.value = albumList.sortedBy { it.played }.take(20)
                    _recentAlbums.value = albumList.sortedBy { it.created }.take(20)
                    _mostPlayedAlbums.value = albumList.sortedBy { it.playCount }.take(20)
                    _shuffledAlbums.value = albumList.shuffled().take(20)
                }
            }
        }
    }
}