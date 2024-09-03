package com.craftworks.music.ui.playing

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.session.MediaController
import com.craftworks.music.data.PlainLyrics
import com.craftworks.music.data.SyncedLyric
import com.craftworks.music.fadingEdge
import com.craftworks.music.lyrics.LyricsManager
import com.craftworks.music.sliderPos
import kotlinx.coroutines.delay

@Composable
fun LyricsView(
    isLandscape: Boolean = false,
    mediaController: MediaController?) {

    var currentLyricIndex by remember { mutableIntStateOf(0) }

    val lyrics by LyricsManager.Lyrics.collectAsState()

    LaunchedEffect(lyrics, sliderPos) {
        snapshotFlow { sliderPos.intValue.toLong() + 750 }.collect { currentPosition ->
            currentLyricIndex =
                ((lyrics.indexOfFirst { it.timestamp > currentPosition }.takeIf { it >= 0 }
                    ?: lyrics.size) - 1).coerceAtLeast(0)

            // Calculate the delay between lyrics. If the nextLyricIndex is -1 then it means we've reached the end of the lyrics.
            val nextLyricIndex =
                lyrics.indexOfFirst { it.timestamp > currentPosition }.takeIf { it >= 0 }
                    ?: lyrics.size
            val delayMillis = if (nextLyricIndex in lyrics.indices) {
                (lyrics[nextLyricIndex].timestamp - currentPosition).coerceAtLeast(0)
            } else {
                1000L
            }
            delay(delayMillis)
        }
    }

    val topBottomFade = Brush.verticalGradient(
        0f to Color.Transparent,
        0.15f to Color.Red,
        0.85f to Color.Red,
        1f to Color.Transparent
    )
    val state = rememberLazyListState()

    LaunchedEffect(currentLyricIndex) {
        delay(100)
        state.animateScrollToItem(currentLyricIndex)
    }

    LazyColumn(
        modifier = if (isLandscape) {
            Modifier
                .widthIn(min = 256.dp)
                .fillMaxHeight()
                .padding(horizontal = 12.dp)
        } else {
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        }
            .fadingEdge(topBottomFade),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        state = state,
    ) {
        item { // For the spacing + loading indicator
            Box(modifier = Modifier.height(if (isLandscape) 32.dp else 60.dp)) {
                if (PlainLyrics == "Getting Lyrics..." && SyncedLyric.size == 0) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(12.dp)
                            .size(32.dp),
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        }

        // For displaying Synced Lyrics
        if (lyrics.size > 1) {
            itemsIndexed(lyrics) { index, lyric ->
                val lyricAlpha: Float by animateFloatAsState(
                    if (currentLyricIndex == index) 1f else 0.5f,
                    label = "Current Lyric Alpha",
                    animationSpec = tween(1000, 0, FastOutSlowInEasing)
                )
                val scale by animateFloatAsState(
                    targetValue = if (currentLyricIndex == index) 1f else 0.9f,
                    label = "Lyric Scale Animation",
                    animationSpec = tween(1000, 0, FastOutSlowInEasing)
                )
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .heightIn(min = 48.dp)
                        .clickable {
                            mediaController?.seekTo(lyric.timestamp.toLong())
                            currentLyricIndex = index
                        }
                        .focusable(false)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .animateContentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (lyric.content == "") "• • •" else lyric.content,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(lyricAlpha),
                        modifier = Modifier
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp
                    )
                }
            }
        } else if (lyrics[0].timestamp == -1 ) {
            item { // For displaying plain lyrics
                Text(
                    text = lyrics[0].content,
                    style = if (isLandscape) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    lineHeight = MaterialTheme.typography.titleLarge.lineHeight.times(1.2f)
                )
            }
        } /*else {
            item { // For the "Retry" section
                Box(
                    modifier = Modifier
                        //.height((dpToSp).dp)
                        .height(64.dp)
                        .clickable {
                            coroutineScope.launch {
                                LyricsManager.getLyrics()
                            }
                            //getLyrics()
                        }
                        .focusRequester(retryLyricsFocus)
                        .focusProperties {
                            down = playFocus
                            right = FocusRequester.Cancel
                            left = FocusRequester.Cancel
                            up = FocusRequester.Cancel
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Retry",
                        style = if (isLandscape) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp
                    )
                }
            }
        } */
    }
}