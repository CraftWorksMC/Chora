package com.craftworks.music.providers.navidrome

import android.util.Log
import com.craftworks.music.data.MediaData
import com.craftworks.music.data.artistList
import com.craftworks.music.data.navidromeServersList
import com.craftworks.music.data.selectedArtist
import com.craftworks.music.data.selectedNavidromeServerIndex
import com.gitlab.mvysny.konsumexml.konsumeXml
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

@Serializable
@SerialName("artists")
data class Artists(val index: List<index>)

@Serializable
data class index(val artist: List<MediaData.Artist>? = listOf())


suspend fun getNavidromeArtists() : List<MediaData.Artist>{
    return sendNavidromeGETRequest(
        navidromeServersList[selectedNavidromeServerIndex.intValue].url,
        navidromeServersList[selectedNavidromeServerIndex.intValue].username,
        navidromeServersList[selectedNavidromeServerIndex.intValue].password,
        "getArtists.view?size=100&f=json"
    ).filterIsInstance<MediaData.Artist>()
}

suspend fun getNavidromeArtistDetails(id: String): MediaData.Artist {
    return sendNavidromeGETRequest(
        navidromeServersList[selectedNavidromeServerIndex.intValue].url,
        navidromeServersList[selectedNavidromeServerIndex.intValue].username,
        navidromeServersList[selectedNavidromeServerIndex.intValue].password,
        "getArtist.view?id=$id&f=json"
    ).filterIsInstance<MediaData.Artist>()[0] //Use index 0 because sendNavidromeGETRequest only returns a list.
}

suspend fun getNavidromeArtistBiography(id: String): MediaData.Artist {
    return sendNavidromeGETRequest(
        navidromeServersList[selectedNavidromeServerIndex.intValue].url,
        navidromeServersList[selectedNavidromeServerIndex.intValue].username,
        navidromeServersList[selectedNavidromeServerIndex.intValue].password,
        "getArtistInfo.view?id=$id&f=json"
    ).filterIsInstance<MediaData.Artist>()[0] //Use index 0 because sendNavidromeGETRequest only returns a list.
}

/*
suspend fun parseNavidromeArtistsXML(
    response: String,
    navidromeUrl: String,
    navidromeUsername: String,
    navidromePassword: String) {

    // Avoid crashing by removing some useless tags.
    val newResponse = response
        .replace("xmlns=\"http://subsonic.org/restapi\" ", "")

    newResponse.konsumeXml().apply {
        child("subsonic-response"){
            child("artists"){
                children("index"){
                    children("artist"){

                        val artistName = attributes.getValue("name")
                        val artistImage = attributes.getValue("artistImageUrl")
                        val artistID = attributes.getValue("id")

                        //val passwordSalt = generateSalt(8)
                        //val passwordHash = md5Hash(navidromePassword + passwordSalt)

                        //val albumArtUri = Uri.parse("$navidromeUrl/rest/getCoverArt.view?&id=$artistID&u=$navidromeUsername&t=$passwordHash&s=$passwordSalt&v=1.16.1&c=Chora")

                        val artist = MediaData.Artist(
                            name = artistName,
                            coverArt = artistImage,
                            navidromeID = artistID
                        )
                        synchronized(artistList){
                            if (artistList.none { it.navidromeID == artistID || it.name == artistID })
                                artistList.add(artist)
                        }

                        skipContents()
                        finish()
                    }
                }
            }
        }
    }

    if (artistList.size % 500 == 0){
        val artistOffset = (artistList.size + 1)

        sendNavidromeGETRequest(
            navidromeUrl,
            navidromeUsername,
            navidromePassword,
            "getArtists.view?size=500&offset=$artistOffset"
        )
    }


    Log.d("NAVIDROME", "Added artists! Total: ${artistList.size}")
}
*/

fun parseNavidromeArtistsJSON(
    response: String
) : List<MediaData.Artist> {
    val jsonParser = Json { ignoreUnknownKeys = true }
    val subsonicResponse = jsonParser.decodeFromJsonElement<SubsonicResponse>(
        jsonParser.parseToJsonElement(response).jsonObject["subsonic-response"]!!
    )

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

fun parseNavidromeArtistXML(
    response: String) {

    // Avoid crashing by removing some useless tags.
    val newResponse = response
        .replace("xmlns=\"http://subsonic.org/restapi\" ", "")

    newResponse.konsumeXml().apply {
        child("subsonic-response"){
            child("artistInfo"){
                val artistBiography = childTextOrNull("biography")?.replace(Regex("<a[^>]*>.*?</a>"), "") ?: ""

                //selectedArtist = selectedArtist.copy(description = artistBiography)

                skipContents()
                finish()
            }
        }
    }
}

fun parseNavidromeArtistAlbumsJSON(
    response: String,
    navidromeUrl: String,
    navidromeUsername: String,
    navidromePassword: String
) : MediaData.Artist {
    val jsonParser = Json { ignoreUnknownKeys = true }
    val subsonicResponse = jsonParser.decodeFromJsonElement<SubsonicResponse>(
        jsonParser.parseToJsonElement(response).jsonObject["subsonic-response"]!!
    )

    val mediaDataArtist = selectedArtist

    // Generate password salt and hash for album coverArt
    val passwordSaltArt = generateSalt(8)
    val passwordHashArt = md5Hash(navidromePassword + passwordSaltArt)

    subsonicResponse.artist?.album?.map {
        it.coverArt = "$navidromeUrl/rest/getCoverArt.view?&id=${it.navidromeID}&u=$navidromeUsername&t=$passwordHashArt&s=$passwordSaltArt&v=1.16.1&c=Chora"
    }

    mediaDataArtist.starred = subsonicResponse.artist?.starred
    mediaDataArtist.album = subsonicResponse.artist?.album

    Log.d("NAVIDROME", "Added Metadata to ${mediaDataArtist.name}")

    return mediaDataArtist
}

fun parseNavidromeArtistBiographyJSON(
    response: String
) : MediaData.Artist {
    val jsonParser = Json { ignoreUnknownKeys = true }
    val subsonicResponse = jsonParser.decodeFromJsonElement<SubsonicResponse>(
        jsonParser.parseToJsonElement(response).jsonObject["subsonic-response"]!!
    )

    val mediaDataArtist = selectedArtist

    mediaDataArtist.description = subsonicResponse.artistInfo?.biography.toString()
    mediaDataArtist.musicBrainzId = subsonicResponse.artistInfo?.musicBrainzId
    mediaDataArtist.similarArtist = subsonicResponse.artistInfo?.similarArtist

    Log.d("NAVIDROME", "Added Metadata to ${mediaDataArtist.name}")

    return mediaDataArtist
}