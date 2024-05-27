package com.craftworks.music.providers.navidrome

import android.net.Uri
import android.util.Log
import com.craftworks.music.data.Playlist
import com.craftworks.music.data.Song
import com.craftworks.music.data.navidromeServersList
import com.craftworks.music.data.playlistList
import com.craftworks.music.data.selectedNavidromeServerIndex
import com.craftworks.music.data.songsList
import com.craftworks.music.ui.screens.selectedPlaylist
import com.gitlab.mvysny.konsumexml.konsumeXml
import java.net.HttpURLConnection
import java.net.URL

fun getNavidromePlaylists(){
    sendNavidromeGETRequest(
        navidromeServersList[selectedNavidromeServerIndex.intValue].url,
        navidromeServersList[selectedNavidromeServerIndex.intValue].username,
        navidromeServersList[selectedNavidromeServerIndex.intValue].password,
        "getPlaylists.view?"
    )
}
fun getNavidromePlaylistDetails(){
    sendNavidromeGETRequest(
        navidromeServersList[selectedNavidromeServerIndex.intValue].url,
        navidromeServersList[selectedNavidromeServerIndex.intValue].username,
        navidromeServersList[selectedNavidromeServerIndex.intValue].password,
        "getPlaylist.view?id=${selectedPlaylist?.navidromeID}"
    )
}

fun createNavidromePlaylist(playlistName: String){
    if (navidromeServersList.isEmpty()) return
    if (navidromeServersList[selectedNavidromeServerIndex.intValue].username == "" ||
        navidromeServersList[selectedNavidromeServerIndex.intValue].url == "" ||
        navidromeStatus.value != "ok") return

    val thread = Thread {
        try {
            val url =
                URL("${navidromeServersList[selectedNavidromeServerIndex.intValue].url}/rest/createPlaylist.view?&u=${navidromeServersList[selectedNavidromeServerIndex.intValue].username}&p=${navidromeServersList[selectedNavidromeServerIndex.intValue].password}&name=${playlistName}&v=1.12.0&c=Chora")

            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"  // optional default is GET

                Log.d("GET", "\nSent 'GET' request to URL : $url; Response Code : $responseCode")
            }

            getNavidromePlaylists()

        } catch (e: Exception) {
            Log.d("Exception", e.toString())
        }
    }
    thread.start()
}
fun deleteNavidromePlaylist(playlistID: String){
    if (navidromeServersList.isEmpty()) return
    if (navidromeServersList[selectedNavidromeServerIndex.intValue].username == "" ||
        navidromeServersList[selectedNavidromeServerIndex.intValue].url == "" ||
        navidromeStatus.value != "ok") return

    val thread = Thread {
        try {
            val url =
                URL("${navidromeServersList[selectedNavidromeServerIndex.intValue].url}/rest/deletePlaylist.view?&u=${navidromeServersList[selectedNavidromeServerIndex.intValue].username}&p=${navidromeServersList[selectedNavidromeServerIndex.intValue].password}&id=${playlistID}&v=1.12.0&c=Chora")

            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"  // optional default is GET

                Log.d("GET", "\nSent 'GET' request to URL : $url; Response Code : $responseCode")
            }

            getNavidromePlaylists()

        } catch (e: Exception) {
            Log.d("Exception", e.toString())
        }
    }
    thread.start()
}
fun addSongToNavidromePlaylist(playlistID: String, songID: String){
    if (navidromeServersList.isEmpty()) return
    if (navidromeServersList[selectedNavidromeServerIndex.intValue].username == "" ||
        navidromeServersList[selectedNavidromeServerIndex.intValue].url == "" ||
        navidromeStatus.value != "ok") return

    val thread = Thread {
        try {
            val url =
                URL("${navidromeServersList[selectedNavidromeServerIndex.intValue].url}/rest/updatePlaylist.view?&u=${navidromeServersList[selectedNavidromeServerIndex.intValue].username}&p=${navidromeServersList[selectedNavidromeServerIndex.intValue].password}&playlistId=${playlistID}&songIdToAdd=${songID}&v=1.12.0&c=Chora")

            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"  // optional default is GET

                Log.d("GET", "\nSent 'GET' request to URL : $url; Response Code : $responseCode")
            }

            getNavidromePlaylists()

        } catch (e: Exception) {
            Log.d("Exception", e.toString())
        }
    }
    thread.start()
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

                    if (playlistList.none { it.navidromeID == playlistID }) {
                        playlistList.add(playlist)
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
                val playlistSongs = mutableListOf<Song>()
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