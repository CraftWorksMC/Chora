package com.craftworks.music.ui.playing

import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.craftworks.music.ui.playing.background.AnimatedGradientBackground
import com.craftworks.music.ui.playing.background.PlainBackground
import com.craftworks.music.ui.playing.background.StaticBlurBackground

enum class NowPlayingBackground {
    PLAIN, STATIC_BLUR, ANIMATED_BLUR
}

@Preview
@Stable
@Composable
fun NowPlaying_Background(
    colorPalette: List<Color> = emptyList(),
    backgroundStyle: NowPlayingBackground = NowPlayingBackground.STATIC_BLUR,
    overlayColor: Color = Color.Transparent
) {
    val safePalette = listOf(
        colorPalette.getOrNull(1) ?: Color.Black,
        colorPalette.getOrNull(2) ?: Color.Black,
        colorPalette.getOrNull(3) ?: Color.Black
    )

    when (backgroundStyle){
        NowPlayingBackground.PLAIN         -> PlainBackground()
        NowPlayingBackground.STATIC_BLUR   -> StaticBlurBackground(safePalette)
        NowPlayingBackground.ANIMATED_BLUR -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) AnimatedGradientBackground(
            color1 = safePalette[0],
            color2 = safePalette[1],
            color3 = safePalette[2],
            overlayColor = overlayColor,
            modifier = Modifier.fillMaxSize()
        )
    }
}