package com.craftworks.music.providers.navidrome

import android.net.Uri
import android.util.Log
import com.craftworks.music.data.Album
import com.craftworks.music.data.albumList
import com.craftworks.music.data.navidromeServersList
import com.craftworks.music.data.selectedNavidromeServerIndex
import com.gitlab.mvysny.konsumexml.konsumeXml

fun getNavidromeAlbums(){
    sendNavidromeGETRequest(
        navidromeServersList[selectedNavidromeServerIndex.intValue].url,
        navidromeServersList[selectedNavidromeServerIndex.intValue].username,
        navidromeServersList[selectedNavidromeServerIndex.intValue].password,
        "getAlbumList.view?type=newest&size=100&offset=0"
    )
}

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

                    val passwordSalt = generateSalt(8)
                    val passwordHash = md5Hash(navidromePassword + passwordSalt)

                    val albumArtUri =
                        Uri.parse("$navidromeUrl/rest/getCoverArt.view?&id=$albumID&u=$navidromeUsername&t=$passwordHash&s=$passwordSalt&v=1.16.1&c=Chora")

                    val album = Album(
                        name = albumTitle,
                        artist = albumArtist,
                        year = albumYear,
                        coverArt = albumArtUri,
                        navidromeID = albumID
                    )
                    synchronized(albumList){
                        if (albumList.none { it.name == albumTitle })
                            albumList.add(album)
                    }

                    skipContents()
                    finish()
                }.apply {
                    // Get albums 100 at a time.
                    if (size == 500){
                        val albumOffset = (albumList.size + 1)
                        sendNavidromeGETRequest(
                            navidromeUrl,
                            navidromeUsername,
                            navidromePassword,
                            "getAlbumList.view?type=newest&size=500&offset=$albumOffset"
                        )
                    }
                }
            }
        }
    }

    Log.d("NAVIDROME", "Added albums! Total: ${albumList.size}")
}