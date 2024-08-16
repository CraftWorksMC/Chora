package com.craftworks.music.ui.elements

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFilter
import androidx.media3.session.MediaController
import coil.compose.AsyncImage
import com.craftworks.music.R
import com.craftworks.music.data.MediaData
import com.craftworks.music.data.songsList
import com.craftworks.music.player.SongHelper
import com.craftworks.music.providers.navidrome.NavidromeManager
import com.craftworks.music.providers.navidrome.getNavidromeAlbumSongs
import com.craftworks.music.ui.screens.selectedAlbum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Preview
@Composable
fun AlbumCard(album: MediaData.Album = MediaData.Album(navidromeID = "", parent = "", album = "", title = "", name = "", songCount = 0, duration = 0, artistId = "", artist = "", coverArt = ""),
              mediaController: MediaController? = null,
              onClick: () -> Unit = {}){
    Card(
        onClick = { onClick() },
        modifier = Modifier
            .padding(12.dp, 12.dp, 0.dp, 0.dp),
            //.aspectRatio(0.8f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            disabledContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
            disabledContentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                //.widthIn(min = 96.dp, max = 256.dp)
                .width(128.dp)
                .height(172.dp),
                //.wrapContentHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box (modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)){
                AsyncImage(
                    model = album.coverArt,
                    placeholder = painterResource(R.drawable.placeholder),
                    fallback = painterResource(R.drawable.placeholder),
                    contentScale = ContentScale.FillHeight,
                    contentDescription = "Album Image",
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                )

                val coroutineScope = rememberCoroutineScope()

                Button(
                    onClick = {
                        // Play First Song in Album
                        coroutineScope.launch {
                            selectedAlbum = album

                            // Fetch songs if the list is empty
                            if (selectedAlbum?.songs.isNullOrEmpty()) {
                                if (NavidromeManager.checkActiveServers()){
                                    selectedAlbum?.navidromeID?.let { albumId ->
                                        withContext(Dispatchers.IO) {
                                            getNavidromeAlbumSongs(albumId)
                                        }
                                    }
                                }
                                else {
                                    selectedAlbum?.songs = songsList.fastFilter { it.album == selectedAlbum?.name }
                                }
                            }

                            // Try to play song
                            selectedAlbum?.songs?.let { songs ->
                                if (songs.isEmpty()) return@launch

                                SongHelper.currentSong = songs[0]
                                SongHelper.currentList = songs
                                SongHelper.playStream(Uri.parse(songs[0].media ?: ""), false, mediaController)
                            }
                        }
                    },
                    shape = CircleShape,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.BottomStart)
                        .padding(6.dp),
                    contentPadding = PaddingValues(2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.background.copy(0.5f)
                    )
                ) {
                    
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        tint = MaterialTheme.colorScheme.onBackground,
                        contentDescription = "Play Album",
                        modifier = Modifier
                            .height(48.dp)
                            .size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            album.name?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }


            Text(
                    text = album.artist,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onBackground.copy(0.75f),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
        }
    }
}