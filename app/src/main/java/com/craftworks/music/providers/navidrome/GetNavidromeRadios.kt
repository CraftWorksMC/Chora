package com.craftworks.music.providers.navidrome

import android.util.Log
import com.craftworks.music.data.model.MediaData
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
    val jsonElement = jsonParser.parseToJsonElement(response).jsonObject["subsonic-response"]
        ?: return emptyList()
    val subsonicResponse = try {
        jsonParser.decodeFromJsonElement<SubsonicResponse>(jsonElement)
    } catch (e: Exception) {
        return emptyList()
    }

    val mediaDataRadios = subsonicResponse.internetRadioStations?.internetRadioStation ?: emptyList()

    Log.d("NAVIDROME", "Added radios. Total: ${mediaDataRadios.size}")

    return mediaDataRadios
}