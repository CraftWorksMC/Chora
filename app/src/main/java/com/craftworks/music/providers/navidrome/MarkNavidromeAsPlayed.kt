package com.craftworks.music.providers.navidrome

import com.craftworks.music.data.MediaData
import com.craftworks.music.managers.NavidromeManager
import com.craftworks.music.player.SongHelper
import com.craftworks.music.sliderPos

suspend fun markNavidromeSongAsPlayed(song: MediaData.Song){
    if (SongHelper.currentSong.isRadio == true || !NavidromeManager.checkActiveServers()) return

    println("Scrobble Percentage: ${(sliderPos.intValue.toFloat() / SongHelper.currentSong.duration.toFloat()) * 100f}, with sliderPos = ${sliderPos.intValue} | songDuration = ${SongHelper.currentSong.duration} | minPercentage = ${SongHelper.minPercentageScrobble}")
    if ((sliderPos.intValue.toFloat() / SongHelper.currentSong.duration.toFloat()) * 100f < SongHelper.minPercentageScrobble.intValue) return

    sendNavidromeGETRequest("scrobble.view?id=${song.navidromeID}&submission=true")
}