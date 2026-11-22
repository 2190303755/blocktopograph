package com.mithrilmania.blocktopograph.ui.screen.world

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.collection.IntIntPair
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Grid
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorPosition
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.lifecycle.viewModelScope
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.block.BlockTemplate
import com.mithrilmania.blocktopograph.block.BlockTemplates
import com.mithrilmania.blocktopograph.editor.world.SelectorCoordinateField
import com.mithrilmania.blocktopograph.editor.world.SelectorSpanField
import com.mithrilmania.blocktopograph.map.Biome
import com.mithrilmania.blocktopograph.map.edit.BlockStates
import com.mithrilmania.blocktopograph.map.edit.BlockStatesPreview
import com.mithrilmania.blocktopograph.map.edit.EditResultCode
import com.mithrilmania.blocktopograph.map.edit.PlaceMode
import com.mithrilmania.blocktopograph.map.edit.SearchMode
import com.mithrilmania.blocktopograph.map.picer.PICER_MAX_AREA
import com.mithrilmania.blocktopograph.map.picer.PICER_MAX_LENGTH
import com.mithrilmania.blocktopograph.map.picer.PICER_MAX_SCALE
import com.mithrilmania.blocktopograph.ui.component.DropdownMenuField
import com.mithrilmania.blocktopograph.ui.component.DropdownMenuItem
import com.mithrilmania.blocktopograph.ui.component.IconButton
import com.mithrilmania.blocktopograph.ui.component.InfoBox
import com.mithrilmania.blocktopograph.ui.component.TextButton
import com.mithrilmania.blocktopograph.ui.component.awaitSnackbar
import com.mithrilmania.blocktopograph.ui.screen.world.layer.renderSatellite
import com.mithrilmania.blocktopograph.util.ConvertUtil
import com.mithrilmania.blocktopograph.util.isDifferentState
import com.mithrilmania.blocktopograph.util.toast
import com.mithrilmania.blocktopograph.util.todoI18n
import com.mithrilmania.blocktopograph.world.access.resolvePlainName
import com.mithrilmania.blocktopograph.world.chunk.ChunkPos
import com.mithrilmania.blocktopograph.world.chunk.SubChunk
import com.mithrilmania.blocktopograph.world.edit.SelectionEditTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.IOException
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sqrt

private class SelectionOperation(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

private enum class SelectionPanelOp {
    DCHUNK,
    CHBIOME,
    LAMPSHADE
}

/**
 * Selection-local search-and-replace state. Unlike the legacy `SearchAndReplaceRequest` it never
 * materialises a `SnrConfig`; the edit below consumes these Kotlin types directly.
 */
private class SelectionSnrRequest {
    var searchMode by mutableStateOf(SearchMode.FOREGROUND)
    var placeMode by mutableStateOf(PlaceMode.FOREGROUND)
    val search = BlockStates("minecraft:grass", "minecraft:air")
    val place = BlockStates("minecraft:grass", "minecraft:water")
}

/** Matches a block against [exemplar] by type id, block states and custom properties. */
private fun BlockTemplate.matches(exemplar: BlockTemplate): Boolean {
    val pattern = exemplar.block
    val candidate = this.block
    return pattern.typeId == candidate.typeId
            && !pattern.isDifferentState(candidate)
            && pattern.customProperties.all { (key, tag) ->
        tag == candidate.customProperties[key]
    }
}

private fun editSnrAt(
    subChunk: SubChunk,
    x: Int,
    y: Int,
    z: Int,
    search: BlockStates,
    searchMode: SearchMode,
    place: BlockStates,
    placeMode: PlaceMode
): Int {
    return try {
        val major = subChunk.getBlock(x, y, z)
        val minor = subChunk.getBlock(x, y, z, 1)
        val matched = when (searchMode) {
            SearchMode.BACKGROUND -> minor.matches(search.major)
            SearchMode.FOREGROUND -> major.matches(search.major)
            SearchMode.ANY -> major.matches(search.major) || minor.matches(search.major)
            SearchMode.BOTH -> major.matches(search.major) && minor.matches(search.minor)
        }
        if (matched) {
            val main = BlockTemplates.getBest(place.major.block)
            when (placeMode) {
                PlaceMode.BACKGROUND -> subChunk.setBlock(x, y, z, main, 1)
                PlaceMode.FOREGROUND -> subChunk.setBlock(x, y, z, main, 0)
                PlaceMode.BOTH -> {
                    subChunk.setBlock(x, y, z, main, 0)
                    subChunk.setBlock(x, y, z, BlockTemplates.getBest(place.minor.block), 1)
                }
            }
        }
        0
    } catch (_: Exception) {
        1
    }
}

/** Replaces every block matching [torch] with [glass] on the foreground layer. */
private fun editLampshadeAt(
    subChunk: SubChunk,
    x: Int,
    y: Int,
    z: Int,
    torch: BlockTemplate,
    glass: BlockTemplate
): Int {
    return try {
        if (subChunk.getBlock(x, y, z).matches(torch)) {
            subChunk.setBlock(x, y, z, glass, 1)
        }
        0
    } catch (_: Exception) {
        1
    }
}

@Composable
fun SelectionAction(editor: WorldEditorModel) {
    val scope = rememberCoroutineScope()
    val selection = editor.selection

    var op by remember { mutableStateOf<SelectionPanelOp?>(null) }
    var snrRequest by remember { mutableStateOf<SelectionSnrRequest?>(null) }
    var picerOutOfSize by remember { mutableStateOf(false) }
    var generatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val resources = LocalResources.current
    // Reports the result of every selection operation through the editor snackbar.
    LaunchedEffect(resources) {
        editor.editResult.collect { code ->
            val message = resources.getString(
                if (code === EditResultCode.SUCCESS) R.string.general_done else R.string.general_failed
            )
            editor.scaffoldState.snackbarHostState.showSnackbar(message)
        }
    }

    /**
     * Builds a [SelectionEditTarget] over the current selection and runs the edit on the
     * IO dispatcher through [WorldEditorModel.launchEdit].
     */
    fun runEdit(block: suspend SelectionEditTarget.() -> EditResultCode) {
        val info = editor.initialization as? InitState.Succeed ?: return
        editor.launchEdit {
            SelectionEditTarget(
                info.chunks,
                editor.dimension,
                editor.selection.toIntRect()
            ).block()
        }
    }

    fun startPicer() {
        val width = selection.right - selection.left
        val height = selection.bottom - selection.top
        val tooLarge = width <= 0 || height <= 0
                || maxOf(width, height) > PICER_MAX_LENGTH
                || width.toLong() * height > PICER_MAX_AREA
        if (tooLarge) {
            picerOutOfSize = true
            return
        }
        val info = editor.initialization as? InitState.Succeed ?: return
        // Each chunk is rendered at 1 block per pixel first; the bitmap can be scaled later.
        val job = scope.launch(Dispatchers.IO) {
            val bitmap = createBitmap(width, height, Bitmap.Config.RGB_565)
            val chunkMinX = selection.left shr 4
            val chunkMaxX = (selection.right - 1) shr 4
            val chunkMinZ = selection.top shr 4
            val chunkMaxZ = (selection.bottom - 1) shr 4
            for (chunkX in chunkMinX..chunkMaxX) {
                for (chunkZ in chunkMinZ..chunkMaxZ) {
                    val pos = ChunkPos(editor.dimension.runtimeId, chunkX, chunkZ)
                    try {
                        val chunk = info.chunks.getAsync(pos).await() ?: continue
                        renderSatellite(
                            bitmap,
                            info.chunks,
                            chunk,
                            chunkX * 16 - selection.left,
                            chunkZ * 16 - selection.top
                        )
                    } catch (_: Exception) {
                        // Leave the map background for chunks that cannot be rendered yet.
                        continue
                    }
                }
            }
            withContext(Dispatchers.Main) {
                generatedBitmap = bitmap
            }
        }
        editor.waitForJob(job)
    }

    val operations = listOf(
        SelectionOperation(stringResource(R.string.map_edit_func_dchunk), Icons.Filled.Delete) {
            if (!editor.waitingJob) op = SelectionPanelOp.DCHUNK
        },
        SelectionOperation(stringResource(R.string.map_edit_func_chbiome), Icons.Filled.Landscape) {
            if (!editor.waitingJob) op = SelectionPanelOp.CHBIOME
        },
        SelectionOperation(stringResource(R.string.map_edit_func_snr), Icons.Filled.FindReplace) {
            if (editor.waitingJob) return@SelectionOperation
            snrRequest = SelectionSnrRequest()
        },
        SelectionOperation(
            stringResource(R.string.map_edit_func_lampshade),
            Icons.Filled.Lightbulb
        ) {
            if (!editor.waitingJob) op = SelectionPanelOp.LAMPSHADE
        },
        SelectionOperation(stringResource(R.string.map_edit_func_picer), Icons.Filled.Image) {
            if (!editor.waitingJob) startPicer()
        }
    )
    SegmentedListItem(
        shapes = ListItemDefaults.segmentedShapes(0, 1),
        leadingContent = {
            IconButton(Icons.Filled.Close) {
                editor.selection.isSelecting = false
            }
        },
        trailingContent = {
            var expended by rememberSaveable { mutableStateOf(false) }
            FilledTonalIconToggleButton(
                checked = expended,
                onCheckedChange = { expended = it },
                shapes = IconButtonDefaults.toggleableShapes()
            ) {
                Icon(Icons.Filled.Edit, null)
            }
            DropdownMenuPopup(
                expanded = expended,
                onDismissRequest = { expended = false },
                popupPositionProvider = MenuDefaults.rememberDropdownMenuPopupPositionProvider(
                    MenuAnchorPosition.Above
                )
            ) {
                DropdownMenuGroup(MenuDefaults.groupShape(0, 1)) {
                    operations.forEachIndexed { index, operation ->
                        DropdownMenuItem(
                            onClick = operation.onClick,
                            icon = operation.icon,
                            label = operation.title,
                            shape = MenuDefaults.itemShape(index, operations.size).shape
                        )
                    }
                }
            }
        },
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = ListItemDefaults.elevation(2.dp)
    ) { }

    when (op) {
        SelectionPanelOp.DCHUNK -> DeleteChunkDialog(
            aligned = selection.isChunkAligned,
            onDismiss = { op = null },
            onConfirm = {
                op = null
                runEdit {
                    setMaxError(Int.MAX_VALUE)
                    forEachChunk { chunk, _, _, _, _ ->
                        try {
                            chunk.delete()
                            0
                        } catch (_: Exception) {
                            1
                        }
                    }
                }
            }
        )

        SelectionPanelOp.CHBIOME -> ChangeBiomeDialog(
            onDismiss = { op = null },
            onConfirm = { from, to ->
                op = null
                runEdit {
                    setMaxError(Int.MAX_VALUE)
                    forEachXz { chunk, x, z ->
                        try {
                            if (from === null || chunk.getBiome(x, 0, z) == from.id) {
                                if (!chunk.setBiome(x, z, to.id)) 1 else 0
                            } else 0
                        } catch (_: Exception) {
                            1
                        }
                    }
                }
            }
        )

        SelectionPanelOp.LAMPSHADE -> LampshadeDialog(
            onDismiss = { op = null },
            onConfirm = {
                op = null
                val torch = BlockTemplates.getOfType("minecraft:torch")[0]
                val glass = BlockTemplates.getOfType("minecraft:glass")[0]
                runEdit {
                    setMaxError(Int.MAX_VALUE)
                    forEachXYZSubChunk { subChunk, x, y, z ->
                        editLampshadeAt(subChunk, x, y, z, torch, glass)
                    }
                }
            }
        )

        null -> {}
    }

    snrRequest?.let { request ->
        SelectionSnrDialog(
            request = request,
            onDismiss = { snrRequest = null },
            onConfirm = {
                val search = request.search
                val searchMode = request.searchMode
                val place = request.place
                val placeMode = request.placeMode
                runEdit {
                    setMaxError(Int.MAX_VALUE)
                    forEachXYZSubChunk { subChunk, x, y, z ->
                        editSnrAt(subChunk, x, y, z, search, searchMode, place, placeMode)
                    }
                }
            }
        )
    }

    if (picerOutOfSize) {
        AlertDialog(
            onDismissRequest = { picerOutOfSize = false },
            title = { Text(stringResource(R.string.map_picer_selection_too_large)) },
            text = {
                Text(
                    stringResource(
                        R.string.map_picer_selection_too_large_detail,
                        PICER_MAX_LENGTH,
                        PICER_MAX_AREA
                    )
                )
            },
            confirmButton = {
                TextButton(
                    stringResource(android.R.string.ok),
                    onClick = { picerOutOfSize = false }
                )
            }
        )
    }

    generatedBitmap?.let { bitmap ->
        GeneratedImageDialog(
            editor = editor,
            bitmap = bitmap,
            onDismiss = { generatedBitmap = null }
        )
    }
}

/**
 * Panel content shown in the floating panel while a selection exists. It presents the live
 * selection coordinates and size (updated while dragging the map handles) and the list of
 * operations applicable to the selection.
 */
@Composable
fun SelectionPanel(editor: WorldEditorModel) {
    val selection = editor.selection

    val resources = LocalResources.current
    // Reports the result of every selection operation through the editor snackbar.
    LaunchedEffect(resources) {
        editor.editResult.collect { code ->
            val message = resources.getString(
                if (code === EditResultCode.SUCCESS) R.string.general_done else R.string.general_failed
            )
            editor.scaffoldState.snackbarHostState.showSnackbar(message)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        InfoBox(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "选区".todoI18n(),
                    style = MaterialTheme.typography.titleSmall
                )
                // Editable start position and span, mirroring the selector pane. Typing a start
                // coordinate shifts the whole selection; the span keeps the start corner fixed.
                Grid(
                    config = {
                        column(0.5F)
                        column(0.5F)
                        gap(4.dp)
                    }
                ) {
                    val state = editor.selectorPaneState
                    val size = Modifier.heightIn(min = 48.dp)
                    val padding =
                        PaddingValues(top = 4.dp, bottom = 4.dp, start = 12.dp, end = 12.dp)
                    SelectorCoordinateField(state.startX, "起点 X".todoI18n(), padding, size) {
                        selection.right += it - selection.left
                        selection.left = it
                        true
                    }
                    SelectorCoordinateField(state.startZ, "起点 Z".todoI18n(), padding, size) {
                        selection.bottom += it - selection.top
                        selection.top = it
                        true
                    }
                    SelectorSpanField(state.spanX, "大小 X".todoI18n(), padding, size) {
                        if (it > 0) {
                            selection.right = selection.left + it
                            true
                        } else false
                    }
                    SelectorSpanField(state.spanZ, "大小 Z".todoI18n(), padding, size) {
                        if (it > 0) {
                            selection.bottom = selection.top + it
                            true
                        } else false
                    }
                    LaunchedEffect(Unit) {
                        snapshotFlow {
                            IntIntPair(
                                selection.left,
                                selection.right
                            )
                        }.collect { (left, right) ->
                            state.startX.setTextAndPlaceCursorAtEnd(left.toString())
                            state.spanX.setTextAndPlaceCursorAtEnd((right - left).toString())
                        }
                    }
                    LaunchedEffect(Unit) {
                        snapshotFlow {
                            IntIntPair(
                                selection.top,
                                selection.bottom
                            )
                        }.collect { (top, bottom) ->
                            state.startZ.setTextAndPlaceCursorAtEnd(top.toString())
                            state.spanZ.setTextAndPlaceCursorAtEnd((bottom - top).toString())
                        }
                    }
                }
                // The end corner is inclusive, matching the block range selected on the map.
                SelectionInfoRow(
                    "终点".todoI18n(),
                    "[ ${selection.right - 1}, ${selection.bottom - 1} ]".todoI18n()
                )
            }
        }
        Text(
            text = stringResource(R.string.map_sel_warn_backup),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun DeleteChunkDialog(aligned: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.map_edit_func_dchunk)) },
        text = {
            Column {
                Text(stringResource(R.string.map_edit_dchunk_explain))
                if (!aligned) {
                    Text(
                        stringResource(R.string.map_edit_dchunk_warn_not_aligned),
                        Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                stringResource(android.R.string.cancel),
                onClick = onDismiss
            )
        },
        confirmButton = {
            TextButton(
                stringResource(
                    if (aligned) android.R.string.ok
                    else R.string.map_edit_dchunk_posbtn_with_auto_adjust
                ),
                onClick = onConfirm
            )
        }
    )
}

@Composable
private fun ChangeBiomeDialog(
    onDismiss: () -> Unit,
    onConfirm: (Biome?, Biome) -> Unit
) {
    val optionalBiomes = remember {
        buildList<Biome?> {
            add(null)
            addAll(Biome.entries)
        }
    }
    var toReplaceWith by remember { mutableStateOf(Biome.PLAINS) }
    var toBeReplaced by remember { mutableStateOf<Biome?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.map_edit_func_chbiome)) },
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
                onClick = onDismiss
            )
        },
        confirmButton = {
            TextButton(
                stringResource(android.R.string.ok),
                onClick = { onConfirm(toBeReplaced, toReplaceWith) }
            )
        }
    )
}

@Composable
private fun LampshadeDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.map_edit_func_lampshade)) },
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
                onClick = onDismiss
            )
        },
        confirmButton = {
            TextButton(
                stringResource(android.R.string.ok),
                onClick = onConfirm
            )
        }
    )
}

@Composable
private fun SelectionSnrDialog(
    request: SelectionSnrRequest,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.map_edit_func_snr)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.map_edit_snr_find_in))
                Grid(
                    config = {
                        column(0.5F)
                        column(0.5F)
                    },
                    modifier = Modifier.selectableGroup()
                ) {
                    SearchMode.entries.forEach { mode ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .selectable(
                                    selected = mode === request.searchMode,
                                    onClick = { request.searchMode = mode },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = mode === request.searchMode,
                                onClick = null
                            )
                            Text(
                                text = stringResource(mode.text),
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.map_edit_snr_find_for),
                    style = MaterialTheme.typography.bodyMedium
                )
                BlockStatesPreview(request.search, request.searchMode !== SearchMode.BOTH)
                Text(
                    text = stringResource(R.string.map_edit_snr_place_in),
                    style = MaterialTheme.typography.bodyMedium
                )
                Grid(
                    config = {
                        column(0.5F)
                        column(0.5F)
                    },
                    modifier = Modifier.selectableGroup()
                ) {
                    PlaceMode.entries.forEach { mode ->
                        val modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .selectable(
                                selected = mode === request.placeMode,
                                onClick = { request.placeMode = mode },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp)
                        Row(
                            if (mode === PlaceMode.BOTH) modifier.gridItem(columnSpan = 2)
                            else modifier,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = mode === request.placeMode, onClick = null)
                            Text(
                                text = stringResource(mode.text),
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
                BlockStatesPreview(request.place, request.placeMode !== PlaceMode.BOTH)
            }
        },
        dismissButton = {
            TextButton(
                stringResource(android.R.string.cancel),
                onClick = onDismiss
            )
        },
        confirmButton = {
            TextButton(stringResource(android.R.string.ok)) {
                onConfirm()
                onDismiss()
            }
        }
    )
}

@Composable
private fun GeneratedImageDialog(
    editor: WorldEditorModel,
    bitmap: Bitmap,
    onDismiss: () -> Unit
) {
    var saving by remember { mutableStateOf(false) }

    val width = bitmap.width
    val height = bitmap.height
    val maxScale = remember(width, height) { maxPicerScale(width, height) }
    val sliderState = remember(maxScale) {
        if (maxScale < 2) null else SliderState(
            value = 1F,
            steps = maxScale - 2,
            trackRange = 1F..maxScale.toFloat()
        )
    }
    val scale = sliderState?.value?.roundToInt() ?: 1
    val scaledBitmap = remember(bitmap, scale) {
        if (scale == 1) bitmap
        else bitmap.scale(width * scale, height * scale)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.picer_preview)) },
        text = {
            Column {
                if (sliderState === null) {
                    Text(stringResource(R.string.picer_warn_not_scalable))
                } else {
                    Text(stringResource(R.string.picer_scale_title))
                    Slider(
                        state = sliderState,
                        onValueChange = { sliderState.value = round(it) }
                    )
                    Text(
                        scale.toString(),
                        Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Text(stringResource(R.string.map_picer_scale_guide))
                }
                Image(
                    bitmap = scaledBitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    filterQuality = FilterQuality.None,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .verticalScroll(rememberScrollState())
                )
            }
        },
        dismissButton = {
            TextButton(
                stringResource(android.R.string.cancel),
                enabled = !saving,
                onClick = onDismiss
            )
        },
        confirmButton = {
            TextButton(
                stringResource(R.string.picer_save),
                enabled = !saving,
                onClick = {
                    if (saving) return@TextButton
                    saving = true
                    editor.viewModelScope.launch {
                        val appContext = editor.world.appContext
                        val result = withContext(Dispatchers.IO) {
                            saveImageToGallery(
                                appContext.contentResolver,
                                editor.world.resolvePlainName(appContext),
                                scaledBitmap
                            )
                        }.getOrNull()
                        if (result === null) {
                            // TODO: consider to provide details
                            appContext.toast(R.string.general_failed)
                        } else {
                            val resources = appContext.resources
                            saving = false
                            editor.scaffoldState.snackbarHostState.awaitSnackbar(
                                resources.getString(R.string.picer_saved),
                                resources.getString(R.string.general_share)
                            ) {
                                editor.intents.send(
                                    Intent.createChooser(
                                        Intent(Intent.ACTION_SEND)
                                            .setType("image/png")
                                            .putExtra(Intent.EXTRA_STREAM, result)
                                            .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                                        resources.getString(R.string.picer_share_title)
                                    )
                                )
                            }
                            onDismiss()
                        }
                    }
                }
            )
        }
    )
}

private fun maxPicerScale(width: Int, height: Int): Int {
    val maxEdgeScale = PICER_MAX_LENGTH / maxOf(width, height)
    if (maxEdgeScale < 1) return 1
    val maxAreaFactor = PICER_MAX_AREA / (width.toLong() * height).toInt()
    val areaScale = sqrt(maxAreaFactor.toDouble()).toInt()
    return minOf(maxEdgeScale, areaScale, PICER_MAX_SCALE).coerceAtLeast(1)
}

private fun saveImageToGallery(
    resolver: ContentResolver,
    baseName: String,
    bitmap: Bitmap
): Result<Uri> {
    val spec = ContentValues().apply {
        put(
            MediaStore.Images.Media.DISPLAY_NAME,
            ConvertUtil.getLegalFileName(baseName) + "_map"
        )
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    return runCatching {
        val uri: Uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, spec)
            ?: throw IOException("Failed to create file")
        resolver.openOutputStream(uri)?.use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, it)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            spec.clear()
            spec.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, spec, null, null)
        }
        uri
    }
}

@Composable
private fun SelectionInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Modal progress indicator shown while a selection operation runs on the IO dispatcher.
 * Dismissing it cancels the running job.
 */
@Composable
fun SelectionJobIndicator(editor: WorldEditorModel) {
    if (!editor.waitingJob) return
    BasicAlertDialog(
        onDismissRequest = { editor.cancelBlockingJob() }
    ) {
        Column(
            Modifier
                .clip(AlertDialogDefaults.shape)
                .background(AlertDialogDefaults.containerColor)
                .padding(24.dp)
        ) {
            Text(
                stringResource(R.string.general_please_wait),
                color = AlertDialogDefaults.titleContentColor,
                style = MaterialTheme.typography.headlineSmall
            )
            androidx.compose.material3.CircularProgressIndicator(
                Modifier
                    .padding(top = 16.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}
