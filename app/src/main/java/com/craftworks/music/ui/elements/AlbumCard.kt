package com.craftworks.music.ui.elements

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.craftworks.music.R
import com.craftworks.music.SongHelper
import com.craftworks.music.data.Album
import com.craftworks.music.data.songsList
import com.craftworks.music.playingSong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumCard(album: Album, onClick: () -> Unit){
    val context = LocalContext.current
    Card(
        onClick = { onClick() },
        modifier = Modifier.padding(12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            disabledContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
            disabledContentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 128.dp)
        ) {
            Box{
                AsyncImage(
                    model = album.coverArt,
                    placeholder = painterResource(R.drawable.placeholder),
                    fallback = painterResource(R.drawable.placeholder),
                    contentScale = ContentScale.FillWidth,
                    contentDescription = "Album Image",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                )
                Button(
                    onClick = { // Play First Song in Album
                        playingSong.selectedSong = songsList.filter { it.album == album.name }[0]
                        playingSong.selectedList = songsList.filter { it.album == album.name }
                        songsList.filter { it.album == album.name }[0].media?.let { SongHelper.PlayStream(context = context, url = it)} },
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp).align(Alignment.BottomStart).padding(6.dp),
                    contentPadding = PaddingValues(2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.background.copy(0.5f)
                    )
                ) {
                    // Inner content including an icon and a text label
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        tint = MaterialTheme.colorScheme.onBackground,
                        contentDescription = "Play Album",
                        modifier = Modifier
                            .height(48.dp)
                            .size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = album.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 3.dp, top = 3.dp),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}