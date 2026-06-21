package com.craftworks.music.managers

import com.craftworks.music.providers.MediaProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object MediaProviderManager {
    private val providers = mutableMapOf<String, MediaProvider>()

    private var _currentProvider = MutableStateFlow<MediaProvider?>(null)
    val currentProvider: StateFlow<MediaProvider?> = _currentProvider.asStateFlow()


}