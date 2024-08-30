package com.craftworks.music.ui.viewmodels

import androidx.compose.ui.util.fastFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.craftworks.music.data.MediaData
import com.craftworks.music.data.songsList
import com.craftworks.music.managers.NavidromeManager
import com.craftworks.music.providers.navidrome.getNavidromeSongs
import com.craftworks.music.providers.navidrome.sendNavidromeGETRequest
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
                    songsList = _allSongs.value.toMutableList()
                } else {
                    _allSongs.value = songsList.sortedBy { it.title }
                }
            }
        }
    }
    suspend fun getMoreSongs(size: Int){
        val songOffset = _allSongs.value.size
        _allSongs.value += sendNavidromeGETRequest("search3.view?query=''&songCount=$size&songOffset=$songOffset&artistCount=0&albumCount=0&f=json").filterIsInstance<MediaData.Song>()
    }

    suspend fun search(query: String){
        if (NavidromeManager.checkActiveServers()){
            _allSongs.value = sendNavidromeGETRequest("search3.view?query=${query}&songCount=500&artistCount=0&albumCount=0&f=json").filterIsInstance<MediaData.Song>()
        } else{
            _allSongs.value = _allSongs.value.fastFilter {
                        it.title.lowercase().contains(query.lowercase()) ||
                        it.artist.lowercase().contains(query.lowercase())
            }
        }
    }
}