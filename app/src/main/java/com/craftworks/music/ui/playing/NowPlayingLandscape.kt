package com.craftworks.music.ui.playing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.craftworks.music.R
import com.craftworks.music.lyrics.LyricsManager
import com.craftworks.music.player.SongHelper
import com.gigamole.composefadingedges.marqueeHorizontalFadingEdges

@Preview(device = "id:tv_1080p", showBackground = true, showSystemUi = true)
@Preview(device = "spec:parent=pixel_6,orientation=landscape", showBackground = true, showSystemUi = true)
@Composable
fun NowPlayingLandscape(
    mediaController: MediaController? = null,
    navHostController: NavHostController = rememberNavController(),
    iconColor: Color = Color.Black
){
    // use dark or light colors for icons and text based on the album art luminance.
    val iconTextColor by animateColorAsState(
        targetValue = iconColor,
        animationSpec = tween(1000, 0, FastOutSlowInEasing),
        label = "Animated text color"
    )

    Row (
        Modifier.padding(start = 80.dp)
    ) {
        Column(
            Modifier.weight(1f).widthIn(min = 512.dp).fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            // Album Art
            if (LocalConfiguration.current.screenHeightDp.dp > 512.dp){
                Column(
                    Modifier.focusable(false).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    /* Album Cover + Lyrics */
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
                            .fillMaxHeight(0.6f)
                            .aspectRatio(1f)
                            .shadow(4.dp, RoundedCornerShape(24.dp), clip = true)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                }
            }

            /* Image (When lyrics open) + Song Title + Artist*/
            Row {
                AnimatedVisibility(
                    visible = lyricsOpen && LocalConfiguration.current.screenHeightDp.dp < 512.dp,
                    modifier = Modifier
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(SongHelper.currentSong.imageUrl)
                            .allowHardware(false)
                            .size(256)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Album Cover Art",
                        placeholder = painterResource(R.drawable.placeholder),
                        fallback = painterResource(R.drawable.placeholder),
                        contentScale = ContentScale.FillWidth,
                        alignment = Alignment.Center,
                        modifier = Modifier
                            .padding(start = 24.dp, end = 12.dp)
                            .height(64.dp)
                            .aspectRatio(1f)
                            .shadow(4.dp, RoundedCornerShape(6.dp), clip = true)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                }

                if (LocalConfiguration.current.screenHeightDp.dp < 512.dp)
                    Spacer(Modifier.animateContentSize().width(if (lyricsOpen) 0.dp else 24.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Top
                ) {
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
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }

            //Spacer(Modifier.height(24.dp))

            if (SongHelper.currentSong.isRadio == true){
                Spacer(Modifier.height(48.dp))
            }
            else {
                PlaybackProgressSlider(iconTextColor, mediaController)
            }

            Row(
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShuffleButton(iconTextColor, mediaController, 32.dp)

                PreviousSongButton(iconTextColor, mediaController, 48.dp)

                PlayPauseButtonUpdating(iconTextColor, mediaController, 92.dp)

                NextSongButton(iconTextColor, mediaController, 48.dp)

                RepeatButton(iconTextColor, mediaController, 32.dp)
            }
            if (LocalConfiguration.current.screenHeightDp.dp < 512.dp){
                Row(
                    modifier = Modifier
                        .wrapContentHeight()
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LyricsButton(iconTextColor, 48.dp)
                }
            }
        }

        val lyrics by LyricsManager.Lyrics.collectAsStateWithLifecycle()

        if (LocalConfiguration.current.screenHeightDp.dp < 512.dp){
            Crossfade(
                lyricsOpen,
                label = ""
            ) {
                if (it){
                    Box(Modifier
                        .padding(32.dp)
                        .fillMaxHeight()
                        .aspectRatio(1f)
                    ){
                        LyricsView(
                            iconTextColor,
                            true,
                            mediaController,
                            PaddingValues(horizontal = 32.dp, vertical = 16.dp)
                        )
                    }
                }
                else {
                    /* Album Cover + Lyrics */
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
                            .padding(32.dp)
                            .fillMaxHeight()
                            .aspectRatio(1f)
                            .shadow(4.dp, RoundedCornerShape(24.dp), clip = true)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                }
            }
        }
        else {
            if (SongHelper.currentSong.isRadio == false &&
                lyrics.isNotEmpty()
            ) {
                Box(Modifier.weight(0.75f).fillMaxHeight()){
                    LyricsView(
                        iconTextColor,
                        true,
                        mediaController,
                        PaddingValues(horizontal = 32.dp, vertical = 16.dp)
                    )
                }
            }
        }
    }
}