package com.craftworks.music.ui.viewmodels

import androidx.compose.ui.util.fastFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.craftworks.music.data.MediaData
import com.craftworks.music.data.albumList
import com.craftworks.music.managers.NavidromeManager
import com.craftworks.music.providers.navidrome.getNavidromeAlbums
import com.craftworks.music.providers.navidrome.searchNavidromeAlbums
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
                if (NavidromeManager.checkActiveServers()) {
                    val allAlbumsDeferred = async { getNavidromeAlbums("alphabeticalByName", 20) }

                    _allAlbums.value = allAlbumsDeferred.await()
                } else {
                    _allAlbums.value = albumList.sortedBy { it.name }
                }
            }
        }
    }

    suspend fun getMoreAlbums(sort: String? = "alphabeticalByName" , size: Int){
        val albumOffset = _allAlbums.value.size
        _allAlbums.value += getNavidromeAlbums(sort, size, albumOffset)
    }

    suspend fun search(query: String){
        if (NavidromeManager.checkActiveServers()){
            _allAlbums.value = searchNavidromeAlbums(query)
        }
        else {
            _allAlbums.value = albumList.fastFilter {
                        it.title?.lowercase()?.contains(query.lowercase()) == true ||
                        it.artist.lowercase().contains(query.lowercase())
            }
        }
    }
}