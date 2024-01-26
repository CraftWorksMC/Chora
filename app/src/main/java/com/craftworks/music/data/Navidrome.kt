package com.craftworks.music.data

import androidx.compose.runtime.mutableStateListOf

var navidromeServersList:MutableList<Navidrome> = mutableStateListOf()


data class Navidrome (
    val url:String,
    val username:String,
    val password:String
)