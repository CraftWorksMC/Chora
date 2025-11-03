package com.craftworks.music.providers.navidrome

import com.craftworks.music.data.NavidromeLibrary
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

@Serializable
data class MusicFolder(
    val musicFolder: List<NavidromeLibrary>
)

fun parseNavidromeLibrariesJSON(
    response: String
): List<NavidromeLibrary> {
    val jsonParser = Json { ignoreUnknownKeys = true }
    val subsonicResponse = jsonParser.decodeFromJsonElement<SubsonicResponse>(
        jsonParser.parseToJsonElement(response).jsonObject["subsonic-response"]!!
    )

    return subsonicResponse.musicFolders?.musicFolder ?: emptyList()
}