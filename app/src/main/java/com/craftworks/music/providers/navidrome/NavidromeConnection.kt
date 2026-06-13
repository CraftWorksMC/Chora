package com.craftworks.music.providers.navidrome

import android.util.Log
import com.craftworks.music.data.model.MediaData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

@Serializable
@SerialName("subsonic-response")
data class SubsonicResponse(
    val status: String,
    val error: SubsonicError? = null,
    val version: String,
    val type: String,
    val serverVersion: String,
    val openSubsonic: Boolean,

    // Music folders
    val musicFolders: MusicFolder? = null,

    // Songs
    val song: MediaData.Song? = null,
    val searchResult3: SearchResult3? = null,

    // Albums
    val albumList: albumList? = null,
    val album: MediaData.Album? = null,

    // Artists
    val artists: Artists? = null,
    val artist: MediaData.Artist? = null,
    val artistInfo: MediaData.ArtistInfo? = null,

    // Radios
    val internetRadioStations: internetRadioStations? = null,

    // Playlists
    val playlist: MediaData.Playlist? = null,
    val playlists: PlaylistContainer? = null,

    //Lyrics
    val lyrics: MediaData.PlainLyrics? = null,
    val lyricsList: LyricsList? = null,

    // Favourites
    val starred: Starred? = null,

    val sonicMatch: List<MediaData.Song>? = null
)

private val jsonParser = Json { ignoreUnknownKeys = true }

fun parseSubsonicResponse(response: String): SubsonicResponse {
    val subsonicResponse = jsonParser.decodeFromJsonElement<SubsonicResponse>(
        jsonParser.parseToJsonElement(response).jsonObject["subsonic-response"]!!
    )

    if (subsonicResponse.status != "ok") {
        val errorCode = subsonicResponse.error?.code
        val errorMessage = subsonicResponse.error?.message
        Log.d("NAVIDROME", "Navidrome Error Code: $errorCode, Message: $errorMessage")
        navidromeStatus.value = "Error $errorCode: $errorMessage"
    }

    return subsonicResponse
}