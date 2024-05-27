package com.craftworks.music.providers.navidrome

import android.net.Uri
import android.util.Log
import com.craftworks.music.data.Artist
import com.craftworks.music.data.albumList
import com.craftworks.music.data.artistList
import com.craftworks.music.data.navidromeServersList
import com.craftworks.music.data.selectedArtist
import com.craftworks.music.data.selectedNavidromeServerIndex
import com.gitlab.mvysny.konsumexml.konsumeXml

fun getNavidromeArtists(){
    sendNavidromeGETRequest(
        navidromeServersList[selectedNavidromeServerIndex.intValue].url,
        navidromeServersList[selectedNavidromeServerIndex.intValue].username,
        navidromeServersList[selectedNavidromeServerIndex.intValue].password,
        "getArtists.view?"
    )
}

fun getNavidromeArtistDetails(id: String){
    sendNavidromeGETRequest(
        navidromeServersList[selectedNavidromeServerIndex.intValue].url,
        navidromeServersList[selectedNavidromeServerIndex.intValue].username,
        navidromeServersList[selectedNavidromeServerIndex.intValue].password,
        "getArtistInfo.view?id=$id"
    )
}

fun parseNavidromeArtistsXML(
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

                        val artist = Artist(
                            name = artistName,
                            imageUri = Uri.parse(artistImage),
                            navidromeID = artistID
                        )
                        if (artistList.none { it.navidromeID == artistID || it.name == artistID })
                            artistList.add(artist)

                        skipContents()
                        finish()
                    }
                }.apply {
                    // Get artists 100 at a time.
                    if (size == 100){
                        val artistOffset = (artistList.size + 1)
                        sendNavidromeGETRequest(
                            navidromeUrl,
                            navidromeUsername,
                            navidromePassword,
                            "getAlbumList.view?type=newest&size=100&offset=$artistOffset"
                        )
                    }
                }
            }
        }
    }

    Log.d("NAVIDROME", "Added albums! Total: ${albumList.size}")
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

                selectedArtist = selectedArtist.copy(description = artistBiography)

                skipContents()
                finish()
            }
        }
    }
}