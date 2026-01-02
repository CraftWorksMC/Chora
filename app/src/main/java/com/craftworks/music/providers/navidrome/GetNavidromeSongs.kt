package com.craftworks.music.providers.navidrome

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import com.craftworks.music.data.model.MediaData
import com.craftworks.music.data.model.albumList
import com.craftworks.music.data.model.artistList
import com.craftworks.music.data.model.songsList
import com.craftworks.music.data.model.toMediaItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import java.net.URLEncoder

@Serializable
data class SearchResult3(
    val song: List<MediaData.Song>? = listOf(),
    val album: List<MediaData.Album>? = listOf(),
    val artist: List<MediaData.Artist>? = listOf(),
)

@OptIn(UnstableApi::class)
fun parseNavidromeRandomSongsJSON(
    response: String,
    navidromeUrl: String,
    navidromeUsername: String,
    navidromePassword: String
): List<MediaItem> {
    val jsonParser = Json { ignoreUnknownKeys = true }
    val jsonElement = jsonParser.parseToJsonElement(response).jsonObject["subsonic-response"]
        ?: return emptyList()
    val subsonicResponse = try {
        jsonParser.decodeFromJsonElement<SubsonicResponse>(jsonElement)
    } catch (e: Exception) {
        return emptyList()
    }

    val passwordSaltMedia = generateSalt(8)
    val passwordHashMedia = md5Hash(navidromePassword + passwordSaltMedia)
    val encodedUsername = URLEncoder.encode(navidromeUsername, "UTF-8")

    return subsonicResponse.randomSongs?.song?.map {
        it.copy(
            media = "$navidromeUrl/rest/stream.view?&id=${it.navidromeID}&u=$encodedUsername&t=$passwordHashMedia&s=$passwordSaltMedia&v=1.12.0&c=Chora",
            imageUrl = "$navidromeUrl/rest/getCoverArt.view?&id=${it.navidromeID}&u=$encodedUsername&t=$passwordHashMedia&s=$passwordSaltMedia&v=1.16.1&c=Chora&size=128"
        ).toMediaItem()
    } ?: emptyList()
}

@OptIn(UnstableApi::class)
fun parseNavidromeSearch3JSON(
    response: String,
    navidromeUrl: String,
    navidromeUsername: String,
    navidromePassword: String,
) : List<Any> {

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

    val updatedSongs = subsonicResponse.searchResult3?.song?.map {
        it.copy(
            media = "$navidromeUrl/rest/stream.view?&id=${it.navidromeID}&u=$encodedUsername&t=$passwordHashMedia&s=$passwordSaltMedia&v=1.12.0&c=Chora",
            imageUrl = "$navidromeUrl/rest/getCoverArt.view?&id=${it.navidromeID}&u=$encodedUsername&t=$passwordHashMedia&s=$passwordSaltMedia&v=1.16.1&c=Chora&size=128"
        )
    }

    val updatedAlbums = subsonicResponse.searchResult3?.album?.map {
        it.copy(coverArt = "$navidromeUrl/rest/getCoverArt.view?&id=${it.navidromeID}&u=$encodedUsername&t=$passwordHashMedia&s=$passwordSaltMedia&v=1.16.1&c=Chora&size=128")
    }

    var mediaDataSongs = emptyList<MediaItem>()
    var mediaDataAlbums = emptyList<MediaItem>()
    var mediaDataArtists = emptyList<MediaData.Artist>()

    updatedSongs?.filterNot { newSong ->
        songsList.any { existingSong ->
            existingSong.navidromeID == newSong.navidromeID
        }
    }?.let { mediaDataSongs = it.map {
        it.copy(
            media = "$navidromeUrl/rest/stream.view?&id=${it.navidromeID}&u=$encodedUsername&t=$passwordHashMedia&s=$passwordSaltMedia&v=1.12.0&c=Chora"
        ).toMediaItem()
        }
    }

    updatedAlbums?.filterNot { newAlbum ->
        albumList.any { existingAlbum ->
            existingAlbum.navidromeID == newAlbum.navidromeID
        }
    }?.let { mediaDataAlbums = it.map { it.toMediaItem() } }

    subsonicResponse.searchResult3?.artist?.filterNot { newArtist ->
        artistList.any { existingArtist ->
            existingArtist.navidromeID == newArtist.navidromeID
        }
    }?.let { mediaDataArtists = it }

    return when {
        mediaDataSongs.isNotEmpty() -> mediaDataSongs
        mediaDataAlbums.isNotEmpty() -> mediaDataAlbums
        else -> mediaDataArtists
    }
}