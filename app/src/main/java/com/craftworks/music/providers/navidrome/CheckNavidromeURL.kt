package com.craftworks.music.providers.navidrome

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.BufferedReader
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
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
                instanceFollowRedirects = true
                Log.d("GET", "\nSent 'GET' request to URL : $url; Response Code : $responseCode")

                if (responseCode == 404){
                    Log.d("NAVIDROME", "404")
                    navidromeStatus.value = "Invalid URL"
                    return@Thread
                }

                inputStream.bufferedReader().use {
                    parseNavidromeStatusXML(it, "/subsonic-response", "/subsonic-response/error")
                }
            }


        } catch (e: Exception) {
            Log.d("NAVIDROME", "Unknown Error.")
            navidromeStatus.value = "Invalid URL"
            return@Thread
        }
    }
    thread.start()

    if (navidromeStatus.value == "ok"){
        Log.d("NAVIDROME", "Successfully logged in!")
        com.craftworks.music.ui.screens.username.value = username
    }
    return navidromeStatus.value == "ok"
}

fun parseNavidromeStatusXML(input: BufferedReader, xpath: String, xpathFailed: String){
    val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(InputSource(StringReader(input.readText())))
    val elementNodeList = XPathFactory.newInstance().newXPath().evaluate(xpath, doc, XPathConstants.NODESET) as NodeList
    val elementNodeListFailed = XPathFactory.newInstance().newXPath().evaluate(xpathFailed, doc, XPathConstants.NODESET) as NodeList

    for (a in 0 until elementNodeList.length) {
        val attributes = elementNodeList.item(a).attributes
        val status = attributes.getNamedItem("status")?.textContent ?: ""

        navidromeStatus.value = status
        Log.d("NAVIDROME", "Navidrome Status: ${navidromeStatus.value}")
    }

    if (navidromeStatus.value == "failed"){
        for (a in 0 until elementNodeListFailed.length) {
            val attributes = elementNodeListFailed.item(a).attributes
            val errorCode = attributes.getNamedItem("code")?.textContent ?: ""
            val errorMessage = attributes.getNamedItem("message")?.textContent ?: ""
            navidromeStatus.value = errorMessage
            Log.d("NAVIDROME", "Navidrome Error Code: $errorCode, Message: $errorMessage")
        }
    }
}

