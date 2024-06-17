package com.craftworks.music.providers.navidrome

import android.util.Log
import com.craftworks.music.data.Album
import com.craftworks.music.data.albumList
import com.craftworks.music.data.navidromeServersList
import com.craftworks.music.data.selectedNavidromeServerIndex
import com.craftworks.music.ui.elements.dialogs.transcodingBitrate
import com.craftworks.music.ui.screens.selectedAlbum
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

@Serializable
data class albumList(val album: List<Album>? = listOf())


suspend fun getNavidromeAlbums(){
    sendNavidromeGETRequest(
        navidromeServersList[selectedNavidromeServerIndex.intValue].url,
        navidromeServersList[selectedNavidromeServerIndex.intValue].username,
        navidromeServersList[selectedNavidromeServerIndex.intValue].password,
        "getAlbumList.view?type=newest&size=500&offset=0&f=json"
    )
}

suspend fun getNavidromeAlbumSongs(albumId: String){
    sendNavidromeGETRequest(
        navidromeServersList[selectedNavidromeServerIndex.intValue].url,
        navidromeServersList[selectedNavidromeServerIndex.intValue].username,
        navidromeServersList[selectedNavidromeServerIndex.intValue].password,
        "getAlbum.view?id=$albumId&f=json"
    )
}

/*
fun parseNavidromeAlbumXML(
    response: String,
    navidromeUrl: String,
    navidromeUsername: String,
    navidromePassword: String) {

    // Avoid crashing by removing some useless tags.
    val newResponse = response
        .replace("xmlns=\"http://subsonic.org/restapi\" ", "")

    newResponse.konsumeXml().apply {
        child("subsonic-response"){
            child("albumList"){
                children("album"){
                    val albumTitle = attributes.getValue("title")
                    val albumArtist = attributes.getValue("artist")
                    val albumYear = attributes.getValueOrNull("year") ?: "0"
                    val albumID = attributes.getValue("id")
                    val albumDateAdded = attributes.getValueOrNull("created") ?: "0"
                    val albumDatePlayed = attributes.getValueOrNull("played") ?: "0"
                    val albumPlayCount = attributes.getValueIntOrNull("playCount") ?: 0

                    val passwordSalt = generateSalt(8)
                    val passwordHash = md5Hash(navidromePassword + passwordSalt)

                    val albumArtUri =
                        Uri.parse("$navidromeUrl/rest/getCoverArt.view?&id=$albumID&u=$navidromeUsername&t=$passwordHash&s=$passwordSalt&v=1.16.1&c=Chora")

                    val album = Album(
                        name = albumTitle,
                        artist = albumArtist,
                        year = albumYear,
                        coverArt = albumArtUri,
                        navidromeID = albumID,
                        dateAdded = albumDateAdded,
                        datePlayed = albumDatePlayed,
                        timesPlayed = albumPlayCount
                    )
                    synchronized(albumList){
                        if (albumList.none { it.name == albumTitle })
                            albumList.add(album)
                    }

                    skipContents()
                    finish()
                }.apply {
                    // Get albums 100 at a time.
//                    if (size == 500){
//                        val albumOffset = (albumList.size + 1)
//                        sendNavidromeGETRequest(
//                            navidromeUrl,
//                            navidromeUsername,
//                            navidromePassword,
//                            "getAlbumList.view?type=newest&size=500&offset=$albumOffset"
//                        )
//                        navidromeSyncInProgress.value = true
//                    }
                }
            }
        }
    }

    Log.d("NAVIDROME", "Added albums! Total: ${albumList.size}")
}
*/

suspend fun parseNavidromeAlbumListJSON(
    response: String,
    navidromeUrl: String,
    navidromeUsername: String,
    navidromePassword: String
){
    val jsonParser = Json { ignoreUnknownKeys = true }
    val subsonicResponse = jsonParser.decodeFromJsonElement<SubsonicResponse>(
        jsonParser.parseToJsonElement(response).jsonObject["subsonic-response"]!!
    )

    subsonicResponse.albumList?.album?.map {
        // Generate password salt and hash for coverArt
        val passwordSaltArt = generateSalt(8)
        val passwordHashArt = md5Hash(navidromePassword + passwordSaltArt)

        it.coverArt = "$navidromeUrl/rest/getCoverArt.view?&id=${it.navidromeID}&u=$navidromeUsername&t=$passwordHashArt&s=$passwordSaltArt&v=1.16.1&c=Chora"
    }

    subsonicResponse.albumList?.album?.filterNot { newAlbum ->
        albumList.any { existingAlbum ->
            existingAlbum.navidromeID == newAlbum.navidromeID
        }
    }?.let { albumList.addAll(it) }

    if (subsonicResponse.albumList?.album?.size == 500) {
        sendNavidromeGETRequest(
            navidromeServersList[selectedNavidromeServerIndex.intValue].url,
            navidromeServersList[selectedNavidromeServerIndex.intValue].username,
            navidromeServersList[selectedNavidromeServerIndex.intValue].password,
            "getAlbumList.view?type=newest&size=500&offset=${albumList.size}&f=json"
        )
    }

    Log.d("NAVIDROME", "Added albums. Total: ${albumList.size}")
}

fun parseNavidromeAlbumSongsJSON(
    response: String,
    navidromeUrl: String,
    navidromeUsername: String,
    navidromePassword: String
){
    val jsonParser = Json { ignoreUnknownKeys = true }
    val subsonicResponse = jsonParser.decodeFromJsonElement<SubsonicResponse>(
        jsonParser.parseToJsonElement(response).jsonObject["subsonic-response"]!!
    )

    val passwordSalt = generateSalt(8)
    val passwordHash = md5Hash(navidromePassword + passwordSalt)

    selectedAlbum?.songs = subsonicResponse.album?.songs
    selectedAlbum?.songs?.map {
        it.media = if (transcodingBitrate.value != "No Transcoding")
            "$navidromeUrl/rest/stream.view?&id=${it.navidromeID}&u=$navidromeUsername&t=$passwordHash&s=$passwordSalt&format=mp3&maxBitRate=${transcodingBitrate.value}&v=1.12.0&c=Chora"
        else
            "$navidromeUrl/rest/stream.view?&id=${it.navidromeID}&u=$navidromeUsername&t=$passwordHash&s=$passwordSalt&v=1.12.0&c=Chora"

        it.imageUrl = "$navidromeUrl/rest/getCoverArt.view?&id=${it.navidromeID}&u=$navidromeUsername&t=$passwordHash&s=$passwordSalt&v=1.16.1&c=Chora"
    }

    Log.d("NAVIDROME", "Got Album Songs: ${subsonicResponse.album?.songs?.get(0)?.title}")
}


