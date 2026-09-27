package com.craftworks.music.data.repository

import com.craftworks.music.data.model.LibraryType
import com.craftworks.music.managers.MediaProviderManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StarredRepository @Inject constructor() {
     suspend fun starItem(ids: List<String>, type: LibraryType) {
         MediaProviderManager.currentProvider.value?.createFavorite(ids, type);
     }

     suspend fun unStarItem(ids: List<String>, type: LibraryType) {
         MediaProviderManager.currentProvider.value?.deleteFavorite(ids, type);
     }
}
