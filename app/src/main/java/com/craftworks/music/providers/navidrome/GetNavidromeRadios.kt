package com.craftworks.music.legacy.providers.navidrome

import android.util.Log
import com.craftworks.music.data.model.MediaModel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// LEGACY CODE! MUST NOT BE USED
// TODO("Delete legacy file")
@Serializable
@SerialName("artists")
data class internetRadioStations(val internetRadioStation: List<MediaModel.Radio>)

fun parseNavidromeRadioJSON(
    response: String
) : List<MediaModel.Radio> {
    val subsonicResponse = parseSubsonicResponse(response)

    val mediaDataRadios = subsonicResponse.internetRadioStations?.internetRadioStation ?: emptyList()

    Log.d("NAVIDROME", "Added radios. Total: ${mediaDataRadios.size}")

    return mediaDataRadios
}