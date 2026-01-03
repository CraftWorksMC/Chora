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
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.craftworks.music.R
import com.craftworks.music.managers.settings.AppearanceSettingsManager
import com.craftworks.music.managers.settings.ArtworkSettingsManager
import com.craftworks.music.ui.elements.GeneratedAlbumArt
import com.craftworks.music.ui.util.LayoutMode
import com.craftworks.music.ui.util.TextDisplayUtils
import com.craftworks.music.ui.util.rememberFoldableState
import com.gigamole.composefadingedges.marqueeHorizontalFadingEdges

@kotlin.OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Preview(
    showSystemUi = false, device = "id:pixel",
    wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE,
    uiMode = Configuration.UI_MODE_TYPE_VR_HEADSET, showBackground = true
)
@Composable
fun NowPlayingPortrait(
    mediaController: MediaController? = null,
    iconColor: Color = Color.Black,
    metadata: MediaMetadata? = null
) {
    val iconTextColor by animateColorAsState(
        targetValue = iconColor,
        animationSpec = tween(1500, 0, FastOutSlowInEasing),
        label = "Animated text color"
    )

    val context = LocalContext.current
    val settingsManager = remember { AppearanceSettingsManager(context) }
    val artworkSettingsManager = remember { ArtworkSettingsManager(context) }
    val showMoreInfo by settingsManager.showMoreInfoFlow.collectAsStateWithLifecycle(true)
    val titleAlignment by settingsManager.nowPlayingTitleAlignment.collectAsStateWithLifecycle(NowPlayingTitleAlignment.LEFT)
    val stripTrackNumbers by settingsManager.stripTrackNumbersFromTitlesFlow.collectAsStateWithLifecycle(false)

    // Artwork settings for generated art fallback
    val generatedArtworkEnabled by artworkSettingsManager.generatedArtworkEnabledFlow.collectAsStateWithLifecycle(true)
    val fallbackMode by artworkSettingsManager.fallbackModeFlow.collectAsStateWithLifecycle(ArtworkSettingsManager.FallbackMode.PLACEHOLDER_DETECT)

    // Responsive sizing based on screen size
    // On unfolded/large screens, keep elements constrained to fit the card
    val foldableState = rememberFoldableState()
    val buttonScale = when (foldableState.layoutMode) {
        LayoutMode.EXPANDED, LayoutMode.BOOK_MODE -> 1.0f  // Normal size on unfolded
        LayoutMode.MEDIUM -> 1.1f
        else -> 1f
    }
    val maxArtHeight = when (foldableState.layoutMode) {
        LayoutMode.EXPANDED, LayoutMode.BOOK_MODE -> 360.dp  // Smaller on unfolded
        LayoutMode.MEDIUM -> 400.dp
        else -> 420.dp
    }
    val buttonRowWidth = when (foldableState.layoutMode) {
        LayoutMode.EXPANDED, LayoutMode.BOOK_MODE -> 340.dp  // Narrower on unfolded
        LayoutMode.MEDIUM -> 360.dp
        else -> 320.dp
    }

    Column (
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        /* Album Cover + Lyrics */
        AnimatedContent(
            lyricsOpen,
            label = "Crossfade between lyrics",
            modifier = Modifier
                .heightIn(min = 256.dp, max = maxArtHeight)
                .fillMaxWidth()
        ) { it ->
            if (it) {
                LyricsView(
                    iconTextColor,
                    false,
                    mediaController,
                    PaddingValues(horizontal = 32.dp)
                )
            } else {
                Crossfade(
                    targetState = metadata?.artworkUri.toString().replace("size=128", "size=500"),
                    animationSpec = tween(durationMillis = 500),
                    label = "Crossfade between album art"
                ) { artworkUri ->
                    val hasArtwork = artworkUri.isNotEmpty() && artworkUri != "null"

                    // Check if we should use generated art based on fallback mode
                    val useGeneratedArt = when {
                        !generatedArtworkEnabled -> false
                        fallbackMode == ArtworkSettingsManager.FallbackMode.ALWAYS -> true
                        !hasArtwork -> true
                        fallbackMode == ArtworkSettingsManager.FallbackMode.PLACEHOLDER_DETECT -> {
                            // Detect Navidrome placeholder patterns
                            artworkUri.contains("placeholder") ||
                            artworkUri.endsWith("/coverArt") ||
                            artworkUri.contains("coverArt?id=&") ||
                            (artworkUri.contains("coverArt?size=") && !artworkUri.contains("id="))
                        }
                        else -> false
                    }

                    if (useGeneratedArt) {
                        GeneratedAlbumArt(
                            title = metadata?.title?.toString() ?: "?",
                            artist = metadata?.artist?.toString(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp)
                                .aspectRatio(1f)
                                .shadow(4.dp, RoundedCornerShape(24.dp))
                                .clip(RoundedCornerShape(24.dp)),
                            size = 400.dp,
                            animate = true
                        )
                    } else {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(artworkUri)
                                .diskCacheKey("np_${metadata?.extras?.getString("navidromeID")}_500")
                                .memoryCacheKey("np_${metadata?.extras?.getString("navidromeID")}_500")
                                .build(),
                            contentDescription = "Album Cover Art",
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp)
                                .aspectRatio(1f)
                                .shadow(4.dp, RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clip(RoundedCornerShape(24.dp)),
                            error = {
                                if (generatedArtworkEnabled) {
                                    GeneratedAlbumArt(
                                        title = metadata?.title?.toString() ?: "?",
                                        artist = metadata?.artist?.toString(),
                                        modifier = Modifier.fillMaxSize(),
                                        size = 400.dp,
                                        animate = true
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        Row(
            Modifier
                .padding(horizontal = 32.dp)
        ) {
            /* Song Title + Artist */
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Crossfade(
                    targetState = TextDisplayUtils.formatSongTitle(metadata?.title.toString(), stripTrackNumbers),
                    animationSpec = tween(durationMillis = 500),
                    label = "Animated Song Title"
                ) { title ->
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMediumEmphasized,
                        color = iconTextColor,
                        maxLines = 1, overflow = TextOverflow.Visible,
                        softWrap = false,
                        textAlign = when (titleAlignment) {
                            NowPlayingTitleAlignment.LEFT -> TextAlign.Start
                            NowPlayingTitleAlignment.CENTER -> TextAlign.Center
                            NowPlayingTitleAlignment.RIGHT -> TextAlign.End
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .marqueeHorizontalFadingEdges(marqueeProvider = { Modifier.basicMarquee() })
                    )
                }

                Crossfade(
                    targetState = TextDisplayUtils.formatArtistName(metadata?.artist?.toString()) + if (metadata?.recordingYear != null && metadata?.recordingYear != 0 && metadata?.mediaType != MediaMetadata.MEDIA_TYPE_RADIO_STATION) " • " + metadata?.recordingYear else "",
                    animationSpec = tween(durationMillis = 500),
                    label = "Animated Artist"
                ) { artistInfo ->
                    Text(
                        text = artistInfo,
                        style = MaterialTheme.typography.bodyLarge,
                        color = iconTextColor,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = when (titleAlignment) {
                            NowPlayingTitleAlignment.LEFT -> TextAlign.Start
                            NowPlayingTitleAlignment.CENTER -> TextAlign.Center
                            NowPlayingTitleAlignment.RIGHT -> TextAlign.End
                        },
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .marqueeHorizontalFadingEdges(marqueeProvider = { Modifier.basicMarquee() })
                    )
                }

                if (showMoreInfo && metadata?.mediaType != MediaMetadata.MEDIA_TYPE_RADIO_STATION) {
                    Crossfade(
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
                    ) { moreInfo ->
                        Text(
                            text = moreInfo,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Light,
                            color = iconTextColor.copy(alpha = 0.5f),
                            maxLines = 1,
                            textAlign = when (titleAlignment) {
                                NowPlayingTitleAlignment.LEFT -> TextAlign.Start
                                NowPlayingTitleAlignment.CENTER -> TextAlign.Center
                                NowPlayingTitleAlignment.RIGHT -> TextAlign.End
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        if (metadata?.mediaType != MediaMetadata.MEDIA_TYPE_RADIO_STATION)
            PlaybackProgressSlider(iconTextColor, mediaController, metadata)

        VolumeSlider(iconTextColor, mediaController)

        //region Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                mediaController?.let {
                    ShuffleButton(
                        it,
                        iconTextColor,
                        Modifier.size((32 * buttonScale).dp)
                    )

                    PreviousSongButton(
                        it,
                        iconTextColor,
                        Modifier.size((48 * buttonScale).dp)
                    )

                    PlayPauseButton(
                        it,
                        iconTextColor,
                        Modifier.size((92 * buttonScale).dp)
                    )

                    NextSongButton(
                        it,
                        iconTextColor,
                        Modifier.size((48 * buttonScale).dp)
                    )

                    RepeatButton(
                        it,
                        iconTextColor,
                        Modifier.size((32 * buttonScale).dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .width(buttonRowWidth)
                    .weight(.75f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val secondaryButtonSize = (48 * buttonScale).dp
                LyricsButton(iconTextColor, secondaryButtonSize)

                FavoriteButton(iconTextColor, secondaryButtonSize, metadata, (metadata?.mediaType != MediaMetadata.MEDIA_TYPE_RADIO_STATION && metadata?.extras?.getString("navidromeID")?.startsWith("Local_") == false))

                DownloadButton(iconTextColor, secondaryButtonSize, metadata, (metadata?.mediaType != MediaMetadata.MEDIA_TYPE_RADIO_STATION && metadata?.extras?.getString("navidromeID")?.startsWith("Local_") == false))

                PlayQueueButton(iconTextColor, secondaryButtonSize)
            }
        }
        //endregion
    }
}