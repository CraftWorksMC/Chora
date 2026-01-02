package com.craftworks.music.ui.elements

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.craftworks.music.data.database.entity.DownloadEntity
import com.craftworks.music.data.database.entity.DownloadStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Composable
fun FloatingDownloadIndicator(
    activeDownloads: Flow<List<DownloadEntity>>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val downloads by activeDownloads.collectAsStateWithLifecycle(initialValue = emptyList())
    val hasActiveDownloads = downloads.isNotEmpty()

    AnimatedVisibility(
        visible = hasActiveDownloads,
        enter = scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn(),
        exit = scaleOut(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeOut(),
        modifier = modifier.zIndex(10f)
    ) {
        // Move animations INSIDE AnimatedVisibility so they only run when visible
        val currentDownload = downloads.firstOrNull { it.status == DownloadStatus.DOWNLOADING }
            ?: downloads.firstOrNull()

        // Animation for disc rotation - only runs when visible
        val infiniteTransition = rememberInfiniteTransition(label = "disc_rotation")
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation"
        )

        // Pulse animation for downloading state - only runs when visible
        val pulse by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )

        // Calculate overall progress
        val overallProgress = if (downloads.isNotEmpty()) {
            downloads.map { it.progress }.average().toFloat()
        } else 0f

        val animatedProgress by animateFloatAsState(
            targetValue = overallProgress,
            animationSpec = tween(300),
            label = "progress"
        )

        BadgedBox(
            badge = {
                if (downloads.size > 1) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ) {
                        Text(
                            text = if (downloads.size > 99) "99+" else downloads.size.toString()
                        )
                    }
                }
            }
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .scale(pulse)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable(onClick = onClick)
                    .bounceClick(),
                contentAlignment = Alignment.Center
            ) {
                // Progress ring around the disc
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(64.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer,
                    strokeWidth = 3.dp,
                    strokeCap = StrokeCap.Round
                )

                // Vinyl disc with album art
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .rotate(rotation)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    // Album art or default disc
                    if (currentDownload?.imageUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(currentDownload.imageUrl)
                                .size(with(LocalDensity.current) { 52.dp.toPx().toInt() })
                                .crossfade(true)
                                .build(),
                            contentDescription = "Downloading album art",
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Center hole (vinyl style)
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingDownloadIndicatorPositioned(
    activeDownloads: Flow<List<DownloadEntity>>,
    onClick: () -> Unit,
    miniPlayerVisible: Boolean,
    modifier: Modifier = Modifier
) {
    val bottomOffset = if (miniPlayerVisible) 140.dp else 80.dp

    FloatingDownloadIndicator(
        activeDownloads = activeDownloads,
        onClick = onClick,
        modifier = modifier
            .padding(16.dp)
            .offset(y = -bottomOffset)
    )
}
