package com.mithrilmania.blocktopograph.editor.world

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.then
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.text.isDigitsOnly
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mithrilmania.blocktopograph.MIME_TYPE_DEFAULT
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.map.Biome
import com.mithrilmania.blocktopograph.nbt.CompoundTag
import com.mithrilmania.blocktopograph.nbt.LongTag
import com.mithrilmania.blocktopograph.nbt.StringTag
import com.mithrilmania.blocktopograph.nbt.io.BedrockNBTInput
import com.mithrilmania.blocktopograph.nbt.io.readBinaryTag
import com.mithrilmania.blocktopograph.nbt.io.writeNBTWithHeader
import com.mithrilmania.blocktopograph.ui.BlockStatePreview
import com.mithrilmania.blocktopograph.ui.PickBlockDialog
import com.mithrilmania.blocktopograph.ui.component.AnimatedBottomSheetDialog
import com.mithrilmania.blocktopograph.ui.component.AppBarNavigationButton
import com.mithrilmania.blocktopograph.ui.component.BottomSheetActionButton
import com.mithrilmania.blocktopograph.ui.component.DropdownMenuField
import com.mithrilmania.blocktopograph.ui.component.HorizontalPadding
import com.mithrilmania.blocktopograph.ui.component.IconButton
import com.mithrilmania.blocktopograph.ui.component.InfoBar
import com.mithrilmania.blocktopograph.ui.component.TooltipBox
import com.mithrilmania.blocktopograph.ui.component.applyInfoBarPadding
import com.mithrilmania.blocktopograph.ui.component.showSnackbar
import com.mithrilmania.blocktopograph.ui.theme.setThemedContent
import com.mithrilmania.blocktopograph.world.FILE_LEVEL_DAT
import com.mithrilmania.blocktopograph.world.KEY_FLAT_WORLD_LAYERS
import com.mithrilmania.blocktopograph.world.KEY_LAST_PLAYED_TIME
import com.mithrilmania.blocktopograph.world.KEY_LEVEL_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

class CreateWorldActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        this.setThemedContent {
            val viewModel = viewModel<CreateWorldModel>()
            val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    TopAppBar(
                        scrollBehavior = scrollBehavior,
                        title = {
                            Text(stringResource(R.string.create_world_title))
                        },
                        navigationIcon = ::AppBarNavigationButton,
                        actions = {
                            val tooltipState = rememberTooltipState()
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                    TooltipAnchorPosition.Above
                                ),
                                tooltip = {
                                    PlainTooltip { Text(stringResource(R.string.edit_flat_help)) }
                                },
                                state = tooltipState
                            ) {
                                val scope = rememberCoroutineScope()
                                IconButton(
                                    Icons.AutoMirrored.Filled.Help,
                                    stringResource(R.string.action_help)
                                ) {
                                    scope.launch { tooltipState.show(MutatePriority.UserInput) }
                                }
                            }

                        }
                    )
                },
                snackbarHost = { SnackbarHost(viewModel.snackbar) },
                floatingActionButton = {
                    TooltipBox(stringResource(R.string.create)) { tooltip ->
                        val picker = rememberLauncherForActivityResult(
                            ActivityResultContracts.OpenDocumentTree()
                        ) { folder ->
                            if (folder == null) return@rememberLauncherForActivityResult
                            val activity = this
                            viewModel.viewModelScope.launch(Dispatchers.IO) {
                                val folder = DocumentFile.fromTreeUri(activity, folder)
                                    ?: return@launch
                                val config = folder.createFile(MIME_TYPE_DEFAULT, FILE_LEVEL_DAT)
                                    ?: return@launch
                                val data = try {
                                    BedrockNBTInput(
                                        activity.assets.open("dats/1_2_13.dat").buffered()
                                    ).use {
                                        it.skipBytes(8)
                                        it.readBinaryTag() as? CompoundTag
                                    }
                                } catch (_: Exception) {
                                    null
                                } ?: return@launch
                                data[KEY_LEVEL_NAME] = StringTag(
                                    viewModel.name.text.ifBlank {
                                        activity.getString(R.string.world_default_name)
                                    }.toString()
                                )
                                data[KEY_LAST_PLAYED_TIME] = LongTag(
                                    System.currentTimeMillis() / 1000
                                )
                                var layers: MutableList<Layer> = viewModel.layers
                                if (layers.size < 3) {
                                    layers = layers.toMutableList()
                                    repeat(3 - layers.size) {
                                        layers.add(Layer(height = 0))
                                    }
                                }
                                layers.toJson(viewModel.biome)?.let {
                                    data[KEY_FLAT_WORLD_LAYERS] = StringTag(it)
                                }
                                (activity.contentResolver.openOutputStream(config.uri)
                                    ?: return@launch)
                                    .writeNBTWithHeader(4U, "Blocktopograph", data)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(activity, "Done!", Toast.LENGTH_SHORT).show()
                                    activity.setResult(RESULT_OK, Intent().setData(folder.uri))
                                    activity.finish()
                                }
                            }
                        }
                        FloatingActionButton(
                            onClick = { picker.launch(null) },
                            modifier = Modifier.padding(end = 16.dp, bottom = 16.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, tooltip)
                        }
                    }
                }
            ) { padding ->
                Column(Modifier.padding(PaddingValues(top = padding.calculateTopPadding()))) {
                    val inset = HorizontalPadding(padding)
                    val spacing = Modifier.padding(
                        inset + PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp)
                    )
                    OutlinedTextField(
                        state = viewModel.name,
                        modifier = spacing.fillMaxWidth(),
                        lineLimits = TextFieldLineLimits.SingleLine,
                        label = { Text(stringResource(R.string.create_world_name)) },
                        labelPosition = TextFieldLabelPosition.Attached(alwaysMinimize = true),
                        placeholder = { Text(stringResource(R.string.world_default_name)) },
                    )
                    DropdownMenuField(
                        options = listOf(Unit),
                        label = stringResource(R.string.create_world_version),
                        selected = Unit,
                        modifier = spacing.fillMaxWidth(),
                        onSelect = { }
                    ) {
                        stringResource(R.string.create_world_version_aqua)
                    }
                    DropdownMenuField(
                        options = Biome.entries,
                        label = stringResource(R.string.biomes),
                        selected = viewModel.biome,
                        modifier = spacing.fillMaxWidth(),
                        onSelect = { viewModel.biome = it }
                    ) { "${it.name} (${it.id})" }
                    val hapticFeedback = LocalHapticFeedback.current
                    val listState = rememberLazyListState()
                    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
                        viewModel.layers.apply {
                            add(to.index, removeAt(from.index))
                        }
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                    }
                    Row(
                        modifier = spacing,
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.create_world_layers),
                            style = MaterialTheme.typography.labelMedium
                        )
                        FilledTonalButton(
                            onClick = {
                                viewModel.snackbar.currentSnackbarData?.dismiss()
                                viewModel.layers.add(0, Layer())
                            },
                            shapes = ButtonDefaults.shapes(),
                            contentPadding = ButtonDefaults.contentPaddingFor(
                                ButtonDefaults.MinHeight,
                                hasStartIcon = true
                            )
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "添加层",
                                modifier = Modifier.size(ButtonDefaults.iconSizeFor(ButtonDefaults.MinHeight)),
                            )
                            Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(ButtonDefaults.MinHeight)))
                            Text("添加层")
                        }
                    }
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = padding.calculateBottomPadding()),
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            count = viewModel.layers.size,
                            key = { viewModel.layers[it].uid }
                        ) { index ->
                            val layer = viewModel.layers[index]
                            ReorderableItem(reorderableState, layer.uid) { _ ->
                                val dismissState = rememberSwipeToDismissBoxState()
                                SwipeToDismissBox(
                                    state = dismissState,
                                    enableDismissFromStartToEnd = false,
                                    backgroundContent = {
                                        val color by animateColorAsState(
                                            if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                                                MaterialTheme.colorScheme.errorContainer
                                            } else {
                                                MaterialTheme.colorScheme.surface
                                            }
                                        )
                                        Spacer(
                                            Modifier
                                                .fillMaxSize()
                                                .background(color)
                                        )
                                    },
                                    onDismiss = { direction ->
                                        if (direction == SwipeToDismissBoxValue.EndToStart) {
                                            viewModel.snackbar.currentSnackbarData?.dismiss()
                                            viewModel.layers.removeAt(index)
                                            scope.launch {
                                                viewModel.snackbar.showSnackbar(
                                                    message = "已删除",
                                                    actionLabel = "撤销",
                                                    duration = SnackbarDuration.Long
                                                ) {
                                                    viewModel.layers.add(index, layer.copy())
                                                }
                                            }
                                        } else {
                                            scope.launch { dismissState.reset() }
                                        }
                                    },
                                ) {
                                    val state = layer.state
                                    val handle = Modifier.draggableHandle(
                                        onDragStarted = {
                                            hapticFeedback.performHapticFeedback(
                                                HapticFeedbackType.GestureThresholdActivate
                                            )
                                        },
                                        onDragStopped = {
                                            hapticFeedback.performHapticFeedback(
                                                HapticFeedbackType.GestureEnd
                                            )
                                        }
                                    )
                                    InfoBar(
                                        title = state.block.name,
                                        description = "${state.block.name} ×${layer.height}",
                                        icon = {
                                            val modifier = handle.size(32.dp)
                                            val icon = state.icon.getIcon(context)
                                            if (icon === null) {
                                                Spacer(modifier)
                                            } else {
                                                Icon(
                                                    bitmap = icon.asImageBitmap(),
                                                    contentDescription = null,
                                                    modifier = modifier,
                                                    tint = Color.Unspecified
                                                )
                                            }
                                        },
                                        modifier = Modifier
                                            .clickable {
                                                viewModel.selected = layer
                                                viewModel.picked = null
                                            }
                                            .padding(inset)
                                            .applyInfoBarPadding()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.DragHandle,
                                            contentDescription = null,
                                            modifier = handle
                                        )
                                    }
                                }
                            }
                        }
                    }
                    AnimatedBottomSheetDialog(viewModel.selected) { sheetState, selected ->
                        var picking by rememberSaveable { mutableStateOf(false) }
                        ModalBottomSheet(
                            onDismissRequest = { viewModel.selected = null },
                            sheetState = sheetState
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val textFieldState =
                                    rememberTextFieldState(selected.height.toString())
                                BlockStatePreview(
                                    state = viewModel.picked ?: selected.state,
                                    context = context,
                                    shape = MaterialTheme.shapes.extraSmall // to match with text field
                                ) {
                                    picking = true
                                }
                                OutlinedTextField(
                                    state = textFieldState,
                                    lineLimits = TextFieldLineLimits.SingleLine,
                                    label = { Text(stringResource(R.string.edit_layer_amount)) },
                                    labelPosition = TextFieldLabelPosition.Attached(alwaysMinimize = true),
                                    placeholder = { Text(selected.height.toString()) },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    inputTransformation = InputTransformation.then {
                                        if (!this.asCharSequence().isDigitsOnly()) {
                                            revertAllChanges()
                                        }
                                    }
                                )
                                BottomSheetActionButton(
                                    text = stringResource(android.R.string.ok),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    viewModel.picked?.let { selected.state = it }
                                    textFieldState.text.toString().toIntOrNull()?.let {
                                        selected.height = it
                                    }
                                    viewModel.selected = null
                                }
                            }
                        }
                        if (picking) {
                            PickBlockDialog(onCancel = { picking = false }) {
                                viewModel.picked = it
                                picking = false
                            }
                        }
                    }
                }
            }
        }
    }
}