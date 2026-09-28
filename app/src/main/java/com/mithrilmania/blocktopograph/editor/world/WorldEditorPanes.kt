package com.mithrilmania.blocktopograph.editor.world

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.input.then
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.ui.component.AnimatedExpanderIndicator
import com.mithrilmania.blocktopograph.ui.component.IconButton
import com.mithrilmania.blocktopograph.ui.component.WindowInsets
import com.mithrilmania.blocktopograph.ui.component.safeLayoutInsets
import com.mithrilmania.blocktopograph.ui.theme.BlocktopographCompatTheme
import com.mithrilmania.blocktopograph.util.isDecimal
import com.mithrilmania.blocktopograph.util.toast
import com.mithrilmania.blocktopograph.world.WorldModel
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.Camera

enum class PaneType {
    NONE,
    SELECTOR,
    LOCATOR
}

class SelectorPaneState {
    val expanded = mutableStateOf(true)

    fun reset() {
        this.expanded.value = true
    }
}

class LocatorPaneState {
    val pagerState = PagerState { 3 }
    val coordinateX = TextFieldState()
    val coordinateZ = TextFieldState()
    val expanded = mutableStateOf(true)

    fun reset() {
        this.pagerState.requestScrollToPage(0)
        this.coordinateX.setTextAndPlaceCursorAtEnd("")
        this.coordinateZ.setTextAndPlaceCursorAtEnd("")
        this.expanded.value = true
    }
}

val PaneBackground = Color(0, 0, 0, 0x60)

@Composable
fun SelectorPane(
    viewer: WorldViewerModel
) {
}

@Composable
fun CoordinateLocatorPane(viewer: WorldViewerModel) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val textField = Modifier
            .fillMaxWidth()
            .focusProperties {
                canFocus = viewer.locatorPaneState.pagerState.currentPage == 0
            }
        val invalidX = remember { mutableStateOf(false) }
        val invalidZ = remember { mutableStateOf(false) }
        val commit: () -> Unit = {
            viewer.locatorPaneState.expanded.value = false
            viewer.viewModelScope.launch {
                val state = viewer.locatorPaneState
                val x = state.coordinateX.text.trim().toString().toDoubleOrNull()
                    ?: return@launch
                val z = state.coordinateZ.text.trim().toString().toDoubleOrNull()
                    ?: return@launch
                // TODO: fallback to current coordinate
                viewer.pendingMovement.send(Camera(x, z))
            }
        }
        CoordinateField(
            viewer.locatorPaneState.coordinateX,
            "X",
            ImeAction.Next,
            invalidX,
            textField
        ) {}
        CoordinateField(
            viewer.locatorPaneState.coordinateZ,
            "Z",
            ImeAction.Go,
            invalidZ,
            textField,
            commit
        )
        TextButton(
            onClick = commit,
            modifier = Modifier.align(Alignment.End),
            enabled = !invalidX.value && !invalidZ.value
        ) {
            Text(text = stringResource(R.string.go_loud))
        }
    }
}

@Composable
fun PlayerLocatorPane(viewer: WorldViewerModel, handle: WorldModel) {
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        if (viewer.players === null) {
            isRefreshing = true
            viewer.loadPlayers(handle.world).join()
            isRefreshing = false
        }
    }
    PullToRefreshBox(
        state = rememberPullToRefreshState(),
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            coroutineScope.launch {
                viewer.loadPlayers(handle.world).join()
                context.toast(R.string.general_done)
                isRefreshing = false
            }
        }
    ) {
        Box(Modifier.fillMaxSize()) {
            val players = viewer.players
            if (players !== null) {
                if (players.isEmpty()) {
                    Text(
                        stringResource(R.string.failed_find_player),
                        Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .align(Alignment.TopCenter)
                    )
                }
                LazyColumn(Modifier.fillMaxSize()) {
                    items(players) {
                        ListItem(
                            onClick = {
                                coroutineScope.launch {
                                    val pos = it.position
                                    viewer.pendingMovement.send(
                                        Camera(
                                            pos.x.toDouble(),
                                            pos.z.toDouble()
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.animateItem(),
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent
                            ),
                            supportingContent = { Text(it.getPositionDescription(context)) }
                        ) {
                            Text(it.dbName)
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
fun MarkerLocatorPane(viewer: WorldViewerModel, handle: WorldModel) {
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        if (viewer.markers === null) {
            isRefreshing = true
            viewer.loadMarkers(handle.world).join()
            isRefreshing = false
        }
    }
    PullToRefreshBox(
        state = rememberPullToRefreshState(),
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            coroutineScope.launch {
                viewer.loadMarkers(handle.world).join()
                // context.toast(R.string.general_done)
                isRefreshing = false
            }
        }
    ) {
        Box(Modifier.fillMaxSize()) {
            val markers = viewer.markers
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
                            onClick = {
                                coroutineScope.launch {
                                    viewer.pendingMovement.send(
                                        Camera(
                                            it.x.toDouble(),
                                            it.z.toDouble()
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.animateItem(),
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent
                            ),
                            supportingContent = { Text(it.getPositionDescription(context)) }
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
fun LocatorPane(
    viewer: WorldViewerModel,
    handle: WorldModel
) {
    val pagerState = viewer.locatorPaneState.pagerState
    val coroutineScope = rememberCoroutineScope()
    PrimaryTabRow(
        selectedTabIndex = pagerState.currentPage,
        modifier = Modifier.fillMaxWidth(),
        containerColor = Color.Transparent
    ) {
        intArrayOf(
            R.string.locator_page_coor,
            R.string.locator_page_player,
            R.string.locator_page_marker
        ).forEachIndexed { index, title ->
            Tab(
                selected = pagerState.currentPage == index,
                onClick = {
                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                },
                text = { Text(stringResource(title)) }
            )
        }
    }
    HorizontalPager(pagerState, Modifier.fillMaxSize()) { page ->
        when (page) {
            0 -> CoordinateLocatorPane(viewer)
            1 -> PlayerLocatorPane(viewer, handle)
            2 -> MarkerLocatorPane(viewer, handle)
        }
    }
}

@Composable
fun PaneHeader(
    @StringRes title: Int,
    expanded: MutableState<Boolean>,
    onDismissRequest: () -> Unit
) {
    Row(
        Modifier
            .clip(MaterialTheme.shapes.medium)
            .background(PaneBackground)
            .toggleable(expanded.value) { expanded.value = it }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(title), Modifier.weight(1F))
        IconButton(Icons.Filled.Close, onClick = onDismissRequest)
        AnimatedExpanderIndicator(expanded.value)
    }
}

@Composable
fun FloatingPanes(viewer: WorldViewerModel, handle: WorldModel) {
    AnimatedContent(targetState = viewer.paneType) { type ->
        when (type) {
            PaneType.NONE -> Spacer(Modifier.fillMaxWidth())
            PaneType.SELECTOR -> PaneLayout(
                title = R.string.gps_advanced_locator,
                expanded = viewer.selectorPaneState.expanded,
                onDismissRequest = { viewer.paneType = PaneType.NONE }
            ) {
                SelectorPane(viewer)
            }

            PaneType.LOCATOR -> PaneLayout(
                title = R.string.gps_advanced_locator,
                expanded = viewer.locatorPaneState.expanded,
                onDismissRequest = { viewer.paneType = PaneType.NONE }
            ) {
                LocatorPane(viewer, handle)
            }
        }
    }
}

@Composable
fun PaneLayout(
    expanded: MutableState<Boolean>,
    @StringRes title: Int,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    BlocktopographCompatTheme(darkTheme = true) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        WindowInsets(
                            horizontal = dimensionResource(R.dimen.map_float_page_margin_h),
                            vertical = dimensionResource(R.dimen.map_float_page_margin_v)
                        ).union(safeLayoutInsets())
                    )
                    .padding(4.dp)
            ) {
                PaneHeader(title, expanded, onDismissRequest)
                AnimatedVisibility(
                    visible = expanded.value,
                    modifier = Modifier.clip(MaterialTheme.shapes.medium)
                ) {
                    Column(
                        Modifier
                            .padding(top = 4.dp)
                            .fillMaxWidth()
                            .fillMaxHeight(0.5F)
                            .clip(MaterialTheme.shapes.medium)
                            .background(PaneBackground)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
fun LocatorPaneTab(pagerState: PagerState, index: Int) {

}

@Composable
fun CoordinateField(
    textFieldState: TextFieldState,
    label: String,
    imeAction: ImeAction,
    isError: MutableState<Boolean>,
    modifier: Modifier = Modifier,
    onKeyboardAction: () -> Unit
) {
    LaunchedEffect(Unit) {
        snapshotFlow { textFieldState.text }.collect {
            val input = it.trim()
            isError.value = it.isNotEmpty() && input.toString().toDoubleOrNull() === null
        }
    }
    OutlinedTextField(
        state = textFieldState,
        modifier = modifier,
        shape = OutlinedTextFieldDefaults.roundedShape,
        lineLimits = TextFieldLineLimits.SingleLine,
        isError = isError.value,
        labelPosition = TextFieldLabelPosition.Cutout(isAlwaysMinimized = true),
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.DecimalSigned,
            imeAction = imeAction
        ),
        inputTransformation = InputTransformation.then {
            if (!this.asCharSequence().isDecimal()) {
                this.revertAllChanges()
            }
        },
        onKeyboardAction = {
            val input = textFieldState.text.trim()
            if (input.isNotEmpty() && input.toString().toDoubleOrNull() === null) {
                isError.value = true
            } else {
                it()
                onKeyboardAction()
            }
        }
    )
}
