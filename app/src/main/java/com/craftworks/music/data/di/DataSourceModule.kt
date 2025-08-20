package com.craftworks.music.data.di

import com.craftworks.music.data.NavidromeProvider
import com.craftworks.music.data.datasource.local.LocalDataSource
import com.craftworks.music.data.datasource.navidrome.NavidromeDataSource
import com.craftworks.music.managers.SettingsManager
import com.craftworks.music.providers.local.LocalProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataSourceModule {

    @Singleton
    @Provides
    fun provideLocalDataSource(
        localProvider: LocalProvider,
        settingsManager: SettingsManager
    ): LocalDataSource {
        return LocalDataSource(localProvider, settingsManager)
    }

    @Singleton
    @Provides
    fun provideNavidromeDataSource(): NavidromeDataSource {
        return NavidromeDataSource()
    }
}