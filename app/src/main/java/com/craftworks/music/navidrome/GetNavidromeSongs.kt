package com.craftworks.music.navidrome

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.craftworks.music.data.Song
import com.craftworks.music.songsList
import com.craftworks.music.ui.screens.useNavidromeServer
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


var navidromeServerIP = mutableStateOf("")
var navidromeUsername = mutableStateOf("")
var navidromePassword = mutableStateOf("")

@Throws(XmlPullParserException::class, IOException::class)
fun getNavidromeSongs(url: URL){
    Log.d("NAVIDROME", "USERNAME: $navidromeUsername.value, PASS: ${navidromePassword.value}")
    if (navidromeUsername.value == "" ||
        navidromePassword.value == "") return

    val thread = Thread {
        try {
            Log.d("useNavidromeServer", "URL: $url")

            songsList.clear()

            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"  // optional default is GET

                Log.d("GET","\nSent 'GET' request to URL : $url; Response Code : $responseCode")

                inputStream.bufferedReader().use {
                    parseXml(it, "/subsonic-response/searchResult3/song", songsList)
                }
            }
        } catch (e: Exception) {
            Log.d("Exception", e.toString())
        }
    }
    thread.start()
}

fun parseXml(input: BufferedReader, xpath: String, songList: MutableList<Song>){
    val dbFactory = DocumentBuilderFactory.newInstance()
    val dBuilder = dbFactory.newDocumentBuilder()
    val xmlInput = InputSource(StringReader(input.readText()))
    val doc = dBuilder.parse(xmlInput)

    val xpFactory = XPathFactory.newInstance()
    val xPath = xpFactory.newXPath()

    val elementNodeList = xPath.evaluate(xpath, doc, XPathConstants.NODESET) as NodeList

    for (a in 0 until elementNodeList.length - 1) {

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
            songMedia = Uri.parse("${navidromeServerIP.value}/rest/stream.view?&id=$songId&u=${navidromeUsername.value}&p=${navidromePassword.value}&v=1.12.0&c=Chora")
            songImageUrl = Uri.parse("${navidromeServerIP.value}/rest/getCoverArt.view?&id=$songId&u=${navidromeUsername.value}&p=${navidromePassword.value}&v=1.12.0&c=Chora")
            songYear = if (attribute.nodeName == "year") attribute.textContent else songYear
            songPlayCount = if (attribute.nodeName == "playCount") attribute.textContent.toInt() else songPlayCount
            songDateAdded = if (attribute.nodeName == "created") attribute.textContent else songDateAdded
            songMimeType = if (attribute.nodeName == "suffix") attribute.textContent.uppercase() else songMimeType
            songBitrate = if (attribute.nodeName == "bitRate") attribute.textContent else songBitrate
            songLastPlayed = if (attribute.nodeName == "played") attribute.textContent else songLastPlayed
        }


        songList.add(Song(
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
        ))
    }
}

fun markSongAsPlayed(song: Song){
    if (useNavidromeServer.value) {
        val thread = Thread {
            try {
                val url = URL("${navidromeServerIP.value}/rest/scrobble.view?id=${song.navidromeID}&submission=true&u=${navidromeUsername.value}&p=${navidromePassword.value}&v=1.12.0&c=Chora")
                with(url.openConnection() as HttpURLConnection) {
                    requestMethod = "GET"  // optional default is GET
                    println("\nSent 'GET' request to URL : $url; Response Code : $responseCode")
                    inputStream.bufferedReader().use {
                        Log.d("PlayedTimes", it.toString())
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        thread.start()
    }
}