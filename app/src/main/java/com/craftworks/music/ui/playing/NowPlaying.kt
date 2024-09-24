package com.craftworks.music.ui.playing

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.player.rememberManagedMediaController

var lyricsOpen by mutableStateOf(false)

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_TELEVISION,
    device = "id:tv_1080p"
)
@Composable
fun NowPlayingContent(
    context: Context = LocalContext.current,
    navHostController: NavHostController = rememberNavController(),
    mediaController: MediaController? = rememberManagedMediaController().value
) {
    //if (scaffoldState == null) return

    Log.d("RECOMPOSITION", "NowPlaying Root")

    NowPlaying_Background(mediaController)

    // handle back presses
//    val coroutineScope = rememberCoroutineScope()
//    BackHandler(scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded) {
//        coroutineScope.launch {
//            scaffoldState.bottomSheetState.partialExpand()
//        }
//    }

    if ((LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION) ||
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE){
        NowPlayingLandscape(mediaController, navHostController)
    }
    else NowPlayingPortrait(mediaController, navHostController)
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
