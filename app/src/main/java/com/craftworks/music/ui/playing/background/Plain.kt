package com.craftworks.music.ui.playing.background

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Stable
@Composable
fun PlainBackground() {
    Box(
        Modifier.Companion
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
    )
}