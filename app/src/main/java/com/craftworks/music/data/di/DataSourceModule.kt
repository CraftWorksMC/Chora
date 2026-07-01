package com.craftworks.music.data.di

import android.content.Context
import com.craftworks.music.data.datasource.lrclib.LrclibDataSource
import com.craftworks.music.data.datasource.netease.NeteaseDataSource
import com.craftworks.music.managers.settings.MediaProviderSettingsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataSourceModule {
    @Singleton
    @Provides
    fun provideLrcLibDataSource(
        settingsManager: MediaProviderSettingsManager,
        @ApplicationContext context: Context
    ): LrclibDataSource {
        return LrclibDataSource(settingsManager, context)
    }

    @Singleton
    @Provides
    fun provideNetEaseDataSource(
        @ApplicationContext context: Context
    ): NeteaseDataSource {
        return NeteaseDataSource(context)
    }
}