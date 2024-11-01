package com.craftworks.music.data

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import kotlinx.serialization.Serializable

var localProviderList:MutableList<LocalProvider> = mutableStateListOf(
    //LocalProvider("/Music/", true)
)
var selectedLocalProvider = mutableIntStateOf(0)

@Serializable
data class LocalProvider (
    var directory:String,
    var enabled:Boolean
)