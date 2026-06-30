package com.craftworks.music.legacy.providers.navidrome

import android.util.Log
import androidx.media3.common.MediaItem
import com.craftworks.music.data.datasource.navidrome.NavidromeDataSource
import com.craftworks.music.data.model.MediaModel
import com.craftworks.music.data.model.artistList
import com.craftworks.music.data.model.toMediaItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// LEGACY CODE! MUST NOT BE USED
// TODO("Delete legacy file")
@Serializable
@SerialName("artists")
data class Artists(val index: List<index>)

@Serializable
data class index(val artist: List<com.craftworks.music.data.model.MediaModel.Artist>? = listOf())

fun parseNavidromeArtistsJSON(
    response: String
) : List<com.craftworks.music.data.model.MediaModel.Artist> {
    val subsonicResponse = parseSubsonicResponse(response)

    val mediaDataArtists = mutableListOf<com.craftworks.music.data.model.MediaModel.Artist>()

    subsonicResponse.artists?.index?.forEach { index ->
        index.artist?.filterNot { newArtist ->
            artistList.any { existingArtist ->
                existingArtist.navidromeID == newArtist.navidromeID
            }
        }?.let { filteredArtists ->
            mediaDataArtists.addAll(filteredArtists)
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
    val subsonicResponse = parseSubsonicResponse(response)

    val passwordSaltArt = NavidromeDataSource.generateSalt(8)
    val passwordHashArt = NavidromeDataSource.md5Hash(navidromePassword + passwordSaltArt)

    subsonicResponse.artist?.album?.forEach {
        it.coverArt = "$navidromeUrl/rest/getCoverArt.view?&id=${it.navidromeID}&u=$navidromeUsername&t=$passwordHashArt&s=$passwordSaltArt&v=1.16.1&c=Chora"
    }

    return subsonicResponse.artist?.album?.map { it.toMediaItem() } ?: emptyList()
}

fun parseNavidromeArtistBiographyJSON(
    response: String
) : com.craftworks.music.data.model.MediaModel.ArtistInfo {
    val subsonicResponse = parseSubsonicResponse(response)

    val mediaDataArtist = MediaModel.ArtistInfo(
        biography = subsonicResponse.artistInfo?.biography,
        musicBrainzId = subsonicResponse.artistInfo?.musicBrainzId,
        similarArtist = subsonicResponse.artistInfo?.similarArtist
    )

    return mediaDataArtist
}