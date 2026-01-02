@file:OptIn(UnstableApi::class)

package com.craftworks.music.ui.playing

import androidx.annotation.OptIn
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
import androidx.compose.foundation.layout.size
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
import com.craftworks.music.ui.util.TextDisplayUtils
import androidx.compose.runtime.remember
import com.craftworks.music.ui.util.LayoutMode
import com.craftworks.music.ui.util.rememberFoldableState
import com.gigamole.composefadingedges.marqueeHorizontalFadingEdges

@Preview(device = "id:tv_1080p", showBackground = true, showSystemUi = true)
@Preview(device = "spec:parent=pixel_6,orientation=landscape", showBackground = true, showSystemUi = true)
@Composable
fun NowPlayingLandscape(
    mediaController: MediaController? = null,
    iconColor: Color = Color.Black,
    metadata: MediaMetadata? = null
){
    val foldableState = rememberFoldableState()
    val isTableTop = foldableState.layoutMode == LayoutMode.TABLE_TOP
    val isCompactHeight = LocalConfiguration.current.screenHeightDp.dp < 512.dp

    val iconTextColor by animateColorAsState(
        targetValue = iconColor,
        animationSpec = tween(1000, 0, FastOutSlowInEasing),
        label = "Animated text color"
    )

    val context = LocalContext.current
    val settingsManager = remember { AppearanceSettingsManager(context) }
    val stripTrackNumbers by settingsManager.stripTrackNumbersFromTitlesFlow.collectAsStateWithLifecycle(false)

    if (isTableTop) {
        NowPlayingTableTop(mediaController, iconTextColor, metadata, stripTrackNumbers)
    } else {
        NowPlayingLandscapeContent(mediaController, iconTextColor, metadata, isCompactHeight, stripTrackNumbers)
    }
}

@Composable
private fun NowPlayingTableTop(
    mediaController: MediaController?,
    iconTextColor: Color,
    metadata: MediaMetadata?,
    stripTrackNumbers: Boolean
) {
    val lyrics by LyricsState.lyrics.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxHeight()) {
        Box(Modifier.weight(1f)) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(metadata?.artworkUri.toString().replace("size=128", "size=500"))
                        .crossfade(true)
                        .diskCacheKey("np_${metadata?.extras?.getString("navidromeID")}_500")
                        .memoryCacheKey("np_${metadata?.extras?.getString("navidromeID")}_500")
                        .build(),
                    contentDescription = "Album Cover Art",
                    placeholder = painterResource(R.drawable.placeholder),
                    fallback = painterResource(R.drawable.placeholder),
                    contentScale = ContentScale.FillWidth,
                    alignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxHeight(0.7f)
                        .aspectRatio(1f)
                        .shadow(4.dp, RoundedCornerShape(24.dp), clip = true)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = TextDisplayUtils.formatSongTitle(metadata?.title.toString(), stripTrackNumbers),
                    style = MaterialTheme.typography.headlineMedium,
                    color = iconTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = TextDisplayUtils.formatArtistName(metadata?.artist?.toString()) + if (metadata?.recordingYear != null && metadata?.recordingYear != 0) " - " + metadata?.recordingYear else "",
                    style = MaterialTheme.typography.titleMedium,
                    color = iconTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }

        Box(Modifier.weight(1f)) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                if (metadata?.mediaType != MediaMetadata.MEDIA_TYPE_RADIO_STATION) {
                    PlaybackProgressSlider(iconTextColor, mediaController, metadata)
                }

                VolumeSlider(iconTextColor, mediaController)

                Row(
                    modifier = Modifier
                        .wrapContentHeight()
                        .fillMaxWidth()
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    mediaController?.let {
                        ShuffleButton(it, iconTextColor, Modifier.size(32.dp))
                        PreviousSongButton(it, iconTextColor, Modifier.size(48.dp))
                        PlayPauseButton(it, iconTextColor, Modifier.size(92.dp))
                        NextSongButton(it, iconTextColor, Modifier.size(48.dp))
                        RepeatButton(it, iconTextColor, Modifier.size(32.dp))
                    }
                }

                Row(
                    modifier = Modifier
                        .wrapContentHeight()
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LyricsButton(iconTextColor, 48.dp)
                    FavoriteButton(iconTextColor, 48.dp, metadata, (metadata?.mediaType != MediaMetadata.MEDIA_TYPE_RADIO_STATION && metadata?.extras?.getString("navidromeID")?.startsWith("Local_") == false))
                    DownloadButton(iconTextColor, 48.dp, metadata, (metadata?.mediaType != MediaMetadata.MEDIA_TYPE_RADIO_STATION && metadata?.extras?.getString("navidromeID")?.startsWith("Local_") == false))
                    PlayQueueButton(iconTextColor, 48.dp)
                }
            }
        }
    }
}

@Composable
private fun NowPlayingLandscapeContent(
    mediaController: MediaController?,
    iconTextColor: Color,
    metadata: MediaMetadata?,
    isCompactHeight: Boolean,
    stripTrackNumbers: Boolean
) {
    // Button sizing for landscape
    val mainButtonSize = 92.dp
    val secondaryButtonSize = 48.dp
    val smallButtonSize = 32.dp

    Row {
        Column(
            Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
        ) {
            // Album Art - shown on non-compact height screens (like unfolded foldables)
            if (!isCompactHeight){
                Column(
                    Modifier.focusable(false).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    /* Album Cover */
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(metadata?.artworkUri.toString().replace("size=128", "size=500"))
                            .crossfade(true)
                            .diskCacheKey("np_${metadata?.extras?.getString("navidromeID")}_500")
                            .memoryCacheKey("np_${metadata?.extras?.getString("navidromeID")}_500")
                            .build(),
                        contentDescription = "Album Cover Art",
                        placeholder = painterResource(R.drawable.placeholder),
                        fallback = painterResource(R.drawable.placeholder),
                        contentScale = ContentScale.FillWidth,
                        alignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxHeight(0.35f)
                            .widthIn(max = 300.dp)
                            .aspectRatio(1f)
                            .shadow(4.dp, RoundedCornerShape(24.dp), clip = true)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            /* Image (When lyrics open) + Song Title + Artist*/
            Row {
                AnimatedVisibility(
                    visible = lyricsOpen && isCompactHeight,
                    modifier = Modifier
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(metadata?.artworkUri.toString().replace("size=128", "size=500"))
                            .diskCacheKey("np_${metadata?.extras?.getString("navidromeID")}_500")
                            .memoryCacheKey("np_${metadata?.extras?.getString("navidromeID")}_500")
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

                if (isCompactHeight)
                    Spacer(Modifier.animateContentSize().width(if (lyricsOpen) 0.dp else 24.dp))
                else
                    Spacer(Modifier.width(24.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Top
                ) {
                    Text(
                        text = TextDisplayUtils.formatSongTitle(metadata?.title.toString(), stripTrackNumbers),
                        style = MaterialTheme.typography.headlineMedium,
                        color = iconTextColor,
                        maxLines = 1, overflow = TextOverflow.Visible,
                        softWrap = false,
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .marqueeHorizontalFadingEdges(marqueeProvider = { Modifier.basicMarquee() })
                    )

                    Text(
                        text = TextDisplayUtils.formatArtistName(metadata?.artist?.toString()) + if (metadata?.recordingYear != null && metadata?.recordingYear != 0) " • " + metadata?.recordingYear else "",
                        style = MaterialTheme.typography.titleMedium,
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
            }

            Spacer(Modifier.height(16.dp))

            if (metadata?.mediaType == MediaMetadata.MEDIA_TYPE_RADIO_STATION) {
                Spacer(Modifier.height(48.dp))
            }
            else {
                PlaybackProgressSlider(iconTextColor, mediaController, metadata)
            }

            Spacer(Modifier.height(8.dp))
            VolumeSlider(iconTextColor, mediaController)
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                mediaController?.let {
                    ShuffleButton(
                        it,
                        iconTextColor,
                        Modifier.size(smallButtonSize)
                    )

                    PreviousSongButton(
                        it,
                        iconTextColor,
                        Modifier.size(secondaryButtonSize)
                    )

                    PlayPauseButton(
                        it,
                        iconTextColor,
                        Modifier.size(mainButtonSize)
                    )

                    NextSongButton(
                        it,
                        iconTextColor,
                        Modifier.size(secondaryButtonSize)
                    )

                    RepeatButton(
                        it,
                        iconTextColor,
                        Modifier.size(smallButtonSize)
                    )
                }
            }
            // Extra buttons row - always visible
            Row(
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LyricsButton(iconTextColor, secondaryButtonSize)
                FavoriteButton(iconTextColor, secondaryButtonSize, metadata, (metadata?.mediaType != MediaMetadata.MEDIA_TYPE_RADIO_STATION && metadata?.extras?.getString("navidromeID")?.startsWith("Local_") == false))
                DownloadButton(iconTextColor, secondaryButtonSize, metadata, (metadata?.mediaType != MediaMetadata.MEDIA_TYPE_RADIO_STATION && metadata?.extras?.getString("navidromeID")?.startsWith("Local_") == false))
                PlayQueueButton(iconTextColor, secondaryButtonSize)
            }
        }

        val lyrics by LyricsState.lyrics.collectAsStateWithLifecycle()

        if (isCompactHeight){
            Crossfade(
                lyricsOpen,
                label = ""
            ) {
                if (it){
                    Box(Modifier
                        .padding(32.dp)
                        .fillMaxHeight()
                        .widthIn(max = 400.dp)
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
                            .data(metadata?.artworkUri.toString().replace("size=128", "size=500"))
                            .diskCacheKey("np_${metadata?.extras?.getString("navidromeID")}_500")
                            .memoryCacheKey("np_${metadata?.extras?.getString("navidromeID")}_500")
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
                            .widthIn(max = 400.dp)
                            .aspectRatio(1f)
                            .shadow(4.dp, RoundedCornerShape(24.dp), clip = true)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                }
            }
        }
        else {
            if (metadata?.mediaType != MediaMetadata.MEDIA_TYPE_RADIO_STATION &&
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