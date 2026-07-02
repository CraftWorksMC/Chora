package com.craftworks.music.providers.subsonic

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubsonicProviderData (
    var url:String,
    var username:String,
    var credentials:String? = null,
    var allowSelfSignedCert: Boolean = false,
)

@Serializable
data class SubsonicResponse(
    @SerialName("subsonic-response")
    val subsonicResponse: SubsonicBody
)

@Serializable
data class SubsonicBody(
    val status: String,
    val version: String,
    val error: SubsonicError? = null,

    val user: SubsonicUser? = null,
)

@Serializable
data class SubsonicError(val code: Int, val message: String)

@Serializable
data class SubsonicUser (
    val username: String,
    val adminRole: Boolean
)