package com.mithrilmania.blocktopograph.editor.world

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalGridApi
import androidx.compose.foundation.layout.Grid
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.block.BlockTemplates
import com.mithrilmania.blocktopograph.map.Biome
import com.mithrilmania.blocktopograph.map.edit.ChBiomeEdit
import com.mithrilmania.blocktopograph.map.edit.DchunkEdit
import com.mithrilmania.blocktopograph.map.edit.EditFunction
import com.mithrilmania.blocktopograph.map.edit.EditResultCode
import com.mithrilmania.blocktopograph.map.edit.RectEditTarget
import com.mithrilmania.blocktopograph.map.edit.SearchAndReplaceRequest
import com.mithrilmania.blocktopograph.map.edit.SnrConfig
import com.mithrilmania.blocktopograph.map.edit.SnrConfig.SearchConditionBlock
import com.mithrilmania.blocktopograph.map.edit.perform
import com.mithrilmania.blocktopograph.map.picer.PicerState
import com.mithrilmania.blocktopograph.ui.component.AnimatedExpanderIndicator
import com.mithrilmania.blocktopograph.ui.component.DropdownMenuField
import com.mithrilmania.blocktopograph.ui.component.IconButton
import com.mithrilmania.blocktopograph.ui.component.TextButton
import com.mithrilmania.blocktopograph.ui.component.WindowInsets
import com.mithrilmania.blocktopograph.ui.component.safeLayoutInsets
import com.mithrilmania.blocktopograph.ui.theme.BlocktopographCompatTheme
import com.mithrilmania.blocktopograph.util.endsWithDigits
import com.mithrilmania.blocktopograph.util.isDecimal
import com.mithrilmania.blocktopograph.util.isNumber
import com.mithrilmania.blocktopograph.util.toast
import com.mithrilmania.blocktopograph.world.WorldModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.Camera

enum class PaneType {
    NONE,
    SELECTOR,
    LOCATOR
}

class SelectorPaneState {
    val expanded = mutableStateOf(false)
    val startX = TextFieldState()
    val startZ = TextFieldState()
    val spanX = TextFieldState()
    val spanZ = TextFieldState()

    fun reset() {
        this.expanded.value = false
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

@OptIn(ExperimentalGridApi::class)
@Composable
fun SelectorPane(
    viewer: WorldViewerModel,
    handle: WorldModel
) {
    Column(
        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Grid(
            config = {
                column(0.5F)
                column(0.5F)
                gap(4.dp)
            }
        ) {//TODO auto submit
            val selection = viewer.selection
            val state = viewer.selectorPaneState
            val size = Modifier.heightIn(min = 48.dp)
            val padding = PaddingValues(top = 4.dp, bottom = 4.dp, start = 12.dp, end = 12.dp)
            SelectorCoordinateField(state.startX, "Start X", padding, size) {
                selection.right += it - selection.left
                selection.left = it
                true
            }
            SelectorCoordinateField(state.startZ, "Start Z", padding, size) {
                selection.bottom += it - selection.top
                selection.top = it
                true
            }
            SelectorSpanField(state.spanX, "Span X", padding, size) {
                if (it > 0) {
                    selection.right = selection.left + it
                    true
                } else false
            }
            SelectorSpanField(state.spanZ, "Span Z", padding, size) {
                if (it > 0) {
                    selection.bottom = selection.top + it
                    true
                } else false
            }
            LaunchedEffect(selection.left, selection.right) {
                state.startX.setTextAndPlaceCursorAtEnd(selection.left.toString())
                state.spanX.setTextAndPlaceCursorAtEnd((selection.right - selection.left).toString())
            }
            LaunchedEffect(selection.top, selection.bottom) {
                state.startZ.setTextAndPlaceCursorAtEnd(selection.top.toString())
                state.spanZ.setTextAndPlaceCursorAtEnd((selection.bottom - selection.top).toString())
            }
        }
        var edit by rememberSaveable { mutableStateOf<EditFunction?>(null) }
        val coroutineScope = rememberCoroutineScope()
        val actions = remember {
            arrayOf(
                R.string.map_edit_func_lampshade to {
                    edit = EditFunction.LAMPSHADE
                },
                R.string.map_edit_func_snr to {
                    // TODO set edit
                    viewer.replacingRequest = SearchAndReplaceRequest { config ->
                        viewer.waitForJob(
                            coroutineScope.launch(Dispatchers.IO) {
                                val code = config.perform(
                                    RectEditTarget(
                                        handle.world.storage ?: return@launch,
                                        viewer.selection.toIntRect(),
                                        viewer.dimension
                                    )
                                )
                                viewer.editResult.emit(code)
                            }
                        )
                    }
                },
                R.string.map_edit_func_dchunk to {
                    edit = EditFunction.DCHUNK
                },
                R.string.map_edit_func_chbiome to {
                    edit = EditFunction.CHBIOME
                },
                R.string.map_edit_func_picer to {
                    viewer.commitAnalyzedState(
                        viewer.selection.toIntRect(),
                        PicerState.SelectionOutOfSize
                    )
                }
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                stringResource(R.string.map_sel_menu_op),
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                stringResource(
                    R.string.map_sel_to,
                    viewer.selection.right - 1,
                    viewer.selection.bottom - 1
                ),
                style = MaterialTheme.typography.labelSmall
            )
        }
        Grid(
            config = {
                column(0.5F)
                column(0.5F)
                gap(4.dp)
            },
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .weight(1.0F)
        ) {
            actions.forEach {
                OutlinedButton(
                    onClick = it.second,
                    modifier = Modifier.fillMaxWidth(),
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(stringResource(it.first))
                }
            }
        }
        Text(
            stringResource(R.string.map_sel_warn_backup),
            style = MaterialTheme.typography.labelSmall
        )
        BlocktopographCompatTheme {
            val onDismissRequest = { edit = null }
            when (edit) {
                EditFunction.LAMPSHADE -> {
                    AlertDialog(
                        onDismissRequest = onDismissRequest,
                        title = {
                            Text(stringResource(R.string.map_edit_func_lampshade))
                        },
                        text = {
                            Row(
                                modifier = Modifier.height(IntrinsicSize.Min),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painterResource(R.drawable.lampshade),
                                    null,
                                    Modifier
                                        .fillMaxHeight()
                                        .weight(1F)
                                )
                                Text(
                                    stringResource(R.string.map_edit_lampshade_note),
                                    Modifier
                                        .padding(start = 8.dp)
                                        .weight(3F)
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(
                                stringResource(android.R.string.cancel),
                                onClick = onDismissRequest
                            )
                        },
                        confirmButton = {
                            TextButton(stringResource(android.R.string.ok)) {
                                edit = null
                                viewer.waitForJob(
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val cfg = SnrConfig()
                                        cfg.searchMode = 2
                                        cfg.placeMode = 1
                                        cfg.searchBlockMain = SearchConditionBlock(
                                            BlockTemplates.getOfType("minecraft:torch")[0].block,
                                            true,
                                            true
                                        )
                                        cfg.placeOldBlockMain =
                                            BlockTemplates.getOfType("minecraft:glass")[0].block
                                        cfg.ignoreSubId = true
                                        viewer.editResult.emit(
                                            cfg.perform(
                                                RectEditTarget(
                                                    handle.world.storage ?: return@launch,
                                                    viewer.selection.toIntRect(),
                                                    viewer.dimension
                                                )
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    )
                }

                EditFunction.DCHUNK -> {
                    AlertDialog(
                        onDismissRequest = onDismissRequest,
                        title = {
                            Text(stringResource(R.string.map_edit_func_dchunk))
                        },
                        text = {
                            Column {
                                Text(stringResource(R.string.map_edit_dchunk_explain))
                                if (!viewer.selection.isChunkAligned) {
                                    Text(
                                        stringResource(R.string.map_edit_dchunk_warn_not_aligned),
                                        Modifier.padding(top = with(LocalDensity.current) {
                                            LocalTextStyle.current.lineHeight.toDp()
                                        })
                                    )
                                }
                            }
                        },
                        dismissButton = {
                            TextButton(
                                stringResource(android.R.string.cancel),
                                onClick = onDismissRequest
                            )
                        },
                        confirmButton = {
                            TextButton(
                                stringResource(
                                    if (viewer.selection.isChunkAligned) android.R.string.ok
                                    else R.string.map_edit_dchunk_posbtn_with_auto_adjust
                                )
                            ) {
                                edit = null
                                viewer.waitForJob(
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val area = RectEditTarget(
                                            handle.world.storage ?: return@launch,
                                            viewer.selection.toIntRect(),
                                            viewer.dimension
                                        )
                                        area.setMaxError(Int.MAX_VALUE)
                                        area.forEachChunk(DchunkEdit())
                                        viewer.editResult.emit(EditResultCode.SUCCESS)
                                    }
                                )
                            }
                        }
                    )
                }

                EditFunction.CHBIOME -> {
                    val optionalBiomes = remember {
                        val list = arrayListOf<Biome?>(null)
                        list.addAll(Biome.entries)
                        list
                    }
                    var toReplaceWith by remember { mutableStateOf(Biome.PLAINS) }
                    var toBeReplaced by remember { mutableStateOf<Biome?>(null) }
                    AlertDialog(
                        onDismissRequest = onDismissRequest,
                        title = {
                            Text(stringResource(R.string.map_edit_func_chbiome))
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                DropdownMenuField(
                                    options = optionalBiomes,
                                    label = stringResource(R.string.map_edit_chbiome_for),
                                    selected = toBeReplaced,
                                    modifier = Modifier.fillMaxWidth(),
                                    onSelect = { toBeReplaced = it }
                                ) {
                                    if (it === null) {
                                        stringResource(R.string.map_edit_chbiome_all)
                                    } else {
                                        "${it.name} (${it.id})"
                                    }
                                }
                                DropdownMenuField(
                                    options = Biome.entries,
                                    label = stringResource(R.string.map_edit_chbiome_to),
                                    selected = toReplaceWith,
                                    modifier = Modifier.fillMaxWidth(),
                                    onSelect = { toReplaceWith = it }
                                ) {
                                    "${it.name} (${it.id})"
                                }
                            }
                        },
                        dismissButton = {
                            TextButton(
                                stringResource(android.R.string.cancel),
                                onClick = onDismissRequest
                            )
                        },
                        confirmButton = {
                            TextButton(stringResource(android.R.string.ok)) {
                                edit = null
                                val area = RectEditTarget(
                                    handle.world.storage ?: return@TextButton,
                                    viewer.selection.toIntRect(),
                                    viewer.dimension
                                )
                                viewer.waitForJob(
                                    coroutineScope.launch(Dispatchers.IO) {
                                        area.setMaxError(Int.MAX_VALUE)
                                        area.forEachXz(ChBiomeEdit(toBeReplaced, toReplaceWith))
                                        viewer.editResult.emit(EditResultCode.SUCCESS)
                                    }
                                )
                            }
                        }
                    )
                }

                else -> {}
            }
        }
    }
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
        LocatorCoordinateField(
            viewer.locatorPaneState.coordinateX,
            "X",
            ImeAction.Next,
            invalidX,
            textField
        ) {}
        LocatorCoordinateField(
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
            PaneType.SELECTOR -> PaneLayout(
                title = R.string.map_sel_panel_title,
                expanded = viewer.selectorPaneState.expanded,
                onDismissRequest = { viewer.paneType = PaneType.NONE }
            ) {
                SelectorPane(viewer, handle)
            }

            PaneType.LOCATOR -> PaneLayout(
                title = R.string.gps_advanced_locator,
                expanded = viewer.locatorPaneState.expanded,
                onDismissRequest = { viewer.paneType = PaneType.NONE }
            ) {
                LocatorPane(viewer, handle)
            }

            else -> Spacer(Modifier.fillMaxWidth())
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

class SelectorFieldState(
    @JvmField val textFieldState: TextFieldState,
    private val onKeyboardAction: (Int) -> Boolean
) : KeyboardActionHandler {
    var isError by mutableStateOf(false)
    override fun onKeyboardAction(performDefaultAction: () -> Unit) {
        val input = textFieldState.text.trim().toString().toIntOrNull()
        if (input !== null && onKeyboardAction(input)) {
            performDefaultAction()
        } else {
            isError = true
        }
    }
}

@Composable
fun SelectorCoordinateField(
    textFieldState: TextFieldState,
    label: String,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onKeyboardAction: (Int) -> Boolean
) {
    val state = remember(textFieldState, onKeyboardAction) {
        SelectorFieldState(textFieldState, onKeyboardAction)
    }
    LaunchedEffect(Unit) {
        snapshotFlow { textFieldState.text }.collect {
            state.isError = it.trim().toString().toIntOrNull() === null
        }
    }
    SelectorField(state, label, contentPadding, KeyboardType.NumberSigned, modifier) {
        if (!this.asCharSequence().isNumber()) {
            this.revertAllChanges()
        }
    }
}

@Composable
fun SelectorSpanField(
    textFieldState: TextFieldState,
    label: String,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onKeyboardAction: (Int) -> Boolean
) {
    val state = remember(textFieldState, onKeyboardAction) {
        SelectorFieldState(textFieldState, onKeyboardAction)
    }
    LaunchedEffect(Unit) {
        snapshotFlow { textFieldState.text }.collect {
            val input = it.trim().toString().toIntOrNull()
            state.isError = input === null || input <= 0
        }
    }
    SelectorField(state, label, contentPadding, KeyboardType.Number, modifier) {
        if (!this.asCharSequence().endsWithDigits()) {
            this.revertAllChanges()
        }
    }
}

@Composable
fun SelectorField(
    state: SelectorFieldState,
    label: String,
    contentPadding: PaddingValues,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    inputTransformation: InputTransformation
) {
    val interactionSource = remember { MutableInteractionSource() }
    OutlinedTextField(
        state = state.textFieldState,
        modifier = modifier.fillMaxWidth(),
        shape = OutlinedTextFieldDefaults.roundedShape,
        lineLimits = TextFieldLineLimits.SingleLine,
        labelPosition = TextFieldLabelPosition.Cutout(isAlwaysMinimized = true),
        label = { Text(label) },
        isError = state.isError,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Next
        ),
        inputTransformation = inputTransformation,
        trailingIcon = {
            if (interactionSource.collectIsFocusedAsState().value) {
                IconButton(Icons.Filled.Check, enabled = !state.isError) {
                    state.onKeyboardAction {}
                }
            }
        },
        contentPadding = contentPadding,
        onKeyboardAction = state,
        interactionSource = interactionSource
    )
}

@Composable
fun LocatorCoordinateField(
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
        inputTransformation = {
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
