package com.craftworks.music.ui.playing.lyrics

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.craftworks.music.data.model.LyricsLine
import com.craftworks.music.data.model.LyricsRole
import com.craftworks.music.ui.playing.NowPlayingAlignment
import com.craftworks.music.ui.playing.calculateLyricBlur
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun WordSyncedLyricItem(
    lyric: LyricsLine,
    index: Int,
    currentLyricIndex: Int,
    currentPosition: Int,
    useBlur: Boolean,
    useWordBounce: Boolean,
    visibleItemsInfo: List<LazyListItemInfo>,
    color: Color,
    lyricsAnimationSpeed: Int = 1200,
    lyricsAlignment: NowPlayingAlignment,
    onClick: () -> Unit = {},
) {
    val lyricBlur: Dp by animateDpAsState(
        targetValue = if (useBlur) calculateLyricBlur(
            index, currentLyricIndex, visibleItemsInfo
        ) else 0.dp,
        label = "Lyric Blur",
        animationSpec = tween(lyricsAnimationSpeed, 0, FastOutSlowInEasing)
    )

    val scale by animateFloatAsState(
        targetValue = if (currentLyricIndex == index) 1f else 0.95f,
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
                            transformOrigin = when (lyricsAlignment) {
                                NowPlayingAlignment.LEFT -> TransformOrigin(0f, 0.5f)
                                NowPlayingAlignment.CENTER -> TransformOrigin(0.5f, 0.5f)
                                NowPlayingAlignment.RIGHT -> TransformOrigin(1f, 0.5f)
                            }
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
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            lyric.lines.forEach { line ->
                FlowRow (
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = when (lyricsAlignment) {
                        NowPlayingAlignment.LEFT -> Arrangement.Start
                        NowPlayingAlignment.CENTER -> Arrangement.Center
                        NowPlayingAlignment.RIGHT -> Arrangement.End
                    }
                ) {
                    line.words?.forEachIndexed { i, word ->
                        val nextWordStart = line.words.getOrNull(i + 1)?.startMs ?: line.endMs ?: lyric.endMs!!
                        val duration = word.endMs?.let { it - word.startMs } ?: (nextWordStart - word.startMs)
                        val isThisWordActive = currentPosition >= word.startMs && currentPosition < (line.endMs ?: lyric.endMs!!)

                        AnimatedWord(
                            wordText = word.text,
                            isActive = isThisWordActive,
                            durationMillis = duration,
                            role = line.role,
                            isOnlyBackgroundLine = !lyric.lines.any { it.role == LyricsRole.MAIN },
                            color = color,
                            useWordBounce = useWordBounce
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun AnimatedWord(
    wordText: String,
    isActive: Boolean,
    durationMillis: Int,
    role: LyricsRole,
    isOnlyBackgroundLine: Boolean,
    color: Color,
    useWordBounce: Boolean
) {
    val targetAlpha = if (role == LyricsRole.MAIN) 1f else 0.7f

    val inactiveColor = color.copy(alpha = 0.4f)
    val wipeProgress = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(targetAlpha) }

    val textStyle = (if (role == LyricsRole.MAIN || isOnlyBackgroundLine) MaterialTheme.typography.headlineMediumEmphasized else MaterialTheme.typography.titleLargeEmphasized)
        .copy(textMotion = TextMotion.Animated)

    val wobbleProgress = remember { Animatable(0f) }
    val wobbleShift = 0.5.sp

    val textMeasurer = rememberTextMeasurer()
    val visibleFraction = remember(wordText, textStyle) {
        val trimmed = wordText.trimEnd()
        if (trimmed.isEmpty() || trimmed.length == wordText.length) {
            1f
        } else {
            val fullWidth = textMeasurer.measure(wordText, style = textStyle).size.width
            val trimmedWidth = textMeasurer.measure(trimmed, style = textStyle).size.width
            trimmedWidth.toFloat() / fullWidth.toFloat()
        }
    }

    LaunchedEffect(isActive) {
        if (isActive) {
            coroutineScope {
                launch { textAlpha.snapTo(targetAlpha) }
                launch { wipeProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = durationMillis,
                        easing = LinearEasing
                    )
                )}
                launch {
                    wobbleProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween((1000 * 0.125f).toInt(), easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f))
                    )
                    wobbleProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween((1000 * (0.75f - 0.125f)).toInt(), easing = CubicBezierEasing(0f, 0f, 0.58f, 1f))
                    )
                }
            }
        } else {
            coroutineScope {
                launch { wipeProgress.snapTo(0f) }
                launch {
                    textAlpha.animateTo(
                        targetValue = 0.4f,
                        animationSpec = tween(durationMillis = 400, easing = LinearEasing)
                    )
                }
            }
        }
    }

    val brush = if (isActive && wipeProgress.isRunning) {
        val sweepWidth = 0.12f
        val currentOffset = wipeProgress.value * (1f + sweepWidth) * visibleFraction
        val activeEnd = (currentOffset - sweepWidth).coerceIn(0f, visibleFraction)
        val inactiveStart = currentOffset.coerceIn(0f, visibleFraction)
        Brush.horizontalGradient(
            0f to color.copy(targetAlpha),
            activeEnd to color.copy(targetAlpha),
            inactiveStart to inactiveColor,
            1f to inactiveColor
        )
    } else {
        SolidColor(color.copy(targetAlpha))
    }

    Text(
        text = wordText,
        style = textStyle,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .graphicsLayer {
                translationX = wobbleShift.toPx() * wobbleProgress.value

                if (useWordBounce)
                    translationY = -wobbleShift.toPx() * 2 * wobbleProgress.value

                scaleX = 1f + 0.025f * wobbleProgress.value
                alpha = textAlpha.value
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithCache {
                onDrawWithContent {
                    drawContent()
                    drawRect(
                        brush = brush, blendMode = BlendMode.SrcIn
                    )
                }
            },
    )
}
