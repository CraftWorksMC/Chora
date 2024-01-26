package com.craftworks.music

import android.content.Context
import android.util.Log
import com.craftworks.music.data.navidromeServersList
import com.craftworks.music.providers.local.getSongsOnDevice
import com.craftworks.music.providers.navidrome.getNavidromePlaylists
import com.craftworks.music.providers.navidrome.getNavidromeRadios
import com.craftworks.music.providers.navidrome.getNavidromeSongs
import com.craftworks.music.providers.navidrome.selectedNavidromeServer
import com.craftworks.music.providers.navidrome.useNavidromeServer
import com.craftworks.music.ui.screens.backgroundType
import com.craftworks.music.ui.screens.showMoreInfo
import com.craftworks.music.ui.screens.transcodingBitrate
import com.craftworks.music.ui.screens.username
import java.net.URL


class saveManager(private val context: Context){
    private val sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)

    fun saveSettings(){
        /* NAVIDROME */
        sharedPreferences.edit().putBoolean("useNavidrome", useNavidromeServer.value).apply()

        val listString = navidromeServersList.joinToString(separator = ";")
        sharedPreferences.edit().putString("navidromeServerList", listString).apply()

        sharedPreferences.edit().putString("transcodingBitRate", transcodingBitrate.value).apply()

        sharedPreferences.edit().putString("username", username.value).apply()
        sharedPreferences.edit().putString("backgroundType", backgroundType.value).apply()
        sharedPreferences.edit().putBoolean("showMoreInfo", showMoreInfo.value).apply()
    }

    fun loadSettings() {
        /* NAVIDROME SETTINGS */
        useNavidromeServer.value = sharedPreferences.getBoolean("useNavidrome", false)

        val listString = sharedPreferences.getString("navidromeServerList", null)
        if (!listString.isNullOrEmpty()) {
            listString.split(";")
        } else {
            emptyList()
        }

        transcodingBitrate.value = sharedPreferences.getString("transcodingBitRate", "No Transcoding") ?: "No Transcoding"

        if (useNavidromeServer.value && (selectedNavidromeServer.value?.username != "" || selectedNavidromeServer.value?.url !="" || selectedNavidromeServer.value?.url != ""))
            try {
                getNavidromeSongs(URL("${selectedNavidromeServer.value?.url}/rest/search3.view?query=''&songCount=10000&u=${selectedNavidromeServer.value?.username}&p=${selectedNavidromeServer.value?.url}&v=1.12.0&c=Chora"))
                getNavidromePlaylists()
                getNavidromeRadios()
            } catch (_: Exception){
                // DO NOTHING
            }
        else
            getSongsOnDevice(this@saveManager.context)

        /* PREFERENCES */
        username.value = sharedPreferences.getString("username", "Username") ?: "Username"
        backgroundType.value = sharedPreferences.getString("backgroundType", "Animated Blur") ?: "Animated Blur"
        showMoreInfo.value = sharedPreferences.getBoolean("showMoreInfo", true)

        Log.d("LOAD", "Loaded Settings!")
    }
}