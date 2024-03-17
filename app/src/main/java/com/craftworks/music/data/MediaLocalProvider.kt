package com.craftworks.music.data

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf

var localProviderList:MutableList<LocalProvider> = mutableStateListOf(
    //LocalProvider("/Music/", true)
)
var selectedLocalProvider = mutableIntStateOf(0)

data class LocalProvider (
    var directory:String,
    var enabled:Boolean
)