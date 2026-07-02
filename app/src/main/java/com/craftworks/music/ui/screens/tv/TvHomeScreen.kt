package com.craftworks.music.ui.screens.tv

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.palette.graphics.Palette
import androidx.tv.material3.Button
import androidx.tv.material3.Carousel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.rememberCarouselState
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.craftworks.music.R
import com.craftworks.music.data.model.Screen
import com.craftworks.music.formatSeconds
import com.craftworks.music.managers.settings.AppearanceSettingsManager
import com.craftworks.music.player.SongHelper
import com.craftworks.music.ui.elements.tv.TvAlbumCard
import com.craftworks.music.ui.screens.HomeItem
import com.craftworks.music.ui.viewmodels.HomeScreenViewModel
import kotlinx.coroutines.launch
import java.net.URLEncoder

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvHomeScreen(
    navHostController: NavHostController = rememberNavController(),
    mediaController: MediaController? = null,
    viewModel: HomeScreenViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    //val libraries by NavidromeManager.libraries.collectAsStateWithLifecycle()

    val recentlyPlayedAlbums by viewModel.recentlyPlayedAlbums.collectAsStateWithLifecycle()
    val recentAlbums by viewModel.recentAlbums.collectAsStateWithLifecycle()
    val mostPlayedAlbums by viewModel.mostPlayedAlbums.collectAsStateWithLifecycle()
    val shuffledAlbums by viewModel.shuffledAlbums.collectAsStateWithLifecycle()

    val orderedHomeItems = AppearanceSettingsManager(context).homeItemsItemsFlow.collectAsState(
        initial = listOf(
            HomeItem("recently_played", true),
            HomeItem("recently_added", true),
            HomeItem("most_played", true)
        )
    ).value

    val titleMap = remember {
        mapOf(
            "recently_played" to R.string.recently_played,
            "recently_added" to R.string.recently_added,
            "most_played" to R.string.most_played
        )
    }


    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val columnState = rememberLazyListState()

    LazyColumn(
        state = columnState,
        modifier = Modifier
            .fillMaxSize()
            .focusGroup()
            .focusRequester(focusRequester)
            .focusRestorer(focusRequester),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        /*
        item (key = "libraries") {
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
                        content = {
                            Text(library.name)
                        },
                        selected = isSelected,
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Done,
                                    contentDescription = null,
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
        */

        item (key = "carousel") {
            val carouselState = rememberCarouselState()
            var carouselFocused by remember { mutableStateOf(false) }
            val coroutineScope = rememberCoroutineScope()

            Carousel(
                itemCount = shuffledAlbums.size,
                modifier = Modifier
                    .height(320.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp)
                    .padding(top = 24.dp)
                    .clip(MaterialTheme.shapes.large)
                    .onFocusChanged {
                        carouselFocused = it.isFocused
                    },
                carouselState = carouselState,
                contentTransformEndToStart =
                    fadeIn(tween(600)).togetherWith(fadeOut(tween(600))),
                contentTransformStartToEnd =
                    fadeIn(tween(600)).togetherWith(fadeOut(tween(600)))
            ) { itemIndex ->
                val album = shuffledAlbums[itemIndex]
                CarouselItem(
                    album = album,
                    carouselFocused = carouselFocused,
                    modifier = Modifier.animateEnterExit(
                        enter = slideInHorizontally(animationSpec = tween(1000)) { it / 16 },
                        exit = slideOutHorizontally(animationSpec = tween(1000)) { -it / 16 }
                    ),
                    onPlay = {
                        coroutineScope.launch {
                            val mediaItems = viewModel.getAlbumSongs(
                                album.mediaMetadata.extras?.getString("navidromeID") ?: ""
                            )
                            if (mediaItems.size > 1)
                                SongHelper.play(
                                    mediaItems = mediaItems.subList(1, mediaItems.size),
                                    index = 0,
                                    mediaController = mediaController
                                )
                            navHostController.navigate(Screen.NowPlayingLandscape.route) {
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
        }

        items(
            orderedHomeItems.filter { it.enabled },
            key = { it.key }
        ) { item ->
            val albums = when (item.key) {
                "recently_played" -> recentlyPlayedAlbums
                "recently_added" -> recentAlbums
                "most_played" -> mostPlayedAlbums
                else -> emptyList()
            }
            if (albums.isEmpty()) return@items

            Column (
                Modifier.focusGroup()
            ) {
                Text(
                    text = stringResource(titleMap[item.key] ?: R.string.recently_played),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 48.dp, vertical = 8.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier
                        .focusGroup(),
                    contentPadding = PaddingValues(horizontal = 48.dp)
                ) {
                    items(albums, key = {it.mediaId} ) { album ->
                        TvAlbumCard(
                            album = album,
                            onClick = {
                                val encodedImage = URLEncoder.encode(album.mediaMetadata.artworkUri.toString(), "UTF-8")
                                navHostController.navigate(Screen.AlbumDetails.route + "/${album.mediaMetadata.extras?.getString("id")}/$encodedImage") {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CarouselItem(
    album: MediaItem,
    carouselFocused: Boolean = false,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val title = album.mediaMetadata.title?.toString() ?: ""
    val artist = album.mediaMetadata.artist?.toString() ?: ""
    val genre = album.mediaMetadata.genre?.toString() ?: ""
    val duration = formatSeconds(
        album.mediaMetadata.durationMs?.div(1000)?.toInt() ?: 0
    )
    val subtitle = listOf(genre, artist, duration)
        .filter { it.isNotBlank() }
        .joinToString("  •  ")

    val buttonModifier =
        if (carouselFocused) {
            Modifier.requestFocusOnFirstGainingVisibility()
        } else {
            Modifier
        }

    // Extract dominant color of the carousel image to use as background
    var dominantColor by remember { mutableStateOf(Color.Black) }
    LaunchedEffect(album.mediaMetadata.artworkUri) {
        val colorRequest = ImageRequest.Builder(context)
            .data(album.mediaMetadata.artworkUri.toString()
                .replace("&size=128", "&size=32"))
            .diskCacheKey(album.mediaMetadata.extras?.getString("navidromeID"))
            .diskCachePolicy(CachePolicy.READ_ONLY)
            .allowHardware(false)
            .build()

        val result = context.imageLoader.execute(colorRequest)
        if (result is SuccessResult) {
            val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
            bitmap?.let {
                Palette.from(it).generate { palette ->
                    // DarkVibrant is usually best for "Music App" backgrounds
                    val color = palette?.darkVibrantSwatch?.rgb ?: palette?.dominantSwatch?.rgb
                    color?.let { dominantColor = Color(it) }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(dominantColor)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(
                    album.mediaMetadata.artworkUri.toString()
                        .replace("&size=128", "&size=1024")
                )
                .diskCacheKey(album.mediaMetadata.extras?.getString("navidromeID"))
                .diskCachePolicy(CachePolicy.DISABLED)
                .crossfade(true)
                .build(),
            fallback = painterResource(R.drawable.placeholder),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth(0.68f)
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.horizontalGradient(
                            0.00f to Color.Black,
                            0.30f to Color.Black.copy(alpha = 0.55f),
                            0.55f to Color.Transparent,
                        ),
                        blendMode = BlendMode.DstOut
                    )
                }
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.60f)
                .background(
                    Brush.horizontalGradient(
                        0.00f to Color.Black.copy(alpha = 0.85f),
                        0.55f to Color.Black.copy(alpha = 0.50f),
                        1.00f to Color.Transparent
                    )
                )
        )

        Column(
            modifier = modifier
                .fillMaxHeight()
                .width(484.dp)
                .padding(32.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
            }

            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = onPlay,
                modifier = buttonModifier
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.Action_Play))
            }
        }
    }
}

@Composable
fun Modifier.onFirstGainingVisibility(onGainingVisibility: () -> Unit): Modifier {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(isVisible) {
        if (isVisible) onGainingVisibility()
    }
    return onPlaced { isVisible = true }
}

@Composable
fun Modifier.requestFocusOnFirstGainingVisibility(): Modifier {
    val focusRequester = remember { FocusRequester() }
    return focusRequester(focusRequester).onFirstGainingVisibility {
        focusRequester.requestFocus()
    }
}