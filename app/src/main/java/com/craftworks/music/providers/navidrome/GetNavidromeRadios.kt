package com.craftworks.music.providers.navidrome

import android.util.Log
import com.craftworks.music.data.MediaData
import com.craftworks.music.data.radioList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

@Serializable
@SerialName("artists")
data class internetRadioStations(val internetRadioStation: List<MediaData.Radio>)

suspend fun getNavidromeRadios(){
    radioList.addAll(sendNavidromeGETRequest("getInternetRadioStations.view?f=json").filterIsInstance<MediaData.Radio>())
}

suspend fun deleteNavidromeRadio(id:String){
    sendNavidromeGETRequest("deleteInternetRadioStation.view?id=$id")
}

suspend fun modifyNavidromeRadio(id:String, name:String, url:String, homePage:String){
    sendNavidromeGETRequest("updateInternetRadioStation.view?name=$name&streamUrl=$url&homepageUrl=$homePage&id=$id")
}

suspend fun createNavidromeRadio(name:String, url:String, homePage:String){
    sendNavidromeGETRequest("createInternetRadioStation.view?name=$name&streamUrl=$url&homepageUrl=$homePage")
}

fun parseNavidromeRadioJSON(
    response: String
) : List<MediaData.Radio> {
    val jsonParser = Json { ignoreUnknownKeys = true }
    val subsonicResponse = jsonParser.decodeFromJsonElement<SubsonicResponse>(
        jsonParser.parseToJsonElement(response).jsonObject["subsonic-response"]!!
    )

    var mediaDataRadios = emptyList<MediaData.Radio>()

    subsonicResponse.internetRadioStations?.internetRadioStation?.filterNot { newRadio ->
        radioList.any { existingRadio ->
            existingRadio.navidromeID == newRadio.navidromeID
        }
    }?.let {
        mediaDataRadios = it
    }

    Log.d("NAVIDROME", "Added playlists. Total: ${mediaDataRadios.size}")

    return mediaDataRadios
}