package com.craftworks.music.ui.playing.background

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush

@Stable
@Composable
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun SimpleAnimatedGradientBackground(
    colors: List<Color>,
    overlayColor: Color = Color.Transparent,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    val c1 = colors.getOrElse(0) { Color.Blue }
    val c2 = colors.getOrElse(1) { colors.getOrElse(0) { Color.Cyan } }
    val c3 = colors.getOrElse(2) { colors.getOrElse(0) { Color.Magenta } }
    val c4 = colors.getOrElse(3) { colors.getOrElse(1) { Color.Yellow } }

    val animC1 by animateColorAsState(c1, tween(1500), label = "c1")
    val animC2 by animateColorAsState(c2, tween(1500), label = "c2")
    val animC3 by animateColorAsState(c3, tween(1500), label = "c3")
    val animC4 by animateColorAsState(c4, tween(1500), label = "c4")
    val animOverlay by animateColorAsState(overlayColor, tween(1500), label = "overlay")

    val shader = remember { RuntimeShader(MESH_GRADIENT_SHADER) }

    val infiniteTransition = rememberInfiniteTransition(label = "time")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(120000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time_float"
    )

    Box(
        modifier = modifier.drawWithCache {
            val brush = ShaderBrush(shader)
            onDrawBehind {
                shader.setFloatUniform("iResolution", size.width, size.height)
                shader.setFloatUniform("iTime", time)

                shader.setFloatUniform("color1", animC1.red, animC1.green, animC1.blue)
                shader.setFloatUniform("color2", animC2.red, animC2.green, animC2.blue)
                shader.setFloatUniform("color3", animC3.red, animC3.green, animC3.blue)
                shader.setFloatUniform("color4", animC4.red, animC4.green, animC4.blue)

                drawRect(brush = brush)

                drawRect(color = animOverlay, blendMode = BlendMode.SrcOver)
            }
        }
    )
}

private const val MESH_GRADIENT_SHADER = """
    uniform float2 iResolution;
    uniform float iTime;
    uniform half3 color1;
    uniform half3 color2;
    uniform half3 color3;
    uniform half3 color4;

    float random(float2 st) {
        return fract(sin(dot(st.xy, float2(12.9898, 78.233))) * 43758.5453123);
    }

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord.xy / iResolution.xy;
        float aspect = iResolution.x / iResolution.y;
        
        float2 p = uv;
        p.x *= aspect; 
        
        float t = iTime * 0.15; 

        p.x += sin(t * 0.8 + p.y * 3.0) * 0.15;
        p.y += cos(t * 0.9 + p.x * 2.5) * 0.1;

        float2 center1 = float2(
            0.5 * aspect + sin(t * 0.6 + 0.0) * 0.45, 
            0.5 + cos(t * 0.5 + 1.0) * 0.35
        );
        float2 center2 = float2(
            0.5 * aspect + cos(t * 0.3 + 2.0) * 0.5, 
            0.5 + sin(t * 0.7 + 3.0) * 0.4
        );
        float2 center3 = float2(
            0.5 * aspect + sin(t * 0.9 + 4.0) * 0.35, 
            0.5 + cos(t * 0.4 + 5.0) * 0.5
        );
        float2 center4 = float2(
            0.5 * aspect + cos(t * 0.5 + 6.0) * 0.4, 
            0.5 + sin(t * 0.8 + 7.0) * 0.3
        );

        float d1 = length(p - center1);
        float d2 = length(p - center2);
        float d3 = length(p - center3);
        float d4 = length(p - center4);

        float w1 = 1.0 / (d1 * d1 + 0.1);
        float w2 = 1.0 / (d2 * d2 + 0.1);
        float w3 = 1.0 / (d3 * d3 + 0.1);
        float w4 = 1.0 / (d4 * d4 + 0.1);

        float total = w1 + w2 + w3 + w4;

        half3 col = (color1 * w1 + color2 * w2 + color3 * w3 + color4 * w4) / total;

        float noise = (random(uv * 10.0 + iTime) - 0.5) * (1.0/255.0) * 4.0;
        col += noise;

        //float vig = 1.0 - pow(length(uv - 0.5) * 2.0, 2.0) * 0.3;
        //col *= vig;

        return half4(col, 1.0);
    }
"""