package com.craftworks.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.craftworks.music.data.model.MediaData
import com.craftworks.music.data.model.toMediaItem
import com.craftworks.music.data.repository.RadioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RadioScreenViewModel @Inject constructor(
    private val radioRepository: RadioRepository
) : ViewModel() {

    private val _radioStations = MutableStateFlow<List<MediaItem>>(emptyList())
    val radioStations: StateFlow<List<MediaItem>> = _radioStations.asStateFlow()

    private val _selectedRadioStation = MutableStateFlow<MediaItem?>(null)
    val selectedRadioStation: StateFlow<MediaItem?> = _selectedRadioStation.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        getRadioStations()
    }

    fun getRadioStations() {
        viewModelScope.launch {
            _isLoading.value = true
            _radioStations.value = radioRepository.getRadios(ignoreCachedResponse = true).map { it.toMediaItem() }
            _isLoading.value = false
        }
    }

    fun selectRadioStation(station: MediaItem) {
        _selectedRadioStation.value = station
    }

    fun addRadioStation(name: String, url: String, homepage: String, addToNavidrome: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            radioRepository.createRadio(name = name, url = url, homePage = homepage, addToNavidrome = addToNavidrome)
            _isLoading.value = false
        }
    }

    fun modifyRadioStation(id: String, name: String, url: String, homepage: String) {
        viewModelScope.launch {
            _isLoading.value = true
            radioRepository.modifyRadio(MediaData.Radio(id, name, url, homepage))
            _isLoading.value = false
        }
    }

    fun deleteRadioStation(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            radioRepository.deleteRadio(id)
            _isLoading.value = false
        }
    }
}