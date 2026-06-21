package com.craftworks.music.providers.navidrome

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.craftworks.music.data.NavidromeProvider
import com.craftworks.music.data.datasource.navidrome.NavidromeDataSource
import com.craftworks.music.managers.NavidromeManager
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

// LEGACY CODE! MUST NOT BE USED
// TODO("Delete legacy file")
var navidromeStatus = mutableStateOf("")

suspend fun getNavidromeStatus(server: NavidromeProvider) {
    withContext(NonCancellable) {
        try {
            NavidromeManager.addServer(server, true)
            NavidromeDataSource().pingNavidromeServer()
        }
        catch (ex: Exception) {
            navidromeStatus.value = ex.message.toString()
        }
        finally {
            NavidromeManager.removeServer(server.id, true)
        }
    }
}

@Serializable
@SerialName("error")
data class SubsonicError(
    val code: Int,
    val message: String
)

fun parseNavidromeStatus(
    response: String
) : List<String> {
    val jsonParser = Json { ignoreUnknownKeys = true }
    val subsonicResponse = jsonParser.decodeFromJsonElement<SubsonicResponse>(
        jsonParser.parseToJsonElement(response).jsonObject["subsonic-response"]!!
    )

    if (subsonicResponse.status == "ok") {
        navidromeStatus.value = "ok"
        return listOf(subsonicResponse.status)
    }

    val errorCode = subsonicResponse.error?.code
    val errorMessage = subsonicResponse.error?.message
    Log.d("NAVIDROME", "Navidrome Error Code: $errorCode, Message: $errorMessage")
    navidromeStatus.value = "Error $errorCode: $errorMessage"
    return listOf(subsonicResponse.error?.message ?: "")
}