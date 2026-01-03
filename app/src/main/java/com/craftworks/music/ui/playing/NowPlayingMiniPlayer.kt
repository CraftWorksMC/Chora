package com.craftworks.music.ui.playing

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.craftworks.music.managers.settings.AppearanceSettingsManager
import com.craftworks.music.ui.util.TextDisplayUtils

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Stable
@Composable
fun NowPlayingMiniPlayer(
    scaffoldState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
    metadata: MediaMetadata? = null,
    mediaController: MediaController? = null,
    onClick: () -> Unit = { }
) {
    val context = LocalContext.current
    val appearanceSettings = remember { AppearanceSettingsManager(context) }
    val stripTrackNumbers by appearanceSettings.stripTrackNumbersFromTitlesFlow.collectAsStateWithLifecycle(false)

    val expanded by remember { derivedStateOf { scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded } }

    val yTrans by animateIntAsState(
        targetValue = if (expanded) dpToPx(72) else 0,
        label = "Fullscreen Translation"
    )

    Box(
        modifier = Modifier
            .offset { IntOffset(x = 0, y = -yTrans) }
            .zIndex(1f)
            .clip(RoundedCornerShape(12.dp, 12.dp, 0.dp, 0.dp))
            .fillMaxWidth()
    ) {
        // Blurred frosted glass background layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = 20.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.2f))
        )

        // Content layer
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(72.dp)
                    .fillMaxWidth()
                    .clickable { onClick.invoke() }
            ) {
                // Album Image
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(metadata?.artworkUri)
                        .diskCacheKey(metadata?.extras?.getString("navidromeID"))
                        .memoryCacheKey(metadata?.extras?.getString("navidromeID"))
                        .crossfade(true)
                        .build(),
                    contentDescription = "Album Cover",
                    contentScale = ContentScale.FillWidth,
                    alignment = Alignment.Center,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(48.dp)
                        .clip(RoundedCornerShape(6.dp))
                )

                // Title + Artist
                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .weight(1f)
                ) {
                    Text(
                        text = TextDisplayUtils.formatSongTitle(metadata?.title.toString(), stripTrackNumbers),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.width(IntrinsicSize.Max)) {
                        Text(
                            text = TextDisplayUtils.formatArtistName(metadata?.artist?.toString()),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Light,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.weight(1f)
                        )
                        if (metadata?.recordingYear != null && metadata?.recordingYear != 0 && metadata?.mediaType != MediaMetadata.MEDIA_TYPE_RADIO_STATION) {
                            Text(
                                text = " • " + metadata?.recordingYear.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Light,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                modifier = Modifier.wrapContentWidth()
                            )
                        }
                    }
                }

                mediaController?.let {
                    PlayPauseButton(
                        it,
                        MaterialTheme.colorScheme.onBackground,
                        Modifier.size(48.dp)
                    )
                }
            }
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}