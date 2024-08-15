package com.craftworks.music.data

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf

var navidromeServersList:MutableList<NavidromeProvider> = mutableStateListOf()
var selectedNavidromeServerIndex = mutableIntStateOf(0)
var useNavidromeServer = mutableStateOf(false)

data class NavidromeProvider (
    val id: String = "0",
    var url:String,
    var username:String,
    val password:String,
    val enabled:Boolean? = true,
    var allowSelfSignedCert: Boolean? = false
)
