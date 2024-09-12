package com.craftworks.music

import android.content.Context
import android.util.Log
import com.craftworks.music.data.LocalProvider
import com.craftworks.music.data.MediaData
import com.craftworks.music.data.localProviderList
import com.craftworks.music.data.playlistList
import com.craftworks.music.data.radioList
import com.craftworks.music.data.selectedLocalProvider
import com.craftworks.music.providers.local.getSongsOnDevice
import com.craftworks.music.providers.local.localPlaylistImageGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class saveManager(private val context: Context){
    private val sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)

    private val scope = CoroutineScope(Job() + Dispatchers.Default)

    fun saveSettings(){

        // Save Local Provider List
        val localListString = localProviderList.joinToString(";") {
            "${it.directory},${it.enabled}" }
        sharedPreferences.edit().putString("localProviderList", localListString).apply()

        saveLocalRadios()

        saveLocalPlaylists()

        // Save Active Providers
        //sharedPreferences.edit().putInt("activeNavidromeServer", selectedNavidromeServerIndex.intValue).apply()
        //sharedPreferences.edit().putInt("activeLocalProvider", selectedLocalProvider.intValue).apply()
    }

    //region Save Single Components
    fun saveLocalRadios(){
        val radiosListString = radioList.joinToString(";") {
            if (it.navidromeID == "Local")
                "${it.navidromeID},${it.name},${it.media},${it.homePageUrl}"
            else
                ""}
        sharedPreferences.edit().putString("radioList", radiosListString).apply()
    }
    fun saveLocalPlaylists(){
        val playlistJson = Json.encodeToString(playlistList)
        sharedPreferences.edit().putString("playlistList", playlistJson).apply()
    }

    //endregion

    suspend fun loadSettings() {
        Log.d("LOAD", "Started Loading Settings!")

        coroutineScope {

            //loadBottomNavItems()
            loadLocalProviders()
            loadRadios()
            loadPlaylists()

            if (localProviderList.isNotEmpty()) {
                if (localProviderList[selectedLocalProvider.intValue].enabled)
                    launch {getSongsOnDevice(context) }
            }
        }

        // Finished Loading Settings
        Log.d("LOAD", "Loaded Settings!")
    }

    fun loadLocalProviders(){
        Log.d("LOAD", "Loading Local Providers")

        // Get Local Providers List
        val localListStrings = (sharedPreferences.getString("localProviderList", "") ?: "").split(";")
        localListStrings.forEach { localString ->
            val parts = localString.split(",")
            if (parts.size == 2) {
                val localProvider = LocalProvider(parts[0], parts[1].toBoolean())
                if (localProviderList.contains(localProvider)) return
                localProviderList.add(localProvider)
            }
        }
        selectedLocalProvider.intValue = sharedPreferences.getInt("activeLocalProvider", 0)
    }

    fun loadRadios(){
        Log.d("LOAD", "Loading Radios")

        // Get Radios List
        val radioListStrings = (sharedPreferences.getString("radioList", "") ?: "").split(";")
        radioListStrings.forEach { localString ->
            val parts = localString.split(",")
            if (parts.size > 1) {
                val radio = MediaData.Radio(
                    parts[0],
                    parts[1],
                    parts[2],
                    parts[3]
                )
                if (radioList.contains(radio)) return
                radioList.add(radio)
            }
        }
    }
    fun loadPlaylists(){
        Log.d("LOAD", "Loading Offline Playlists")

        // Get Local Playlists
        scope.launch {
            val savedPlaylistJson = sharedPreferences.getString("playlistList", null)
            playlistList = savedPlaylistJson?.let { Json.decodeFromString<List<MediaData.Playlist>>(it) }?.toMutableList() ?: mutableListOf()

            playlistList.forEach { playlist ->
                playlist.coverArt = localPlaylistImageGenerator(playlist.songs ?: emptyList(), context).toString()
            }
        }
    }

    //endregion
}