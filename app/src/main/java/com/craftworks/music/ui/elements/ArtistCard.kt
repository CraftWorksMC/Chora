package com.craftworks.music.ui.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.craftworks.music.R
import com.craftworks.music.data.model.MediaData

@Stable
@Composable
fun ArtistCard(artist: MediaData.Artist, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(12.dp)
            .aspectRatio(0.8f)
            .widthIn(min = 96.dp, max = 256.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .wrapContentHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SubcomposeAsyncImage (
            model = ImageRequest.Builder(LocalContext.current)
                .data(artist.artistImageUrl)
                .crossfade(true)
                .diskCacheKey(
                    artist.navidromeID
                )
                .build(),
            contentScale = ContentScale.Crop,
            contentDescription = "Album Image",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp)),
            loading = { painter ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clip(RoundedCornerShape(8.dp))
                )
            },
            error = { painter ->
                Icon(
                    painter = painterResource(id = R.drawable.rounded_artist_24),
                    contentDescription = "Artist Icon",
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)
                )
            },
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = artist.name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}