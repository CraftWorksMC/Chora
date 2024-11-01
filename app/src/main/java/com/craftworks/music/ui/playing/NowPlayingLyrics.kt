package com.craftworks.music.ui.playing

import android.util.Log
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    val currentLyricIndex = remember {
        mutableIntStateOf(
            lyrics.indexOfFirst {
                it.timestamp > currentPositionValue.intValue
            }.takeIf { it >= 0 } ?: lyrics.size
        )
    }

    val state = rememberLazyListState()

    // Update the current playback position every second
    LaunchedEffect(mediaController) {
        while (true) {
            delay(1000)
            if (mediaController?.isPlaying == true) {
                currentPositionValue.intValue = mediaController.currentPosition.toInt()

                val newCurrentLyricIndex = lyrics.indexOfFirst { it.timestamp > currentPositionValue.intValue }
                    .takeIf { it >= 0 } ?: 0

                currentLyricIndex.intValue = (newCurrentLyricIndex - 1).coerceAtLeast(0)

                state.animateScrollToItem(currentLyricIndex.intValue)
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
//                val blur by remember(currentLyricIndex.intValue, isCurrentLyricVisible) {
//                    derivedStateOf {
//                        calculateBlur(
//                            currentLyricIndex = currentLyricIndex.intValue,
//                            currentIndex = index,
//                            isCurrentLyricVisible = isCurrentLyricVisible
//                        )
//                    }
//                }

                var isCurrentLyric by remember { mutableStateOf(index == currentLyricIndex.intValue) }

                LaunchedEffect(currentLyricIndex) {
                    isCurrentLyric = index == currentLyricIndex.intValue
                }

                SyncedLyricItem(
                    lyric = lyric.content,
                    //index = index,
                    //currentLyricIndex = currentLyricIndex.intValue,
                    color = color,
                    isCurrentLyric = isCurrentLyric,
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

@Stable
@Composable
fun SyncedLyricItem(
    lyric: String,
    color: Color,
    isCurrentLyric: Boolean,
) {
    SideEffect {
        Log.d("RECOMPOSITION", "Recomposed SyncedLyricsItem")
    }


    val alpha by animateFloatAsState(
        targetValue = if (isCurrentLyric) 1f else 0.3f,
        animationSpec = tween(durationMillis = 300),
        label = "Non-current lyrics alpha"
    )


    Box(modifier = Modifier
        .padding(vertical = 12.dp)
        .heightIn(min = 48.dp)
        .focusable(false),
        contentAlignment = Alignment.Center) {
        Text(
            text = if (lyric == "") "• • •" else lyric,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            //color = color.copy(lyricAlpha),
            color = color,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer(
                    alpha = alpha
                ),
            textAlign = TextAlign.Center,
            lineHeight = 32.sp
        )
    }
}

//@Stable
//fun calculateLyricBlur(
//    index: Int,
//    currentLyricIndex: Int,
//    isVisible: Boolean
//): Dp {
//    return when {
//        index == currentLyricIndex || !isVisible -> 0.dp
//        else -> minOf(abs(currentLyricIndex - index).toFloat(), 8f).dp
//    }
//}