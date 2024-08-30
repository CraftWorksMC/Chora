package com.craftworks.music.data

import kotlinx.serialization.Serializable

@Serializable
data class BottomNavItem(
    var title: String,
    var icon: Int,
    val screenRoute: String,
    var enabled: Boolean = true
)