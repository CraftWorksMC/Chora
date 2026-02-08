package com.craftworks.music.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.R
import com.craftworks.music.data.model.Screen
import com.craftworks.music.managers.NavidromeManager
import com.craftworks.music.managers.settings.AppearanceSettingsManager
import com.craftworks.music.player.SongHelper
import com.craftworks.music.ui.elements.AlbumRow
import com.craftworks.music.ui.elements.RippleEffect
import com.craftworks.music.ui.playing.dpToPx
import com.craftworks.music.ui.viewmodels.HomeScreenViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.net.URLEncoder

@Stable
@Serializable
data class HomeItem(
    var key: String,
    var enabled: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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

    val libraries by NavidromeManager.libraries.collectAsStateWithLifecycle()

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
                Row (Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    val username = AppearanceSettingsManager(context).usernameFlow.collectAsState("Username")
                    val showNavidromeLogo =
                        AppearanceSettingsManager(context).showNavidromeLogoFlow.collectAsState(true).value && NavidromeManager.checkActiveServers()

                    if (showNavidromeLogo) NavidromeLogo()

                    Text(
                        text = "${stringResource(R.string.welcome_text)},\n${username.value}!",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .offset(x = if (showNavidromeLogo) (-36).dp else 0.dp),
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                if (libraries.size > 1) {
                    libraries.forEach { (library, isSelected) ->
                        FilterChip(
                            onClick = {
                                NavidromeManager.currentServerId.value?.let { serverId ->
                                    NavidromeManager.toggleServerLibraryEnabled(
                                        serverId,
                                        library.id,
                                        !isSelected
                                    )
                                }
                            },
                            label = {
                                Text(library.name)
                            },
                            selected = isSelected,
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.Done,
                                        contentDescription = "Done icon",
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                }
                            } else {
                                null
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            }


            val orderedHomeItems = AppearanceSettingsManager(context).homeItemsItemsFlow.collectAsState(
                initial = listOf(
                    HomeItem(
                        "recently_played",
                        true
                    ),
                    HomeItem(
                        "recently_added",
                        true
                    ),
                    HomeItem(
                        "most_played",
                        true
                    ),
                    HomeItem(
                        "random_songs",
                        true
                    )
                )
            ).value

            orderedHomeItems.forEach { item ->
                if (item.enabled) {
                    val albums = when (item.key) {
                        "recently_played" -> recentlyPlayedAlbums
                        "recently_added" -> recentAlbums
                        "most_played" -> mostPlayedAlbums
                        "random_songs" -> shuffledAlbums
                        else -> emptyList()
                    }

                    val titleMap = remember {
                        mapOf(
                            "recently_played" to R.string.recently_played,
                            "recently_added" to R.string.recently_added,
                            "most_played" to R.string.most_played,
                            "random_songs" to R.string.random_songs
                        )
                    }

                    AlbumRow(
                        item.key,
                        titleMap[item.key],
                        albums,
                        mediaController,
                        navHostController,
                        viewModel
                    )
                }
            }
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
            .size(76.dp)
            .offset(x = (-36).dp)
            .shadow(24.dp, CircleShape)
            .graphicsLayer {
                rotationZ = animatedRotation
            }
            .then(isClickable)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable fun AlbumRow(
    key: String,
    title: Int?,
    albums: List<MediaItem>,
    mediaController: MediaController?,
    navHostController: NavHostController,
    viewModel: HomeScreenViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = if (LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION)
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            else
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .clickable {
                        navHostController.navigate(Screen.HomeLists.route + "/$key") {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
        ) {
            Text(
                text = stringResource(title ?: androidx.media3.session.R.string.error_message_fallback),
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