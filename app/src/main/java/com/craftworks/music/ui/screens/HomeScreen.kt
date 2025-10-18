package com.craftworks.music.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.R
import com.craftworks.music.data.model.Screen
import com.craftworks.music.managers.NavidromeManager
import com.craftworks.music.managers.SettingsManager
import com.craftworks.music.player.SongHelper
import com.craftworks.music.ui.elements.AlbumRow
import com.craftworks.music.ui.elements.RippleEffect
import com.craftworks.music.ui.playing.dpToPx
import com.craftworks.music.ui.viewmodels.HomeScreenViewModel
import kotlinx.coroutines.launch
import java.net.URLEncoder

enum class AlbumCategory(val key: String, val displayNameRes: Int) {
    RECENTLY_PLAYED("recently_played", R.string.recently_played),
    RECENTLY_ADDED("recently_added", R.string.recently_added),
    MOST_PLAYED("most_played", R.string.most_played),
    RANDOM_SONGS("random_songs", R.string.random_songs)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navHostController: NavHostController = rememberNavController(),
    mediaController: MediaController? = null,
    viewModel: HomeScreenViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val recentlyPlayedAlbums by viewModel.recentlyPlayedAlbums.collectAsStateWithLifecycle()
    val recentAlbums by viewModel.recentAlbums.collectAsStateWithLifecycle()
    val mostPlayedAlbums by viewModel.mostPlayedAlbums.collectAsStateWithLifecycle()
    val shuffledAlbums by viewModel.shuffledAlbums.collectAsStateWithLifecycle()

    val state = rememberPullToRefreshState()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    var showRipple by remember { mutableIntStateOf(0) }
    val rippleXOffset = LocalWindowInfo.current.containerSize.width / 2
    val rippleYOffset = dpToPx(12)
    val onRefresh: () -> Unit = {
        viewModel.loadHomeScreenData()
        showRipple++
    }

    PullToRefreshBox(
        modifier = Modifier,
        state = state,
        isRefreshing = isLoading,
        onRefresh = onRefresh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(
                        top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                    )
            ) {
                Box(Modifier.weight(1f)) {
                    val username = SettingsManager(context).usernameFlow.collectAsState("Username")
                    val showNavidromeLogo =
                        SettingsManager(context).showNavidromeLogoFlow.collectAsState(true).value && NavidromeManager.checkActiveServers()

                    if (showNavidromeLogo) NavidromeLogo()

                    Text(
                        text = "${stringResource(R.string.welcome_text)},\n${username.value}!",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                        modifier = Modifier.padding(
                            start = if (showNavidromeLogo) 42.dp else 12.dp
                        ),
                        lineHeight = MaterialTheme.typography.headlineLarge.lineHeight
                    )
                }
                IconButton(
                    onClick = {
                        navHostController.navigate(Screen.Setting.route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }, modifier = Modifier
                        .padding(end = 12.dp)
                        .size(48.dp)
                ) {
                    Icon(
                        ImageVector.vectorResource(R.drawable.rounded_settings_24),
                        contentDescription = "Settings",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            AlbumRow(AlbumCategory.RECENTLY_PLAYED, recentlyPlayedAlbums, mediaController, navHostController, viewModel)
            AlbumRow(AlbumCategory.RECENTLY_ADDED, recentAlbums, mediaController, navHostController, viewModel)
            AlbumRow(AlbumCategory.MOST_PLAYED, mostPlayedAlbums, mediaController, navHostController, viewModel)
            AlbumRow(AlbumCategory.RANDOM_SONGS, shuffledAlbums, mediaController, navHostController, viewModel)
        }
    }

    RippleEffect(
        center = Offset(rippleXOffset.toFloat(), rippleYOffset.toFloat()),
        color = MaterialTheme.colorScheme.surfaceVariant,
        key = showRipple
    )
}

@Composable fun NavidromeLogo(){
    var rotation by remember { mutableFloatStateOf(-10f) }
    val animatedRotation by animateFloatAsState(
        targetValue = rotation,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessVeryLow
        ),
        label = "Navidrome Logo Rotate"
    )
    val offsetX = dpToPx(-36)
    val clickAction = rememberUpdatedState {
        rotation += 180f
    }

    val isClickable =
        if (LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK != Configuration.UI_MODE_TYPE_TELEVISION)
            Modifier.clickable { clickAction.value.invoke() }
        else
            Modifier

    Image(
        painter = painterResource(R.drawable.s_m_navidrome),
        contentDescription = "Navidrome Icon",
        modifier = Modifier
            .size(72.dp)
            .offset { IntOffset(offsetX, 0) }
            .shadow(24.dp, CircleShape)
            .graphicsLayer {
                rotationZ = animatedRotation
            }
            .then(isClickable)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable fun AlbumRow(
    title: AlbumCategory,
    albums: List<MediaItem>,
    mediaController: MediaController?,
    navHostController: NavHostController,
    viewModel: HomeScreenViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxWidth().padding(bottom = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val category = when (title) {
                        AlbumCategory.RECENTLY_PLAYED  -> "recently_played"
                        AlbumCategory.RECENTLY_ADDED -> "recently_added"
                        AlbumCategory.MOST_PLAYED -> "most_played"
                        AlbumCategory.RANDOM_SONGS -> "random_songs"
                    }

                    navHostController.navigate(Screen.HomeLists.route + "/$category") {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Text(
                text = stringResource(title.displayNameRes),
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
                fontSize = MaterialTheme.typography.headlineSmall.fontSize
            )

            Spacer(modifier = Modifier.weight(1f))

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .size(MaterialTheme.typography.headlineSmall.fontSize.value.dp * 1.2f)
            )
        }

        AlbumRow(
            albums,
            mediaController,
            onAlbumSelected = { album ->
                val encodedImage = URLEncoder.encode(album.coverArt, "UTF-8")
                navHostController.navigate(Screen.AlbumDetails.route + "/${album.navidromeID}/$encodedImage") {
                    launchSingleTop = true
                }
            },
            onPlay = { album ->
                coroutineScope.launch {
                    val mediaItems = viewModel.getAlbumSongs(album.mediaMetadata.extras?.getString("navidromeID") ?: "")
                    if (mediaItems.isNotEmpty())
                        SongHelper.play(
                            mediaItems = mediaItems.subList(1, mediaItems.size),
                            index = 0,
                            mediaController = mediaController
                        )
                }
            }
        )
    }
}