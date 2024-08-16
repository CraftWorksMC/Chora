package com.craftworks.music.providers.navidrome

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.craftworks.music.data.NavidromeProvider
import com.gitlab.mvysny.konsumexml.konsumeXml

var navidromeStatus = mutableStateOf("")

suspend fun getNavidromeStatus(server: NavidromeProvider){
    NavidromeManager.addServer(server)
    sendNavidromeGETRequest("ping.view?")
    NavidromeManager.removeServer(server.id)
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

