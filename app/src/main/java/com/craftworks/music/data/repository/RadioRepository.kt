package com.craftworks.music.data.repository

import com.craftworks.music.data.model.MediaModel
import com.craftworks.music.managers.MediaProviderManager
import kotlinx.coroutines.coroutineScope
import javax.inject.Singleton

@Singleton
class RadioRepository {

    suspend fun getRadios(): List<MediaModel.InternetRadioStation> = coroutineScope {
        MediaProviderManager.currentProvider.value?.getInternetRadioStations() ?: listOf()
    }

    suspend fun createRadio(name:String, streamUrl:String, homepageUrl:String) {
        MediaProviderManager.currentProvider.value?.createInternetRadioStation(name, streamUrl, homepageUrl)
    }

    suspend fun modifyRadio(radio: MediaModel.InternetRadioStation) {
        MediaProviderManager.getProvider(radio.providerId)?.updateInternetRadioStation(radio.id,radio.name, radio.streamUrl, radio.homepageUrl)
    }

    suspend fun deleteRadio(providerId: String, id: String){
        MediaProviderManager.getProvider(providerId)?.deleteInternetRadioStation(id)
    }
}
