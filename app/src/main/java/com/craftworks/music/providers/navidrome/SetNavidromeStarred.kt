package com.craftworks.music.providers.navidrome

suspend fun setNavidromeStar(star: Boolean, id: String = "", albumId: String = "", artistId: String = ""){
    val queryParams = mutableListOf<String>()
    if (id.isNotEmpty()) {
        queryParams.add("id=$id")
    }
    if (albumId.isNotEmpty()) {
        queryParams.add("albumId=$albumId")
    }
    if (artistId.isNotEmpty()) {
        queryParams.add("artistId=$artistId")
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