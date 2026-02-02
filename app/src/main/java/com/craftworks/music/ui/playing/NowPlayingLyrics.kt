package com.craftworks.music.ui.playing

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.craftworks.music.data.model.Lyric
import com.craftworks.music.data.repository.LyricsState
import com.craftworks.music.managers.settings.AppearanceSettingsManager
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
import kotlin.math.sin

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LyricsView(
    color: Color,
    isLandscape: Boolean = false,
    mediaController: MediaController?,
    paddingValues: PaddingValues = PaddingValues(),
) {
    val lyrics by LyricsState.lyrics.collectAsStateWithLifecycle()
    val loading by LyricsState.loading.collectAsStateWithLifecycle()

    val useBlur by AppearanceSettingsManager(LocalContext.current).nowPlayingLyricsBlurFlow.collectAsState(true)
    val lyricsAnimationSpeed by AppearanceSettingsManager(LocalContext.current).lyricsAnimationSpeedFlow.collectAsState(100)

    // State holding the current position
    val currentPosition = remember { mutableIntStateOf(mediaController?.currentPosition?.toInt() ?: 0) }
    val currentLyricIndex = remember { mutableIntStateOf(-1) }

    val state = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val visibleItemsInfo by remember { derivedStateOf { state.layoutInfo.visibleItemsInfo } }

    val scrollOffset = dpToPx(128)

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
                    if (trackingJob.isActive) return

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

            val targetIndex = (newCurrentLyricIndex - 1).coerceAtLeast(-1)

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
                            index = targetIndex.coerceAtLeast(0),
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

    Crossfade(
        loading
    ) {
        if (it) {
            Box(modifier = if (isLandscape) {
                Modifier
                    .widthIn(min = 256.dp)
                    .fillMaxHeight()
            } else {
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            }
                .padding(paddingValues)) {
                    LoadingIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = color
                    )
                }
        }
        else {
            LazyColumn(
                modifier = if (isLandscape) {
                    Modifier
                        .widthIn(min = 256.dp)
                        .fillMaxHeight()
                } else {
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                }
                    .padding(paddingValues)
                    .verticalFadingEdges(
                        FadingEdgesContentType.Dynamic.Lazy.List(FadingEdgesScrollConfig.Dynamic(), state),
                        FadingEdgesGravity.All,
                        96.dp
                    ),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(vertical = 32.dp),
                state = state,
            ) {
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
                            lyricsAnimationSpeed = lyricsAnimationSpeed,
                            onClick = {
                                mediaController?.seekTo(lyric.timestamp.toLong())
                                currentPosition.intValue = lyric.timestamp
                            }
                        )
                    }
                }
                else if (lyrics.isNotEmpty()) {
                    item {
                        Text(
                            text = lyrics[0].content,
                            style = MaterialTheme.typography.headlineMedium,
                            color = color,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
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

    if (lyric.content == "") {
        AnimatedContent(
            targetState = currentLyricIndex == index
        ) {
            if (it) {
                Box(modifier = Modifier
                    .focusable(false)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                    contentAlignment = Alignment.Center
                ) {
                    InterludeIndicator(color)
                }
            }
        }
    }
    else {
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
                text = lyric.content,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color.copy(lyricAlpha),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            )
        }
    }
}

// Bouncing dots for interlude.
@Composable
fun InterludeIndicator(
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_master")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Row(
        modifier = modifier
            .height(48.dp)
            .wrapContentWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Dot(color, phase, 0)
        Dot(color, phase, 1)
        Dot(color, phase, 2)
    }
}

@Composable
fun Dot(color: Color, phase: Float, index: Int) {
    Canvas(modifier = Modifier.size(8.dp)) {
        val offset = index * 0.8f
        val sineValue = sin(phase - offset)
        val yOffset = sineValue * 6f
        val alpha = 0.4f + ((sineValue + 1) / 2) * 0.6f

        drawCircle(
            color = color.copy(alpha = alpha),
            radius = size.minDimension / 2,
            center = center.copy(y = center.y + yOffset)
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