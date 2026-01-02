package com.craftworks.music.ui.screens.onboarding

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import kotlin.random.Random

/**
 * A serene starfield - like gently flying over a field of stars.
 * Stars drift past with trails as we glide through space.
 * Optimized for smooth, lag-free animation.
 */
@Stable
@Composable
fun StarfieldBackground(
    modifier: Modifier = Modifier
) {
    val backgroundColor = MaterialTheme.colorScheme.surface
    val starColor = MaterialTheme.colorScheme.primary

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        DriftingStarsShader(
            backgroundColor = backgroundColor,
            starColor = starColor,
            modifier = modifier
        )
    } else {
        DriftingStarsCanvas(
            backgroundColor = backgroundColor,
            starColor = starColor,
            modifier = modifier
        )
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun DriftingStarsShader(
    backgroundColor: Color,
    starColor: Color,
    modifier: Modifier = Modifier
) {
    val shader = remember { RuntimeShader(DRIFTING_STARS_SHADER) }

    val infiniteTransition = rememberInfiniteTransition(label = "drift_time")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 100000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Box(
        modifier = modifier.drawWithCache {
            val brush = ShaderBrush(shader)
            onDrawBehind {
                shader.setFloatUniform("iResolution", size.width, size.height)
                shader.setFloatUniform("iTime", time)
                shader.setFloatUniform(
                    "bgColor",
                    backgroundColor.red,
                    backgroundColor.green,
                    backgroundColor.blue
                )
                shader.setFloatUniform(
                    "starColor",
                    starColor.red,
                    starColor.green,
                    starColor.blue
                )
                drawRect(brush = brush)
            }
        }
    )
}

// Pre-generated stable star data to avoid allocations
@Stable
private class StarData(seed: Int) {
    val stars: List<DriftingStar> = List(50) { index ->
        val rng = Random(seed + index * 1337)
        DriftingStar(
            x = rng.nextFloat(),
            y = rng.nextFloat(),
            speed = rng.nextFloat() * 0.5f + 0.3f,
            size = rng.nextFloat() * 2.5f + 1.5f,
            trailLength = rng.nextFloat() * 0.12f + 0.04f,
            brightness = rng.nextFloat() * 0.5f + 0.5f,
            layer = index % 3
        )
    }
}

@Stable
private data class DriftingStar(
    val x: Float,
    val y: Float,
    val speed: Float,
    val size: Float,
    val trailLength: Float,
    val brightness: Float,
    val layer: Int
)

@Composable
private fun DriftingStarsCanvas(
    backgroundColor: Color,
    starColor: Color,
    modifier: Modifier = Modifier
) {
    // Stable star data - generated once
    val starData = remember { StarData(42) }

    val infiniteTransition = rememberInfiniteTransition(label = "drift_canvas")

    // Smooth drift animation - one full cycle per 4 seconds
    val driftProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drift"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        // Draw background once
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    backgroundColor,
                    backgroundColor.copy(alpha = 0.95f)
                )
            )
        )

        // Draw stars drifting diagonally (like flying over them)
        starData.stars.forEach { star ->
            // Calculate position - stars drift from top-right to bottom-left
            val adjustedProgress = (driftProgress * star.speed + star.x) % 1f

            // Current position
            val currentX = (1f - adjustedProgress) * size.width * 1.2f - size.width * 0.1f
            val currentY = (adjustedProgress + star.y) % 1f * size.height

            // Trail position (where the star came from)
            val trailX = currentX + star.trailLength * size.width
            val trailY = currentY - star.trailLength * size.height * 0.5f

            // Layer-based brightness (distant stars dimmer)
            val layerAlpha = when (star.layer) {
                0 -> 0.3f
                1 -> 0.5f
                else -> 0.8f
            } * star.brightness

            // Only draw if on screen
            if (currentX in -50f..size.width + 50f && currentY in -50f..size.height + 50f) {
                // Draw trail
                drawLine(
                    color = starColor.copy(alpha = layerAlpha * 0.4f),
                    start = Offset(trailX, trailY),
                    end = Offset(currentX, currentY),
                    strokeWidth = star.size * 0.6f,
                    cap = StrokeCap.Round
                )

                // Draw star head
                drawCircle(
                    color = starColor.copy(alpha = layerAlpha),
                    radius = star.size,
                    center = Offset(currentX, currentY)
                )

                // Bright core for close stars
                if (star.layer == 2) {
                    drawCircle(
                        color = Color.White.copy(alpha = layerAlpha * 0.6f),
                        radius = star.size * 0.4f,
                        center = Offset(currentX, currentY)
                    )
                }
            }
        }
    }
}

// Optimized GLSL shader - stars drift across screen with trails
private const val DRIFTING_STARS_SHADER = """
    uniform float2 iResolution;
    uniform float iTime;
    uniform half3 bgColor;
    uniform half3 starColor;

    float hash(float2 p) {
        return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453);
    }

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord / iResolution;

        float stars = 0.0;
        float t = iTime * 0.3; // Double speed drift

        // 3 layers of stars
        for (int layer = 0; layer < 3; layer++) {
            float layerSpeed = 0.1 + float(layer) * 0.06;
            float layerBrightness = 0.25 + float(layer) * 0.25;
            float gridSize = 12.0 + float(layer) * 8.0;

            // Offset UV for drift (diagonal movement)
            float2 driftUV = uv + float2(t * layerSpeed, -t * layerSpeed * 0.5);

            // Grid for stars
            float2 grid = floor(driftUV * gridSize);
            float2 f = fract(driftUV * gridSize);

            // Star in this cell?
            float rand = hash(grid);
            if (rand < 0.3) {
                // Star position within cell
                float2 starPos = float2(
                    hash(grid + 1.0) * 0.6 + 0.2,
                    hash(grid + 2.0) * 0.6 + 0.2
                );

                float2 d = f - starPos;
                float dist = length(d);

                // Star size
                float starSize = 0.03 + hash(grid + 3.0) * 0.04;

                // Star brightness with soft edge
                float star = smoothstep(starSize, starSize * 0.3, dist);

                // Trail (elongated in direction of movement)
                float2 trailDir = normalize(float2(1.0, -0.5));
                float trailDist = dot(d, trailDir);
                float trailWidth = abs(dot(d, float2(-trailDir.y, trailDir.x)));
                float trail = smoothstep(0.15, 0.0, trailDist) *
                              smoothstep(starSize * 1.5, 0.0, trailWidth) *
                              smoothstep(0.0, 0.05, trailDist) * 0.3;

                stars += (star + trail) * layerBrightness;
            }
        }

        stars = clamp(stars, 0.0, 1.0);

        half3 color = bgColor + starColor * stars;

        return half4(color, 1.0);
    }
"""
