package com.craftworks.music.navidrome

import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableStateOf
import com.craftworks.music.data.Album
import com.craftworks.music.data.Playlist
import com.craftworks.music.data.Radio
import com.craftworks.music.data.Song
import com.craftworks.music.playingSong
import com.craftworks.music.songsList
import com.craftworks.music.ui.screens.albumList
import com.craftworks.music.ui.screens.playlistList
import com.craftworks.music.ui.screens.radioList
import com.craftworks.music.ui.screens.transcodingBitrate
import com.craftworks.music.ui.screens.useNavidromeServer
import com.craftworks.music.ui.screens.username
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import org.xmlpull.v1.XmlPullParserException
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
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

            // Clear everything!
            songsList.clear()
            albumList.clear()
            radioList.clear()
            playlistList.clear()

            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"  // optional default is GET

                Log.d("GET","\nSent 'GET' request to URL : $url; Response Code : $responseCode")

                //Set Username To Navidrome Login Username.
                if (responseCode == 200){
                    username.value = navidromeUsername.value
                }

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
@Throws(XmlPullParserException::class, IOException::class)
fun getNavidromePlaylists(){
    if (navidromeUsername.value == "" ||
        navidromePassword.value == "") return

    val thread = Thread {
        try {
            val url = URL("${navidromeServerIP.value}/rest/getPlaylists.view?&u=${navidromeUsername.value}&p=${navidromePassword.value}&v=1.12.0&c=Chora")

            playlistList.clear()

            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"  // optional default is GET

                Log.d("GET","\nSent 'GET' request to URL : $url; Response Code : $responseCode")

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
@Throws(XmlPullParserException::class, IOException::class)
fun getNavidromeRadios(){
    if (navidromeUsername.value == "" ||
        navidromePassword.value == "") return

    val thread = Thread {
        try {
            val url = URL("${navidromeServerIP.value}/rest/getInternetRadioStations.view?&u=${navidromeUsername.value}&p=${navidromePassword.value}&v=1.12.0&c=Chora")

            radioList.clear()

            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"  // optional default is GET

                Log.d("GET","\nSent 'GET' request to URL : $url; Response Code : $responseCode")

                inputStream.bufferedReader().use {
                    parseRadioXML(it, "/subsonic-response/internetRadioStations/internetRadioStation", radioList)
                }
            }
        } catch (e: Exception) {
            Log.d("Exception", e.toString())
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
                    Uri.parse("${navidromeServerIP.value}/rest/stream.view?&id=$songId&u=${navidromeUsername.value}&p=${navidromePassword.value}&format=mp3&maxBitRate=${transcodingBitrate.value}&v=1.12.0&c=Chora")
                else
                    Uri.parse("${navidromeServerIP.value}/rest/stream.view?&id=$songId&u=${navidromeUsername.value}&p=${navidromePassword.value}&v=1.12.0&c=Chora")
            songImageUrl = Uri.parse("${navidromeServerIP.value}/rest/getCoverArt.view?&id=$songId&u=${navidromeUsername.value}&p=${navidromePassword.value}&v=1.12.0&c=Chora")
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

        songList.add(song)

        // Add songs to album
        val album = Album(
            name = songAlbum,
            artist = songArtist,
            year = songYear,
            coverArt = songImageUrl
        )
        if (!albumList.contains(album)){
            albumList.add(album)
        }
    }
}
fun parsePlaylistXML(input: BufferedReader, xpath: String, playlistList: MutableList<Playlist>){
    val dbFactory = DocumentBuilderFactory.newInstance()
    val dBuilder = dbFactory.newDocumentBuilder()
    val xmlInput = InputSource(StringReader(input.readText()))
    val doc = dBuilder.parse(xmlInput)

    val xpFactory = XPathFactory.newInstance()
    val xPath = xpFactory.newXPath()

    val elementNodeList = xPath.evaluate(xpath, doc, XPathConstants.NODESET) as NodeList

    for (a in 0 until elementNodeList.length) {

        val firstElement = elementNodeList.item(a)

        var playlistID = ""
        var playlistName = ""
        var playlistCover = Uri.EMPTY


        @Suppress("ReplaceRangeToWithUntil")
        for (i in 0..firstElement.attributes.length - 1) {
            val attribute = firstElement.attributes.item(i)
            playlistID = if (attribute.nodeName == "id") attribute.textContent else playlistID
            playlistName = if (attribute.nodeName == "name") attribute.textContent else playlistName
            playlistCover = if (attribute.nodeName == "coverArt") Uri.parse("${navidromeServerIP.value}/rest/getCoverArt.view?id=${attribute.textContent}&u=${navidromeUsername.value}&p=${navidromePassword.value}&v=1.12.0&c=Chora") else playlistCover
            if (attribute.nodeName == "title") Log.d("NAVIDROME", "Added Playlist: ${attribute.textContent}")
        }
        // GET PLAYLIST SONGS
        var playlistSongs:MutableList<Song> = mutableListOf()
        val playlistSongsURL = URL("${navidromeServerIP.value}/rest/getPlaylist.view?id=${playlistID}&u=${navidromeUsername.value}&p=${navidromePassword.value}&v=1.12.0&c=Chora")
        with(playlistSongsURL.openConnection() as HttpURLConnection) {
            requestMethod = "GET"  // optional default is GET

            Log.d("GET","\nSent 'GET' request to URL : $url; Response Code : $responseCode")

            inputStream.bufferedReader().use {
                parseSongXML(it, "/subsonic-response/playlist/entry", playlistSongs)
            }
        }


        playlistList.add(Playlist(
            name = playlistName,
            coverArt = playlistCover,
            navidromeID = playlistID,
            songs = playlistSongs
        ))
    }
}
fun parseRadioXML(input: BufferedReader, xpath: String, radiosList: MutableList<Radio>){
    val dbFactory = DocumentBuilderFactory.newInstance()
    val dBuilder = dbFactory.newDocumentBuilder()
    val xmlInput = InputSource(StringReader(input.readText()))
    val doc = dBuilder.parse(xmlInput)

    val xpFactory = XPathFactory.newInstance()
    val xPath = xpFactory.newXPath()

    val elementNodeList = xPath.evaluate(xpath, doc, XPathConstants.NODESET) as NodeList

    for (a in 0 until elementNodeList.length) {

        val firstElement = elementNodeList.item(a)

        var radioID = ""
        var radioName = ""
        var radioUrl = Uri.EMPTY
        var radioImage = Uri.EMPTY


        @Suppress("ReplaceRangeToWithUntil")
        for (i in 0..firstElement.attributes.length - 1) {
            val attribute = firstElement.attributes.item(i)
            radioID = if (attribute.nodeName == "id") attribute.textContent else radioID
            radioName = if (attribute.nodeName == "name") attribute.textContent else radioName
            radioUrl = if (attribute.nodeName == "streamUrl") Uri.parse(attribute.textContent) else radioUrl
            radioImage = if (attribute.nodeName == "homePageUrl") Uri.parse(attribute.textContent + "/favicon.ico") else radioImage
            if (attribute.nodeName == "name") Log.d("NAVIDROME", "Added Radio: ${attribute.textContent}")
        }

        radiosList.add(Radio(
            name = radioName,
            imageUrl = radioImage,
            media = radioUrl,
            navidromeID = radioID
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

fun downloadNavidromeSong(url: String, snackbarHostState: SnackbarHostState? = SnackbarHostState(), coroutineScope: CoroutineScope) {
    val thread = Thread {
        try {
            if (playingSong.selectedSong?.isRadio == true || !useNavidromeServer.value) return@Thread
            println("\nSent 'GET' request to URL : $url")
            val destinationFolder =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath
            println("MUSIC_DIR: $destinationFolder")
            val fileUrl = URL(url)
            val connection: HttpURLConnection = fileUrl.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            val inputStream = BufferedInputStream(connection.inputStream)

            //val fileName = playingSong.selectedSong?.title + "." + playingSong.selectedSong?.format
            val contentDisposition = connection.getHeaderField("Content-Disposition")
            val fileName = extractNavidromeSongName(contentDisposition) ?: "downloaded_song.mp3"
            val outputFile = File(destinationFolder, fileName)
            val outputStream = FileOutputStream(outputFile)

            val bufferSize = 1024
            val buffer = ByteArray(bufferSize)
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }



            inputStream.close()
            outputStream.close()
            connection.disconnect()
            println("Song downloaded to: ${outputFile.absolutePath}")
            coroutineScope.launch {
                snackbarHostState?.showSnackbar("Song downloaded to: ${outputFile.absolutePath}")
            }

        } catch (e: Exception) {
            println(e)
        }
    }
    thread.start()
}
fun extractNavidromeSongName(contentDisposition: String?): String? {
    if (contentDisposition == null) return null

    val startIndex = contentDisposition.indexOf("filename=")
    if (startIndex == -1) return null

    var endIndex = contentDisposition.indexOf(";", startIndex)
    if (endIndex == -1) {
        endIndex = contentDisposition.length
    }

    var fileName = contentDisposition.substring(startIndex + 9, endIndex).trim('\"')

    // Remove any path or folder structure and keep only the base filename
    fileName = fileName.substringAfterLast('/')

    return fileName
}

fun createNavidromeRadioStation(name:String, url:String, homePage:String){
    if (navidromeUsername.value == "" ||
        navidromePassword.value == "") return

    val thread = Thread {
        try {
            Log.d("useNavidromeServer", "URL: $url")

            val navidromeUrl = URL("${navidromeServerIP.value}/rest/createInternetRadioStation.view?name=$name&streamUrl=$url&homepageUrl=$homePage&u=${navidromeUsername.value}&p=${navidromePassword.value}&v=1.12.0&c=Chora")

            with(navidromeUrl.openConnection() as HttpURLConnection) {
                requestMethod = "GET"  // optional default is GET
                Log.d("GET","\nSent 'GET' request to URL : $url; Response Code : $responseCode")
            }
        } catch (e: Exception) {
            Log.d("Exception", e.toString())
        }
    }
    thread.start()
}
fun deleteNavidromeRadioStation(id:String){
    if (navidromeUsername.value == "" ||
        navidromePassword.value == "") return

    val thread = Thread {
        try {
            val navidromeUrl = URL("${navidromeServerIP.value}/rest/deleteInternetRadioStation.view?id=$id&u=${navidromeUsername.value}&p=${navidromePassword.value}&v=1.12.0&c=Chora")

            with(navidromeUrl.openConnection() as HttpURLConnection) {
                requestMethod = "GET"  // optional default is GET
                Log.d("GET","\nSent 'GET' request to URL : $url; Response Code : $responseCode")
            }
        } catch (e: Exception) {
            Log.d("Exception", e.toString())
        }
    }
    thread.start()
}
fun modifyNavidromeRadoStation(id:String, name:String, url:String, homePage:String){
    if (navidromeUsername.value == "" ||
        navidromePassword.value == "") return

    val thread = Thread {
        try {
            val navidromeUrl = URL("${navidromeServerIP.value}/rest/updateInternetRadioStation.view?name=$name&streamUrl=$url&homepageUrl=$homePage&id=$id&u=${navidromeUsername.value}&p=${navidromePassword.value}&v=1.12.0&c=Chora")

            with(navidromeUrl.openConnection() as HttpURLConnection) {
                requestMethod = "GET"  // optional default is GET
                Log.d("GET","\nSent 'GET' request to URL : $navidromeUrl; Response Code : $responseCode")
            }

            getNavidromeRadios()
        } catch (e: Exception) {
            Log.d("Exception", e.toString())
        }
    }
    thread.start()
}