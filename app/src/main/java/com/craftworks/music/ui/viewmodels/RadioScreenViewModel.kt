package com.craftworks.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.craftworks.music.data.model.MediaModel
import com.craftworks.music.data.model.ProviderType
import com.craftworks.music.data.repository.RadioRepository
import com.craftworks.music.managers.DataRefreshManager
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

        viewModelScope.launch {
            DataRefreshManager.dataSourceChangedEvent.collect {
                getRadioStations()
            }
        }
    }

    fun getRadioStations() {
        viewModelScope.launch {
            _isLoading.value = true
            _radioStations.value = radioRepository.getRadios().map { it.toMediaItem() }
            _isLoading.value = false
        }
    }

    fun selectRadioStation(station: MediaItem) {
        _selectedRadioStation.value = station
    }

    fun addRadioStation(name: String, url: String, homepage: String) {
        viewModelScope.launch {
            _isLoading.value = true
            radioRepository.createRadio(name = name, streamUrl = url, homepageUrl = homepage)
            _isLoading.value = false
            getRadioStations()
        }
    }

    fun modifyRadioStation(providerId: String, id: String, name: String, url: String, homepage: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val modifiedRadio = MediaModel.InternetRadioStation(name = name, streamUrl =  url, homepageUrl = homepage)
            modifiedRadio.providerId = providerId
            modifiedRadio.id = id
            radioRepository.modifyRadio(modifiedRadio)
            _isLoading.value = false
            getRadioStations()
        }
    }

    fun deleteRadioStation(providerId: String, id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            radioRepository.deleteRadio(providerId, id)
            _isLoading.value = false
            getRadioStations()
        }
    }
}