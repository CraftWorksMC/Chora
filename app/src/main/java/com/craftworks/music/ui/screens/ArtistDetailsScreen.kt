package com.craftworks.music.ui.screens

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.craftworks.music.R
import com.craftworks.music.data.model.MediaData
import com.craftworks.music.data.model.Screen
import com.craftworks.music.data.model.toAlbum
import com.craftworks.music.data.model.toMediaItem
import com.craftworks.music.player.SongHelper
import com.craftworks.music.providers.getAlbum
import com.craftworks.music.providers.getArtistDetails
import com.craftworks.music.ui.elements.AlbumCard
import com.craftworks.music.ui.elements.dialogs.dialogFocusable
import com.craftworks.music.ui.viewmodels.ArtistsScreenViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLEncoder
import androidx.core.net.toUri

@OptIn(ExperimentalComposeUiApi::class)
@ExperimentalFoundationApi
@Composable
fun ArtistDetails(
    navHostController: NavHostController = rememberNavController(),
    mediaController: MediaController? = null,
    artistId: String,
    viewModel: ArtistsScreenViewModel = hiltViewModel()
) {
    val leftPadding =
        if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) 0.dp else 80.dp

    val showLoading by viewModel.isLoading.collectAsStateWithLifecycle()
//    var artist by remember(artistId) {
//        mutableStateOf<MediaData.Artist?>(null)
//    }
    val artist = viewModel.selectedArtist.collectAsStateWithLifecycle().value
    val artistAlbums = viewModel.artistAlbums.collectAsStateWithLifecycle().value
    val context = LocalContext.current

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
        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = leftPadding,
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                    end = 12.dp
                )
                .dialogFocusable(),
            //verticalArrangement = Arrangement.spacedBy(6.dp),
            columns = GridCells.Adaptive(128.dp)
        ) {
            // Group songs by their source (Local or Navidrome)
            val groupedAlbums =
                artistAlbums.groupBy { it.mediaMetadata.recordingYear }
                    .toSortedMap(compareByDescending { it })

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
                    artist?.description?.let { description ->
                        var expanded by remember { mutableStateOf(false) }
                        Column (
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .fillMaxWidth()
                                .heightIn(min = if (description.isBlank()) 0.dp else 32.dp)
                                .clip(RoundedCornerShape(0.dp, 0.dp, 12.dp, 12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .animateContentSize()
                                .clickable {
                                    expanded = !expanded
                                },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (description.isNotBlank()) {
                                Text(
                                    text = description.split("<a target").first(),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.Light,
                                    fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                                    textAlign = TextAlign.Start,
                                    maxLines = if (expanded) 100 else 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(6.dp, 6.dp, 6.dp, 0.dp)
                                )
                                // Show more on last.fm  button
                                if (expanded) {
                                    Button(
                                        onClick = {
                                            val regex = Regex("""<a\s+(?:[^>]*?\s+)?href="([^"]*)"""")
                                            val matchResult = regex.find(description)
                                            val extractedUrl = matchResult?.groups?.get(1)?.value

                                            extractedUrl?.let { url ->
                                                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                                                context.startActivity(intent)
                                            }
                                        },
                                        modifier = Modifier.widthIn(128.dp).padding(vertical = 6.dp),
                                    ) {
                                        Text(
                                            text = "Last.FM",
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Play and shuffle buttons
            item(span = { GridItemSpan(maxLineSpan) }) {
                val coroutineScope = rememberCoroutineScope()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val allArtistSongsList = artistAlbums.map {
                                    it.mediaMetadata.extras?.getString("navidromeID").let {
                                        val album = getAlbum(it ?: "")
                                        album?.subList(1, album.size) ?: emptyList()
                                    }
                                }

                                SongHelper.play(
                                    allArtistSongsList.flatten(),
                                    0,
                                    mediaController
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onBackground
                        ),
                        modifier = Modifier.widthIn(min = 128.dp, max = 320.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(24.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, "Play Album")
                            Text(stringResource(R.string.Action_Play))
                        }
                    }
                    Button(
                        onClick = {
                            coroutineScope.launch {

                                val allArtistSongsList = artist?.album?.map {
                                    val album = getAlbum(it.navidromeID)
                                    album?.subList(1, album.size) ?: emptyList()
                                }

                                mediaController?.shuffleModeEnabled = true
                                val random = allArtistSongsList?.indices?.random() ?: 0
                                SongHelper.play(
                                    allArtistSongsList?.flatten() ?: emptyList(),
                                    random,
                                    mediaController
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onBackground
                        ),
                        modifier = Modifier.widthIn(min = 128.dp, max = 320.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(24.dp)
                        ) {
                            Icon(
                                ImageVector.vectorResource(R.drawable.round_shuffle_28),
                                "Shuffle Album"
                            )
                            Text(stringResource(R.string.Action_Shuffle))
                        }
                    }
                }
            }

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

            groupedAlbums.forEach { (groupName, albumsInGroup) ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = groupName.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(top = 12.dp)
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