package com.craftworks.music.providers.navidrome

import android.net.Uri
import android.util.Log
import com.craftworks.music.data.Album
import com.craftworks.music.data.Artist
import com.craftworks.music.data.Song
import com.craftworks.music.data.albumList
import com.craftworks.music.data.artistList
import com.craftworks.music.data.navidromeServersList
import com.craftworks.music.data.songsList
import com.craftworks.music.ui.screens.transcodingBitrate
import com.craftworks.music.ui.screens.username
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import org.xmlpull.v1.XmlPullParserException
import java.io.BufferedReader
import java.io.IOException
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

@Throws(XmlPullParserException::class, IOException::class)
fun getNavidromeSongs(url: URL){
    if (navidromeServersList.isEmpty()) return
    Log.d("NAVIDROME", "USERNAME: ${navidromeServersList[selectedNavidromeServerIndex.intValue].username}, PASS: ${navidromeServersList[selectedNavidromeServerIndex.intValue].password}")
    if (navidromeServersList[selectedNavidromeServerIndex.intValue].username == "" ||
        navidromeServersList[selectedNavidromeServerIndex.intValue].password == "") return

    val thread = Thread {
        try {
            navidromeStatus.value = "Loading"

            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"  // optional default is GET

                Log.d("GET", "\nSent 'GET' request to URL : $url; Response Code : $responseCode")

                if (responseCode == 404){
                    navidromeStatus.value = "Invalid URL"
                    return@Thread
                }

                //Set Username To Navidrome Login Username.
                if (responseCode == 200){
                    username.value = navidromeServersList[selectedNavidromeServerIndex.intValue].username
                }

                inputStream.bufferedReader().use {
                    parseSongXML(it, "/subsonic-response/searchResult3/song", songsList)
                }
            }
        } catch (e: Exception) {
            Log.d("Exception", e.toString())
            if (e == java.net.ConnectException())
                navidromeStatus.value = "Invalid URL"
        }
    }
    thread.start()
}

fun parseSongXML(input: BufferedReader, xpath: String, songList: MutableList<Song>){
    val dbFactory = DocumentBuilderFactory.newInstance()
    val dBuilder = dbFactory.newDocumentBuilder()
    val xmlInput = InputSource(StringReader(input.readText()))
    val doc = dBuilder.parse(xmlInput)

    val xpFactory = XPathFactory.newInstance()
    val xPath = xpFactory.newXPath()

    val elementNodeList = xPath.evaluate(xpath, doc, XPathConstants.NODESET) as NodeList

    for (a in 0 until elementNodeList.length) {

        val firstElement = elementNodeList.item(a)

        var songId = ""
        var songTitle = ""
        var songAlbum = ""
        var songArtist = ""
        var songDuration = 0
        var songMedia = Uri.EMPTY
        var songImageUrl = Uri.EMPTY
        var songYear = ""
        var songPlayCount = 0
        var songDateAdded = ""
        var songMimeType = ""
        var songBitrate = ""
        var songLastPlayed = ""

        @Suppress("ReplaceRangeToWithUntil")
        for (i in 0..firstElement.attributes.length - 1) {
            val attribute = firstElement.attributes.item(i)
            songId = if (attribute.nodeName == "id") attribute.textContent else songId
            songTitle = if (attribute.nodeName == "title") attribute.textContent else songTitle
            songAlbum = if (attribute.nodeName == "album") attribute.textContent else songAlbum
            songArtist = if (attribute.nodeName == "artist") attribute.textContent else songArtist
            songDuration = if (attribute.nodeName == "duration") attribute.textContent.toInt() else songDuration
            songMedia =
                if (transcodingBitrate.value != "No Transcoding")
                    Uri.parse("${navidromeServersList[selectedNavidromeServerIndex.intValue].url}/rest/stream.view?&id=$songId&u=${navidromeServersList[selectedNavidromeServerIndex.intValue].username}&p=${navidromeServersList[selectedNavidromeServerIndex.intValue].password}&format=mp3&maxBitRate=${transcodingBitrate.value}&v=1.12.0&c=Chora")
                else
                    Uri.parse("${navidromeServersList[selectedNavidromeServerIndex.intValue].url}/rest/stream.view?&id=$songId&u=${navidromeServersList[selectedNavidromeServerIndex.intValue].username}&p=${navidromeServersList[selectedNavidromeServerIndex.intValue].password}&v=1.12.0&c=Chora")
            songImageUrl =
                Uri.parse("${navidromeServersList[selectedNavidromeServerIndex.intValue].url}/rest/getCoverArt.view?&id=$songId&u=${navidromeServersList[selectedNavidromeServerIndex.intValue].username}&p=${navidromeServersList[selectedNavidromeServerIndex.intValue].password}&v=1.12.0&c=Chora")
            songYear = if (attribute.nodeName == "year") attribute.textContent else songYear
            songPlayCount = if (attribute.nodeName == "playCount") attribute.textContent.toInt() else songPlayCount
            songDateAdded = if (attribute.nodeName == "created") attribute.textContent else songDateAdded
            songMimeType = if (attribute.nodeName == "suffix") attribute.textContent.uppercase() else songMimeType
            songBitrate = if (attribute.nodeName == "bitRate") attribute.textContent else songBitrate
            songLastPlayed = if (attribute.nodeName == "played") attribute.textContent else songLastPlayed
        }

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

        if (!songList.contains(songList.firstOrNull { it.title == song.title && it.artist == song.artist })) {
            songList.add(song)
        }

        // Add songs to album
        val album = Album(
            name = songAlbum,
            artist = songArtist,
            year = songYear,
            coverArt = songImageUrl
        )
        if (!albumList.contains(albumList.firstOrNull { it.name == album.name && it.artist == album.artist })){
            albumList.add(album)
        }

        // Add Artists
        val artist = Artist(
            name = songArtist,
            navidromeID = "Local"
        )
        if (!artistList.contains(artistList.firstOrNull { it.name == artist.name })){
            artistList.add(artist)
        }

        navidromeStatus.value = "Success"
    }
}