package com.craftworks.music.providers.navidrome

import android.graphics.Color
import android.net.Uri
import android.util.Log
import com.craftworks.music.data.Song
import com.craftworks.music.data.navidromeServersList
import com.craftworks.music.data.search3SongList
import com.craftworks.music.data.selectedNavidromeServerIndex
import com.craftworks.music.data.songsList
import com.craftworks.music.repeatSong
import com.craftworks.music.ui.elements.dialogs.transcodingBitrate
import com.gitlab.mvysny.konsumexml.getValueIntOrNull
import com.gitlab.mvysny.konsumexml.konsumeXml
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.internal.synchronized
import kotlinx.coroutines.launch
import kotlinx.serialization.*
import kotlinx.serialization.builtins.IntArraySerializer
import kotlinx.serialization.json.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*

fun getNavidromeSongs(){
    sendNavidromeGETRequest(
        navidromeServersList[selectedNavidromeServerIndex.intValue].url,
        navidromeServersList[selectedNavidromeServerIndex.intValue].username,
        navidromeServersList[selectedNavidromeServerIndex.intValue].password,
        "search3.view?query=''&songCount=500&songOffset=0&artistCount=0&albumCount=0&f=json"
    )
}

// OLD CODE. PLS DON'T USE.
/*
fun parseNavidromeSongXML(
    response: String,
    navidromeUrl: String,
    navidromeUsername: String,
    navidromePassword: String) {

    // Avoid crashing by removing some useless tags.
    val newResponse = response
        .replace("xmlns=\"http://subsonic.org/restapi\" ", "")

    newResponse.konsumeXml().apply {
        child("subsonic-response"){
            child("searchResult3"){
                children("song"){

                    navidromeSyncInProgress.value = true

                    //region getValues
                    val songTitle = attributes.getValue("title")
                    val songArtist = attributes.getValue("artist")
                    val songAlbum = attributes.getValue("album")
                    val songYear = attributes.getValueOrNull("year") ?: "0"
                    val songID = attributes.getValue("id")
                    val songDuration = (attributes.getValueIntOrNull("duration") ?: 0) * 1000
                    val songPlayCount = attributes.getValueIntOrNull("playCount") ?: 0
                    val songDateAdded = attributes.getValue("created")
                    val songFormat = attributes.getValue("suffix").uppercase()
                    val songBitrate = attributes.getValueOrNull("bitRate") ?: ""
                    val songLastPlayed = attributes.getValueOrNull("played") ?: ""
                    val songTrackIndex = attributes.getValueIntOrNull("track") ?: 0

                    // Generate password salt and hash for songArtUri
                    val passwordSaltArt = generateSalt(8)
                    val passwordHashArt = md5Hash(navidromePassword + passwordSaltArt)

                    val songArtUri =
                        Uri.parse("$navidromeUrl/rest/getCoverArt.view?&id=$songID&u=$navidromeUsername&t=$passwordHashArt&s=$passwordSaltArt&v=1.16.1&c=Chora")

                    // Generate password salt and hash for songMedia
                    val passwordSaltMedia = generateSalt(8)
                    val passwordHashMedia = md5Hash(navidromePassword + passwordSaltMedia)

                    val songMedia = if (transcodingBitrate.value != "No Transcoding")
                        Uri.parse("$navidromeUrl/rest/stream.view?&id=$songID&u=$navidromeUsername&t=$passwordHashMedia&s=$passwordSaltMedia&format=mp3&maxBitRate=${transcodingBitrate.value}&v=1.12.0&c=Chora")
                    else
                        Uri.parse("$navidromeUrl/rest/stream.view?&id=$songID&u=$navidromeUsername&t=$passwordHashMedia&s=$passwordSaltMedia&v=1.12.0&c=Chora")

                    //endregion

                    //region Add song to songsList
//                    val song = Song(
//                        songArtUri,
//                        songTitle,
//                        songArtist,
//                        songAlbum,
//                        songDuration,
//                        isRadio = false,
//                        songMedia,
//                        songPlayCount,
//                        songDateAdded,
//                        songYear,
//                        songFormat,
//                        songBitrate,
//                        songID,
//                        songLastPlayed,
//                        songTrackIndex
//                    )

//                    synchronized(songsList){
//                        if (songsList.none { it.title == songTitle && it.artist == songArtist && it.navidromeID == songID })
//                            //songsList.add(song)
//                    }
                    //endregion

                    skipContents()
                    finish()
                }.apply {
                    // Get songs 200 at a time.
                    if (size == 500){
                        val songOffset = songsList.size
                        sendNavidromeGETRequest(
                            navidromeUrl,
                            navidromeUsername,
                            navidromePassword,
                            "search3.view?query=''&songCount=500&songOffset=$songOffset&artistCount=0&albumCount=0"
                        )
                    }
                }
            }
        }
    }

    Log.d("NAVIDROME", "Added songs! Total: ${songsList.size}")
}
*/


@OptIn(InternalCoroutinesApi::class)
fun parseNavidromeSongJSON(
    response: String,
    navidromeUrl: String,
    navidromeUsername: String,
    navidromePassword: String,
    endpoint: String
){
    val jsonParser = Json { ignoreUnknownKeys = true }
    val subsonicResponse = jsonParser.decodeFromJsonElement<SubsonicResponse>(
        jsonParser.parseToJsonElement(response).jsonObject["subsonic-response"]!!
    )

    // Generate password salt and hash
    val passwordSaltMedia = generateSalt(8)
    val passwordHashMedia = md5Hash(navidromePassword + passwordSaltMedia)

    subsonicResponse.searchResult3.song?.map {
        it.media = if (transcodingBitrate.value != "No Transcoding")
            "$navidromeUrl/rest/stream.view?&id=${it.navidromeID}&u=$navidromeUsername&t=$passwordHashMedia&s=$passwordSaltMedia&format=mp3&maxBitRate=${transcodingBitrate.value}&v=1.12.0&c=Chora"
        else
            "$navidromeUrl/rest/stream.view?&id=${it.navidromeID}&u=$navidromeUsername&t=$passwordHashMedia&s=$passwordSaltMedia&v=1.12.0&c=Chora"

        it.imageUrl = "$navidromeUrl/rest/getCoverArt.view?&id=${it.navidromeID}&u=$navidromeUsername&t=$passwordHashMedia&s=$passwordSaltMedia&v=1.16.1&c=Chora"
    }

    //Check if query is empty for allSongsList

    subsonicResponse.searchResult3.song?.filterNot { newSong ->
        songsList.any { existingSong ->
            // Define your criteria for considering songs as duplicates
            existingSong.navidromeID == newSong.navidromeID
        }
    }?.let { songsList.addAll(it) }


    //songsList.addAll(subsonicResponse.searchResult3.song)
    Log.d("NAVIDROME", "Added songs. Total: ${songsList.size}")
}