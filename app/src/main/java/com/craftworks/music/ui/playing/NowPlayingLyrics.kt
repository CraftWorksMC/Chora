package com.craftworks.music.ui.playing

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.craftworks.music.data.model.LyricsAgentType
import com.craftworks.music.data.model.LyricsLine
import com.craftworks.music.data.repository.LyricsState
import com.craftworks.music.managers.settings.AppearanceSettingsManager
import com.craftworks.music.ui.playing.lyrics.SyncedLyricItem
import com.craftworks.music.ui.playing.lyrics.WordSyncedLyricItem
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
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LyricsView(
    color: Color,
    isLandscape: Boolean = false,
    mediaController: MediaController?,
    paddingValues: PaddingValues = PaddingValues(),
    onRefreshLyrics: () -> Unit = {},
) {
    val lyrics = LyricsState.lyrics.collectAsStateWithLifecycle().value ?: return

    val loading by LyricsState.loading.collectAsStateWithLifecycle()
    var isRefreshing by remember { mutableStateOf(false) }

    val appearanceSettingsManager = AppearanceSettingsManager(LocalContext.current)

    val useBlur by appearanceSettingsManager.nowPlayingLyricsBlurFlow.collectAsState(
        true
    )
    val lyricsAnimationSpeed by appearanceSettingsManager.lyricsAnimationSpeedFlow.collectAsState(
        100
    )
    val lyricsAlignment by appearanceSettingsManager.nowPlayingLyricsAlignment.collectAsStateWithLifecycle(
        NowPlayingAlignment.CENTER
    )
    val lyricsAutoscroll by appearanceSettingsManager.lyricsAutoScroll.collectAsStateWithLifecycle(
        true
    )
    val lyricsRecenter by appearanceSettingsManager.lyricsRecenterAfterScroll.collectAsStateWithLifecycle(
        true
    )
    val lyricsWordBounce by appearanceSettingsManager.lyricsBounce.collectAsStateWithLifecycle(
        true
    )

    // State holding the current position
    var currentPositionLyrics by remember {
        mutableIntStateOf(mediaController?.currentPosition?.toInt() ?: 0)
    }
    var currentPositionScroll by remember {
        mutableIntStateOf(mediaController?.currentPosition?.toInt() ?: 0)
    }

    val currentLyricIndex = remember { mutableIntStateOf(-1) }
    var lastScrolledIndex by remember { mutableIntStateOf(-2) }

    val state = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val visibleItemsInfo by remember { derivedStateOf { state.layoutInfo.visibleItemsInfo } }

    var scrollOffset = dpToPx(128)
    val interludeHeight = dpToPx(48)

    var userScrolled by remember { mutableStateOf(false) }
    val isDragged by state.interactionSource.collectIsDraggedAsState()

    LaunchedEffect(isDragged) {
        if (state.isScrollInProgress) {
            userScrolled = true
        }
    }

    // Update current position only each lyrics change.
    LaunchedEffect(mediaController, lyrics) {
        var lyricsTrackingJon: Job = Job()
        var scrollTrackingJob: Job = Job()
        val scope = CoroutineScope(Dispatchers.Main)

        if (mediaController?.isPlaying == true) {
            lyricsTrackingJon = scope.launch {
                var position = mediaController.currentPosition.toInt()
                currentPositionLyrics = position

                while (isActive) {
                    position = mediaController.currentPosition.toInt()
                    currentPositionLyrics = position
                    delay(getNextUpdateDelay(position, lyrics.lines).milliseconds)
                }
            }

            scrollTrackingJob = scope.launch {
                var position = mediaController.currentPosition.toInt() + lyricsAnimationSpeed
                currentPositionScroll = position

                while (isActive) {
                    position = mediaController.currentPosition.toInt() + lyricsAnimationSpeed
                    currentPositionScroll = position
                    delay(getNextUpdateDelay(position, lyrics.lines).milliseconds)
                }
            }
        }

        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                if (isPlaying) {
                    if (lyricsTrackingJon.isActive) return
                    if (scrollTrackingJob.isActive) return

                    lyricsTrackingJon = scope.launch {
                        var position = mediaController.currentPosition.toInt()
                        currentPositionLyrics = position

                        while (isActive) {
                            position = mediaController.currentPosition.toInt()
                            currentPositionLyrics = position
                            delay(getNextUpdateDelay(position, lyrics.lines).milliseconds)
                        }
                    }

                    scrollTrackingJob = scope.launch {
                        var position = mediaController.currentPosition.toInt() + lyricsAnimationSpeed / 2
                        currentPositionScroll = position

                        while (isActive) {
                            position = mediaController.currentPosition.toInt() + lyricsAnimationSpeed / 2
                            currentPositionScroll = position
                            delay(getNextUpdateDelay(position, lyrics.lines).milliseconds)
                        }
                    }
                } else {
                    lyricsTrackingJon.cancel()
                    scrollTrackingJob.cancel()
                }
            }
        })
    }

    // Lyrics index update
    LaunchedEffect(currentPositionLyrics, lyrics) {
        val newCurrentLyricIndex =
            lyrics.lines.indexOfFirst { it.startMs > currentPositionLyrics }
                .takeIf { it >= 0 } ?: lyrics.lines.size

        val targetIndex = (newCurrentLyricIndex - 1).coerceAtLeast(-1)

        if (targetIndex != currentLyricIndex.intValue) {
            currentLyricIndex.intValue = targetIndex
        }
    }

    // Lyrics scrolling
    LaunchedEffect(currentPositionScroll, lyrics) {
        val newCurrentLyricIndex =
            lyrics.lines.indexOfFirst { it.startMs > currentPositionScroll }
                .takeIf { it >= 0 } ?: lyrics.lines.size

        val targetIndex = (newCurrentLyricIndex - 1).coerceAtLeast(-1)

        if (targetIndex != lastScrolledIndex) {
            lastScrolledIndex = targetIndex

            if (lyricsRecenter || !(!lyricsRecenter && userScrolled)) {
                coroutineScope.launch {
                    val targetItemAfter =
                        state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }

                    if (targetItemAfter != null) {
                        var finalScrollDelta = targetItemAfter.offset - scrollOffset

                        if (lyrics.lines[(targetIndex - 1).coerceAtLeast(0)].lines[0].text == "")
                            finalScrollDelta -= interludeHeight

                        state.animateScrollBy(
                            value = finalScrollDelta.toFloat(),
                            animationSpec = tween(lyricsAnimationSpeed, 0,
                                CubicBezierEasing(0.5f, 0.5f, 0.2f, 1f)
                            )
                        )
                    } else
                        state.animateScrollToItem(
                            index = targetIndex.coerceAtLeast(0),
                            scrollOffset = -scrollOffset
                        )
                }
            }
        }
    }

    // Plain lyrics scrolling
    var plainLyricsViewportHeightPx by remember { mutableFloatStateOf(0f) }
    var plainLyricsItemHeightPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(mediaController, lyrics, plainLyricsItemHeightPx, plainLyricsViewportHeightPx) {
        if (lyrics.lines.size == 1 && lyricsAutoscroll) {
            val updateIntervalMs = 500L

            while (isActive) {
                if (mediaController?.isPlaying == true) {
                    val totalDuration = mediaController.duration
                    val position = mediaController.currentPosition

                    val maxScroll = (plainLyricsItemHeightPx - plainLyricsViewportHeightPx).coerceAtLeast(0f)
                    val currentScrollPx = state.firstVisibleItemScrollOffset.toFloat()

                    val remainingDuration = totalDuration - position
                    val remainingScroll = maxScroll - currentScrollPx

                    if (remainingDuration > 0 && maxScroll > 0f) {
                        val speedPxPerMs = remainingScroll / remainingDuration.toFloat()

                        val delta = speedPxPerMs * updateIntervalMs

                        if (abs(delta) > 0.5f) {
                            launch {
                                state.animateScrollBy(
                                    value = delta,
                                    animationSpec = tween(updateIntervalMs.toInt(), 0, LinearEasing)
                                )
                            }
                        }
                    }
                }
                delay(updateIntervalMs.milliseconds)
            }
        }
    }

    val agentAlignment = remember(lyrics.agents, lyricsAlignment) {
        val opposite = when (lyricsAlignment) {
            NowPlayingAlignment.LEFT -> NowPlayingAlignment.RIGHT
            NowPlayingAlignment.RIGHT -> NowPlayingAlignment.LEFT
            NowPlayingAlignment.CENTER -> NowPlayingAlignment.RIGHT
        }

        var preferredTaken = false
        var oppositeTaken = false

        lyrics.agents.associate { agent ->
            val alignment = when (agent.type) {
                LyricsAgentType.GROUP -> NowPlayingAlignment.CENTER
                else -> when {
                    !preferredTaken -> { preferredTaken = true; lyricsAlignment }
                    !oppositeTaken -> { oppositeTaken = true; opposite }
                    else -> if (lyricsAlignment == NowPlayingAlignment.CENTER)
                                NowPlayingAlignment.LEFT
                            else
                                NowPlayingAlignment.CENTER
                }
            }
            agent.id to alignment
        }
    }

    Crossfade(
        loading
    ) {
        if (it) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                LoadingIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = color
                )
            }
        } else {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    if (mediaController?.currentMediaItem != null) {
                        isRefreshing = true
                        try {
                            onRefreshLyrics()
                        } finally {
                            isRefreshing = false
                        }
                    }
                },
                modifier = if (isLandscape) {
                    Modifier
                        .widthIn(min = 256.dp)
                        .fillMaxHeight()
                } else {
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                }
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .onSizeChanged { size ->
                            scrollOffset = (size.height * 0.2f).toInt()
                            plainLyricsViewportHeightPx = size.height.toFloat()
                        }
                        .verticalFadingEdges(
                            FadingEdgesContentType.Dynamic.Lazy.List(
                                FadingEdgesScrollConfig.Dynamic(),
                                state
                            ),
                            FadingEdgesGravity.All,
                            96.dp
                        ),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    contentPadding = PaddingValues(vertical = 32.dp),
                    state = state,
                ) {
                    if (lyrics.lines.size > 1) {
                        itemsIndexed(
                            lyrics.lines,
                            key = { index, lyric -> "${index}:${lyric.lines[0].text}" }
                        ) { index, lyric ->
                            val alignment = agentAlignment[lyric.agentId] ?: lyricsAlignment

                            if (!lyric.lines.any { it.words.isNullOrEmpty() }) {
                                WordSyncedLyricItem(
                                    lyric = lyric,
                                    index = index,
                                    currentLyricIndex = currentLyricIndex.intValue,
                                    currentPosition = currentPositionLyrics,
                                    useBlur = useBlur,
                                    useWordBounce = lyricsWordBounce,
                                    visibleItemsInfo = visibleItemsInfo,
                                    color = color,
                                    lyricsAnimationSpeed = lyricsAnimationSpeed,
                                    lyricsAlignment = alignment,
                                    onClick = {
                                        mediaController?.seekTo(lyric.startMs.toLong())
                                        currentPositionLyrics = lyric.startMs
                                        currentPositionScroll = lyric.startMs
                                        userScrolled = false
                                    }
                                )
                            } else {
                                SyncedLyricItem(
                                    lyric = lyric,
                                    index = index,
                                    currentLyricIndex = currentLyricIndex.intValue,
                                    useBlur = useBlur,
                                    visibleItemsInfo = visibleItemsInfo,
                                    color = color,
                                    lyricsAnimationSpeed = lyricsAnimationSpeed,
                                    lyricsAlignment = alignment,
                                    onClick = {
                                        mediaController?.seekTo(lyric.startMs.toLong())
                                        currentPositionLyrics = lyric.startMs
                                        currentPositionScroll = lyric.startMs
                                        userScrolled = false
                                    }
                                )
                            }
                        }
                    } else if (lyrics.lines.isNotEmpty()) {
                        item {
                            Text(
                                text = lyrics.lines[0].lines[0].text,
                                style = MaterialTheme.typography.headlineMedium,
                                lineHeight = MaterialTheme.typography.displayMedium.lineHeight,
                                color = color,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onSizeChanged { size ->
                                        plainLyricsItemHeightPx = size.height.toFloat()
                                    },
                                textAlign = when (lyricsAlignment) {
                                    NowPlayingAlignment.LEFT -> TextAlign.Start
                                    NowPlayingAlignment.CENTER -> TextAlign.Center
                                    NowPlayingAlignment.RIGHT -> TextAlign.End
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Stable
fun calculateLyricBlur(
    index: Int,
    currentLyricIndex: Int,
    visibleItemsInfo: List<LazyListItemInfo>
): Dp {
    return when {
        index == currentLyricIndex || !visibleItemsInfo.any { it.index == currentLyricIndex } -> 0.dp
        else -> minOf(abs(currentLyricIndex - index).toFloat(), 8f).dp
    }
}

private fun getNextUpdateDelay(currentTime: Int, lyrics: List<LyricsLine>): Long {
    val nextTimestamp = lyrics.asSequence()
        .flatMap { lyric ->
            val timestamps = mutableListOf(lyric.startMs)
            lyric.endMs?.let { timestamps.add(it) }
            lyric.lines.forEach { line ->
                line.words?.forEach { word ->
                    timestamps.add(word.startMs)
                    word.endMs?.let { timestamps.add(it) }
                }
            }
            timestamps
        }
        .filter { it > currentTime }
        .minOrNull()
        ?: return 1000L

    return (nextTimestamp - currentTime).toLong()
}