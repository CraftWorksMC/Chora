package com.craftworks.music.providers.navidrome

import android.net.Uri
import android.util.Log
import com.craftworks.music.data.Album
import com.craftworks.music.data.Artist
import com.craftworks.music.data.Song
import com.craftworks.music.data.albumList
import com.craftworks.music.data.artistList
import com.craftworks.music.data.navidromeServersList
import com.craftworks.music.data.selectedNavidromeServerIndex
import com.craftworks.music.data.songsList
import com.craftworks.music.ui.elements.dialogs.transcodingBitrate
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.BufferedReader
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

fun getNavidromeSongs(url: URL){
    println("getting navidrome songs!")
    if (navidromeServersList.isEmpty()) return

    if (navidromeServersList[selectedNavidromeServerIndex.intValue].username == "" ||
        navidromeServersList[selectedNavidromeServerIndex.intValue].password == "") return

    val thread = Thread {
        try {
            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"  // optional default is GET

                Log.d("GET", "\nSent 'GET' request to URL : $url; Response Code : $responseCode")

                inputStream.bufferedReader().use {
                    parseSongXML(it, "/subsonic-response/searchResult3/song", songsList)
                }
            }
        } catch (e: Exception) {
            Log.d("Exception", e.toString())
        }
    }
    thread.start()
}

fun parseSongXML(input: BufferedReader, xpath: String, songList: MutableList<Song>){
    val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(InputSource(StringReader(input.readText())))
    val elementNodeList = XPathFactory.newInstance().newXPath().evaluate(xpath, doc, XPathConstants.NODESET) as NodeList

    for (a in 0 until elementNodeList.length) {
        val attributes = elementNodeList.item(a).attributes

        val songId = attributes.getNamedItem("id")?.textContent ?: ""
        val songTitle = attributes.getNamedItem("title")?.textContent ?: ""
        val songAlbum = attributes.getNamedItem("album")?.textContent ?: ""
        val songArtist = attributes.getNamedItem("artist")?.textContent ?: ""
        val songDuration = attributes.getNamedItem("duration")?.textContent?.toIntOrNull() ?: 0
        val songYear = attributes.getNamedItem("year")?.textContent ?: ""
        val songPlayCount = attributes.getNamedItem("playCount")?.textContent?.toIntOrNull() ?: 0
        val songDateAdded = attributes.getNamedItem("created")?.textContent ?: ""
        val songMimeType = attributes.getNamedItem("suffix")?.textContent?.uppercase() ?: ""
        val songBitrate = attributes.getNamedItem("bitRate")?.textContent ?: ""
        val songLastPlayed = attributes.getNamedItem("played")?.textContent ?: ""

        val songMedia = if (transcodingBitrate.value != "No Transcoding")
            Uri.parse("${navidromeServersList[selectedNavidromeServerIndex.intValue].url}/rest/stream.view?&id=$songId&u=${navidromeServersList[selectedNavidromeServerIndex.intValue].username}&p=${navidromeServersList[selectedNavidromeServerIndex.intValue].password}&format=mp3&maxBitRate=${transcodingBitrate.value}&v=1.12.0&c=Chora")
        else
            Uri.parse("${navidromeServersList[selectedNavidromeServerIndex.intValue].url}/rest/stream.view?&id=$songId&u=${navidromeServersList[selectedNavidromeServerIndex.intValue].username}&p=${navidromeServersList[selectedNavidromeServerIndex.intValue].password}&v=1.12.0&c=Chora")

        val songImageUrl = Uri.parse("${navidromeServersList[selectedNavidromeServerIndex.intValue].url}/rest/getCoverArt.view?&id=$songId&u=${navidromeServersList[selectedNavidromeServerIndex.intValue].username}&p=${navidromeServersList[selectedNavidromeServerIndex.intValue].password}&v=1.12.0&c=Chora")

        val song = Song(
            title = songTitle,
            album = songAlbum,
            artist = songArtist,
            duration = songDuration * 1000,
            media = songMedia,
            imageUrl = songImageUrl,
            year = songYear,
            timesPlayed = songPlayCount,
            navidromeID = songId,
            format = songMimeType,
            bitrate = songBitrate,
            dateAdded = songDateAdded,
            lastPlayed = songLastPlayed
        )

        if (songList.none { it.title == song.title && it.artist == song.artist }) {
            songList.add(song)
        }

        val album = Album(
            name = songAlbum,
            artist = songArtist,
            year = songYear,
            coverArt = songImageUrl
        )
        if (albumList.none { it.name == album.name && it.artist == album.artist }) {
            albumList.add(album)
        }

        val artist = Artist(
            name = songArtist,
            navidromeID = "Local"
        )
        if (artistList.none { it.name == artist.name }) {
            artistList.add(artist)
        }
    }
}