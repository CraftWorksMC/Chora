@file:OptIn(UnstableApi::class)

package com.craftworks.music.ui.playing

import android.content.res.Configuration
import android.os.Build
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.craftworks.music.R
import com.craftworks.music.managers.SettingsManager
import com.craftworks.music.player.ChoraMediaLibraryService
import com.gigamole.composefadingedges.marqueeHorizontalFadingEdges

@kotlin.OptIn(ExperimentalAnimationApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Preview(
    showSystemUi = false, device = "id:pixel_2",
    wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE,
    uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true
)
@Composable
fun NowPlayingPortrait(
    mediaController: MediaController? = null,
    iconColor: Color = Color.Black,
    metadata: MediaMetadata? = null
) {
    // use dark or light colors for icons and text based on the album art luminance.
    val iconTextColor by animateColorAsState(
        targetValue = iconColor,
        animationSpec = tween(1000, 0, FastOutSlowInEasing),
        label = "Animated text color"
    )

    val showMoreInfo =
        SettingsManager(LocalContext.current).showMoreInfoFlow.collectAsStateWithLifecycle(true)

    Column {
        // Top padding
        Spacer(Modifier.height(24.dp))

        /* Album Cover + Lyrics */
        AnimatedContent(
            lyricsOpen,
            label = "Crossfade between lyrics",
            modifier = Modifier
                .heightIn(min = 256.dp, max = 420.dp)
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
                Crossfade (
                    targetState = metadata?.artworkUri,
                    animationSpec = tween(1000, 0, FastOutSlowInEasing),
                ) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(it)
                            .size(1024)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Album Cover Art",
                        contentScale = ContentScale.FillWidth,
                        alignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 16.dp)
                            .aspectRatio(1f)
                            .shadow(4.dp, RoundedCornerShape(24.dp), clip = true)
                    )
                }
            }
        }


        Row(
            Modifier
                .padding(top = 8.dp)
                .padding(horizontal = 32.dp)
        ) {
            /* Song Title + Artist */
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top
            ) {
                val transition = (
                    fadeIn(animationSpec = tween(durationMillis = 500)) togetherWith fadeOut(animationSpec = tween(durationMillis = 500))
                )

                AnimatedContent(
                    targetState = metadata?.title.toString(),
                    transitionSpec = { transition },
                    label = "Animated Song Title",
                ) {
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

                AnimatedContent(
                    targetState = metadata?.artist.toString() + if (metadata?.recordingYear != 0 && metadata?.mediaType != MediaMetadata.MEDIA_TYPE_RADIO_STATION) " • " + metadata?.recordingYear else "",
                    transitionSpec = { transition },
                    label = "Animated Song Title",
                ) {
                    Text(
                        text = it,
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

                if (showMoreInfo.value && metadata?.mediaType != MediaMetadata.MEDIA_TYPE_RADIO_STATION) {
                    AnimatedContent(
                        targetState = "${
                            metadata?.extras?.getString("format")?.uppercase()
                        } • ${metadata?.extras?.getLong("bitrate")} • ${
                            if (metadata?.extras?.getString("navidromeID")
                                    ?.startsWith("Local_") == true
                            )
                                stringResource(R.string.Source_Local)
                            else
                                stringResource(R.string.Source_Navidrome)
                        } ",
                        transitionSpec = { transition },
                        label = "Animated Song Title",
                    ) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Light,
                            color = iconTextColor.copy(alpha = 0.5f),
                            maxLines = 1,
                            textAlign = TextAlign.Start
                        )
                    }
                }
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
                ChoraMediaLibraryService.getInstance()?.player?.let {
                    ShuffleButton(
                        it,
                        iconTextColor,
                        Modifier.size(32.dp)
                    )

                    PreviousSongButton(
                        it,
                        iconTextColor,
                        Modifier.size(48.dp)
                    )

                    PlayPauseButton(
                        it,
                        iconTextColor,
                        Modifier.size(92.dp)
                    )

                    NextSongButton(
                        it,
                        iconTextColor,
                        Modifier.size(48.dp)
                    )

                    RepeatButton(
                        it,
                        iconTextColor,
                        Modifier.size(32.dp)
                    )
                }
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

                DownloadButton(iconTextColor, 64.dp, metadata, (metadata?.mediaType != MediaMetadata.MEDIA_TYPE_RADIO_STATION || metadata.extras?.getString("navidromeID")?.startsWith("Local_") == true))
            }
        }
        //endregion

        Spacer(Modifier.height(64.dp))
    }
}