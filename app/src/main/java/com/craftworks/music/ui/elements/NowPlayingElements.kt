package com.craftworks.music.ui.elements

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.craftworks.music.R
import com.craftworks.music.data.MediaData
import com.craftworks.music.data.Screen
import com.craftworks.music.data.artistList
import com.craftworks.music.data.selectedArtist
import com.craftworks.music.fadingEdge
import com.craftworks.music.player.SongHelper
import com.craftworks.music.ui.DownloadButton
import com.craftworks.music.ui.LyricsButton
import com.craftworks.music.ui.LyricsView
import com.craftworks.music.ui.MainButtons
import com.craftworks.music.ui.RepeatButton
import com.craftworks.music.ui.ShuffleButton
import com.craftworks.music.ui.SliderUpdating
import com.craftworks.music.ui.lyricsOpen
import com.craftworks.music.ui.screens.showMoreInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingPortraitCover (
    navHostController: NavHostController = rememberNavController(),
    scaffoldState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
    mediaController: MediaController?
){
    val textFadingEdge = Brush.horizontalGradient(0.85f to Color.Red, 1f to Color.Transparent)

    Column(modifier = Modifier.heightIn(min=420.dp)) {
        /* Album Cover */
        Box(modifier = Modifier
            .heightIn(min = 320.dp)
            .fillMaxWidth(),
            contentAlignment = Alignment.Center){
            AnimatedContent(lyricsOpen, label = "Crossfade between lyrics") {
                if (it){
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                        contentAlignment = Alignment.Center){
                        LyricsView(false, mediaController)
                    }
                }
                else {
                    AsyncImage(
                        model = SongHelper.currentSong.imageUrl,
                        contentDescription = "Album Cover",
                        placeholder = painterResource(R.drawable.placeholder),
                        fallback = painterResource(R.drawable.placeholder),
                        contentScale = ContentScale.FillWidth,
                        alignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                            .aspectRatio(1f)
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
                .padding(top = 6.dp)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            SongHelper.currentSong.title.let {
                Text(
                    text =
                    if (SongHelper.currentSong.isRadio == true)
                        it.split(" - ").last()
                    else
                        it,
                    fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1, overflow = TextOverflow.Visible,
                    softWrap = false,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fadingEdge(textFadingEdge)
                )
            }

            Row(modifier = Modifier
                .fillMaxWidth()
                .fadingEdge(textFadingEdge)) {
                val coroutine = rememberCoroutineScope()
                SongHelper.currentSong.artist.let { artistName ->
                    Text(
                        text = artistName,
                        fontSize = MaterialTheme.typography.titleMedium.fontSize,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1, overflow = TextOverflow.Visible,
                        softWrap = false,
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedArtist =
                                    artistList.firstOrNull() {
                                        it.name.equals(
                                            artistName,
                                            ignoreCase = true
                                        )
                                    }!!
                                coroutine.launch {
                                    scaffoldState.bottomSheetState.partialExpand()
                                }
                                navHostController.navigate(Screen.AristDetails.route) {
                                    launchSingleTop = true
                                }
                            }
                    )
                }
                if (SongHelper.currentSong.year != 0){
                    SongHelper.currentSong.year?.let { year ->
                        Text(
                            text = " • $year",
                            fontSize = MaterialTheme.typography.titleMedium.fontSize,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1, overflow = TextOverflow.Visible,
                            softWrap = false,
                            textAlign = TextAlign.Start,
                            modifier = Modifier

                        )
                    }
                }
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
    }
}

@Composable
fun NowPlayingLandscape(
    collapsed: Boolean? = false,
    isPlaying: Boolean? = false,
    song: MediaData.Song = SongHelper.currentSong,
    snackbarHostState: SnackbarHostState? = SnackbarHostState(),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    mediaController: MediaController?
) {
    Row(modifier = Modifier
        .fillMaxSize()
        .padding(start = 6.dp),
        verticalAlignment = Alignment.Top) {

        /* Album Cover */
        Box(modifier = Modifier
            .heightIn(min = 256.dp)
            .width(256.dp),
            contentAlignment = Alignment.Center){
            AnimatedContent(lyricsOpen, label = "Crossfade between lyrics") {
                if (it){
                    LyricsView(true, mediaController)
                }
                else {
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
            Column(modifier = Modifier.height(72.dp)){
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
                            text = format.toString(),
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
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(end = 12.dp)
                .padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                SliderUpdating(true, mediaController)
            }

            //region BUTTONS
            Column(modifier = Modifier.fillMaxWidth(),horizontalAlignment = Alignment.CenterHorizontally) {
                /* MAIN ACTIONS */
                Row(modifier = Modifier
                    .height(96.dp)
                    .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    MainButtons(mediaController, isPlaying)
                }
                // BUTTONS
                Row(modifier = Modifier
                    .height(64.dp)
                    //.width(256.dp)
                    .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically) {
                    LyricsButton(48.dp)

                    ShuffleButton(48.dp, mediaController)

                    RepeatButton(48.dp, mediaController)

                    DownloadButton(snackbarHostState, coroutineScope, 48.dp)
                }
            }
            //endregion
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingMiniPlayer(
    scaffoldState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
    mediaController: MediaController?
) {
    val coroutineScope = rememberCoroutineScope()
    val isPlaying = remember { mutableStateOf(false) }


    Box (modifier = Modifier
        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
        .height(72.dp)
        .fillMaxWidth()
        .clickable {
            coroutineScope.launch {
                if (scaffoldState.bottomSheetState.currentValue == SheetValue.PartiallyExpanded)
                    scaffoldState.bottomSheetState.expand()
                else
                    scaffoldState.bottomSheetState.partialExpand()
            }
        }) {
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)) {
            // Album Image
            AsyncImage(
                model = SongHelper.currentSong.imageUrl,
                contentDescription = "Album Cover",
                placeholder = painterResource(R.drawable.placeholder),
                fallback = painterResource(R.drawable.placeholder),
                contentScale = ContentScale.FillWidth,
                alignment = Alignment.Center,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    //.shadow(4.dp, RoundedCornerShape(6.dp), clip = true)
                    .background(MaterialTheme.colorScheme.surfaceVariant)

            )

            // Title + Artist
            Column(modifier = Modifier
                .padding(horizontal = 12.dp)
                .weight(1f)) {
                SongHelper.currentSong.title.let {
                    Text(
                        text = //Limit the artist name length.
                        if (SongHelper.currentSong.isRadio == true)
                            it.split(" - ").last()
                        else
                            if (it.length > 24) it.substring(0, 21) + "..."
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
                        text = //Limit the artist name length.
                        if (it.isBlank()) ""
                        else if (it.length > 24)
                            it.substring(0, 21) + "..." + " • " + SongHelper.currentSong.year
                        else
                            it + if (SongHelper.currentSong.year != 0) " • " + SongHelper.currentSong.year
                                 else "",
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                }
            }

            // Play/Pause Icon
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isPlaying.value)
                        ImageVector.vectorResource(R.drawable.media3_notification_pause)
                    else
                        Icons.Rounded.PlayArrow,
                    tint = MaterialTheme.colorScheme.onBackground,
                    contentDescription = "Play/Pause",
                    modifier = Modifier
                        .height(48.dp)
                        .size(48.dp)
                        .bounceClick()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            mediaController?.playWhenReady =
                                !(mediaController?.playWhenReady ?: true)
                        }
                )
            }
        }
    }
}