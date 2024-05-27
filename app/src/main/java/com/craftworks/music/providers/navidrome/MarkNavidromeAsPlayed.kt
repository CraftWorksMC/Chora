package com.craftworks.music.providers.navidrome

import com.craftworks.music.data.Song
import com.craftworks.music.data.navidromeServersList
import com.craftworks.music.data.selectedNavidromeServerIndex
import com.craftworks.music.data.useNavidromeServer
import com.craftworks.music.player.SongHelper
import com.craftworks.music.sliderPos

fun markNavidromeSongAsPlayed(song: Song){
    if (SongHelper.currentSong.isRadio == true || !useNavidromeServer.value) return

    println("Scrobble Percentage: ${(sliderPos.intValue.toFloat() / SongHelper.currentSong.duration.toFloat()) * 100f}, with sliderPos = ${sliderPos.intValue} | songDuration = ${SongHelper.currentSong.duration} | minPercentage = ${SongHelper.minPercentageScrobble}")
    if ((sliderPos.intValue.toFloat() / SongHelper.currentSong.duration.toFloat()) * 100f < SongHelper.minPercentageScrobble.intValue) return

    sendNavidromeGETRequest(
        navidromeServersList[selectedNavidromeServerIndex.intValue].url,
        navidromeServersList[selectedNavidromeServerIndex.intValue].username,
        navidromeServersList[selectedNavidromeServerIndex.intValue].password,
        "scrobble.view?id=${song.navidromeID}&submission=true"
    )
}