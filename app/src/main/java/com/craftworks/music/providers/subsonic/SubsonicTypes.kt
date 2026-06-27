package com.craftworks.music.providers.subsonic

import io.ktor.http.Headers
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable

@Serializable
data class SubsonicServerInfo (
    var url:String,
    var username:String,
    var credentials:String,
    var allowSelfSignedCert: Boolean = false
)


@Serializable
data class SubsonicUserResponse (
    val user: SubsonicUser
)
@Serializable
data class SubsonicUser (
    val username: String,
    val adminRole: Boolean
)