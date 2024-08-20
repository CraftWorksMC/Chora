package com.craftworks.music.ui.viewmodels

import com.craftworks.music.data.radioList
import com.craftworks.music.providers.navidrome.NavidromeManager

//
class RadioScreenViewModel : ViewModel() {
    private val _allRadios = MutableStateFlow<List<MediaData.Song>>(emptyList())
    val allRadios: StateFlow<List<MediaData.Album>> = _allRadios.asStateFlow()

    init {
        fetchRadios()
    }

    fun fetchRadios() {
        viewModelScope.launch {
            coroutineScope {
                if (NavidromeManager.getCurrentServer() != null) {
                    val allRadiosDeferred  = async { getNavidromeAlbums("recent", 20) }

                    _allRadios.value = allRadiosDeferred.await()
                }
                else {
                    _allRadios.value = radioList.sortedBy { it.name }
                }
            }
        }
    }
}