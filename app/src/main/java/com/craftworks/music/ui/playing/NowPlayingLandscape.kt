@file:OptIn(UnstableApi::class)

package com.craftworks.music.ui.playing

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.craftworks.music.R
import com.craftworks.music.data.repository.LyricsState
import com.craftworks.music.managers.settings.AppearanceSettingsManager
import com.craftworks.music.player.ChoraMediaLibraryService
import com.gigamole.composefadingedges.marqueeHorizontalFadingEdges

@Preview(device = "id:tv_1080p", showBackground = true, showSystemUi = true)
@Preview(
    device = "spec:parent=pixel_6,orientation=landscape",
    showBackground = true,
    showSystemUi = true
)
@Composable
fun NowPlayingLandscape(
    mediaController: MediaController? = null,
    metadata: MediaMetadata? = null,
    iconColor: Color = Color.White,
    sleepTimerMinutes: Int = 10,
    onOpenSleepTimer: () -> Unit = {},
    onToggleQueue: () -> Unit = {},
    onRefreshLyrics: () -> Unit = {}
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
        NowPlayingAlignment.LEFT
    )

    val isRadio = metadata?.mediaType == MediaMetadata.MEDIA_TYPE_RADIO_STATION

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(metadata?.artworkUri.toString().replace("size=128", "size=500"))
                    .placeholderMemoryCacheKey(metadata?.artworkUri.toString())
                    .crossfade(true)
                    .diskCachePolicy(CachePolicy.DISABLED)
                    .build(),
                contentDescription = "Album Cover Art",
                fallback = painterResource(R.drawable.placeholder),
                contentScale = ContentScale.FillWidth,
                alignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .shadow(4.dp, RoundedCornerShape(24.dp), clip = true)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Crossfade(
                    targetState = metadata?.title.toString(),
                    animationSpec = tween(
                        durationMillis = 400,
                        easing = FastOutSlowInEasing
                    ),
                    label = "Animated Song Title",
                    modifier = Modifier
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
                            NowPlayingAlignment.LEFT -> TextAlign.Start
                            NowPlayingAlignment.CENTER -> TextAlign.Center
                            NowPlayingAlignment.RIGHT -> TextAlign.End
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .marqueeHorizontalFadingEdges(
                                marqueeProvider = { Modifier.basicMarquee() })
                    )
                }

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
                            NowPlayingAlignment.LEFT -> TextAlign.Start
                            NowPlayingAlignment.CENTER -> TextAlign.Center
                            NowPlayingAlignment.RIGHT -> TextAlign.End
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
                            NowPlayingAlignment.LEFT -> TextAlign.Start
                            NowPlayingAlignment.CENTER -> TextAlign.Center
                            NowPlayingAlignment.RIGHT -> TextAlign.End
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (metadata?.mediaType != MediaMetadata.MEDIA_TYPE_RADIO_STATION)
                    PlaybackProgressSlider(iconTextColor, mediaController)

            }
            Row(
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth(),
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
                    .fillMaxWidth()
                    .requiredHeightIn(min = 48.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DownloadButton(
                    iconTextColor,
                    32.dp,
                    metadata,
                    !(metadata?.extras?.getString("navidromeID")?.startsWith("Local_") ?: true)
                )
                SleepTimerButton(iconTextColor, 32.dp, sleepTimerMinutes, onOpenSleepTimer)
                PlayQueueButton(iconTextColor, 32.dp, onToggleQueue)
            }
        }

        val lyrics by LyricsState.lyrics.collectAsStateWithLifecycle()

        AnimatedVisibility(
            metadata?.mediaType != MediaMetadata.MEDIA_TYPE_RADIO_STATION && lyrics.isNotEmpty()
        ) {
            Box(Modifier
                .widthIn(max = 480.dp, min = 256.dp)
                .fillMaxHeight()) {
                LyricsView(
                    iconTextColor,
                    true,
                    mediaController,
                    PaddingValues(horizontal = 32.dp, vertical = 16.dp),
                    onRefreshLyrics
                )
            }
        }
    }
}