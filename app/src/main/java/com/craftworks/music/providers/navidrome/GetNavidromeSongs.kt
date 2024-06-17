package com.craftworks.music.providers.navidrome

import android.util.Log
import com.craftworks.music.data.MediaData
import com.craftworks.music.data.navidromeServersList
import com.craftworks.music.data.selectedNavidromeServerIndex
import com.craftworks.music.data.songsList
import com.craftworks.music.ui.elements.dialogs.transcodingBitrate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

@Serializable
data class SearchResult3(val song: List<MediaData.Song>? = listOf())


suspend fun getNavidromeSongs() : List<MediaData.Song> {
    return sendNavidromeGETRequest(
        navidromeServersList[selectedNavidromeServerIndex.intValue].url,
        navidromeServersList[selectedNavidromeServerIndex.intValue].username,
        navidromeServersList[selectedNavidromeServerIndex.intValue].password,
        "search3.view?query=''&songCount=100&songOffset=0&artistCount=0&albumCount=0&f=json"
    ).filterIsInstance<MediaData.Song>()
}

fun parseNavidromeSongJSON(
    response: String,
    navidromeUrl: String,
    navidromeUsername: String,
    navidromePassword: String
) : List<MediaData.Song> {
    val jsonParser = Json { ignoreUnknownKeys = true }
    val subsonicResponse = jsonParser.decodeFromJsonElement<SubsonicResponse>(
        jsonParser.parseToJsonElement(response).jsonObject["subsonic-response"]!!
    )

    // Generate password salt and hash
    val passwordSaltMedia = generateSalt(8)
    val passwordHashMedia = md5Hash(navidromePassword + passwordSaltMedia)

    subsonicResponse.searchResult3?.song?.map {
        it.media = if (transcodingBitrate.value != "No Transcoding")
            "$navidromeUrl/rest/stream.view?&id=${it.navidromeID}&u=$navidromeUsername&t=$passwordHashMedia&s=$passwordSaltMedia&format=mp3&maxBitRate=${transcodingBitrate.value}&v=1.12.0&c=Chora"
        else
            "$navidromeUrl/rest/stream.view?&id=${it.navidromeID}&u=$navidromeUsername&t=$passwordHashMedia&s=$passwordSaltMedia&v=1.12.0&c=Chora"

        it.imageUrl = "$navidromeUrl/rest/getCoverArt.view?&id=${it.navidromeID}&u=$navidromeUsername&t=$passwordHashMedia&s=$passwordSaltMedia&v=1.16.1&c=Chora"
    }

    //Check if query is empty for allSongsList

    var mediaDataSongs = emptyList<MediaData.Song>()

    subsonicResponse.searchResult3?.song?.filterNot { newSong ->
        songsList.any { existingSong ->
            existingSong.navidromeID == newSong.navidromeID
        }
    }?.let { // songsList.addAll(it)
        mediaDataSongs = it
        }

    //songsList.addAll(subsonicResponse.searchResult3.song)
    Log.d("NAVIDROME", "Added songs. Total: ${songsList.size}")

    return mediaDataSongs
}

