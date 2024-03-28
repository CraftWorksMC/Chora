package com.craftworks.music.providers.navidrome

import android.net.Uri
import android.util.Log
import com.craftworks.music.data.Artist
import com.craftworks.music.data.artistList
import com.craftworks.music.data.navidromeServersList
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.BufferedReader
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

//region Get All Navidrome Artists
fun getNavidromeArtists(){
    if (navidromeServersList.isEmpty()) return

    if (navidromeServersList[selectedNavidromeServerIndex.intValue].username == "" ||
        navidromeServersList[selectedNavidromeServerIndex.intValue].password == "") return

    val thread = Thread {
        try {
            Log.d("NAVIDROME", "Getting Artists")

            val url =
                URL("${navidromeServersList[selectedNavidromeServerIndex.intValue].url}/rest/getArtists.view?&u=${navidromeServersList[selectedNavidromeServerIndex.intValue].username}&p=${navidromeServersList[selectedNavidromeServerIndex.intValue].password}&v=1.12.0&c=Chora")


            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"  // optional default is GET

                Log.d("GET", "\nSent 'GET' request to URL : $url; Response Code : $responseCode")

                inputStream.bufferedReader().use {
                    parseArtistsXML(it, "/subsonic-response/artists/index/artist")
                }
            }
        } catch (e: Exception) {
            Log.d("Exception", e.toString())
        }
    }
    thread.start()
}

fun parseArtistsXML(input: BufferedReader, xpath: String){
    val dbFactory = DocumentBuilderFactory.newInstance()
    val dBuilder = dbFactory.newDocumentBuilder()
    val xmlInput = InputSource(StringReader(input.readText()))
    val doc = dBuilder.parse(xmlInput)

    val xpFactory = XPathFactory.newInstance()
    val xPath = xpFactory.newXPath()

    val elementNodeList = xPath.evaluate(xpath, doc, XPathConstants.NODESET) as NodeList

    for (a in 0 until elementNodeList.length) {

        val firstElement = elementNodeList.item(a)

        var artistName = ""
        var artistId = ""

        @Suppress("ReplaceRangeToWithUntil")
        for (i in 0..firstElement.attributes.length - 1) {
            val attribute = firstElement.attributes.item(i)
            artistName = if (attribute.nodeName == "name") attribute.textContent else artistName
            artistId = if (attribute.nodeName == "id") attribute.textContent else artistId
        }
        Log.d("NAVIDROME", "Got Artist Name + ID")
        //getNavidromeArtistDetails(artistId, artistName)

        // Add Artists
        val artist = Artist(
            name = artistName,
            imageUri = Uri.EMPTY,
            navidromeID = artistId,
            description = ""
        )
        Log.d("NAVIDROME", "Added Artist $artistName")
        if (!artistList.contains(artistList.firstOrNull { it.name == artist.name })){
            artistList.add(artist)
        }
    }
}

//endregion


//region Get Artist Details + Add to list
fun getNavidromeArtistDetails(id: String, name:String){
    if (navidromeServersList.isEmpty()) return

    if (navidromeServersList[selectedNavidromeServerIndex.intValue].username == "" ||
        navidromeServersList[selectedNavidromeServerIndex.intValue].password == "") return

    val thread = Thread {
        try {
            Log.d("NAVIDROME", "Getting Artist Details")

            val url =
                URL("${navidromeServersList[selectedNavidromeServerIndex.intValue].url}/rest/getArtistInfo.view?&u=${navidromeServersList[selectedNavidromeServerIndex.intValue].username}&p=${navidromeServersList[selectedNavidromeServerIndex.intValue].password}&id=$id&v=1.12.0&c=Chora")


            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"  // optional default is GET

                Log.d("GET", "\nSent 'GET' request to URL : $url; Response Code : $responseCode")

                inputStream.bufferedReader().use {
                    parseArtistDetailsXML(it, "/subsonic-response/artistInfo", id, name)
                }
            }
        } catch (e: Exception) {
            Log.d("Exception", e.toString())
        }
    }
    thread.start()
}

fun parseArtistDetailsXML(input: BufferedReader, xpath: String, artistId: String, artistName: String){
    val dbFactory = DocumentBuilderFactory.newInstance()
    val dBuilder = dbFactory.newDocumentBuilder()
    val xmlInput = InputSource(StringReader(input.readText()))
    val doc = dBuilder.parse(xmlInput)

    val xpFactory = XPathFactory.newInstance()
    val xPath = xpFactory.newXPath()

    val elementNodeList = xPath.evaluate(xpath, doc, XPathConstants.NODESET) as NodeList

    for (a in 0 until elementNodeList.length) {

        val userNode = elementNodeList.item(a)
        println(userNode)

        val nodeList = userNode.attributes.item(0).childNodes
        //val node = nodeList.item(0)
        println(nodeList)


        val artistDescription: String = elementNodeList.item(0).textContent
        val artistImage: Uri = Uri.parse(elementNodeList.item(4).textContent)



        /*
        @Suppress("ReplaceRangeToWithUntil")
        for (i in 0..firstElement.attributes.length - 1) {
            val attribute = firstElement.attributes.item(i)
            println(attribute)
            artistDescription = if (attribute.nodeName == "biography") attribute.textContent else artistDescription
            artistImage = if (attribute.nodeName == "largeImageUrl") Uri.parse(attribute.textContent) else artistImage
        }
        */

        Log.d("NAVIDROME", "Gotten details for $artistName")
        val index = artistList.indexOfFirst { it.navidromeID == artistId }
        artistList[index] = artistList[index].copy(imageUri = artistImage, description = artistDescription)
    }
}
//endregion