package com.craftworks.music.ui.elements

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import coil.compose.AsyncImage
import com.craftworks.music.R
import com.craftworks.music.data.Song
import com.craftworks.music.data.radioList
import com.craftworks.music.formatMilliseconds
import com.craftworks.music.ui.screens.selectedRadioIndex
import com.craftworks.music.ui.screens.showRadioModifyDialog

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongsCard(song: Song, onClick: () -> Unit){
                Card(
                    modifier = Modifier
                        .padding(12.dp, 48.dp, 0.dp, 0.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .combinedClickable(
                            onClick = { onClick(); Log.d("Play", "Clicked Song: " + song.title) },
                            onLongClick = {
                                if (song.isRadio == false) return@combinedClickable
                                showRadioModifyDialog.value = true
                                selectedRadioIndex.intValue =
                                    radioList.indexOf(radioList.firstOrNull { it.name == song.artist && it.media == song.media })
                            },
                            onLongClickLabel = "Modify Radio"
                        ),
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
                            .width(128.dp)
                            .height(172.dp)
                    ) {

                        AsyncImage(
                            model =
                            if (song.imageUrl == Uri.EMPTY && song.isRadio == true)
                                R.drawable.rounded_cell_tower_24
                            else song.imageUrl,
                            placeholder = painterResource(R.drawable.placeholder),
                            fallback = painterResource(R.drawable.placeholder),
                            contentScale = ContentScale.FillHeight,
                            contentDescription = "Album Image",
                            modifier = Modifier
                                .height(128.dp)
                                .width(128.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = song.artist,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HorizontalSongCard(song: Song, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            disabledContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
            disabledContentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ),
        modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 12.dp)
            .combinedClickable(
                onClick = { onClick(); Log.d("Play", "Clicked Song: " + song.title) },
                onLongClick = {
                    println("Add Song To Playlist")
                    showAddSongToPlaylistDialog.value = true
                    songToAddToPlaylist.value = song
                },
                onLongClickLabel = "Add Song To Playlist"
            )
            .clip(RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .height(72.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.imageUrl,
                placeholder = painterResource(R.drawable.placeholder),
                fallback = painterResource(R.drawable.placeholder),
                contentDescription = "Album Image",
                contentScale = ContentScale.FillHeight,
                modifier = Modifier
                    .height(64.dp)
                    .width(64.dp)
                    .padding(4.dp, 0.dp, 0.dp, 0.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                Modifier
                    .padding(end = 12.dp)
                    .weight(1f)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start
                )

                Text(
                    text = song.artist + " • " + song.year,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onBackground.copy(0.75f),
                    modifier = Modifier,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start
                )
            }
            Text(
                text = formatMilliseconds(song.duration.toFloat()),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(end = 12.dp),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End
            )

            var expanded by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier.width(32.dp)
            ) {
                IconButton(
                    modifier = Modifier,
                    onClick = { expanded = true },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.background,
                        disabledContentColor = MaterialTheme.colorScheme.onBackground
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "More menu"
                    )
                }

                DropdownMenu(
                    modifier = Modifier,
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Add To Playlist") },
                        onClick = {
                            println("Add Song To Playlist")
                            showAddSongToPlaylistDialog.value = true
                            songToAddToPlaylist.value = song
                            expanded = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Add To Playlist Icon"
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
@Preview
fun HorizontalSongCardPreview(){
        HorizontalSongCard(song = Song(title = "Very Long Song Title That Overflows", artist = "Song Artist", duration = 69420, imageUrl = Uri.EMPTY, year = "2023", dateAdded = "10/11/23", album = "Album")){
    }
}