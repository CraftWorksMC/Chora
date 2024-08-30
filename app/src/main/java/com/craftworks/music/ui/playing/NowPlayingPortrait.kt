package com.craftworks.music.ui.playing

import android.content.res.Configuration
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.craftworks.music.R
import com.craftworks.music.data.Screen
import com.craftworks.music.data.artistList
import com.craftworks.music.data.selectedArtist
import com.craftworks.music.fadingEdge
import com.craftworks.music.player.SongHelper
import com.craftworks.music.ui.screens.showMoreInfo
import kotlinx.coroutines.launch

@Preview(showSystemUi = false, showBackground = true, device = "spec:parent=pixel_8_pro",
    wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingPortrait(
    mediaController: MediaController? = null,
    scaffoldState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
    navHostController: NavHostController = rememberNavController()
){
    Log.d("RECOMPOSITION", "NowPlayingPortrait")
    Column {
        // Top padding (for mini-player)
        Spacer(Modifier.height(48.dp))

        // Album Art + Info
        val textFadingEdge = Brush.horizontalGradient(0.85f to Color.Red, 1f to Color.Transparent)

        Column(modifier = Modifier.heightIn(min=420.dp)) {
            Log.d("RECOMPOSITION", "Album cover or lyrics")
            /* Album Cover */
            Box(modifier = Modifier
                .heightIn(min = 320.dp)
                .fillMaxWidth(),
                contentAlignment = Alignment.Center){
                AnimatedContent(lyricsOpen, label = "Crossfade between lyrics") {
                    if (it){
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                            contentAlignment = Alignment.Center){
                            LyricsView(false, mediaController)
                        }
                    }
                    else {
                        AsyncImage(
                            model = SongHelper.currentSong.imageUrl,
                            contentDescription = "Album Cover",
                            placeholder = painterResource(R.drawable.placeholder),
                            fallback = painterResource(R.drawable.placeholder),
                            contentScale = ContentScale.FillWidth,
                            alignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp)
                                .aspectRatio(1f)
                                .shadow(4.dp, RoundedCornerShape(24.dp), clip = true)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
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
                        text =
                        if (SongHelper.currentSong.isRadio == true)
                            it.split(" - ").last()
                        else
                            it,
                        fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1, overflow = TextOverflow.Visible,
                        softWrap = false,
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fadingEdge(textFadingEdge)
                    )
                }

                Row(modifier = Modifier
                    .fillMaxWidth()
                    .fadingEdge(textFadingEdge)) {
                    val coroutine = rememberCoroutineScope()
                    SongHelper.currentSong.artist.let { artistName ->
                        Text(
                            text = artistName,
                            fontSize = MaterialTheme.typography.titleMedium.fontSize,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1, overflow = TextOverflow.Visible,
                            softWrap = false,
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    try {
                                        selectedArtist =
                                            artistList.firstOrNull() {
                                                it.name.equals(
                                                    artistName,
                                                    ignoreCase = true
                                                )
                                            }!!
                                    } catch (e: Exception) {
                                        Log.d("NAVIDROME", "Artist not found!")
                                        return@clickable
                                    }
                                    coroutine.launch {
                                        scaffoldState.bottomSheetState.partialExpand()
                                    }
                                    navHostController.navigate(Screen.AristDetails.route) {
                                        launchSingleTop = true
                                    }
                                }
                        )
                    }
                    if (SongHelper.currentSong.year != 0){
                        SongHelper.currentSong.year?.let { year ->
                            Text(
                                text = " • $year",
                                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1, overflow = TextOverflow.Visible,
                                softWrap = false,
                                textAlign = TextAlign.Start,
                                modifier = Modifier

                            )
                        }
                    }
                }

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
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        maxLines = 1,
                        textAlign = TextAlign.Start
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Seek Bar
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Log.d("RECOMPOSITION", "SliderUpdating")
            SliderUpdating(false, mediaController)
        }

        //region Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .height(98.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShuffleButton(32.dp, mediaController)

                MainButtons(mediaController)

                RepeatButton(32.dp, mediaController)
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
                LyricsButton(64.dp)

                DownloadButton(64.dp)
            }
        }
        //endregion
    }
}