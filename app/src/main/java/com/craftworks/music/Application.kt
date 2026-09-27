package com.craftworks.music

import android.app.Application
import com.craftworks.music.managers.MediaProviderManager
import com.craftworks.music.managers.MigrationManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ChoraApplication : Application(){
    override fun onCreate() {
        super.onCreate()
        MigrationManager.init(this)
        MediaProviderManager.init(this)
    }
}
