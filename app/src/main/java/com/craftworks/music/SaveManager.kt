package com.craftworks.music

import android.content.Context
import android.net.Uri
import android.util.Log
import com.craftworks.music.data.LocalProvider
import com.craftworks.music.data.NavidromeProvider
import com.craftworks.music.data.Radio
import com.craftworks.music.data.localProviderList
import com.craftworks.music.data.navidromeServersList
import com.craftworks.music.data.radioList
import com.craftworks.music.data.selectedLocalProvider
import com.craftworks.music.providers.local.getSongsOnDevice
import com.craftworks.music.providers.navidrome.getNavidromePlaylists
import com.craftworks.music.providers.navidrome.getNavidromeRadios
import com.craftworks.music.providers.navidrome.getNavidromeSongs
import com.craftworks.music.providers.navidrome.selectedNavidromeServerIndex
import com.craftworks.music.providers.navidrome.useNavidromeServer
import com.craftworks.music.ui.screens.backgroundType
import com.craftworks.music.ui.screens.showMoreInfo
import com.craftworks.music.ui.screens.transcodingBitrate
import com.craftworks.music.ui.screens.username
import java.net.URL


class saveManager(private val context: Context){
    private val sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)

    fun saveSettings(){
        sharedPreferences.edit().putBoolean("useNavidrome", useNavidromeServer.value).apply()
        // Save Navidrome Server List
        val serverListString = navidromeServersList.joinToString(";") { "${it.url},${it.username},${it.password}" }
        sharedPreferences.edit().putString("navidromeServerList", serverListString).apply()

        // Save Local Provider List
        val localListString = localProviderList.joinToString(";") { "${it.directory},${it.enabled}" }
        sharedPreferences.edit().putString("localProviderList", localListString).apply()

        // Save Radios List
        val radiosListString = radioList.joinToString(";") {
            "${it.name},${it.media},${it.homepageUrl},${it.imageUrl},${it.navidromeID}"
        }
        sharedPreferences.edit().putString("radioList", radiosListString).apply()

        // Save Active Providers
        sharedPreferences.edit().putInt("activeNavidromeServer", selectedNavidromeServerIndex.intValue).apply()
        sharedPreferences.edit().putInt("activeLocalProvider", selectedLocalProvider.intValue).apply()


        // Preferences
        sharedPreferences.edit().putString("username", username.value).apply()
        sharedPreferences.edit().putString("backgroundType", backgroundType.value).apply()
        sharedPreferences.edit().putBoolean("showMoreInfo", showMoreInfo.value).apply()
        sharedPreferences.edit().putString("transcodingBitRate", transcodingBitrate.value).apply()


    }

    fun loadSettings() {
        //region Preferences
        username.value = sharedPreferences.getString("username", "Username") ?: "Username"
        backgroundType.value = sharedPreferences.getString("backgroundType", "Animated Blur") ?: "Animated Blur"
        showMoreInfo.value = sharedPreferences.getBoolean("showMoreInfo", true)
        transcodingBitrate.value = sharedPreferences.getString("transcodingBitRate", "No Transcoding") ?: "No Transcoding"
        //endregion

        //region Navidrome

        useNavidromeServer.value = sharedPreferences.getBoolean("useNavidrome", false)

        // Get Navidrome Server List
        val serverListString = sharedPreferences.getString("navidromeServerList", "") ?: ""
        val navidromeStrings = serverListString.split(";")
        navidromeStrings.forEach { navidromeString ->
            val parts = navidromeString.split(",")
            if (parts.size == 3) {
                val navidromeProvider = NavidromeProvider(parts[0], parts[1], parts[2])
                navidromeServersList.add(navidromeProvider)
                println(navidromeProvider)
            }
        }
        selectedNavidromeServerIndex.intValue = sharedPreferences.getInt("activeNavidromeServer", 0)

        //endregion

        //region Local
        // Get Local Providers List
        val localListString = sharedPreferences.getString("localProviderList", "") ?: ""
        val localListStrings = localListString.split(";")
        println(localListStrings)
        localListStrings.forEach { localString ->
            val parts = localString.split(",")
            if (parts.size == 2) {
                val localProvider = LocalProvider(parts[0], parts[1].toBoolean())
                localProviderList.add(localProvider)
                println(localProvider)
            }
        }
        selectedLocalProvider.intValue = sharedPreferences.getInt("activeLocalProvider", 0)
        //endregion

        //region Radios
        // Get Radios List
        val radioListString = sharedPreferences.getString("radioList", "") ?: ""
        val radioListStrings = radioListString.split(";")
        println(radioListStrings)
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
                radioList.add(radio)
            }
        }
        //endregion

        // Get Media Items
        if (useNavidromeServer.value && (
                    navidromeServersList.isNotEmpty() && (
                            navidromeServersList[selectedNavidromeServerIndex.intValue].username != "" ||
                            navidromeServersList[selectedNavidromeServerIndex.intValue].url !="" ||
                            navidromeServersList[selectedNavidromeServerIndex.intValue].url != "")
                    )
            )
            try {
                if (localProviderList[selectedLocalProvider.intValue].enabled)
                    getSongsOnDevice(context)
                getNavidromeSongs(URL("${navidromeServersList[selectedNavidromeServerIndex.intValue].url}/rest/search3.view?query=''&songCount=10000&u=${navidromeServersList[selectedNavidromeServerIndex.intValue].username}&p=${navidromeServersList[selectedNavidromeServerIndex.intValue].password}&v=1.12.0&c=Chora"))
                getNavidromePlaylists()
                getNavidromeRadios()
            } catch (_: Exception){
                // DO NOTHING
            }
        else if (localProviderList.isNotEmpty())
            getSongsOnDevice(this@saveManager.context)

        // Finished Loading Settings
        Log.d("LOAD", "Loaded Settings!")
    }
}