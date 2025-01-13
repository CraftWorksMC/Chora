package com.craftworks.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.craftworks.music.data.MediaData
import com.craftworks.music.providers.getAlbums
import com.craftworks.music.providers.searchAlbum
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
                val allAlbumsDeferred = async { getAlbums("alphabeticalByName", 20) }

                _allAlbums.value = allAlbumsDeferred.await()
            }
        }
    }

    fun getMoreAlbums(sort: String? = "alphabeticalByName" , size: Int){
        viewModelScope.launch {
            val albumOffset = _allAlbums.value.size
            val newAlbums = getAlbums(sort, size, albumOffset)
            _allAlbums.value += newAlbums
        }
    }

    suspend fun search(query: String){
        _allAlbums.value = searchAlbum(query)
    }
}