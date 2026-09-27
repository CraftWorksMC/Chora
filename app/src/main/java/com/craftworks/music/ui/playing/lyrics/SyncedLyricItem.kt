package com.craftworks.music.ui.playing.lyrics

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.craftworks.music.data.model.LyricsLine
import com.craftworks.music.data.model.LyricsRole
import com.craftworks.music.ui.playing.NowPlayingAlignment
import com.craftworks.music.ui.playing.calculateLyricBlur

@Composable
fun SyncedLyricItem(
    lyric: LyricsLine,
    index: Int,
    currentLyricIndex: Int,
    useBlur: Boolean,
    visibleItemsInfo: List<LazyListItemInfo>,
    color: Color,
    lyricsAnimationSpeed: Int = 1200,
    lyricsAlignment: NowPlayingAlignment,
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
        animationSpec = tween(lyricsAnimationSpeed / 2, 0, FastOutSlowInEasing)
    )

    val scale by animateFloatAsState(
        targetValue = if (currentLyricIndex == index) 1f else 0.9f,
        label = "Lyric Scale Animation",
        animationSpec = tween(lyricsAnimationSpeed, 0, CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))
    )

    if (lyric.lines[0].text.isEmpty()) {
        AnimatedContent(
            targetState = currentLyricIndex == index
        ) {
            if (it) {
                Box(
                    modifier = Modifier
                        .focusable(false)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        },
                    contentAlignment = when (lyricsAlignment) {
                        NowPlayingAlignment.LEFT -> Alignment.TopStart
                        NowPlayingAlignment.CENTER -> Alignment.TopCenter
                        NowPlayingAlignment.RIGHT -> Alignment.TopEnd
                    }
                ) {
                    InterludeIndicator(color)
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .padding(vertical = 12.dp)
                .heightIn(min = 48.dp)
                .focusable(false)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = when (lyricsAlignment) {
                        NowPlayingAlignment.LEFT -> TransformOrigin(0f, 0.5f)
                        NowPlayingAlignment.CENTER -> TransformOrigin(0.5f, 0.5f)
                        NowPlayingAlignment.RIGHT -> TransformOrigin(1f, 0.5f)
                    }
                }
                .blur(lyricBlur)
                .clickable {
                    onClick()
                },
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            lyric.lines.forEach{ line ->
                val isMain = line.role == LyricsRole.MAIN

                Text(
                    text = line.text,
                    style = if (isMain || !lyric.lines.any { it.role == LyricsRole.MAIN }) MaterialTheme.typography.headlineMediumEmphasized
                    else MaterialTheme.typography.titleLargeEmphasized,
                    fontWeight = FontWeight.SemiBold,
                    color = color.copy(alpha = if (isMain) lyricAlpha else lyricAlpha * 0.65f),
                    modifier = Modifier.fillMaxWidth(),
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
