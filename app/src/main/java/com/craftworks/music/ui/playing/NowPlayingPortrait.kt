@file:OptIn(UnstableApi::class)

package com.craftworks.music.ui.playing

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.craftworks.music.R
import com.craftworks.music.data.repository.LyricsState
import com.craftworks.music.managers.settings.AppearanceSettingsManager
import com.craftworks.music.player.ChoraMediaLibraryService
import com.gigamole.composefadingedges.marqueeHorizontalFadingEdges

@kotlin.OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Preview(
    showSystemUi = true, device = "id:pixel_9a",
    wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE, showBackground = true
)
@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Preview(
    showSystemUi = true, device = "id:pixel",
    wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE, showBackground = true
)
@Composable
fun NowPlayingPortrait(
    mediaController: MediaController? = null,
    metadata: MediaMetadata? = null,
    iconColor: Color = Color.White,
    lyricsOpen: Boolean = false,
    onToggleLyrics: () -> Unit = {},
    onToggleQueue: () -> Unit = {},
    onToggleDetails: () -> Unit = {},
    onOpenSleepTimer: () -> Unit = {}
) {
    val iconTextColor by animateColorAsState(
        targetValue = iconColor,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessVeryLow
        ),
        label = "Animated text color"
    )

    val context = LocalContext.current
    val settingsManager = remember { AppearanceSettingsManager(context) }
    val showMoreInfo by settingsManager.showMoreInfoFlow.collectAsStateWithLifecycle(true)
    val titleAlignment by settingsManager.nowPlayingTitleAlignment.collectAsStateWithLifecycle(
        NowPlayingTitleAlignment.LEFT
    )
    val lyrics by LyricsState.lyrics.collectAsStateWithLifecycle()
    val loadingLyrics by LyricsState.loading.collectAsStateWithLifecycle()

    val isLyricsActive = lyricsOpen && (lyrics.isNotEmpty() || loadingLyrics)
    val isRadio = metadata?.mediaType == MediaMetadata.MEDIA_TYPE_RADIO_STATION

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Column(
                Modifier.padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedContent(
                    targetState = isLyricsActive,
                    modifier = Modifier
                        .weight(1f, fill = false),
                    label = "Crossfade between artwork and lyrics view",
                    transitionSpec = {
                        val enterSlideSpec = spring<IntOffset>(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                        val exitSlideSpec =
                            tween<IntOffset>(durationMillis = 200, easing = FastOutLinearInEasing)
                        val exitFadeSpec =
                            tween<Float>(durationMillis = 200, easing = FastOutLinearInEasing)

                        val enterFadeSpec = if (targetState) {
                            tween<Float>(durationMillis = 300, delayMillis = 100)
                        } else {
                            tween<Float>(durationMillis = 300)
                        }

                        val direction = if (targetState) 1 else -1

                        (fadeIn(enterFadeSpec) + slideInVertically(enterSlideSpec) { direction * (it / 4) }) togetherWith
                                (fadeOut(exitFadeSpec) + slideOutVertically(exitSlideSpec) { -direction * (it / 4) })
                    }
                ) { showLyrics ->
                    val laggedOffset by transition.animateFloat(
                        label = "Artwork Bounce",
                        transitionSpec = {
                            spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        }
                    ) { targetLyricsState ->
                        if (targetLyricsState == EnterExitState.Visible) 0f else -60f
                    }

                    if (showLyrics) {
                        Column(
                            Modifier
                                .statusBarsPadding()
                                .padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = metadata?.title.toString(),
                                style = MaterialTheme.typography.headlineSmallEmphasized,
                                fontWeight = FontWeight.Bold,
                                color = iconTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Visible,
                                softWrap = false,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .marqueeHorizontalFadingEdges(
                                        marqueeProvider = { Modifier.basicMarquee() })
                            )
                            Crossfade(
                                targetState = metadata?.artist.toString(),
                                animationSpec = tween(
                                    durationMillis = 400,
                                    easing = FastOutSlowInEasing
                                ),
                                label = "Animated Artist"
                            ) { artistInfo ->
                                Text(
                                    text = artistInfo,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Normal,
                                    color = iconTextColor.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    softWrap = false,
                                    textAlign = TextAlign.Center,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .marqueeHorizontalFadingEdges(marqueeProvider = { Modifier.basicMarquee() })
                                )
                            }
                            LyricsView(iconTextColor, false, mediaController).apply {
                                Modifier.graphicsLayer {
                                    translationY = -laggedOffset
                                }
                            }
                        }
                    } else {
                        Column(
                            Modifier.statusBarsPadding(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Crossfade(
                                targetState = metadata?.artworkUri.toString()
                                    .replace("size=128", "size=500"),
                                animationSpec = tween(
                                    durationMillis = 400,
                                    easing = FastOutSlowInEasing
                                ),
                                label = "Crossfade between albums"
                            ) { artworkUri ->
                                SubcomposeAsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(artworkUri)
                                        .placeholderMemoryCacheKey(metadata?.artworkUri.toString())
                                        .diskCachePolicy(CachePolicy.DISABLED)
                                        .build(),
                                    contentDescription = "Album Cover Art",
                                    contentScale = ContentScale.Crop,
                                    alignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp)
                                        .aspectRatio(1f)
                                        .graphicsLayer {
                                            translationY = laggedOffset
                                        }
                                        .shadow(
                                            elevation = 12.dp,
                                            shape = RoundedCornerShape(24.dp)
                                        )
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clip(RoundedCornerShape(24.dp))
                                )
                            }

                            Column(
                                Modifier.padding(horizontal = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CompositionLocalProvider(
                                    LocalLayoutDirection provides
                                            if (titleAlignment == NowPlayingTitleAlignment.RIGHT)
                                                LayoutDirection.Rtl
                                            else LayoutDirection.Ltr
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                            Crossfade(
                                                targetState = metadata?.title.toString(),
                                                animationSpec = tween(
                                                    durationMillis = 400,
                                                    easing = FastOutSlowInEasing
                                                ),
                                                label = "Animated Song Title",
                                                modifier = Modifier.weight(1f)
                                            ) { title ->
                                                Text(
                                                    text = title,
                                                    style = MaterialTheme.typography.headlineMediumEmphasized,
                                                    fontWeight = FontWeight.Bold,
                                                    color = iconTextColor,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Visible,
                                                    softWrap = false,
                                                    textAlign = when (titleAlignment) {
                                                        NowPlayingTitleAlignment.LEFT -> TextAlign.Start
                                                        NowPlayingTitleAlignment.CENTER -> TextAlign.Center
                                                        NowPlayingTitleAlignment.RIGHT -> TextAlign.End
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .marqueeHorizontalFadingEdges(
                                                            marqueeProvider = { Modifier.basicMarquee() })
                                                )
                                            }

                                            IconButton(onClick = onToggleDetails) {
                                                Icon(
                                                    Icons.Rounded.MoreVert,
                                                    tint = iconTextColor.copy(alpha = 0.8f),
                                                    contentDescription = null
                                                )
                                            }
                                        }
                                    }
                                }

                                // Artist + year
                                Crossfade(
                                    targetState = metadata?.artist.toString(),
                                    animationSpec = tween(
                                        durationMillis = 400,
                                        easing = FastOutSlowInEasing
                                    ),
                                    label = "Animated Artist"
                                ) { artistInfo ->
                                    Text(
                                        text = artistInfo,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Normal,
                                        color = iconTextColor.copy(alpha = 0.7f),
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
                                            .marqueeHorizontalFadingEdges(marqueeProvider = { Modifier.basicMarquee() })
                                    )
                                }

                                if (showMoreInfo && !isRadio) {
                                    Text(
                                        text = buildString {
                                            append(
                                                metadata?.extras?.getString("format")?.uppercase()
                                                    ?: ""
                                            )
                                            append(" · ")
                                            append(metadata?.extras?.getLong("bitrate") ?: "")
                                            append(" · ")
                                            append(
                                                if (metadata?.extras?.getString("navidromeID")
                                                        ?.startsWith("Local_") == true
                                                ) stringResource(R.string.Source_Local)
                                                else stringResource(R.string.Source_Navidrome)
                                            )
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = iconTextColor.copy(alpha = 0.45f),
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

                            if (!isRadio) {
                                Box(
                                    Modifier.padding(horizontal = 24.dp)
                                ) {
                                    PlaybackProgressSlider(iconTextColor, mediaController)
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .requiredHeightIn(min = 88.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChoraMediaLibraryService.getInstance()?.player?.let { player ->
                        ShuffleButton(player, iconTextColor, Modifier.size(24.dp))
                        PreviousSongButton(player, iconTextColor, Modifier.size(40.dp))
                        PlayPauseButton(player, iconTextColor, Modifier.size(88.dp))
                        NextSongButton(player, iconTextColor, Modifier.size(40.dp))
                        RepeatButton(player, iconTextColor, Modifier.size(24.dp))
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .requiredHeightIn(min = 48.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LyricsButton(iconTextColor, 32.dp, lyricsOpen, onToggleLyrics)
            DownloadButton(iconTextColor, 32.dp, metadata, true)
            SleepTimerButton(iconTextColor, 32.dp, onOpenSleepTimer)
            PlayQueueButton(iconTextColor, 32.dp, onToggleQueue)
        }
    }
}