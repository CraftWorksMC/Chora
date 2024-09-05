package com.craftworks.music.ui.playing

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.craftworks.music.R
import com.craftworks.music.player.SongHelper
import com.craftworks.music.player.rememberManagedMediaController
import com.craftworks.music.ui.screens.showMoreInfo
import kotlinx.coroutines.launch

var lyricsOpen by mutableStateOf(false)

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_TELEVISION,
    device = "id:tv_1080p"
)
@Composable
fun NowPlayingContent(
    context: Context = LocalContext.current,
    scaffoldState: BottomSheetScaffoldState? = rememberBottomSheetScaffoldState(),
    snackbarHostState: SnackbarHostState? = SnackbarHostState(),
    navHostController: NavHostController = rememberNavController(),
    mediaController: MediaController? = rememberManagedMediaController().value
) {
    if (scaffoldState == null) return

    Log.d("RECOMPOSITION", "NowPlaying Root")

    NowPlaying_Background(mediaController)

    // handle back presses
    val coroutineScope = rememberCoroutineScope()
    BackHandler(scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded) {
        coroutineScope.launch {
            scaffoldState.bottomSheetState.partialExpand()
        }
    }

    NowPlayingPortrait(mediaController, scaffoldState, navHostController)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NowPlaying_TV(
    collapsed: Boolean? = false,
    mediaController: MediaController?
) {
    val (prev, play, next, shuffle, replay) = remember { FocusRequester.createRefs() }

    Row {
        Column(modifier = Modifier
            .width(512.dp)
            .padding(start = 80.dp + 6.dp),
            horizontalAlignment = Alignment.Start) {

            /* Album Cover */
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(256.dp),
                contentAlignment = Alignment.Center){
                AsyncImage(
                    model = SongHelper.currentSong.imageUrl,
                    contentDescription = "Album Cover",
                    placeholder = painterResource(R.drawable.placeholder),
                    fallback = painterResource(R.drawable.placeholder),
                    contentScale = ContentScale.FillHeight,
                    alignment = Alignment.Center,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .shadow(4.dp, RoundedCornerShape(24.dp), clip = true)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
//                LyricsView(true, mediaController)
            }

            /* Song Title + Artist*/
            Column(
                modifier = Modifier
                    .width(512.dp)
                    .fillMaxHeight()
                    .padding(top = 4.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top
            ) {
                Column(modifier = Modifier
                    .height(72.dp)
                    .padding(start = 24.dp)){
                    SongHelper.currentSong.title.let {
                        Text(
                            text = // Limit Song Title Length (if not collapsed).
                            if (it.length > 24 && collapsed == false) it.substring(0, 21) + "..."
                            else it,
                            fontSize = MaterialTheme.typography.titleLarge.fontSize,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start
                        )
                    }
                    SongHelper.currentSong.artist.let {
                        Text(
                            text = //Limit the artist name length (if not collapsed).
                            if (it.length > 20 && collapsed == false)
                                it.substring(0, 17) + "..." + " • " + SongHelper.currentSong.year
                            else
                                it + if (SongHelper.currentSong.year != 0) " • " + SongHelper.currentSong.year
                                else "",
                            fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start
                        )
                    }
                    if (showMoreInfo.value) {
                        Text(
                            text = "${SongHelper.currentSong.format} • ${SongHelper.currentSong.bitrate} • ${
                                if (SongHelper.currentSong.navidromeID == "Local")
                                    stringResource(R.string.Source_Local)
                                else
                                    stringResource(R.string.Source_Navidrome)
                            } ",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Light,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            maxLines = 1,
                            textAlign = TextAlign.Start
                        )
                    }
                }

                /* Progress Bar */
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 12.dp)
                    .padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    //SliderUpdating(MaterialTheme.colorScheme.onBackground, mediaController)
                }

                //region BUTTONS
                Column(modifier = Modifier.fillMaxWidth(),horizontalAlignment = Alignment.CenterHorizontally) {

                    LaunchedEffect(Unit) {
                        play.requestFocus()
                    }

                    /* MAIN ACTIONS */
                    Row(modifier = Modifier
                        .height(96.dp)
                        .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        //MainButtons(mediaController, prev, play, next, shuffle)
                    }
                    // BUTTONS
                    Row(modifier = Modifier
                        .height(64.dp)
                        //.width(256.dp)
                        .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically) {
                        //ShuffleButton(48.dp, mediaController, play, shuffle)

                        //RepeatButton(48.dp, mediaController, play, replay)

                        //DownloadButton(snackbarHostState, coroutineScope, 48.dp)
                    }
                }
                //endregion
            }
        }
    }
}

@Composable
fun dpToPx(dp: Int): Int {
    return with(LocalDensity.current) { dp.dp.toPx() }.toInt()
}

// Returns the normalized center item offset (-1,1)
fun LazyListLayoutInfo.normalizedItemPosition(key: Any) : Float =
    visibleItemsInfo
        .firstOrNull { it.index == key }
        ?.let {
            val center = (viewportEndOffset + viewportStartOffset - it.size) / 2F
            (it.offset.toFloat() - center) / center
        }
        ?: 0F

@Composable
fun NowPlayingLandscape(
    collapsed: Boolean? = false,
    mediaController: MediaController?
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 6.dp),
        verticalAlignment = Alignment.Top
    ) {

        /* Album Cover */
        Box(
            modifier = Modifier
                .heightIn(min = 256.dp)
                .width(256.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(lyricsOpen, label = "Crossfade between lyrics") {
                if (it) {
                    //LyricsView(MaterialTheme.colorScheme.onBackground,true, mediaController)
                } else {
                    AsyncImage(
                        model = SongHelper.currentSong.imageUrl,
                        contentDescription = "Album Cover",
                        placeholder = painterResource(R.drawable.placeholder),
                        fallback = painterResource(R.drawable.placeholder),
                        contentScale = ContentScale.FillHeight,
                        alignment = Alignment.Center,
                        modifier = Modifier
                            .size(256.dp)
                            .padding(12.dp)
                            .shadow(4.dp, RoundedCornerShape(24.dp), clip = true)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }
        }

        /* Song Title + Artist*/
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(top = 4.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            Column(modifier = Modifier.height(72.dp)) {
                SongHelper.currentSong.title.let {
                    Text(
                        text = // Limit Song Title Length (if not collapsed).
                        if (it.length > 24 && collapsed == false) it.substring(0, 21) + "..."
                        else it,
                        fontSize = MaterialTheme.typography.titleLarge.fontSize,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                }
                SongHelper.currentSong.artist.let {
                    Text(
                        text = //Limit the artist name length (if not collapsed).
                        if (it.length > 20 && collapsed == false)
                            it.substring(0, 17) + "..." + " • " + SongHelper.currentSong.year
                        else
                            it + if (SongHelper.currentSong.year != 0) " • " + SongHelper.currentSong.year
                            else "",
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                }
                if (showMoreInfo.value) {
                    SongHelper.currentSong.format.let { format ->
                        Text(
                            text = format,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Light,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }

            /* Progress Bar */
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 12.dp)
                    .padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally
            ) {
                //SliderUpdating(MaterialTheme.colorScheme.onBackground, mediaController)
            }

            //region BUTTONS
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                /* MAIN ACTIONS */
                Row(
                    modifier = Modifier
                        .height(96.dp)
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    //MainButtons(mediaController)
                }
                // BUTTONS
                Row(
                    modifier = Modifier
                        .height(64.dp)
                        //.width(256.dp)
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    //LyricsButton(48.dp)

                    //ShuffleButton(48.dp, mediaController)

                    //RepeatButton(48.dp, mediaController)

                    //DownloadButton(48.dp)
                }
            }
            //endregion
        }
    }
}
