package com.craftworks.music.providers.navidrome

import com.craftworks.music.player.SongHelper
import com.craftworks.music.data.Song
import com.craftworks.music.data.navidromeServersList
import com.craftworks.music.data.selectedNavidromeServerIndex
import com.craftworks.music.data.useNavidromeServer
import com.craftworks.music.sliderPos
import java.net.HttpURLConnection
import java.net.URL

fun markNavidromeSongAsPlayed(song: Song){
        val thread = Thread {
        try {
            println("Scrobble: Marking ${SongHelper.currentSong.title} as played! | isRadio: ${SongHelper.currentSong.isRadio} | useNavidromeServer: ${useNavidromeServer.value}")
            if (SongHelper.currentSong.isRadio == true || !useNavidromeServer.value) return@Thread

            // Scrobble Percentage
            println("Scrobble Percentage: ${(sliderPos.intValue.toFloat() / SongHelper.currentSong.duration.toFloat()) * 100f}, with sliderPos = ${sliderPos.intValue} | songDuration = ${SongHelper.currentSong.duration} | minPercentage = ${SongHelper.minPercentageScrobble}")
            if ((sliderPos.intValue.toFloat() / SongHelper.currentSong.duration.toFloat()) * 100f < SongHelper.minPercentageScrobble.intValue) return@Thread

            println("Scrobble Checks Passed. Contacting Navidrome Server")

            val url =
                URL("${navidromeServersList[selectedNavidromeServerIndex.intValue].url}/rest/scrobble.view?id=${song.navidromeID}&submission=true&u=${navidromeServersList[selectedNavidromeServerIndex.intValue].username}&p=${navidromeServersList[selectedNavidromeServerIndex.intValue].password}&v=1.12.0&c=Chora")

            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"  // optional default is GET
                println("\nSent 'GET' request to URL : $url; Response Code : $responseCode")
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    thread.start()
}