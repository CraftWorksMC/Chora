package com.craftworks.music.ui.playing

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.craftworks.music.data.model.Lyric
import com.craftworks.music.lyrics.LyricsManager
import com.craftworks.music.managers.SettingsManager
import com.gigamole.composefadingedges.FadingEdgesGravity
import com.gigamole.composefadingedges.content.FadingEdgesContentType
import com.gigamole.composefadingedges.content.scrollconfig.FadingEdgesScrollConfig
import com.gigamole.composefadingedges.verticalFadingEdges
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun LyricsView(
    color: Color,
    isLandscape: Boolean = false,
    mediaController: MediaController?,
    paddingValues: PaddingValues = PaddingValues(),
) {
    val lyrics by LyricsManager.Lyrics.collectAsState()


    val useBlur by SettingsManager(LocalContext.current).nowPlayingLyricsBlurFlow.collectAsState(true)
    val lyricsAnimationSpeed by SettingsManager(LocalContext.current).lyricsAnimationSpeedFlow.collectAsState(100)

    // State holding the current position
    val currentPosition = remember { mutableIntStateOf(mediaController?.currentPosition?.toInt() ?: 0) }
    val currentLyricIndex = remember { mutableIntStateOf(0) }

    val state = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val visibleItemsInfo by remember { derivedStateOf { state.layoutInfo.visibleItemsInfo } }

    val scrollOffset = dpToPx(128)

    val configuration = LocalConfiguration.current
    val appViewHeightDp = configuration.screenHeightDp.dp
  
    // Update current position only each lyrics change.
    LaunchedEffect(mediaController, lyrics) {
        var trackingJob: Job = Job()
        val scope = CoroutineScope(Dispatchers.Main)

        if (mediaController?.isPlaying == true) {
            trackingJob = scope.launch {
                var position = mediaController.currentPosition.toInt()
                currentPosition.intValue = position

                while (isActive) {
                    position = mediaController.currentPosition.toInt()
                    currentPosition.intValue = position
                    delay(getNextUpdateDelay(position, lyrics))
                }
            }
        }

        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                if (isPlaying) {
                    if (trackingJob.isActive == true) return

                    trackingJob = scope.launch {
                        var position = mediaController.currentPosition.toInt()
                        currentPosition.intValue = position

                        while (isActive) {
                            position = mediaController.currentPosition.toInt()
                            currentPosition.intValue = position
                            delay(getNextUpdateDelay(position, lyrics))
                        }
                    }
                } else trackingJob.cancel()
            }
        })
    }

    // Lyric index updates and scrolling
    LaunchedEffect(currentPosition.intValue, lyrics) {
        if (mediaController?.isPlaying == true) {
            val newCurrentLyricIndex = lyrics.indexOfFirst { it.timestamp > (currentPosition.intValue + lyricsAnimationSpeed / 2) }
                .takeIf { it >= 0 } ?: lyrics.size

            val targetIndex = (newCurrentLyricIndex - 1).coerceAtLeast(0)

            if (targetIndex != currentLyricIndex.intValue) {
                currentLyricIndex.intValue = targetIndex

                coroutineScope.launch {
                    // If the next item is visible, animate smoothly to it using FastOutSlowIn Easing
                    // else use animateScrollToItem.
                    val currentItem = visibleItemsInfo.firstOrNull { it.index == currentLyricIndex.intValue }

                    if (visibleItemsInfo.any { it.index == targetIndex }) {
                        val scrollBy = (currentItem?.offset ?: 0) + (currentItem?.size ?: 0) - scrollOffset

                        state.animateScrollBy(
                            value = scrollBy.toFloat(),
                            animationSpec = tween(lyricsAnimationSpeed, 0, FastOutSlowInEasing)
                        )
                    }
                    else
                        state.animateScrollToItem(
                            index = targetIndex,
                            scrollOffset = -(currentItem?.size ?: 0)
                        )
                }
            }
        }
    }

    // Plain lyrics scrolling
    LaunchedEffect(mediaController, lyrics) {
        if (lyrics.size == 1) {
            while (true) {
                if (mediaController?.isPlaying == true) {
                    val totalDuration = mediaController.duration / 1000f // duration in seconds
                    val scrollRate = state.layoutInfo.viewportSize.height / totalDuration

                    coroutineScope.launch {
                        state.animateScrollBy(scrollRate * 2.5f, tween(500, 0, LinearEasing))
                    }
                    delay(500)
                } else {
                    delay(500)
                }
            }
        }
    }

    LazyColumn(
        modifier = if (isLandscape) {
            Modifier
                .widthIn(min = 256.dp)
                .fillMaxHeight()
        } else {
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.95f)
        }
            .padding(paddingValues)
            .verticalFadingEdges(
                FadingEdgesContentType.Dynamic.Lazy.List(FadingEdgesScrollConfig.Dynamic(), state),
                FadingEdgesGravity.All,
                96.dp
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        state = state,
    ) {
//        item {
//            Spacer(modifier = Modifier.height(appViewHeightDp / 3))
//        }
        // Synced Lyrics
        if (lyrics.size > 1) {
            itemsIndexed(
                lyrics,
                key = { index, lyric -> "${index}:${lyric.content}" }
            ) { index, lyric ->
                SyncedLyricItem(
                    lyric = lyric,
                    index = index,
                    currentLyricIndex = currentLyricIndex.intValue,
                    useBlur = useBlur,
                    visibleItemsInfo = visibleItemsInfo,
                    color = color,
                    lyricsAnimationSpeed = lyricsAnimationSpeed
                ) {
                    mediaController?.seekTo(lyric.timestamp.toLong())
                    currentPosition.intValue = lyric.timestamp
                    currentLyricIndex.intValue = index

                    coroutineScope.launch {
                        state.animateScrollToItem(
                            index = index,
                            scrollOffset = -scrollOffset + (visibleItemsInfo.firstOrNull { it.index == index }?.size ?: 0)
                        )
                    }
                }
            }
        }
        // plain lyrics
        else if (lyrics.isNotEmpty()) {
            println("Added plain lyrics item. ${lyrics[0]}")
            item {
                Text(
                    text = lyrics[0].content,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    textAlign = TextAlign.Center,
                    lineHeight = MaterialTheme.typography.titleLarge.lineHeight.times(1.2f)
                )
            }
        }
//        item {
//            Spacer(modifier = Modifier.height(appViewHeightDp / 3))
//        }
    }
}

@Composable
fun SyncedLyricItem(
    lyric: Lyric,
    index: Int,
    currentLyricIndex: Int,
    useBlur: Boolean,
    visibleItemsInfo: List<LazyListItemInfo>,
    color: Color,
    lyricsAnimationSpeed: Int = 1200,
    onClick: () -> Unit = {},
) {
    val lyricAlpha: Float by animateFloatAsState(
        targetValue = if (currentLyricIndex == index) 1f else 0.5f,
        label = "Current Lyric Alpha",
        animationSpec = tween(lyricsAnimationSpeed, 0, FastOutSlowInEasing)
    )

    val lyricBlur: Dp by animateDpAsState(
        targetValue = if (useBlur) calculateLyricBlur(
            index, currentLyricIndex, visibleItemsInfo
        ) else 0.dp,
        label = "Lyric Blur",
        animationSpec = tween(lyricsAnimationSpeed, 0, FastOutSlowInEasing)
    )

    val scale by animateFloatAsState(
        targetValue = if (currentLyricIndex == index) 1f else 0.9f,
        label = "Lyric Scale Animation",
        animationSpec = tween(lyricsAnimationSpeed, 0, FastOutSlowInEasing)
    )

    Box(modifier = Modifier
        .padding(vertical = 12.dp)
        .heightIn(min = 48.dp)
        .focusable(false)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .blur(lyricBlur)
        .clickable {
            onClick()
        }, contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (lyric.content == "") "• • •" else lyric.content,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color.copy(lyricAlpha),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            lineHeight = 32.sp
        )
    }
}

// Calculate the amount of blur for each lyrics item depending on it's distance to the current lyric.
@Stable
private fun calculateLyricBlur(
    index: Int,
    currentLyricIndex: Int,
    visibleItemsInfo: List<LazyListItemInfo>
): Dp {
    return when {
        index == currentLyricIndex || !visibleItemsInfo.any { it.index == currentLyricIndex } -> 0.dp
        else -> minOf(abs(currentLyricIndex - index).toFloat(), 8f).dp
    }
}

// Calculate next update delay based on lyrics timestamps
private fun getNextUpdateDelay(currentTime: Int, lyrics: List<Lyric>): Long {
    val nextTimestamp = lyrics
        .filter { it.timestamp > currentTime }
        .minByOrNull { it.timestamp }
        ?.timestamp ?: return 1000L

    val timeUntilNext = nextTimestamp - currentTime

    return timeUntilNext.toLong()
}