package com.craftworks.music.ui.elements

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.session.MediaController
import coil.compose.AsyncImage
import com.craftworks.music.R
import com.craftworks.music.data.Album
import com.craftworks.music.data.songsList
import com.craftworks.music.player.SongHelper
import com.craftworks.music.player.rememberManagedMediaController

@Preview
@Composable
fun AlbumCard(album: Album = Album("","","", Uri.EMPTY),
              mediaController: MediaController? = null,
              onClick: () -> Unit = {}){
    Card(
        onClick = { onClick() },
        modifier = Modifier
            .padding(12.dp, 12.dp, 0.dp, 0.dp),
            //.aspectRatio(0.8f),
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
                //.widthIn(min = 96.dp, max = 256.dp)
                .width(128.dp)
                .height(172.dp),
                //.wrapContentHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box (modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)){
                AsyncImage(
                    model = album.coverArt,
                    placeholder = painterResource(R.drawable.placeholder),
                    fallback = painterResource(R.drawable.placeholder),
                    contentScale = ContentScale.FillHeight,
                    contentDescription = "Album Image",
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                )
                Button(
                    onClick = { // Play First Song in Album
                        SongHelper.currentSong = songsList.filter { it.album == album.name }[0]
                        SongHelper.currentList = songsList.filter { it.album == album.name }
                        songsList.filter { it.album == album.name }[0].media?.let { SongHelper.playStream(Uri.parse(it), false, mediaController)} },
                    shape = CircleShape,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.BottomStart)
                        .padding(6.dp),
                    contentPadding = PaddingValues(2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.background.copy(0.5f)
                    )
                ) {
                    
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
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )


            Text(
                text = album.artist,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onBackground.copy(0.75f),
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}