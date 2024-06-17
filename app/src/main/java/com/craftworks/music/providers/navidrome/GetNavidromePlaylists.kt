package com.craftworks.music.providers.navidrome

import android.net.Uri
import com.craftworks.music.data.MediaData
import com.craftworks.music.data.Playlist
import com.craftworks.music.data.navidromeServersList
import com.craftworks.music.data.playlistList
import com.craftworks.music.data.selectedNavidromeServerIndex
import com.craftworks.music.data.songsList
import com.craftworks.music.ui.screens.selectedPlaylist
import com.gitlab.mvysny.konsumexml.konsumeXml

suspend fun getNavidromePlaylists(){
    sendNavidromeGETRequest(
        navidromeServersList[selectedNavidromeServerIndex.intValue].url,
        navidromeServersList[selectedNavidromeServerIndex.intValue].username,
        navidromeServersList[selectedNavidromeServerIndex.intValue].password,
        "getPlaylists.view?"
    )
}
suspend fun getNavidromePlaylistDetails(){
    sendNavidromeGETRequest(
        navidromeServersList[selectedNavidromeServerIndex.intValue].url,
        navidromeServersList[selectedNavidromeServerIndex.intValue].username,
        navidromeServersList[selectedNavidromeServerIndex.intValue].password,
        "getPlaylist.view?id=${selectedPlaylist?.navidromeID}"
    )
}

suspend fun createNavidromePlaylist(playlistName: String){
    sendNavidromeGETRequest(
        navidromeServersList[selectedNavidromeServerIndex.intValue].url,
        navidromeServersList[selectedNavidromeServerIndex.intValue].username,
        navidromeServersList[selectedNavidromeServerIndex.intValue].password,
        "createPlaylist.view?name=$playlistName"
    )
}
suspend fun deleteNavidromePlaylist(playlistID: String){
    sendNavidromeGETRequest(
        navidromeServersList[selectedNavidromeServerIndex.intValue].url,
        navidromeServersList[selectedNavidromeServerIndex.intValue].username,
        navidromeServersList[selectedNavidromeServerIndex.intValue].password,
        "deletePlaylist.view?id=$playlistID"
    )
}
suspend fun addSongToNavidromePlaylist(playlistID: String, songID: String){
    sendNavidromeGETRequest(
        navidromeServersList[selectedNavidromeServerIndex.intValue].url,
        navidromeServersList[selectedNavidromeServerIndex.intValue].username,
        navidromeServersList[selectedNavidromeServerIndex.intValue].password,
        "updatePlaylist.view?playlistId=$playlistID&songIdToAdd=$songID"
    )
}

fun parseNavidromePlaylistsXML(response: String,
                               navidromeUrl: String,
                               navidromeUsername: String,
                               navidromePassword: String){

    // Avoid crashing by removing some useless tags.
    val newResponse = response
        .replace("xmlns=\"http://subsonic.org/restapi\" ", "")

    newResponse.konsumeXml().apply {
        child("subsonic-response") {
            child("playlists") {
                children("playlist"){
                    val passwordSalt = generateSalt(8)
                    val passwordHash = md5Hash(navidromePassword + passwordSalt)

                    val playlistID = attributes.getValue("id")
                    val playlistName = attributes.getValue("name")
                    val playlistArt = Uri.parse("$navidromeUrl/rest/getCoverArt.view?id=$playlistID&u=$navidromeUsername&t=$passwordHash&s=$passwordSalt&v=1.12.0&c=Chora")

                    val playlist = Playlist(
                        name = playlistName,
                        coverArt = playlistArt,
                        navidromeID = playlistID
                    )
                    synchronized(playlistList){
                        if (playlistList.none { it.navidromeID == playlistID }) {
                            playlistList.add(playlist)
                        }
                    }
                }
            }
        }
    }
}

fun parseNavidromePlaylistXML(response: String){

    // Avoid crashing by removing some useless tags.
    val newResponse = response
        .replace("xmlns=\"http://subsonic.org/restapi\" ", "")

    newResponse.konsumeXml().apply {
        child("subsonic-response") {
            child("playlist") {
                val playlistSongs = mutableListOf<MediaData.Song>()
                children("entry"){
                    val songID = attributes.getValue("id")
                    playlistSongs.add(songsList.first { it.navidromeID == songID })

                    skipContents()
                }
                selectedPlaylist = selectedPlaylist?.copy(songs = playlistSongs)
            }
        }
    }
}