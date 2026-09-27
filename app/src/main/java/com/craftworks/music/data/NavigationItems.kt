package com.craftworks.music.data

import androidx.compose.runtime.Stable
import com.craftworks.music.data.model.Screen
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class BottomNavItem(
    var title: String,
    var icon: Int,
    val screenRoute: Screen,
    var enabled: Boolean = true
)