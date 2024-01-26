package com.craftworks.music.data

import androidx.compose.runtime.mutableStateListOf

var navidromeServersList:MutableList<Navidrome> = mutableStateListOf()


data class Navidrome (
    var url:String,
    var username:String,
    val password:String
)