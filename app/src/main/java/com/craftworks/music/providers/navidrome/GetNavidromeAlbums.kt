package com.craftworks.music.providers.navidrome

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.craftworks.music.data.MediaData
import com.craftworks.music.data.albumList
import com.craftworks.music.ui.screens.selectedAlbum
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

@Serializable
data class albumList(val album: List<MediaData.Album>? = listOf())


suspend fun getNavidromeAlbums(sort: String? = "alphabeticalByName", size: Int? = 100, offset: Int? = 0) : List<MediaData.Album>{
    return sendNavidromeGETRequest("getAlbumList.view?type=$sort&size=$size&offset=$offset&f=json").filterIsInstance<MediaData.Album>()
}

suspend fun searchNavidromeAlbums(query: String? = ""): List<MediaData.Album> {
    return sendNavidromeGETRequest("search3.view?query=$query&songCount=0&songOffset=0&artistCount=0&albumCount=100&f=json").filterIsInstance<MediaData.Album>()
}

suspend fun getNavidromeAlbumSongs(albumId: String) {
    sendNavidromeGETRequest("getAlbum.view?id=$albumId&f=json")
}

fun parseNavidromeAlbumListJSON(
    response: String,
    navidromeUrl: String,
    navidromeUsername: String,
    navidromePassword: String,
) : List<MediaData.Album> {
    val jsonParser = Json { ignoreUnknownKeys = true }
    val subsonicResponse = jsonParser.decodeFromJsonElement<SubsonicResponse>(
        jsonParser.parseToJsonElement(response).jsonObject["subsonic-response"]!!
    )

    // Generate password salt and hash for coverArt
    val passwordSaltArt = generateSalt(8)
    val passwordHashArt = md5Hash(navidromePassword + passwordSaltArt)

    subsonicResponse.albumList?.album?.map {
        it.coverArt = "$navidromeUrl/rest/getCoverArt.view?&id=${it.navidromeID}&u=$navidromeUsername&t=$passwordHashArt&s=$passwordSaltArt&v=1.16.1&c=Chora"
    }

    var mediaDataAlbums = emptyList<MediaData.Album>()

    subsonicResponse.albumList?.album?.filterNot { newAlbum ->
        albumList.any { existingAlbum ->
            existingAlbum.navidromeID == newAlbum.navidromeID
        }
    }?.let { mediaDataAlbums = it }

    Log.d("NAVIDROME", "Added albums. Total: ${mediaDataAlbums.size}")

    return mediaDataAlbums
}

@OptIn(UnstableApi::class)
fun parseNavidromeAlbumSongsJSON(
    response: String,
    navidromeUrl: String,
    navidromeUsername: String,
    navidromePassword: String,
){
    val jsonParser = Json { ignoreUnknownKeys = true }
    val subsonicResponse = jsonParser.decodeFromJsonElement<SubsonicResponse>(
        jsonParser.parseToJsonElement(response).jsonObject["subsonic-response"]!!
    )

    val passwordSalt = generateSalt(8)
    val passwordHash = md5Hash(navidromePassword + passwordSalt)

    //val transcodingBitrate = SettingsManager(context).transcodingBitrateFlow.first()

    selectedAlbum?.songs = subsonicResponse.album?.songs
    selectedAlbum?.songs?.map {
        it.media = "$navidromeUrl/rest/stream.view?&id=${it.navidromeID}&u=$navidromeUsername&t=$passwordHash&s=$passwordSalt&v=1.12.0&c=Chora"
        it.imageUrl = "$navidromeUrl/rest/getCoverArt.view?&id=${it.navidromeID}&u=$navidromeUsername&t=$passwordHash&s=$passwordSalt&v=1.16.1&c=Chora"
    }

    Log.d("NAVIDROME", "Got Album Songs: ${subsonicResponse.album?.songs?.get(0)?.title}")
}
