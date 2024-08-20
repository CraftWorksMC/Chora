package com.craftworks.music.ui.viewmodels

import android.util.Log

// Interface for ViewModels that can reload
interface ReloadableViewModel {
    fun reloadData()
}

object GlobalViewModels {
    private val viewModels = mutableSetOf<ReloadableViewModel>()

    fun registerViewModel(viewModel: ReloadableViewModel) {
        viewModels.add(viewModel)
    }

    // Refresh functions
    fun refreshAll() {
        Log.d("NAVIDROME", "Reloading all ViewModels")
        viewModels.forEach { it.reloadData() }
    }

//    fun refreshHomeScreen() {
//        viewModels.filterIsInstance<HomeScreenViewModel>().forEach { it.reloadData() }
//    }
//
//    fun refreshSongsScreen() {
//        viewModels.filterIsInstance<SongsScreenViewModel>().forEach { it.reloadData() }
//    }
}
