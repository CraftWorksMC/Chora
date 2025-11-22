package com.craftworks.music.data.di

import android.content.Context
import com.craftworks.music.data.datasource.local.LocalDataSource
import com.craftworks.music.data.datasource.lrclib.LrclibDataSource
import com.craftworks.music.data.datasource.navidrome.NavidromeDataSource
import com.craftworks.music.managers.settings.AppearanceSettingsManager
import com.craftworks.music.managers.settings.LocalDataSettingsManager
import com.craftworks.music.managers.settings.MediaProviderSettingsManager
import com.craftworks.music.providers.local.LocalProvider
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
    fun provideLocalDataSource(
        localProvider: LocalProvider,
        localDataSettingsManager: LocalDataSettingsManager,
        appearanceSettingsManager: AppearanceSettingsManager
    ): LocalDataSource {
        return LocalDataSource(localProvider, localDataSettingsManager, appearanceSettingsManager)
    }

    @Singleton
    @Provides
    fun provideNavidromeDataSource(): NavidromeDataSource {
        return NavidromeDataSource()
    }

    @Singleton
    @Provides
    fun provideLrcLibDataSource(
        settingsManager: MediaProviderSettingsManager,
        @ApplicationContext context: Context
    ): LrclibDataSource {
        return LrclibDataSource(settingsManager, context)
    }
}