package com.craftworks.music

import android.content.Context
import android.util.Log
import com.craftworks.music.navidrome.getNavidromePlaylists
import com.craftworks.music.navidrome.getNavidromeRadios
import com.craftworks.music.navidrome.getNavidromeSongs
import com.craftworks.music.navidrome.navidromePassword
import com.craftworks.music.navidrome.navidromeServerIP
import com.craftworks.music.navidrome.navidromeUsername
import com.craftworks.music.ui.screens.backgroundType
import com.craftworks.music.ui.screens.showMoreInfo
import com.craftworks.music.ui.screens.transcodingBitrate
import com.craftworks.music.ui.screens.useNavidromeServer
import com.craftworks.music.ui.screens.username
import java.net.URL

class saveManager(private val context: Context){
    private val sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)

    fun saveSettings(){
        /* NAVIDROME */
        sharedPreferences.edit().putBoolean("useNavidrome", useNavidromeServer.value).apply()
        sharedPreferences.edit().putString("navidromeServerIP", navidromeServerIP.value).apply()
        sharedPreferences.edit().putString("navidromeUsername", navidromeUsername.value).apply()
        sharedPreferences.edit().putString("navidromePassword", navidromePassword.value).apply()
        sharedPreferences.edit().putString("transcodingBitRate", transcodingBitrate.value).apply()

        sharedPreferences.edit().putString("username", username.value).apply()
        sharedPreferences.edit().putString("backgroundType", backgroundType.value).apply()
        sharedPreferences.edit().putBoolean("showMoreInfo", showMoreInfo.value).apply()
    }

    fun loadSettings() {
        /* NAVIDROME SETTINGS */
        useNavidromeServer.value = sharedPreferences.getBoolean("useNavidrome", false)
        navidromeServerIP.value = sharedPreferences.getString("navidromeServerIP", "") ?: ""
        navidromeUsername.value = sharedPreferences.getString("navidromeUsername", "") ?: ""
        navidromePassword.value = sharedPreferences.getString("navidromePassword", "") ?: ""
        transcodingBitrate.value = sharedPreferences.getString("transcodingBitRate", "No Transcoding") ?: "No Transcoding"

        if (useNavidromeServer.value && (navidromeUsername.value != "" || navidromePassword.value !="" || navidromeServerIP.value != ""))
            try {
                getNavidromeSongs(URL("${navidromeServerIP.value}/rest/search3.view?query=''&songCount=10000&u=${navidromeUsername.value}&p=${navidromePassword.value}&v=1.12.0&c=Chora"))
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