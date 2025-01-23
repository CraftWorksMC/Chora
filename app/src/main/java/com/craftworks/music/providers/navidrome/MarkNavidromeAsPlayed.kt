package com.craftworks.music.providers.navidrome

import com.craftworks.music.managers.NavidromeManager
import com.craftworks.music.player.SongHelper
import com.craftworks.music.sliderPos

suspend fun markNavidromeSongAsPlayed(navidromeId: String){
    if (SongHelper.currentSong.isRadio == true || !NavidromeManager.checkActiveServers()) return

    val scrobblePercentage = sliderPos.intValue.toFloat() / (SongHelper.currentSong.duration.toFloat() * 1000f)
    println("Scrobble Percentage: ${scrobblePercentage * 100}, with sliderPos = ${sliderPos.intValue} | songDuration = ${SongHelper.currentSong.duration * 1000} | minPercentage = ${SongHelper.minPercentageScrobble.intValue}")

    if (scrobblePercentage < SongHelper.minPercentageScrobble.intValue / 100) return

    sendNavidromeGETRequest("scrobble.view?id=$navidromeId&submission=true", true) // Never cache scrobbles
}