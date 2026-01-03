package com.craftworks.music.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.R
import com.craftworks.music.player.SongHelper
import com.craftworks.music.player.rememberManagedMediaController
import com.craftworks.music.ui.playing.PlayQueueContent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(navHostController: NavHostController = rememberNavController()) {
    val mediaControllerState = rememberManagedMediaController()
    val mediaController = mediaControllerState.value
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.Queue_Title)) },
                navigationIcon = {
                    IconButton(
                        onClick = { navHostController.popBackStack() },
                        modifier = Modifier.size(56.dp, 70.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            tint = MaterialTheme.colorScheme.onBackground,
                            contentDescription = "Back",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actions = {
                    // Shuffle queue button
                    IconButton(onClick = {
                        coroutineScope.launch {
                            SongHelper.shuffleQueue(mediaController)
                        }
                    }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.rounded_shuffle_24),
                            contentDescription = "Shuffle Queue",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Clear queue button
                    IconButton(onClick = {
                        coroutineScope.launch {
                            SongHelper.clearQueue(mediaController)
                        }
                    }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.rounded_clear_all_24),
                            contentDescription = stringResource(R.string.Queue_Clear),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .fillMaxSize()
        ) {
            PlayQueueContent(
                mediaController = mediaController
            )
        }
    }
}
