package com.craftworks.music.providers.subsonic

import retrofit2.Call
import retrofit2.http.GET

interface SubsonicService {
    @GET("rest/getUser.view")
    fun authenticate(username: String) : Call<SubsonicUserResponse>
}