package com.craftworks.music.ui.elements

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.session.MediaController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.craftworks.music.data.MediaData
import com.craftworks.music.player.SongHelper
import com.craftworks.music.providers.getAlbum
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Stable
@Composable
fun AlbumCard(
    album: MediaData.Album = MediaData.Album(
        navidromeID = "",
        parent = "",
        album = "",
        title = "",
        name = "",
        songCount = 0,
        duration = 0,
        artistId = "",
        artist = "",
        coverArt = ""
    ),
    mediaController: MediaController? = null,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .padding(12.dp, 12.dp, 0.dp, 0.dp)
            .width(128.dp)
            .height(172.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(album.coverArt)
                    .crossfade(true)
                    .build(),
                contentDescription = "Album Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
            )

            val coroutineScope = rememberCoroutineScope()

            IconButton(
                onClick = { playSelectedAlbum(context, coroutineScope, mediaController, album.navidromeID) },
                modifier = Modifier
                    .padding(6.dp)
                    .background(
                        MaterialTheme.colorScheme.background.copy(alpha = 0.75f),
                        shape = CircleShape
                    )
                    .height(36.dp)
                    .size(36.dp)
                    .align(Alignment.BottomStart)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    tint = MaterialTheme.colorScheme.onBackground,
                    contentDescription = "Play Album",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = album.name ?: "",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )

        Text(
            text = album.artist,
            style = MaterialTheme.typography.bodySmall,
            color = LocalContentColor.current.copy(alpha = 0.75f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

private fun playSelectedAlbum(
    context: Context,
    coroutineScope: CoroutineScope,
    mediaController: MediaController?,
    albumId: String
) {
    coroutineScope.launch {
        val currentAlbum = getAlbum(albumId)

        // Try to play song
        if (currentAlbum?.songs.isNullOrEmpty()) return@launch

        //SongHelper.currentSong = currentAlbum?.songs?.get(0)!!
        SongHelper.currentList = currentAlbum.songs ?: emptyList()
        SongHelper.playStream(
            context,
            Uri.parse(currentAlbum.songs?.get(0)!!.media ?: ""),
            false,
            mediaController
        )
    }
}