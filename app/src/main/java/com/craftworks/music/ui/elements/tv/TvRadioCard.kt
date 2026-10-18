package com.craftworks.music.ui.elements.tv

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.tv.material3.Card
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.StandardCardContainer
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.craftworks.music.R

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvRadioCard(
    radio: MediaItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (radio: MediaItem) -> Unit = { }
) {
    val radioName = radio.mediaMetadata.station?.toString() ?: ""
    StandardCardContainer(
        modifier = modifier.width(128.dp),
        imageCard = {
            Card(
                onClick = onClick,
                onLongClick = { onLongClick(radio) },
                interactionSource = it,
                content = {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(("android.resource://com.craftworks.music/" + R.drawable.radioplaceholder).toUri())
                            .crossfade(true).build(),
                        fallback = painterResource(R.drawable.placeholder),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                    )
                }
            )
        },
        title = {
            Text(
                text = radioName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
            )
        },
    )
}