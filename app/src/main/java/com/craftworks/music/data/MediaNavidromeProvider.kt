package com.craftworks.music.data

import androidx.compose.runtime.mutableStateListOf

var navidromeServersList:MutableList<NavidromeProvider> = mutableStateListOf()

data class NavidromeProvider (
    var url:String,
    var username:String,
    val password:String
)