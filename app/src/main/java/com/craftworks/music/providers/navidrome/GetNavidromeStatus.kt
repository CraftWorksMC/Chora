package com.craftworks.music.providers.navidrome

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.gitlab.mvysny.konsumexml.konsumeXml

var navidromeStatus = mutableStateOf("")

fun getNavidromeStatus(url: String, username: String, password: String){
    sendNavidromeGETRequest(url, username, password, "ping.view?")
}

fun parseNavidromeStatusXML(response: String){
    // Avoid crashing by removing some useless tags.
    val newResponse = response
        .replace("xmlns=\"http://subsonic.org/restapi\" ", "")

    newResponse.konsumeXml().apply {
        child("subsonic-response"){
            val status = attributes.getValue("status")

            if (status == "failed"){
                childOrNull("error"){
                    val errorCode = attributes.getValue("code")
                    val errorMessage = attributes.getValue("message")
                    Log.d("NAVIDROME", "Navidrome Error Code: $errorCode, Message: $errorMessage")
                    navidromeStatus.value = "Error $errorCode: $errorMessage"
                }
            }
            else navidromeStatus.value = "ok"

            skipContents()
        }
    }
}

