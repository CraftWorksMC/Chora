package com.craftworks.music

import android.content.Context
import android.util.Log
import com.craftworks.music.data.Navidrome
import com.craftworks.music.data.navidromeServersList
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
        /* NAVIDROME */
        sharedPreferences.edit().putBoolean("useNavidrome", useNavidromeServer.value).apply()

        val serverListString = navidromeServersList.joinToString(";") { "${it.url},${it.username},${it.password}" }
        sharedPreferences.edit().putString("navidromeServerList", serverListString).apply()
        println(serverListString)

        sharedPreferences.edit().putInt("activeNavidromeServer", selectedNavidromeServerIndex.intValue).apply()

        sharedPreferences.edit().putString("transcodingBitRate", transcodingBitrate.value).apply()

        sharedPreferences.edit().putString("username", username.value).apply()
        sharedPreferences.edit().putString("backgroundType", backgroundType.value).apply()
        sharedPreferences.edit().putBoolean("showMoreInfo", showMoreInfo.value).apply()
    }

    fun loadSettings() {
        /* PREFERENCES */
        username.value = sharedPreferences.getString("username", "Username") ?: "Username"
        backgroundType.value = sharedPreferences.getString("backgroundType", "Animated Blur") ?: "Animated Blur"
        showMoreInfo.value = sharedPreferences.getBoolean("showMoreInfo", true)

        /* NAVIDROME SETTINGS */
        //useNavidromeServer.value = sharedPreferences.getBoolean("useNavidrome", false)

        val serverListString = sharedPreferences.getString("navidromeServerList", "") ?: ""
        println(serverListString)
        val navidromeStrings = serverListString.split(";")
        println(navidromeStrings)
        navidromeStrings.forEach { navidromeString ->
            val parts = navidromeString.split(",")
            if (parts.size == 3) {
                val navidrome = Navidrome(parts[0], parts[1], parts[2])
                navidromeServersList.add(navidrome)
                println(navidrome)
            }
        }

        selectedNavidromeServerIndex.intValue = sharedPreferences.getInt("activeNavidromeServer", 0)
        println(selectedNavidromeServerIndex.intValue)

        transcodingBitrate.value = sharedPreferences.getString("transcodingBitRate", "No Transcoding") ?: "No Transcoding"

        if (navidromeServersList.isEmpty()) return

        if (useNavidromeServer.value && (navidromeServersList[selectedNavidromeServerIndex.intValue].username != "" || navidromeServersList[selectedNavidromeServerIndex.intValue].url !="" || navidromeServersList[selectedNavidromeServerIndex.intValue].url != ""))
            try {
                getNavidromeSongs(URL("${navidromeServersList[selectedNavidromeServerIndex.intValue].url}/rest/search3.view?query=''&songCount=10000&u=${navidromeServersList[selectedNavidromeServerIndex.intValue].username}&p=${navidromeServersList[selectedNavidromeServerIndex.intValue].password}&v=1.12.0&c=Chora"))
                getNavidromePlaylists()
                getNavidromeRadios()
            } catch (_: Exception){
                // DO NOTHING
            }
        else
            getSongsOnDevice(this@saveManager.context)

        Log.d("LOAD", "Loaded Settings!")
    }
}