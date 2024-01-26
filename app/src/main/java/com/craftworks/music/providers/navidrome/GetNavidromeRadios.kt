package com.craftworks.music.providers.navidrome

import android.net.Uri
import android.util.Log
import com.craftworks.music.data.Radio
import com.craftworks.music.data.radioList
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
fun getNavidromeRadios(){
    if (navidromeServersList[selectedNavidromeServerIndex.intValue].username == "" ||
        navidromeServersList[selectedNavidromeServerIndex.intValue].url == "") return

    val thread = Thread {
        try {
            val url =
                URL("${navidromeServersList[selectedNavidromeServerIndex.intValue].url}/rest/getInternetRadioStations.view?&u=${navidromeServersList[selectedNavidromeServerIndex.intValue].username}&p=${navidromeServersList[selectedNavidromeServerIndex.intValue].password}&v=1.12.0&c=Chora")

            radioList.clear()

            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"  // optional default is GET

                Log.d("GET", "\nSent 'GET' request to URL : $url; Response Code : $responseCode")

                inputStream.bufferedReader().use {
                    parseRadioXML(
                        it,
                        "/subsonic-response/internetRadioStations/internetRadioStation",
                        radioList
                    )
                }
            }
        } catch (e: Exception) {
            Log.d("Exception", e.toString())
        }
    }
    thread.start()
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
            if (attribute.nodeName == "name") Log.d(
                "NAVIDROME",
                "Added Radio: ${attribute.textContent}"
            )
        }

        radiosList.add(
            Radio(
                name = radioName,
                imageUrl = radioImage,
                media = radioUrl,
                navidromeID = radioID
            )
        )
    }
}

fun deleteNavidromeRadioStation(id:String){
    if (navidromeServersList[selectedNavidromeServerIndex.intValue].username == "" ||
        navidromeServersList[selectedNavidromeServerIndex.intValue].url == "") return

    val thread = Thread {
        try {
            val navidromeUrl =
                URL("${navidromeServersList[selectedNavidromeServerIndex.intValue].url}/rest/deleteInternetRadioStation.view?id=$id&u=${navidromeServersList[selectedNavidromeServerIndex.intValue].username}&p=${navidromeServersList[selectedNavidromeServerIndex.intValue].password}&v=1.12.0&c=Chora")

            with(navidromeUrl.openConnection() as HttpURLConnection) {
                requestMethod = "GET"  // optional default is GET
                Log.d("GET", "\nSent 'GET' request to URL : $url; Response Code : $responseCode")
            }
        } catch (e: Exception) {
            Log.d("Exception", e.toString())
        }
    }
    thread.start()
}

fun modifyNavidromeRadoStation(id:String, name:String, url:String, homePage:String){
    if (navidromeServersList[selectedNavidromeServerIndex.intValue].username == "" ||
        navidromeServersList[selectedNavidromeServerIndex.intValue].url == "") return

    val thread = Thread {
        try {
            val navidromeUrl =
                URL("${navidromeServersList[selectedNavidromeServerIndex.intValue].url}/rest/updateInternetRadioStation.view?name=$name&streamUrl=$url&homepageUrl=$homePage&id=$id&u=${navidromeServersList[selectedNavidromeServerIndex.intValue].username}&p=${navidromeServersList[selectedNavidromeServerIndex.intValue].password}&v=1.12.0&c=Chora")

            with(navidromeUrl.openConnection() as HttpURLConnection) {
                requestMethod = "GET"  // optional default is GET
                Log.d(
                    "GET",
                    "\nSent 'GET' request to URL : $navidromeUrl; Response Code : $responseCode"
                )
            }

            getNavidromeRadios()
        } catch (e: Exception) {
            Log.d("Exception", e.toString())
        }
    }
    thread.start()
}

fun createNavidromeRadioStation(name:String, url:String, homePage:String){
    if (navidromeServersList[selectedNavidromeServerIndex.intValue].username == "" ||
        navidromeServersList[selectedNavidromeServerIndex.intValue].url == "") return

    val thread = Thread {
        try {
            Log.d("useNavidromeServer", "URL: $url")

            val navidromeUrl =
                URL("${navidromeServersList[selectedNavidromeServerIndex.intValue].url}/rest/createInternetRadioStation.view?name=$name&streamUrl=$url&homepageUrl=$homePage&u=${navidromeServersList[selectedNavidromeServerIndex.intValue].username}&p=${navidromeServersList[selectedNavidromeServerIndex.intValue].password}&v=1.12.0&c=Chora")

            with(navidromeUrl.openConnection() as HttpURLConnection) {
                requestMethod = "GET"  // optional default is GET
                Log.d("GET", "\nSent 'GET' request to URL : $url; Response Code : $responseCode")
            }
        } catch (e: Exception) {
            Log.d("Exception", e.toString())
        }
    }
    thread.start()
}