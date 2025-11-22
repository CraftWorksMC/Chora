package com.craftworks.music.ui.playing.background

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Stable
@Composable
fun StaticBlurBackground(
    colors: List<Color?>
) {
    Log.d("RECOMPOSITION", "Recomposing Static Blur BG")

    if (colors.isEmpty())
        return

    val animatedColors = colors.map { color ->
        animateColorAsState(
            targetValue = color ?: MaterialTheme.colorScheme.background,
            animationSpec = tween(durationMillis = 1500),
            label = "Animated Colors"
        ).value
    }

    Canvas(
        modifier = Modifier.Companion
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        drawRect(
            Brush.Companion.radialGradient(
                colors = listOf(
                    animatedColors.elementAtOrNull(0) ?: Color.Companion.Black,
                    Color.Companion.Transparent
                ),
                center = Offset(size.width * 0.05f, size.height / 2),
                radius = size.width * 2
            )
        )
        drawRect(
            Brush.Companion.radialGradient(
                colors = listOf(
                    animatedColors.elementAtOrNull(1) ?: animatedColors.firstNotNullOf { it },
                    Color.Companion.Transparent
                ),
                center = Offset(0f, size.height),
                radius = size.height
            )
        )
        drawRect(
            Brush.Companion.radialGradient(
                colors = listOf(
                    animatedColors.elementAtOrNull(2) ?: animatedColors.firstNotNullOf { it },
                    Color.Companion.Transparent
                ),
                center = Offset(size.width * 1.1f, size.height * 0.1f),
                radius = size.height
            )
        )
        drawRect(
            Brush.Companion.radialGradient(
                colors = listOf(
                    animatedColors.elementAtOrNull(3) ?: animatedColors.firstNotNullOf { it },
                    Color.Companion.Transparent
                ),
                center = Offset(size.width, size.height * 0.9f),
                radius = size.height
            )
        )
    }
}