package com.craftworks.music.ui.playing

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import androidx.core.graphics.luminance
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.craftworks.music.R
import com.craftworks.music.data.Screen
import com.craftworks.music.data.artistList
import com.craftworks.music.data.selectedArtist
import com.craftworks.music.managers.SettingsManager
import com.craftworks.music.player.SongHelper
import com.craftworks.music.ui.screens.showMoreInfo
import com.gigamole.composefadingedges.marqueeHorizontalFadingEdges
import kotlinx.coroutines.launch

@Preview(showSystemUi = false, device = "spec:parent=pixel_8_pro",
    wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE,
    uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true
)
@Composable
fun NowPlayingPortrait(
    mediaController: MediaController? = null,
    navHostController: NavHostController = rememberNavController(),
    iconColor: Color = Color.White
){
    SideEffect {
        println("Changed iconColor: $iconColor")
    }

    Log.d("RECOMPOSITION", "NowPlaying Portrait")

    // use dark or light colors for icons and text based on the album art luminance.
    val iconTextColor by animateColorAsState(
        targetValue = iconColor,
        animationSpec = tween(1000, 0, FastOutSlowInEasing),
        label = "Animated text color"
    )

    Column {
        // Top padding (for mini-player)
        Spacer(Modifier.height(24.dp))

        // Album Art + Info
        Log.d("RECOMPOSITION", "Album cover or lyrics")

        /* Album Cover + Lyrics */
        AnimatedContent(
            lyricsOpen, label = "Crossfade between lyrics",
            modifier = Modifier
                .heightIn(min = 320.dp, max=420.dp)
                .fillMaxWidth(),
        ) {
            if (it) {
                LyricsView(
                    iconTextColor,
                    false,
                    mediaController,
                    PaddingValues(horizontal = 32.dp, vertical = 16.dp)
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(SongHelper.currentSong.imageUrl)
                        .allowHardware(false)
                        .size(1024)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Album Cover Art",
                    placeholder = painterResource(R.drawable.placeholder),
                    fallback = painterResource(R.drawable.placeholder),
                    contentScale = ContentScale.FillWidth,
                    alignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 16.dp)
                        .aspectRatio(1f)
                        .shadow(4.dp, RoundedCornerShape(24.dp), clip = true)
                    //.background(MaterialTheme.colorScheme.surfaceVariant),
                )
            }
        }

        /* Song Title + Artist*/
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            Log.d("RECOMPOSITION", "Titles and artist")
            SongHelper.currentSong.title.let {
                Text(
                    text = it,
                    fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                    fontWeight = FontWeight.SemiBold,
                    color = iconTextColor,
                    maxLines = 1, overflow = TextOverflow.Visible,
                    softWrap = false,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .marqueeHorizontalFadingEdges(marqueeProvider = { Modifier.basicMarquee() })
                )
            }

            SongHelper.currentSong.artist.let { artistName ->
                Text(
                    text = artistName + if (SongHelper.currentSong.year != 0) " • " + SongHelper.currentSong.year else "",
                    fontSize = MaterialTheme.typography.titleMedium.fontSize,
                    fontWeight = FontWeight.Normal,
                    color = iconTextColor,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Start,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .marqueeHorizontalFadingEdges(marqueeProvider = { Modifier.basicMarquee() })
//                            .clickable {
//                                try {
//                                    selectedArtist = artistList.firstOrNull() {
//                                        it.name.equals(
//                                            artistName, ignoreCase = true
//                                        )
//                                    }!!
//                                } catch (e: Exception) {
//                                    Log.d("NAVIDROME", "Artist not found!")
//                                    return@clickable
//                                }
//                                coroutine.launch {
//                                    scaffoldState.bottomSheetState.partialExpand()
//                                }
//                                navHostController.navigate(Screen.AristDetails.route) {
//                                    launchSingleTop = true
//                                }
//                            }
                )
            }

            Spacer(Modifier.height(8.dp))

            if (showMoreInfo.value) {
                Text(
                    text = "${SongHelper.currentSong.format.uppercase()} • ${SongHelper.currentSong.bitrate} • ${
                        if (SongHelper.currentSong.navidromeID == "Local")
                            stringResource(R.string.Source_Local)
                        else
                            stringResource(R.string.Source_Navidrome)
                    } ",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Light,
                    color = iconTextColor.copy(alpha = 0.5f),
                    maxLines = 1,
                    textAlign = TextAlign.Start
                )
            }
        }

        PlaybackProgressSlider(iconTextColor, mediaController)

        //region Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top buttons
            Row(
                modifier = Modifier
                    .height(98.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShuffleButton(iconTextColor, mediaController, 32.dp)

                PreviousSongButton(iconTextColor, mediaController, 48.dp)

                PlayPauseButtonUpdating(iconTextColor, mediaController, 92.dp)

                NextSongButton(iconTextColor, mediaController, 48.dp)

                RepeatButton(iconTextColor, mediaController, 32.dp)
            }

            Row(
                modifier = Modifier
                    .height(64.dp)
                    .width(256.dp)
                    .weight(.75f)
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LyricsButton(iconTextColor, 64.dp)

                DownloadButton(iconTextColor, 64.dp)
            }
        }
        //endregion
    }
}