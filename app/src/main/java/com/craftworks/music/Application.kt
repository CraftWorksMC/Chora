package com.craftworks.music

import android.app.Application
import com.craftworks.music.managers.LocalProviderManager
import com.craftworks.music.managers.NavidromeManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ChoraApplication : Application(){
    override fun onCreate() {
        super.onCreate()
        NavidromeManager.init(this)
        LocalProviderManager.init(this)
    }
}
