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

@Serializable
data class SearchResult3(
    val song: List<MediaData.Song>? = listOf(),
    val album: List<MediaData.Album>? = listOf(),
    val artist: List<MediaData.Artist>? = listOf(),
)

@OptIn(UnstableApi::class)
fun parseNavidromeSearch3JSON(
    response: String,
    navidromeUrl: String,
    navidromeUsername: String,
    navidromePassword: String,
) : List<Any> {

    val jsonParser = Json { ignoreUnknownKeys = true }
    val subsonicResponse = jsonParser.decodeFromJsonElement<SubsonicResponse>(
        jsonParser.parseToJsonElement(response).jsonObject["subsonic-response"]!!
    )

    // Generate password salt and hash
    val passwordSaltMedia = generateSalt(8)
    val passwordHashMedia = md5Hash(navidromePassword + passwordSaltMedia)

    subsonicResponse.searchResult3?.song?.map {
        it.media = "$navidromeUrl/rest/stream.view?&id=${it.navidromeID}&u=$navidromeUsername&t=$passwordHashMedia&s=$passwordSaltMedia&v=1.12.0&c=Chora"
        it.imageUrl = "$navidromeUrl/rest/getCoverArt.view?&id=${it.navidromeID}&u=$navidromeUsername&t=$passwordHashMedia&s=$passwordSaltMedia&v=1.16.1&c=Chora&size=128"
    }

    subsonicResponse.searchResult3?.album?.map {
        it.coverArt = "$navidromeUrl/rest/getCoverArt.view?&id=${it.navidromeID}&u=$navidromeUsername&t=$passwordHashMedia&s=$passwordSaltMedia&v=1.16.1&c=Chora&size=128"
    }

    var mediaDataSongs = emptyList<MediaItem>()
    var mediaDataAlbums = emptyList<MediaItem>()
    var mediaDataArtists = emptyList<MediaData.Artist>()

    subsonicResponse.searchResult3?.song?.filterNot { newSong ->
        songsList.any { existingSong ->
            existingSong.navidromeID == newSong.navidromeID
        }
    }?.let { mediaDataSongs = it.map {
        it.copy(
            media = "$navidromeUrl/rest/stream.view?&id=${it.navidromeID}&u=$navidromeUsername&t=$passwordHashMedia&s=$passwordSaltMedia&v=1.12.0&c=Chora"
        ).toMediaItem()
        }
    }

    subsonicResponse.searchResult3?.album?.filterNot { newAlbum ->
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