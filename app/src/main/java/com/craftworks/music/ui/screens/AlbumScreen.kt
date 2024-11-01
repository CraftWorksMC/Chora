package com.craftworks.music.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.R
import com.craftworks.music.data.Screen
import com.craftworks.music.ui.elements.AlbumGrid
import com.craftworks.music.ui.elements.HorizontalLineWithNavidromeCheck
import com.craftworks.music.ui.viewmodels.AlbumScreenViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalFoundationApi
@Preview(showBackground = true, showSystemUi = false)
@Composable
fun AlbumScreen(
    navHostController: NavHostController = rememberNavController(),
    mediaController: MediaController? = null,
    viewModel: AlbumScreenViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val leftPadding = if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) 0.dp else 80.dp
    val context = LocalContext.current

    var isSearchFieldOpen by remember { mutableStateOf(false) }
    var searchFilter by remember { mutableStateOf("") }

    val state = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }

    val onRefresh: () -> Unit = {
        isRefreshing = true
        viewModel.reloadData()
        isRefreshing = false
    }

    val allAlbumsList by viewModel.allAlbums.collectAsState()

    PullToRefreshBox(
        //modifier = Modifier.background(MaterialTheme.colorScheme.background),
        state = state,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = leftPadding,
                top = WindowInsets.statusBars
                    .asPaddingValues()
                    .calculateTopPadding()
            )) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.placeholder),
                    contentDescription = "Songs Icon",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(48.dp))
                Text(
                    text = stringResource(R.string.Albums),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.headlineLarge.fontSize
                )

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = { isSearchFieldOpen = !isSearchFieldOpen },
                    modifier = Modifier
                        .size(48.dp)) {
                    Icon(Icons.Rounded.Search, contentDescription = "Search all songs")
                }
            }

            HorizontalLineWithNavidromeCheck()

            val coroutineScope = rememberCoroutineScope()
            AnimatedVisibility(
                visible = isSearchFieldOpen,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                ) {
                    val focusRequester = remember { FocusRequester() }
                    TextField(
                        value = searchFilter,
                        onValueChange = {
                            searchFilter = it
                            if (it.isBlank()){
                                viewModel.reloadData()
                            } },
                        label = { Text(stringResource(R.string.Action_Search)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                coroutineScope.launch {
                                    viewModel.search(searchFilter)
                                    isSearchFieldOpen = false
                                }
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            //.clip(RoundedCornerShape(0.dp, 0.dp, 12.dp, 12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .focusRequester(focusRequester))

                    LaunchedEffect(isSearchFieldOpen) {
                        if (isSearchFieldOpen)
                            focusRequester.requestFocus()
                        else
                            focusRequester.freeFocus()
                    }
                }
            }

//            LaunchedEffect(searchFilter) {
//                if (searchFilter.isNotBlank() && NavidromeManager.checkActiveServers()){
//                    albumList.clear()
//                }
//            }

            AlbumGrid(allAlbumsList,
                mediaController,
                onAlbumSelected = { album ->
                        navHostController.navigate(Screen.AlbumDetails.route) {
                            launchSingleTop = true
                    }
                selectedAlbum = album },
                searchFilter.isNotBlank(),
                "alphabeticalByName",
                viewModel)
        }
    }
}