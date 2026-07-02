package com.craftworks.music.providers.subsonic

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface SubsonicService {
    @GET("rest/getUser.view")
    fun authenticate(@Query("username") username: String) : Call<SubsonicResponse>
}