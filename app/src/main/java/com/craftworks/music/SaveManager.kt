package com.craftworks.music

import android.content.Context
import android.net.Uri
import android.util.Log
import com.craftworks.music.data.BottomNavItem
import com.craftworks.music.data.LocalProvider
import com.craftworks.music.data.MediaData
import com.craftworks.music.data.NavidromeProvider
import com.craftworks.music.data.Radio
import com.craftworks.music.data.artistList
import com.craftworks.music.data.bottomNavigationItems
import com.craftworks.music.data.localProviderList
import com.craftworks.music.data.navidromeServersList
import com.craftworks.music.data.playlistList
import com.craftworks.music.data.radioList
import com.craftworks.music.data.selectedLocalProvider
import com.craftworks.music.data.selectedNavidromeServerIndex
import com.craftworks.music.data.useNavidromeServer
import com.craftworks.music.providers.local.getSongsOnDevice
import com.craftworks.music.providers.local.localPlaylistImageGenerator
import com.craftworks.music.providers.navidrome.getNavidromeRadios
import com.craftworks.music.providers.navidrome.getNavidromeStatus
import com.craftworks.music.ui.elements.dialogs.backgroundType
import com.craftworks.music.ui.elements.dialogs.transcodingBitrate
import com.craftworks.music.ui.screens.showMoreInfo
import com.craftworks.music.ui.screens.username
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
        // Old junk that i'm keeping in case the new stuff doesn't work:
        // sharedPreferences.edit().putBoolean("useNavidrome", useNavidromeServer.value).apply()

        // Check if there are any enabled navidrome servers and save if there are any.
        val useNavidrome = navidromeServersList.any { it.enabled == true }
        sharedPreferences.edit().putBoolean("useNavidrome", useNavidrome).apply()

        //region Save Lists
        // Save Navidrome Server List
        val serverListString = navidromeServersList.joinToString(";") { "${it.url},${it.username},${it.password}" }
        sharedPreferences.edit().putString("navidromeServerList", serverListString).apply()

        // Save Local Provider List
        val localListString = localProviderList.joinToString(";") {
            "${it.directory},${it.enabled}" }
        sharedPreferences.edit().putString("localProviderList", localListString).apply()

        /* Save Artists List
        val artistsListString = artistList.joinToString(";") {
            "${it.name}|${it.artistImageUrl}|${it.navidromeID}" }
        sharedPreferences.edit().putString("artistsList", artistsListString).apply()
        */

        saveLocalRadios()

        saveLocalPlaylists()

        // Save Active Providers
        sharedPreferences.edit().putInt("activeNavidromeServer", selectedNavidromeServerIndex.intValue).apply()
        sharedPreferences.edit().putInt("activeLocalProvider", selectedLocalProvider.intValue).apply()

        saveBottomNavItems()

        //endregion

        // Preferences
        sharedPreferences.edit().putString("username", username.value).apply()
        sharedPreferences.edit().putString("backgroundType", backgroundType.value).apply()
        sharedPreferences.edit().putBoolean("showMoreInfo", showMoreInfo.value).apply()
        sharedPreferences.edit().putString("transcodingBitRate", transcodingBitrate.value).apply()
    }

    //region Save Single Components

    fun saveBottomNavItems(){
        val navItems = bottomNavigationItems.joinToString(";") {
            "${it.title}|${it.icon}|${it.screenRoute}|${it.enabled}" }
        sharedPreferences.edit().putString("bottomNavItems", navItems).apply()
    }
    fun saveLocalRadios(){
        val radiosListString = radioList.joinToString(";") {
            if (it.navidromeID == "Local")
                "${it.name},${it.media},${it.homepageUrl},${it.imageUrl},${it.navidromeID}"
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

            loadNavidromeProviders()
            loadLocalProviders()

            loadRadios()
            loadPlaylists()

            if (useNavidromeServer.value) {
                launch { getNavidromeStatus(
                    navidromeServersList[selectedNavidromeServerIndex.intValue].url,
                    navidromeServersList[selectedNavidromeServerIndex.intValue].username,
                    navidromeServersList[selectedNavidromeServerIndex.intValue].password,
                ) }
                //launch { songsList.addAll(getNavidromeSongs()) }
                //launch { albumList.addAll(getNavidromeAlbums()) }
                //launch { getNavidromePlaylists() }
                //launch { getNavidromeRadios() }
                //launch { artistList.addAll(getNavidromeArtists()) }
            }

            if (localProviderList.isNotEmpty()) {
                if (localProviderList[selectedLocalProvider.intValue].enabled)
                    launch {getSongsOnDevice(context) }
            }

            if (navidromeServersList.isEmpty() && localProviderList.isEmpty()) showNoProviderDialog.value = true
        }

        // Finished Loading Settings
        Log.d("LOAD", "Loaded Settings!")
    }

    //region Load Single Components

    fun loadPreferences(){
        Log.d("LOAD", "Loading Preferences")

        username.value = sharedPreferences.getString("username", "Username") ?: "Username"
        backgroundType.value = sharedPreferences.getString("backgroundType", "Animated Blur") ?: "Animated Blur"
        showMoreInfo.value = sharedPreferences.getBoolean("showMoreInfo", true)
        transcodingBitrate.value = sharedPreferences.getString("transcodingBitRate", "No Transcoding") ?: "No Transcoding"
    }
    fun loadBottomNavItems(){
        Log.d("LOAD", "Loading Bottom Nav Items")

        // Get Artists List
        val bottomNavItemsString = (sharedPreferences.getString("bottomNavItems", "") ?: "").split(";")
        bottomNavItemsString.forEach { bottomNavItem ->
            val parts = bottomNavItem.split("|")
            if (parts.size > 1) {
                try {
                    val navItem = BottomNavItem(
                        parts[0],
                        parts[1].toInt(),
                        parts[2],
                        parts[3].toBoolean()
                    )
                    val index = bottomNavItemsString.indexOf(bottomNavItem)
                    bottomNavigationItems[index] = navItem
                }
                catch (e:Exception){
                    println("Failed to add all artists, motive: $e")
                }
            }
        }
    }
    fun loadNavidromeProviders(){
        Log.d("LOAD", "Loading Navidrome Providers")

        useNavidromeServer.value = sharedPreferences.getBoolean("useNavidrome", false)

        val navidromeStrings = (sharedPreferences.getString("navidromeServerList", "") ?: "").split(";")
        navidromeStrings.forEach { navidromeString ->
            val parts = navidromeString.split(",")
            if (parts.size == 3) {
                val navidromeProvider = NavidromeProvider(parts[0], parts[1], parts[2])
                if (navidromeServersList.contains(navidromeProvider)) return
                navidromeServersList.add(navidromeProvider)
            }
        }
        selectedNavidromeServerIndex.intValue = sharedPreferences.getInt("activeNavidromeServer", 0)
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

    /*
    private fun loadArtists(){
        Log.d("LOAD", "Loading Cached Artists")

        // Get Artists List
        val artistListStrings = (sharedPreferences.getString("artistsList", "") ?: "").split(";")
        artistListStrings.forEach { localString ->
            val parts = localString.split("|")
            if (parts.size > 1) {
                try {
                    val artist = MediaData.Artist(
                        parts[0],
                        parts[1],
                        parts[2],
                    )
                    if (artistList.contains(artistList.firstOrNull { it.name == artist.name && it.navidromeID == "Local"})){
                        artistList[artistList.indexOfFirst { it.name == artist.name && it.navidromeID == "Local" }].apply {
                            navidromeID = artist.navidromeID
                        }
                    }
                    else{
                        if (!artistList.contains(artistList.firstOrNull { it.name == artist.name }))
                            artistList.add(artist)
                    }
                }
                catch (e:Exception){
                    println("Failed to add all artists, motive: $e")
                }

            }
        }
    } */

    fun loadRadios(){
        Log.d("LOAD", "Loading Radios")

        // Get Radios List
        val radioListStrings = (sharedPreferences.getString("radioList", "") ?: "").split(";")
        radioListStrings.forEach { localString ->
            val parts = localString.split(",")
            if (parts.size > 1) {
                val radio = Radio(
                    parts[0],
                    Uri.parse(parts[1]),
                    parts[2],
                    Uri.parse(parts[3]),
                    parts[4]
                )
                if (radioList.contains(radio)) return
                radioList.add(radio)
            }
        }
        if (useNavidromeServer.value){
            scope.launch {
                getNavidromeRadios()
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