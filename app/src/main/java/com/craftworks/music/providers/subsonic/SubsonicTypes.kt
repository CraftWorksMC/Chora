package com.craftworks.music.providers.subsonic

import com.craftworks.music.data.model.MediaProviderData
import io.ktor.http.Headers
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable

@Serializable
data class SubsonicProviderData (
    var url:String,
    var username:String,
    var credentials:String,
    var allowSelfSignedCert: Boolean = false,
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