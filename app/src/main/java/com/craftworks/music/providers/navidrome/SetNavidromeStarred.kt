package com.craftworks.music.providers.navidrome

import java.net.URLEncoder

suspend fun setNavidromeStar(star: Boolean, id: String = "", albumId: String = "", artistId: String = ""){
    val queryParams = mutableListOf<String>()
    // URL-encode all IDs to prevent URL injection attacks
    if (id.isNotEmpty()) {
        queryParams.add("id=${URLEncoder.encode(id, "UTF-8")}")
    }
    if (albumId.isNotEmpty()) {
        queryParams.add("albumId=${URLEncoder.encode(albumId, "UTF-8")}")
    }
    if (artistId.isNotEmpty()) {
        queryParams.add("artistId=${URLEncoder.encode(artistId, "UTF-8")}")
    }

    val queryString = if (queryParams.isNotEmpty()) {
        queryParams.joinToString("&", prefix = "?")
    } else {
        ""
    }

    if (star)
        sendNavidromeGETRequest("star.view$queryString", true)
    else
        sendNavidromeGETRequest("unstar.view$queryString", true)
}