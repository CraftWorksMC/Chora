package com.craftworks.music.ui.playing.lyrics

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.sin

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

