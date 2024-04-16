package com.craftworks.music.providers.navidrome

import android.net.Uri
import android.util.Log
import com.craftworks.music.data.Playlist
import com.craftworks.music.data.Song
import com.craftworks.music.data.navidromeServersList
import com.craftworks.music.data.playlistList
import com.craftworks.music.data.selectedNavidromeServerIndex
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
fun getNavidromePlaylists(){
    //if (navidromeServersList.isEmpty()) return
    if (navidromeServersList[selectedNavidromeServerIndex.intValue].username == "" ||
        navidromeServersList[selectedNavidromeServerIndex.intValue].url == "") return

    val thread = Thread {
        try {
            val url =
                URL("${navidromeServersList[selectedNavidromeServerIndex.intValue].url}/rest/getPlaylists.view?&u=${navidromeServersList[selectedNavidromeServerIndex.intValue].username}&p=${navidromeServersList[selectedNavidromeServerIndex.intValue].password}&v=1.12.0&c=Chora")

            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"  // optional default is GET

                Log.d("GET", "\nSent 'GET' request to URL : $url; Response Code : $responseCode")

                inputStream.bufferedReader().use {
                    parsePlaylistXML(it, "/subsonic-response/playlists/playlist", playlistList)
                }
            }
        } catch (e: Exception) {
            Log.d("Exception", e.toString())
        }
    }
    thread.start()
}

fun createNavidromePlaylist(playlistName: String){
    if (navidromeServersList.isEmpty()) return
    if (navidromeServersList[selectedNavidromeServerIndex.intValue].username == "" ||
        navidromeServersList[selectedNavidromeServerIndex.intValue].url == "") return

    val thread = Thread {
        try {
            val url =
                URL("${navidromeServersList[selectedNavidromeServerIndex.intValue].url}/rest/createPlaylist.view?&u=${navidromeServersList[selectedNavidromeServerIndex.intValue].username}&p=${navidromeServersList[selectedNavidromeServerIndex.intValue].password}&name=${playlistName}&v=1.12.0&c=Chora")

            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"  // optional default is GET

                Log.d("GET", "\nSent 'GET' request to URL : $url; Response Code : $responseCode")
            }

            getNavidromePlaylists()

        } catch (e: Exception) {
            Log.d("Exception", e.toString())
        }
    }
    thread.start()
}
fun deleteNavidromePlaylist(playlistID: String){
    if (navidromeServersList.isEmpty()) return
    if (navidromeServersList[selectedNavidromeServerIndex.intValue].username == "" ||
        navidromeServersList[selectedNavidromeServerIndex.intValue].url == "") return

    val thread = Thread {
        try {
            val url =
                URL("${navidromeServersList[selectedNavidromeServerIndex.intValue].url}/rest/deletePlaylist.view?&u=${navidromeServersList[selectedNavidromeServerIndex.intValue].username}&p=${navidromeServersList[selectedNavidromeServerIndex.intValue].password}&id=${playlistID}&v=1.12.0&c=Chora")

            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"  // optional default is GET

                Log.d("GET", "\nSent 'GET' request to URL : $url; Response Code : $responseCode")
            }

            getNavidromePlaylists()

        } catch (e: Exception) {
            Log.d("Exception", e.toString())
        }
    }
    thread.start()
}
fun addSongToNavidromePlaylist(playlistID: String, songID: String){
    if (navidromeServersList.isEmpty()) return
    if (navidromeServersList[selectedNavidromeServerIndex.intValue].username == "" ||
        navidromeServersList[selectedNavidromeServerIndex.intValue].url == "") return

    val thread = Thread {
        try {
            val url =
                URL("${navidromeServersList[selectedNavidromeServerIndex.intValue].url}/rest/updatePlaylist.view?&u=${navidromeServersList[selectedNavidromeServerIndex.intValue].username}&p=${navidromeServersList[selectedNavidromeServerIndex.intValue].password}&playlistId=${playlistID}&songIdToAdd=${songID}&v=1.12.0&c=Chora")

            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"  // optional default is GET

                Log.d("GET", "\nSent 'GET' request to URL : $url; Response Code : $responseCode")
            }

            getNavidromePlaylists()

        } catch (e: Exception) {
            Log.d("Exception", e.toString())
        }
    }
    thread.start()
}

fun parsePlaylistXML(input: BufferedReader, xpath: String, playlistList: MutableList<Playlist>){
    val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(InputSource(StringReader(input.readText())))
    val elementNodeList = XPathFactory.newInstance().newXPath().evaluate(xpath, doc, XPathConstants.NODESET) as NodeList

    for (a in 0 until elementNodeList.length) {
        val attributes = elementNodeList.item(a).attributes
        val playlistID = attributes.getNamedItem("id")?.textContent ?: ""
        val playlistName = attributes.getNamedItem("name")?.textContent ?: ""
        val playlistCover = attributes.getNamedItem("coverArt")?.textContent?.let {
            Uri.parse("${navidromeServersList[selectedNavidromeServerIndex.intValue].url}/rest/getCoverArt.view?id=$it&u=${navidromeServersList[selectedNavidromeServerIndex.intValue].username}&p=${navidromeServersList[selectedNavidromeServerIndex.intValue].password}&v=1.12.0&c=Chora")
        } ?: Uri.EMPTY

        if (playlistName.isNotEmpty()) {
            Log.d("NAVIDROME", "Added Playlist: $playlistName")
        }

        val playlistSongs: MutableList<Song> = mutableListOf()
        val playlistSongsURL = URL("${navidromeServersList[selectedNavidromeServerIndex.intValue].url}/rest/getPlaylist.view?id=$playlistID&u=${navidromeServersList[selectedNavidromeServerIndex.intValue].username}&p=${navidromeServersList[selectedNavidromeServerIndex.intValue].password}&v=1.12.0&c=Chora")

        with(playlistSongsURL.openConnection() as HttpURLConnection) {
            requestMethod = "GET"
            Log.d("GET", "\nSent 'GET' request to URL : $url; Response Code : $responseCode")
            inputStream.bufferedReader().use {
                parseSongXML(it, "/subsonic-response/playlist/entry", playlistSongs)
            }
        }

        val playlist = Playlist(
            name = playlistName,
            coverArt = playlistCover,
            navidromeID = playlistID,
            songs = playlistSongs
        )

        if (playlistList.none { it.navidromeID == playlistID }) {
            playlistList.add(playlist)
        }
    }
}