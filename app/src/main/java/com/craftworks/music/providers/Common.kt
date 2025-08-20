package com.craftworks.music.providers

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.craftworks.music.data.model.MediaData
import com.craftworks.music.data.model.playlistList
import com.craftworks.music.data.model.toMediaItem
import com.craftworks.music.data.model.toSong
import com.craftworks.music.managers.NavidromeManager
import com.craftworks.music.managers.SettingsManager
import com.craftworks.music.providers.local.localPlaylistImageGenerator
import com.craftworks.music.providers.navidrome.sendNavidromeGETRequest
import com.craftworks.music.ui.elements.dialogs.songToAddToPlaylist
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope


suspend fun getAlbum(albumId: String, ignoreCachedResponse: Boolean = false): List<MediaItem>? = coroutineScope {
    val deferredAlbum = async {
        sendNavidromeGETRequest("getAlbum.view?id=${albumId}&f=json", ignoreCachedResponse)
            .filterIsInstance<MediaItem>()
    }
    deferredAlbum.await()
}

//region Playlists
suspend fun getPlaylists(context: Context, ignoreCachedResponse: Boolean = false): List<MediaItem> = coroutineScope {
    val deferredPlaylists = mutableListOf<Deferred<List<MediaItem>>>()

    if (NavidromeManager.checkActiveServers()) {
        deferredPlaylists.add(async {
            sendNavidromeGETRequest("getPlaylists.view?f=json", ignoreCachedResponse).filterIsInstance<MediaItem>()
        })
    }
    deferredPlaylists.add(
        async {
            playlistList.map {
                it.toMediaItem().buildUpon().setMediaMetadata(
                    it.toMediaItem().mediaMetadata.buildUpon()
                        .setArtworkData(localPlaylistImageGenerator(it.songs?.map { it.toMediaItem() } ?: emptyList(), context), MediaMetadata.PICTURE_TYPE_OTHER)
                        .build()
                ).build()
            }
        }
    )

    deferredPlaylists.awaitAll().flatten()
}

suspend fun createPlaylist(playlistName: String, addToNavidrome: Boolean, context: Context) {
    if (NavidromeManager.checkActiveServers() && addToNavidrome) {
        println("creating navidrome $playlistName")
        sendNavidromeGETRequest("createPlaylist.view?name=$playlistName&songId=${songToAddToPlaylist.value.mediaMetadata.extras?.getString("navidromeID")}", true) // Always ignore cache when creating playlists
    }
    else {
        playlistList.add(
            MediaData.Playlist(
                "Local_$playlistName",
                playlistName,
                Uri.EMPTY.toString(),
                changed = "",
                created = "",
                duration = 0,
                songCount = 0,
                songs = mutableListOf(songToAddToPlaylist.value.toSong())
            )
        )
        SettingsManager(context).saveLocalPlaylists()
    }
}
suspend fun deletePlaylist(playlistID: String, context: Context){
    if (NavidromeManager.checkActiveServers() && !playlistID.startsWith("Local"))
        sendNavidromeGETRequest("deletePlaylist.view?id=$playlistID", true) // Always ignore cache when deleting playlists
    else{
        playlistList = playlistList.filter { it.navidromeID != playlistID }.toMutableList()
        SettingsManager(context).saveLocalPlaylists()
    }
}
suspend fun addSongToPlaylist(playlistId: String, songID: String, context: Context) {
    println("Adding song $songID to playlist $playlistId")

    if (NavidromeManager.checkActiveServers() && !playlistId.startsWith("Local"))
        sendNavidromeGETRequest("updatePlaylist.view?playlistId=${playlistId}&songIdToAdd=$songID", true) // Always ignore cache when modifying playlists
    else {
        println("Adding song to local playlist")
        val index = playlistList.indexOfFirst { it.navidromeID == playlistId }
        playlistList[index].songs = playlistList[index].songs?.plus(songToAddToPlaylist.value.toSong())?.toMutableList()

        playlistList[index].coverArt =
            localPlaylistImageGenerator(playlistList[index].songs?.map { it.toMediaItem() } ?: emptyList(), context).toString()
        SettingsManager(context).saveLocalPlaylists()
    }
}