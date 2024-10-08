package com.craftworks.music.ui.elements

import android.content.res.Configuration
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.craftworks.music.player.SongHelper

@Composable
fun BottomSpacer(){
    // I'm sorry for this code.
    // I couldn't find a better way to do it.

//    Spacer(modifier = Modifier.height(
//        if (SongHelper.currentSong.title == "" &&
//            SongHelper.currentSong.duration == 0 &&
//            SongHelper.currentSong.imageUrl == "") {
//            if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT)
//                12.dp + 80.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
//            else
//                0.dp
//        }
//        else {
//            if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT)
//                72.dp + 80.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
//            else
//                72.dp
//        })
//    )

    Spacer(modifier = Modifier.height(0.dp))
}

@Composable
fun bottomSpacerHeightDp(): Dp {
    return if (SongHelper.currentSong.title == "" &&
        SongHelper.currentSong.duration == 0 &&
        SongHelper.currentSong.imageUrl == "" ||
        LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION ||
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            0.dp
    }
    else {
        72.dp
    }
}