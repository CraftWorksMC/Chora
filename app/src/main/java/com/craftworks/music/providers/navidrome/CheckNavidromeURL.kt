package com.craftworks.music.providers.navidrome

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.BufferedReader
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

var navidromeStatus = mutableStateOf("")

fun checkNavidromeURL(navidromeUrl: String, username: String, password: String): Boolean {
    if (navidromeUrl.take(4) != "http"){
        navidromeStatus.value = "Invalid URL"
        Log.d("NAVIDROME", "Invalid URL")
        return false
    }

    val thread = Thread {
        try {
            Log.d("NAVIDROME", "Checking Connection...")

            val statusUrl =
                URL("$navidromeUrl/rest/ping.view?&u=$username&p=$password&v=1.12.0&c=Chora")

            with(statusUrl.openConnection() as HttpURLConnection) {
                requestMethod = "GET"  // optional default is GET

                Log.d("GET", "\nSent 'GET' request to URL : $url; Response Code : $responseCode")

                if (responseCode == 404){
                    Log.d("NAVIDROME", "404")
                    navidromeStatus.value = "Invalid URL"
                    return@Thread
                }

                inputStream.bufferedReader().use {
                    parseNavidromeStatusXML(it, "/subsonic-response")

                    if (navidromeStatus.value != "Ok" &&
                        navidromeStatus.value != "")
                        parseNavidromeStatusXML(it, "/subsonic-response/error")
                }
            }

        } catch (e: UnknownHostException) {
            Log.d("NAVIDROME", "Unknown Host")
            navidromeStatus.value = "Invalid URL"
            return@Thread
        }
    }
    thread.start()

    if (navidromeStatus.value == "Success"){
        Log.d("NAVIDROME", "Successfully logged in!")
        com.craftworks.music.ui.screens.username.value = username
    }
    return navidromeStatus.value == "Success"
}

fun parseNavidromeStatusXML(input: BufferedReader, xpath: String){
    val dbFactory = DocumentBuilderFactory.newInstance()
    val dBuilder = dbFactory.newDocumentBuilder()
    val xmlInput = InputSource(StringReader(input.readText()))
    val doc = dBuilder.parse(xmlInput)

    val xpFactory = XPathFactory.newInstance()
    val xPath = xpFactory.newXPath()

    val elementNodeList = xPath.evaluate(xpath, doc, XPathConstants.NODESET) as NodeList

    for (a in 0 until elementNodeList.length) {

        val firstElement = elementNodeList.item(a)

        var status = ""

        var errorCode = ""
        var errorMessage = ""

        @Suppress("ReplaceRangeToWithUntil")
        for (i in 0..firstElement.attributes.length - 1) {
            val attribute = firstElement.attributes.item(i)
            status = if (attribute.nodeName == "status") attribute.textContent else status
            errorCode = if (attribute.nodeName == "code") attribute.textContent else errorCode
            errorMessage = if (attribute.nodeName == "message") attribute.textContent else errorMessage
        }
        navidromeStatus.value = status
        Log.d("NAVIDROME", "Navidrome Status: ${navidromeStatus.value}")

        if (status == "failed"){
            Log.d("NAVIDROME", "Navidrome Error Message: $errorMessage")
        }
    }
}

