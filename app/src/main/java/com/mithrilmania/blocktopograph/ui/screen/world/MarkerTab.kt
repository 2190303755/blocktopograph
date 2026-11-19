package com.mithrilmania.blocktopograph.ui.screen.world

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.ui.component.IconButton
import kotlinx.coroutines.launch

@Composable
fun MarkerTab(
    viewModel: WorldEditorModel,
    info: InitState.Succeed,
    padding: PaddingValues
) {
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        if (viewModel.markers === null) {
            isRefreshing = true
            viewModel.loadMarkers().join()
            isRefreshing = false
        }
    }
    val refresh: () -> Unit = {
        if (!isRefreshing) {
            isRefreshing = true
            coroutineScope.launch {
                viewModel.loadMarkers().join()
                isRefreshing = false
            }
        }
    }
    Column(Modifier.fillMaxSize()) {
        // TODO: wire the switch to on-map marker rendering once markers are rendered here.
        SegmentedListItem(
            selected = false,
            onClick = { viewModel.showMarkers = !viewModel.showMarkers },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
            colors = ListItemDefaults.segmentedColors(),
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RefreshIndicator(isRefreshing, refresh)
                    Switch(
                        checked = viewModel.showMarkers,
                        onCheckedChange = null
                    )
                }
            },
            content = { Text(stringResource(R.string.toggle_markers)) }
        )
        Box(Modifier.fillMaxSize()) {
            val markers = viewModel.markers
            if (markers !== null) {
                if (markers.isEmpty()) {
                    Text(
                        stringResource(R.string.no_custom_markers),
                        Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .align(Alignment.TopCenter)
                    )
                }
                LazyColumn(Modifier.fillMaxSize()) {
                    items(markers) {
                        ListItem(
                            onClick = { viewModel.moveCamera(it.x.toDouble(), it.z.toDouble()) },
                            modifier = Modifier.animateItem(),
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent
                            ),
                            supportingContent = {
                                Text(it.getPositionDescription(context))
                            }
                        ) {
                            Text(it.namedBitmapProvider.bitmapDataName)
                        }
                    }
                }
            } else if (!isRefreshing) {
                Text(
                    stringResource(R.string.general_failed),
                    Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .align(Alignment.TopCenter)
                )
            }
        }
    }
}

@Composable
internal fun RefreshIndicator(isRefreshing: Boolean, onRefresh: () -> Unit) {
    AnimatedContent(
        targetState = isRefreshing,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "refreshIndicator"
    ) { refreshing ->
        if (refreshing) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }
        } else {
            IconButton(Icons.Filled.Refresh, onClick = onRefresh)
        }
    }
}
