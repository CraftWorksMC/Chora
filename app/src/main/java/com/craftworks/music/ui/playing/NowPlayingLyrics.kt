package com.craftworks.music.ui.playing

import android.util.Log
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.session.MediaController
import com.craftworks.music.data.Lyric
import com.craftworks.music.lyrics.LyricsManager
import com.craftworks.music.managers.SettingsManager
import com.gigamole.composefadingedges.FadingEdgesGravity
import com.gigamole.composefadingedges.content.FadingEdgesContentType
import com.gigamole.composefadingedges.content.scrollconfig.FadingEdgesScrollConfig
import com.gigamole.composefadingedges.verticalFadingEdges
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun LyricsView(
    color: Color,
    isLandscape: Boolean = false,
    mediaController: MediaController?,
    paddingValues: PaddingValues = PaddingValues(),
) {
    SideEffect {
        Log.d("RECOMPOSITION", "Recomposed Lyrics View")
    }

    val lyrics by LyricsManager.Lyrics.collectAsState()
    val useBlur by SettingsManager(LocalContext.current).nowPlayingLyricsBlurFlow.collectAsState(true)

    // State holding the current position
    val currentPositionValue = remember { mutableIntStateOf(mediaController?.currentPosition?.toInt() ?: 0) }

    val currentLyricIndex = remember { mutableIntStateOf(0) }

    val state = rememberLazyListState()
    val visibleItemsInfo by remember { derivedStateOf { state.layoutInfo.visibleItemsInfo } }

    val scrollOffset = dpToPx(64)

    // Update the current playback position every second
    LaunchedEffect(mediaController) {
        while (true) {
            delay(1000)
            if (mediaController?.isPlaying == true) {
                currentPositionValue.intValue = mediaController.currentPosition.toInt()

                val newCurrentLyricIndex = lyrics.indexOfFirst { it.timestamp > currentPositionValue.intValue }
                    .takeIf { it >= 0 } ?: 0

                currentLyricIndex.intValue = (newCurrentLyricIndex - 1).coerceAtLeast(0)

                state.animateScrollToItem(currentLyricIndex.intValue, -scrollOffset)
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
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        state = state,
    ) {

        // Loading spinner thingy
        if (lyrics[0].content.isEmpty() && lyrics.size == 1) {
            item {
                Box(modifier = Modifier.height(64.dp)) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(12.dp)
                            .size(32.dp), strokeCap = StrokeCap.Round
                    )
                }
            }
        }

        // Synced Lyrics
        if (lyrics.size > 1) {
            item {
                Spacer(Modifier.height(64.dp))
            }
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
                    color = color
                )
            }
        }

        // plain lyrics
        else if (lyrics[0].timestamp == -1) {
            item {
                Text(
                    text = lyrics[0].content,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    lineHeight = MaterialTheme.typography.titleLarge.lineHeight.times(1.2f)
                )
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
) {
    SideEffect {
        println("Recomposing Synced Lyrics Item $index")
    }

    val lyricAlpha: Float by animateFloatAsState(
        if (currentLyricIndex == index) 1f else 0.5f,
        label = "Current Lyric Alpha",
        animationSpec = tween(1000, 0, FastOutSlowInEasing)
    )
    val lyricBlur: Dp by animateDpAsState(
        targetValue = if (useBlur) calculateLyricBlur(
            index, currentLyricIndex, visibleItemsInfo
        ) else 0.dp,
        label = "Lyric Blur",
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
    )
    val scale by animateFloatAsState(
        targetValue = if (currentLyricIndex == index) 1f else 0.9f,
        label = "Lyric Scale Animation",
        animationSpec = tween(1000, 0, FastOutSlowInEasing)
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
        , contentAlignment = Alignment.Center) {
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