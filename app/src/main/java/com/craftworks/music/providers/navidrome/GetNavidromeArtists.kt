package com.craftworks.music.providers.navidrome

import android.util.Log
import androidx.media3.common.MediaItem
import com.craftworks.music.data.model.MediaData
import com.craftworks.music.data.model.artistList
import com.craftworks.music.data.model.toMediaItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import java.net.URLEncoder

@Serializable
@SerialName("artists")
data class Artists(val index: List<index>)

@Serializable
data class index(val artist: List<MediaData.Artist>? = listOf())

fun parseNavidromeArtistsJSON(
    response: String
) : List<MediaData.Artist> {
    val jsonParser = Json { ignoreUnknownKeys = true }
    val jsonElement = jsonParser.parseToJsonElement(response).jsonObject["subsonic-response"]
        ?: return emptyList()
    val subsonicResponse = try {
        jsonParser.decodeFromJsonElement<SubsonicResponse>(jsonElement)
    } catch (e: Exception) {
        return emptyList()
    }

    val mediaDataArtists = mutableListOf<MediaData.Artist>()

    subsonicResponse.artists?.index?.forEach { index ->
        index.artist?.filterNot { newArtist ->
            //println(newArtist)
            artistList.any { existingArtist ->
                existingArtist.navidromeID == newArtist.navidromeID
            }
        }?.let { filteredArtists ->
            mediaDataArtists.addAll(filteredArtists) // Assuming mediaDataArtists is a mutable list
        }
    }

    Log.d("NAVIDROME", "Added artists. Total: ${mediaDataArtists.size}")

    return mediaDataArtists
}

fun parseNavidromeArtistAlbumsJSON(
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

    val passwordSaltArt = generateSalt(8)
    val passwordHashArt = md5Hash(navidromePassword + passwordSaltArt)
    val encodedUsername = URLEncoder.encode(navidromeUsername, "UTF-8")

    val updatedAlbums = subsonicResponse.artist?.album?.map {
        it.copy(coverArt = "$navidromeUrl/rest/getCoverArt.view?&id=${it.navidromeID}&u=$encodedUsername&t=$passwordHashArt&s=$passwordSaltArt&v=1.16.1&c=Chora")
    }

    return updatedAlbums?.map { it.toMediaItem() } ?: emptyList()
}

fun parseNavidromeArtistBiographyJSON(
    response: String
) : MediaData.ArtistInfo {
    val jsonParser = Json { ignoreUnknownKeys = true }
    val jsonElement = jsonParser.parseToJsonElement(response).jsonObject["subsonic-response"]
        ?: return MediaData.ArtistInfo()
    val subsonicResponse = try {
        jsonParser.decodeFromJsonElement<SubsonicResponse>(jsonElement)
    } catch (e: Exception) {
        return MediaData.ArtistInfo()
    }

    val mediaDataArtist = MediaData.ArtistInfo(
        biography = subsonicResponse.artistInfo?.biography,
        musicBrainzId = subsonicResponse.artistInfo?.musicBrainzId,
        similarArtist = subsonicResponse.artistInfo?.similarArtist
    )

    return mediaDataArtist
}