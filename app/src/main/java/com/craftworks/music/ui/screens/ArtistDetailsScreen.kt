package com.craftworks.music.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.craftworks.music.R
import com.craftworks.music.data.MediaData
import com.craftworks.music.data.Screen
import com.craftworks.music.data.toAlbum
import com.craftworks.music.data.toMediaItem
import com.craftworks.music.providers.getArtistDetails
import com.craftworks.music.ui.elements.AlbumCard
import com.craftworks.music.ui.elements.dialogs.dialogFocusable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLEncoder

@OptIn(ExperimentalComposeUiApi::class)
@ExperimentalFoundationApi
@Composable
fun ArtistDetails(
    navHostController: NavHostController = rememberNavController(),
    mediaController: MediaController? = null,
    artistId: String
) {
    val leftPadding = if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) 0.dp else 80.dp

    var showLoading by remember { mutableStateOf(false) }
    var artist by remember(artistId) {
        mutableStateOf<MediaData.Artist?>(null)
    }

    LaunchedEffect(artistId) {
        val loadingJob = launch {
            delay(1000)
            if (artist?.name.isNullOrBlank()) {
                showLoading = true
            }
        }

        // Load the album
        getArtistDetails(artistId).let {
            artist = it
        }

        loadingJob.cancel()
        showLoading = false
    }

    // Loading spinner
    AnimatedVisibility(
        visible = showLoading,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = leftPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 6.dp
            )
            Text(
                text = "Loading",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }

    // Main Content
    AnimatedVisibility(
        visible = artist?.name?.isNotBlank() == true,
        enter = fadeIn()
    ) {
        LazyVerticalGrid (
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = leftPadding,
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                )
                .dialogFocusable(),
            //verticalArrangement = Arrangement.spacedBy(6.dp),
            columns = GridCells.Adaptive(128.dp)
        ) {
            // Group songs by their source (Local or Navidrome)
            val groupedAlbums = artist?.album?.map { it.toMediaItem() }?.groupBy { it.mediaMetadata.recordingYear }?.toSortedMap(compareByDescending { it })

            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .height(192.dp)
                            .fillMaxWidth()
                    ) {
                        //Image and Name
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(artist?.artistImageUrl)
                                .allowHardware(false)
                                .size(256)
                                .crossfade(true)
                                .build(),
                            placeholder = painterResource(R.drawable.s_a_username),
                            fallback = painterResource(R.drawable.s_a_username),
                            contentScale = ContentScale.FillWidth,
                            contentDescription = "Artist Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                //.fadingEdge(imageFadingEdge)
                                .clip(
                                    if (artist?.description != "") RoundedCornerShape(
                                        12.dp,
                                        12.dp,
                                        0.dp,
                                        0.dp
                                    )
                                    else RoundedCornerShape(12.dp)
                                )
                                .blur(12.dp)
                        )
                        Button(
                            onClick = {
                                navHostController.popBackStack()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .padding(top = 12.dp, start = 12.dp)
                                .size(32.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                contentColor = MaterialTheme.colorScheme.onBackground
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                tint = MaterialTheme.colorScheme.onBackground,
                                contentDescription = "Go Back",
                                modifier = Modifier
                                    .height(32.dp)
                                    .size(32.dp)
                            )
                        }


                        // Album Name and Artist
                        Column(modifier = Modifier.align(Alignment.BottomCenter)) {
                            Text(
                                text = artist?.name.toString(),
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                                lineHeight = 32.sp
                            )
                        }
                    }

                    // Description
                    artist?.description?.let {
                        var expanded by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .fillMaxWidth()
                                .heightIn(min = if (it.isBlank()) 0.dp else 32.dp)
                                .clip(RoundedCornerShape(0.dp, 0.dp, 12.dp, 12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .animateContentSize()
                                .clickable {
                                    expanded = !expanded
                                }) {
                            if (it.isNotBlank()) {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.Light,
                                    fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                                    textAlign = TextAlign.Start,
                                    maxLines = if (expanded) 100 else 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }
                    }
                }
            }

            /* Play and shuffle buttons
        Row (modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = {
                    if (artistSongs.isEmpty()) return@Button
                    SongHelper.currentSong = artistSongs[0]
                    SongHelper.currentList = artistSongs
                    artistSongs[0].media?.let { SongHelper.playStream(context, Uri.parse(it), false, mediaController)}
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onBackground),
                modifier = Modifier.width(128.dp)
            ) {
                Row (verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(24.dp)) {
                    Icon(Icons.Rounded.PlayArrow, "Play Album")
                    Text(stringResource(R.string.Action_Play))
                }
            }
            Button(
                onClick = {
                    if (artistSongs.isEmpty()) return@Button
                    shuffleSongs.value = true
                    mediaController?.shuffleModeEnabled = true

                    val random = artistSongs.indices.random()
                    SongHelper.currentSong = artistSongs[random]
                    SongHelper.currentList = artistSongs
                    artistSongs[random].media?.let { SongHelper.playStream(context, Uri.parse(it), false, mediaController)}
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onBackground),
                modifier = Modifier.width(128.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(24.dp)) {
                    Icon(ImageVector.vectorResource(R.drawable.round_shuffle_28), "Shuffle Album")
                    Text(stringResource(R.string.Action_Shuffle))
                }
            }
        }
        */

            /* Discography header */
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(R.string.Screen_Discography) + ":",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                    modifier = Modifier.padding(start = 12.dp, top = 6.dp)
                )
            }

            groupedAlbums?.forEach { (groupName, albumsInGroup) ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = groupName.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp).padding(top = 12.dp)
                    )
                }
                itemsIndexed(albumsInGroup) { index, album ->
                    AlbumCard(
                        album = album,
                        mediaController = mediaController,
                        onClick = {
                            val album = album.toAlbum()
                            val encodedImage = URLEncoder.encode(album.coverArt, "UTF-8")
                            navHostController.navigate(Screen.AlbumDetails.route + "/${album.navidromeID}/$encodedImage") {
                                launchSingleTop = true
                            }
                            selectedAlbum = album
                        })
                }
            }
        }
    }
}