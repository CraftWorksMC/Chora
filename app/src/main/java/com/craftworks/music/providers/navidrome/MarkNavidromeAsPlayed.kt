package com.craftworks.music.providers.navidrome

import android.util.Log
import com.craftworks.music.data.Song
import java.net.HttpURLConnection
import java.net.URL

fun markSongAsPlayed(song: Song){
    if (useNavidromeServer.value) {
        val thread = Thread {
            try {
                val url =
                    URL("${selectedNavidromeServer.value?.url}/rest/scrobble.view?id=${song.navidromeID}&submission=true&u=${selectedNavidromeServer.value?.username}&p=${selectedNavidromeServer.value?.url}&v=1.12.0&c=Chora")
                with(url.openConnection() as HttpURLConnection) {
                    requestMethod = "GET"  // optional default is GET
                    println("\nSent 'GET' request to URL : $url; Response Code : $responseCode")
                    inputStream.bufferedReader().use {
                        Log.d("PlayedTimes", it.toString())
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        thread.start()
    }
}