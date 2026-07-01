package com.craftworks.music

import android.app.Application
import com.craftworks.music.managers.MediaProviderManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ChoraApplication : Application(){
    override fun onCreate() {
        super.onCreate()
        MediaProviderManager.init(this)
    }
}
