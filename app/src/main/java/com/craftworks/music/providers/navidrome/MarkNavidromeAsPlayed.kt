package com.craftworks.music.providers.navidrome

import com.craftworks.music.managers.NavidromeManager
import com.craftworks.music.player.SongHelper

suspend fun markNavidromeSongAsPlayed(navidromeId: String, playerPos: Float, duration: Float){
    if (!NavidromeManager.checkActiveServers()) return

    val scrobblePercentage = playerPos / (duration * 1000f)
    if (scrobblePercentage < SongHelper.minPercentageScrobble.intValue / 100) return

    println("Scrobble Percentage: ${scrobblePercentage * 100}, with sliderPos = $playerPos | songDuration = $duration | minPercentage = ${SongHelper.minPercentageScrobble.intValue}")
    sendNavidromeGETRequest("scrobble.view?id=$navidromeId&submission=true", true) // Never cache scrobbles
}