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
    if (colorPalette.isEmpty())
        return

    when (backgroundStyle){
        NowPlayingBackground.PLAIN         -> PlainBackground()
        NowPlayingBackground.STATIC_BLUR   -> StaticBlurBackground(colorPalette)
        NowPlayingBackground.ANIMATED_BLUR -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) AnimatedGradientBackground(
            color1 = colorPalette[0],
            color2 = colorPalette.getOrNull(1) ?: colorPalette[0],
            color3 = colorPalette.getOrNull(2) ?: colorPalette.getOrNull(1) ?: colorPalette[0],
            overlayColor = overlayColor,
            modifier = Modifier.fillMaxSize()
        )
    }
}