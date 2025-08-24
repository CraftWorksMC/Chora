package com.craftworks.music.data.repository

import com.craftworks.music.data.datasource.local.LocalDataSource
import com.craftworks.music.data.datasource.navidrome.NavidromeDataSource
import com.craftworks.music.data.model.MediaData
import com.craftworks.music.managers.NavidromeManager
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RadioRepository @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val navidromeDataSource: NavidromeDataSource
) {

    suspend fun getRadios(ignoreCachedResponse: Boolean = false): List<MediaData.Radio> = coroutineScope {
        val deferredRadios = mutableListOf<Deferred<List<MediaData.Radio>>>()

        if (NavidromeManager.checkActiveServers())
            deferredRadios.add(async { navidromeDataSource.getNavidromeRadios(ignoreCachedResponse) })

        //if (LocalProviderManager.checkActiveFolders())
        deferredRadios.add(async { localDataSource.getLocalRadios() })

        deferredRadios.awaitAll().flatten()
    }

    suspend fun createRadio(name:String, url:String, homePage:String, addToNavidrome: Boolean) {
        if (addToNavidrome && NavidromeManager.checkActiveServers())
            navidromeDataSource.createNavidromeRadio(name, url, homePage)
        else
            localDataSource.createLocalRadio(name,url,homePage)
    }
    suspend fun modifyRadio(radio: MediaData.Radio) {
        if (NavidromeManager.checkActiveServers() && !radio.navidromeID.startsWith("Local_"))
            navidromeDataSource.updateNavidromeRadio(radio.navidromeID, radio.name, radio.media, radio.homePageUrl)
        else
            localDataSource.updateLocalRadio(radio)
    }
    suspend fun deleteRadio(id: String){
        if (NavidromeManager.checkActiveServers() && !id.startsWith("Local_"))
            navidromeDataSource.deleteNavidromeRadio(id)
        else
            localDataSource.deleteLocalRadio(id)
    }
}
