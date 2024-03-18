package com.craftworks.music.ui.elements.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.craftworks.music.R
import com.craftworks.music.SongHelper
import com.craftworks.music.ui.elements.bounceClick
import com.craftworks.music.ui.screens.showMoreInfo
import kotlinx.coroutines.launch

@Composable
@Preview(showBackground = true)
fun PortraitAlbumCover (){
    Column(modifier = Modifier.height(420.dp)) {
        println("Recomposing Image + Text")
        /* Album Cover */
        Box(
            modifier = Modifier
                .height(320.dp)
                .fillMaxWidth(), contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = SongHelper.currentSong.imageUrl,
                contentDescription = "Album Cover",
                placeholder = painterResource(R.drawable.placeholder),
                fallback = painterResource(R.drawable.placeholder),
                contentScale = ContentScale.FillHeight,
                alignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(320.dp)
                    .shadow(4.dp, RoundedCornerShape(24.dp), clip = true)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clip(RoundedCornerShape(24.dp))
            )
        }

        /* Song Title + Artist*/
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, start = 36.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            SongHelper.currentSong.title.let {
                Text(
                    text = //Limit the artist name length.
                    if (SongHelper.currentSong.isRadio == true)
                        it.split(" - ").last()
                    else
                        if (it.length > 24) it.substring(0, 21) + "..."
                        else it,
                    fontSize = MaterialTheme.typography.headlineLarge.fontSize,
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
                        it + " • " + SongHelper.currentSong.year,
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start
                )
            }
            if (showMoreInfo.value) {
                SongHelper.currentSong.format.let {format ->
                    Text(
                        text = "${format.toString()} • ${
                            if (SongHelper.currentSong.navidromeID == "Local")
                                stringResource(R.string.Source_Local)
                            else
                                stringResource(R.string.Source_Navidrome)
                        } ",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Thin,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview(showBackground = true)
fun PortraitMiniPlayer(
    scaffoldState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
    isPlaying: Boolean = true){
    val coroutineScope = rememberCoroutineScope()
    Box (modifier = Modifier
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
            Column(modifier = Modifier.padding(horizontal = 12.dp).weight(1f)) {
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
                            it + " • " + SongHelper.currentSong.year,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                }
                /*
                if (showMoreInfo.value) {
                    SongHelper.currentSong.format.let {format ->
                        Text(
                            text = "${format.toString()} • ${
                                if (SongHelper.currentSong.navidromeID == "Local")
                                    stringResource(R.string.Source_Local)
                                else
                                    stringResource(R.string.Source_Navidrome)
                            } ",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Thin,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start
                        )
                    }
                }*/
            }

            // Play/Pause Icon
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isPlaying)
                        ImageVector.vectorResource(R.drawable.round_pause_24)
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
                            SongHelper.player.playWhenReady =
                                !SongHelper.player.playWhenReady
                        }
                )
            }
        }
    }
}