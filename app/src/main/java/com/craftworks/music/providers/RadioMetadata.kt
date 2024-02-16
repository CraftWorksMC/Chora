package com.craftworks.music.providers

import com.craftworks.music.SongHelper
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

fun getIcecastMetadata(urlString: String): String {
    val url = URL(urlString)
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "GET"

    val responseCode = connection.responseCode
    if (responseCode == HttpURLConnection.HTTP_OK) {
        val reader = BufferedReader(InputStreamReader(connection.inputStream))
        val response = StringBuilder()

        var line: String?
        while (reader.readLine().also { line = it } != null) {
            response.append(line)
        }
        reader.close()
        parseIcecastMetadata(response.toString())
        return response.toString()
    } else {
        throw Exception("Failed to retrieve metadata. Response code: $responseCode")
    }
}

fun parseIcecastMetadata(json: String){
    val jsonObject = JSONObject(json)
    val sourceArray = jsonObject.getJSONObject("icestats").getJSONArray("source")

    var title = ""
    for (i in 0 until sourceArray.length()) {
        val sourceObject = sourceArray.getJSONObject(i)
        title = sourceObject.optString("title").split(" - ").last()
        println(title)
    }
    SongHelper.currentSong = SongHelper.currentSong.copy(
        title = title,
        artist = SongHelper.currentSong.artist,
        year = SongHelper.currentSong.year)
}