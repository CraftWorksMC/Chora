package com.craftworks.music.providers.navidrome

import android.util.Log
import androidx.media3.common.MediaItem
import com.craftworks.music.data.MediaData
import com.craftworks.music.data.playlistList
import com.craftworks.music.data.toMediaItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

@Serializable
data class PlaylistContainer(val playlist: List<MediaData.Playlist>? = listOf())

fun parseNavidromePlaylistsJSON(
    response: String,
    navidromeUrl: String,
    navidromeUsername: String,
    navidromePassword: String
) : List<MediaItem> {

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

    var mediaDataPlaylists = emptyList<MediaItem>()

    subsonicResponse.playlists?.playlist?.filterNot { newPlaylist ->
        playlistList.any { existingPlaylist ->
            existingPlaylist.navidromeID == newPlaylist.navidromeID
        }
    }?.let {
        mediaDataPlaylists = it.map { it.toMediaItem() }
    }

    Log.d("NAVIDROME", "Added playlists. Total: ${mediaDataPlaylists.size}")

    return mediaDataPlaylists
}

fun parseNavidromePlaylistJSON(
    response: String,
    navidromeUrl: String,
    navidromeUsername: String,
    navidromePassword: String
) : List<MediaItem> {
    val jsonParser = Json { ignoreUnknownKeys = true }
    val subsonicResponse = jsonParser.decodeFromJsonElement<SubsonicResponse>(
        jsonParser.parseToJsonElement(response).jsonObject["subsonic-response"]!!
    )

    val mediaDataPlaylist = mutableListOf(subsonicResponse.playlist?.toMediaItem() ?: MediaItem.EMPTY)

    // Generate password salt and hash
    val passwordSalt = generateSalt(8)
    val passwordHash = md5Hash(navidromePassword + passwordSalt)

    subsonicResponse.playlist?.songs?.map {
        it.imageUrl = "$navidromeUrl/rest/getCoverArt.view?&id=${it.navidromeID}&u=$navidromeUsername&t=$passwordHash&s=$passwordSalt&v=1.16.1&c=Chora"
        it.media = "$navidromeUrl/rest/stream.view?&id=${it.navidromeID}&u=$navidromeUsername&t=$passwordHash&s=$passwordSalt&v=1.12.0&c=Chora"
    }
    subsonicResponse.playlist?.coverArt = "$navidromeUrl/rest/getCoverArt.view?&id=${subsonicResponse.playlist.navidromeID}&u=$navidromeUsername&t=$passwordHash&s=$passwordSalt&v=1.16.1&c=Chora"

    Log.d("NAVIDROME", "Added Metadata to ${mediaDataPlaylist[0].mediaMetadata.title}")

    mediaDataPlaylist.addAll(subsonicResponse.playlist?.songs?.map { it.toMediaItem() } ?: emptyList())

    return mediaDataPlaylist
}