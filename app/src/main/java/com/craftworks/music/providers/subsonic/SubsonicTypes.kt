package com.craftworks.music.providers.subsonic

import io.ktor.http.Headers
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable

@Serializable
data class SubsonicServerInfo (
    var url:String,
    var username:String,
    val password:String,
    var allowSelfSignedCert: Boolean = false
)
