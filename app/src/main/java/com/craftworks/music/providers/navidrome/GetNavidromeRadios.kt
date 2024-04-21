package com.craftworks.music.providers.navidrome

import android.net.Uri
import android.util.Log
import com.craftworks.music.R
import com.craftworks.music.data.Radio
import com.craftworks.music.data.navidromeServersList
import com.craftworks.music.data.radioList
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
fun getNavidromeRadios(){
    if (navidromeServersList.isEmpty()) return
    if (navidromeServersList[selectedNavidromeServerIndex.intValue].username == "" ||
        navidromeServersList[selectedNavidromeServerIndex.intValue].url == "") return

    val thread = Thread {
        try {
            val url =
                URL("${navidromeServersList[selectedNavidromeServerIndex.intValue].url}/rest/getInternetRadioStations.view?&u=${navidromeServersList[selectedNavidromeServerIndex.intValue].username}&p=${navidromeServersList[selectedNavidromeServerIndex.intValue].password}&v=1.12.0&c=Chora")

            //radioList.clear()

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
    val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(InputSource(StringReader(input.readText())))
    val elementNodeList = XPathFactory.newInstance().newXPath().evaluate(xpath, doc, XPathConstants.NODESET) as NodeList

    for (a in 0 until elementNodeList.length) {
        val attributes = elementNodeList.item(a).attributes
        val radioID = attributes.getNamedItem("id")?.textContent ?: ""
        val radioName = attributes.getNamedItem("name")?.textContent ?: ""
        val radioUrl = attributes.getNamedItem("streamUrl")?.textContent?.let { Uri.parse(it) } ?: Uri.EMPTY

        if (radioName.isNotEmpty()) {
            Log.d("NAVIDROME", "Added Radio: $radioName")
        }

        val radio = Radio(
            name = radioName,
            imageUrl = Uri.parse("android.resource://com.craftworks.music/" + R.drawable.radioplaceholder),
            homepageUrl = "",
            media = radioUrl,
            navidromeID = radioID
        )

        if (radiosList.none { it.media == radio.media }) {
            radiosList.add(radio)
        }
    }
}

fun deleteNavidromeRadioStation(id:String){
    if (navidromeServersList[selectedNavidromeServerIndex.intValue].username == "" ||
        navidromeServersList[selectedNavidromeServerIndex.intValue].url == "" ||
        navidromeStatus.value != "ok") return

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
        navidromeServersList[selectedNavidromeServerIndex.intValue].url == "" ||
        navidromeStatus.value != "ok") return

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
        navidromeServersList[selectedNavidromeServerIndex.intValue].url == "" ||
        navidromeStatus.value != "ok") return

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