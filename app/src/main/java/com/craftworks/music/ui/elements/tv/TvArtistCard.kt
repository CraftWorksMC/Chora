package com.craftworks.music.ui.elements.tv

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.CompactCard
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.craftworks.music.R
import com.craftworks.music.data.model.MediaModel

@Composable
fun TvArtistCard(
    artist: MediaModel.Artist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CompactCard(
        modifier = modifier.width(128.dp),
        onClick = onClick,
        image = {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(artist.imageUrl ?: artist.imageId?.let {artist.getProvider()?.getImageUrl(it)})
                    .diskCacheKey(artist.id)
                    .placeholderMemoryCacheKey(artist.id)
                    .memoryCacheKey(artist.id)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .crossfade(true)
                    .build(),
                placeholder = painterResource(R.drawable.placeholder),
                fallback = painterResource(R.drawable.placeholder),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().aspectRatio(1f)
            )
        },
        title = {
            Text(
                text = artist.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(12.dp)
            )
        }
    )
}