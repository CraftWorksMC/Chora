package com.craftworks.music.data

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class BottomNavItem(
    var title: String,
    var icon: Int,
    val screenRoute: String,
    var enabled: Boolean = true
)