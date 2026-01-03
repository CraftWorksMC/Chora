package com.craftworks.music.providers.navidrome

import androidx.media3.common.MediaItem
import com.craftworks.music.data.model.MediaData
import com.craftworks.music.data.model.playlistList
import com.craftworks.music.data.model.toMediaItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import java.net.URLEncoder

@Serializable
data class PlaylistContainer(val playlist: List<MediaData.Playlist>? = listOf())

fun parseNavidromePlaylistsJSON(
    response: String,
    navidromeUrl: String,
    navidromeUsername: String,
    navidromePassword: String
) : List<MediaItem> {

    val jsonParser = Json { ignoreUnknownKeys = true }
    val jsonElement = jsonParser.parseToJsonElement(response).jsonObject["subsonic-response"]
        ?: return emptyList()
    val subsonicResponse = try {
        jsonParser.decodeFromJsonElement<SubsonicResponse>(jsonElement)
    } catch (e: Exception) {
        return emptyList()
    }

    // Generate password salt and hash
    val passwordSaltMedia = generateSalt(8)
    val passwordHashMedia = md5Hash(navidromePassword + passwordSaltMedia)
    val encodedUsername = URLEncoder.encode(navidromeUsername, "UTF-8")

    val updatedPlaylists = subsonicResponse.playlists?.playlist?.map {
        it.copy(coverArt = "$navidromeUrl/rest/getCoverArt.view?&id=${it.navidromeID}&u=$encodedUsername&t=$passwordHashMedia&s=$passwordSaltMedia&v=1.16.1&c=Chora&size=128")
    }

    updatedPlaylists?.filterNot { newPlaylist ->
        playlistList.any { existingPlaylist ->
            existingPlaylist.navidromeID == newPlaylist.navidromeID
        }
    }
    return updatedPlaylists?.map { it.toMediaItem() } ?: emptyList()
}

fun parseNavidromePlaylistJSON(
    response: String,
    navidromeUrl: String,
    navidromeUsername: String,
    navidromePassword: String
) : List<MediaItem> {
    val jsonParser = Json { ignoreUnknownKeys = true }
    val jsonElement = jsonParser.parseToJsonElement(response).jsonObject["subsonic-response"]
        ?: return emptyList()
    val subsonicResponse = try {
        jsonParser.decodeFromJsonElement<SubsonicResponse>(jsonElement)
    } catch (e: Exception) {
        return emptyList()
    }

    val mediaDataPlaylist = mutableListOf<MediaItem>()

    // Generate password salt and hash
    val passwordSalt = generateSalt(8)
    val passwordHash = md5Hash(navidromePassword + passwordSalt)
    val encodedUsername = URLEncoder.encode(navidromeUsername, "UTF-8")

    val updatedSongs = subsonicResponse.playlist?.songs?.map {
        it.copy(
            imageUrl = "$navidromeUrl/rest/getCoverArt.view?&id=${it.navidromeID}&u=$encodedUsername&t=$passwordHash&s=$passwordSalt&v=1.16.1&c=Chora&size=128",
            media = "$navidromeUrl/rest/stream.view?&id=${it.navidromeID}&u=$encodedUsername&t=$passwordHash&s=$passwordSalt&v=1.12.0&c=Chora"
        )
    }

    mediaDataPlaylist.addAll(updatedSongs?.map { it.toMediaItem() } ?: emptyList())

    return mediaDataPlaylist
}