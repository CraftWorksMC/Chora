package com.craftworks.music.providers.navidrome

import android.util.Log
import com.craftworks.music.data.model.MediaData
import com.craftworks.music.data.model.radioList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

@Serializable
@SerialName("artists")
data class internetRadioStations(val internetRadioStation: List<MediaData.Radio>)

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

    Log.d("NAVIDROME", "Added radios. Total: ${mediaDataRadios.size}")

    return mediaDataRadios
}