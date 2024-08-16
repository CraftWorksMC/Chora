package com.craftworks.music.providers.navidrome

import android.util.Log
import com.craftworks.music.data.MediaData
import com.craftworks.music.data.playlistList
import com.craftworks.music.data.songsList
import com.craftworks.music.ui.screens.selectedPlaylist
import com.gitlab.mvysny.konsumexml.konsumeXml
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

@Serializable
data class PlaylistContainer(val playlist: List<MediaData.Playlist>? = listOf())

suspend fun getNavidromePlaylists() : List<MediaData.Playlist> {
    return sendNavidromeGETRequest("getPlaylists.view?f=json").filterIsInstance<MediaData.Playlist>()
}
suspend fun getNavidromePlaylistDetails(id: String) : List<MediaData.Playlist> {
    return sendNavidromeGETRequest("getPlaylist.view?id=$id&f=json").filterIsInstance<MediaData.Playlist>()
}

suspend fun createNavidromePlaylist(playlistName: String){
    sendNavidromeGETRequest("createPlaylist.view?name=$playlistName")
}
suspend fun deleteNavidromePlaylist(playlistID: String){
    sendNavidromeGETRequest("deletePlaylist.view?id=$playlistID")
}
suspend fun addSongToNavidromePlaylist(playlistID: String, songID: String){
    sendNavidromeGETRequest("updatePlaylist.view?playlistId=$playlistID&songIdToAdd=$songID")
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
                selectedPlaylist = selectedPlaylist.copy(songs = playlistSongs)
            }
        }
    }
}

fun parseNavidromePlaylistsJSON(
    response: String,
    navidromeUrl: String,
    navidromeUsername: String,
    navidromePassword: String
) : List<MediaData.Playlist> {

    val jsonParser = Json { ignoreUnknownKeys = true }
    val subsonicResponse = jsonParser.decodeFromJsonElement<SubsonicResponse>(
        jsonParser.parseToJsonElement(response).jsonObject["subsonic-response"]!!
    )

    // Generate password salt and hash
    val passwordSaltMedia = generateSalt(8)
    val passwordHashMedia = md5Hash(navidromePassword + passwordSaltMedia)

    subsonicResponse.playlists?.playlist?.map {
        it.coverArt = "$navidromeUrl/rest/getCoverArt.view?&id=${it.navidromeID}&u=$navidromeUsername&t=$passwordHashMedia&s=$passwordSaltMedia&v=1.16.1&c=Chora"
    }

    var mediaDataPlaylists = emptyList<MediaData.Playlist>()

    subsonicResponse.playlists?.playlist?.filterNot { newPlaylist ->
        playlistList.any { existingPlaylist ->
            existingPlaylist.navidromeID == newPlaylist.navidromeID
        }
    }?.let {
        mediaDataPlaylists = it
    }

    Log.d("NAVIDROME", "Added playlists. Total: ${mediaDataPlaylists.size}")

    return mediaDataPlaylists
}

fun parseNavidromePlaylistJSON(
    response: String,
    navidromeUrl: String,
    navidromeUsername: String,
    navidromePassword: String
) : MediaData.Playlist {
    val jsonParser = Json { ignoreUnknownKeys = true }
    val subsonicResponse = jsonParser.decodeFromJsonElement<SubsonicResponse>(
        jsonParser.parseToJsonElement(response).jsonObject["subsonic-response"]!!
    )

    val mediaDataPlaylist = selectedPlaylist

    // Generate password salt and hash
    val passwordSaltArt = generateSalt(8)
    val passwordHashArt = md5Hash(navidromePassword + passwordSaltArt)

    subsonicResponse.playlist?.songs?.map {
        it.imageUrl = "$navidromeUrl/rest/getCoverArt.view?&id=${it.navidromeID}&u=$navidromeUsername&t=$passwordHashArt&s=$passwordSaltArt&v=1.16.1&c=Chora"
    }

    mediaDataPlaylist.songs = subsonicResponse.playlist?.songs

    Log.d("NAVIDROME", "Added Metadata to ${mediaDataPlaylist.name}")

    return mediaDataPlaylist
}