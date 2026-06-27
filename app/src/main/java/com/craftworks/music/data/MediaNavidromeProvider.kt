package com.craftworks.music.legacy.data

import kotlinx.serialization.Serializable

// LEGACY CODE! MUST NOT BE USED
// TODO("Delete legacy file")
@Serializable
data class NavidromeProvider (
    val id: String = "0",
    var url:String,
    var username:String,
    val password:String,
    val enabled:Boolean? = true,
    var allowSelfSignedCert: Boolean? = false,
    // List of library folders and if they're enabled or not.
    var libraryIds: List<Pair<NavidromeLibrary, Boolean>> = listOf(Pair(NavidromeLibrary(0, "Media Library"), true))
)

@Serializable
data class NavidromeLibrary (
    val id: Int = 0,
    var name:String,
)