package com.craftworks.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import com.craftworks.music.R
import com.craftworks.music.player.SongHelper
import com.craftworks.music.ui.elements.RadioCard
import com.craftworks.music.ui.elements.RippleEffect
import com.craftworks.music.ui.elements.dialogs.AddRadioDialog
import com.craftworks.music.ui.elements.dialogs.ModifyRadioDialog
import com.craftworks.music.ui.playing.dpToPx
import com.craftworks.music.ui.viewmodels.RadioScreenViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalFoundationApi
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RadioScreen(
    mediaController: MediaController? = null,
    viewModel: RadioScreenViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()

    val state = rememberPullToRefreshState()

    var showRadioModifyDialog by remember { mutableStateOf(false) }
    var showRadioAddDialog by remember { mutableStateOf(false) }

    val isRefreshing by viewModel.isLoading.collectAsStateWithLifecycle()
    val radios by viewModel.radioStations.collectAsStateWithLifecycle()

    var showRipple by remember { mutableIntStateOf(0) }
    val rippleXOffset = LocalWindowInfo.current.containerSize.width / 2
    val rippleYOffset = dpToPx(12)


    val onRefresh: () -> Unit = {
        viewModel.getRadioStations()
        showRipple++
    }

    PullToRefreshBox(
        state = state,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh
    ) {
        /* RADIO ICON + TEXT */
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = WindowInsets.statusBars
                        .asPaddingValues()
                        .calculateTopPadding()
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.rounded_radio),
                    contentDescription = "Songs Icon",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.radios),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.headlineLarge.fontSize
                )
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { showRadioAddDialog = true },
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp, 70.dp),
                    contentPadding = PaddingValues(2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.rounded_add_24),
                        tint = MaterialTheme.colorScheme.onBackground,
                        contentDescription = "Previous Song",
                        modifier = Modifier
                            .height(32.dp)
                            .size(32.dp)
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(128.dp),
                modifier = Modifier
                    .wrapContentWidth()
                    .fillMaxHeight(),
            ) {
                items(radios) { radio ->
                    RadioCard(
                        radio = radio,
                        onClick = {
                            coroutineScope.launch {
                                SongHelper.play(
                                    listOf(radio),
                                    0,
                                    mediaController
                                )
                            }
                        },
                        onLongClick = {
                            showRadioModifyDialog = true
                            viewModel.selectRadioStation(it)
                        }
                    )
                }
            }
        }
    }
    RippleEffect(
        center = Offset(rippleXOffset.toFloat(), rippleYOffset.toFloat()),
        color = MaterialTheme.colorScheme.surfaceVariant,
        key = showRipple
    )

    if (showRadioAddDialog)
        AddRadioDialog(
            setShowDialog = { showRadioAddDialog = it },
            onAdded = { name, url, homePageUrl, addToNavidrome ->
                viewModel.addRadioStation(name, url, homePageUrl, addToNavidrome)
                onRefresh.invoke()
            }
        )
    if (showRadioModifyDialog) {
        val selectedRadio by viewModel.selectedRadioStation.collectAsStateWithLifecycle()
        ModifyRadioDialog(
            setShowDialog = { showRadioModifyDialog = it },
            radio = selectedRadio,
            onModified = { id, name, url, homepage ->
                viewModel.modifyRadioStation(id, name, url, homepage)
                onRefresh.invoke()
            },
            onDeleted = {
                viewModel.deleteRadioStation(it)
                onRefresh.invoke()
            }
        )
    }
}