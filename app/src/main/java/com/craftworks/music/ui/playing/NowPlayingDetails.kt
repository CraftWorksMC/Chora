package com.craftworks.music.ui.playing

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.craftworks.music.R

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun NowPlayingDetails(
    isStarred: Boolean = false,
    currentRating: Int = 0,
    onOpenRating: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        ListItem(
            modifier = Modifier.clickable { onOpenRating() },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            ),
            headlineContent = { Text("Rating") },
            trailingContent = {
                Row (
                    horizontalArrangement = Arrangement.End
                ) {
                    if (currentRating > 0) {
                        repeat(currentRating) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    else {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.rounded_star_outline_24),
                            contentDescription = "not rated"
                        )
                    }
                }
            }
        )

        Spacer(Modifier.height(24.dp))
    }
}