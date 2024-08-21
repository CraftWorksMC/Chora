package com.craftworks.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.craftworks.music.data.MediaData
import com.craftworks.music.data.radioList
import com.craftworks.music.providers.navidrome.NavidromeManager
import com.craftworks.music.providers.navidrome.getNavidromeAlbums
import com.craftworks.music.providers.navidrome.getNavidromeRadios
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

//
//class RadioScreenViewModel : ViewModel() {
//    private val _allRadios = MutableStateFlow<List<MediaData.Radio>>(emptyList())
//    val allRadios: StateFlow<List<MediaData.Radio>> = _allRadios.asStateFlow()
//
//    init {
//        fetchRadios()
//    }
//
//    fun fetchRadios() {
//        viewModelScope.launch {
//            coroutineScope {
//                if (NavidromeManager.getCurrentServer() != null) {
//                    val allRadiosDeferred  = async { getNavidromeRadios() }
//
//                    _allRadios.value = allRadiosDeferred.await()
//                }
//                else {
//                    _allRadios.value = radioList.sortedBy { it.name }
//                }
//            }
//        }
//    }
//}