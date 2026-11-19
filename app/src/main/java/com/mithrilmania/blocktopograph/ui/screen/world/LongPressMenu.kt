package com.mithrilmania.blocktopograph.ui.screen.world

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.storage.VirtualFile
import com.mithrilmania.blocktopograph.ui.AppNavKey
import com.mithrilmania.blocktopograph.ui.component.TextButton
import com.mithrilmania.blocktopograph.ui.theme.BlocktopographCompatTheme
import com.mithrilmania.blocktopograph.util.todoI18n
import com.mithrilmania.blocktopograph.util.upcoming
import com.mithrilmania.blocktopograph.world.Dimension
import com.mithrilmania.blocktopograph.world.chunk.ChunkPos
import com.mithrilmania.blocktopograph.world.chunk.ChunkTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Map position captured by a long-press, along with its chunk coordinates.
 */
class LongPressPos(
    val worldX: Double,
    val worldZ: Double,
    val chunkX: Int,
    val chunkZ: Int
)

/**
 * A request to show the "create custom marker" dialog at the given map position.
 */
data class CreateMarkerRequest(
    val x: Int,
    val z: Int,
    val dimension: Dimension
)

/**
 * Long-press options menu ported from the view-based world editor. Visibility is driven by
 * [WorldEditorModel.longPressPos] so the dialog survives recomposition like every other
 * editor dialog.
 */
@Composable
fun LongPressMenu(
    editor: WorldEditorModel,
    info: InitState.Succeed,
    onNavigate: (AppNavKey) -> Unit
) {
    val pos = editor.longPressPos
    if (pos !== null) {
        BlocktopographCompatTheme(darkTheme = true) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                val context = LocalContext.current
                val resources = LocalResources.current
                val scope = rememberCoroutineScope()
                val snackbar = editor.scaffoldState.snackbarHostState
                val onDismissRequest = { editor.longPressPos = null }

                /**
                 * Opens the chunk entity / tile-entity NBT in the editor, reporting missing
                 * data or unreadable chunks through the snackbar.
                 */
                fun openChunkNbt(isEntity: Boolean) {
                    onDismissRequest()
                    scope.launch {
                        val chunk = withContext(Dispatchers.IO) {
                            info.chunks.getAsync(
                                ChunkPos(editor.dimension.runtimeId, pos.chunkX, pos.chunkZ)
                            ).await()
                        }
                        if (chunk === null) {
                            snackbar.showSnackbar(
                                resources.getString(R.string.error_could_not_open_world)
                            )
                            return@launch
                        }
                        val tag = if (isEntity) ChunkTag.ENTITY else ChunkTag.BLOCK_ENTITY
                        val key = chunk.pos.buildPrefix()
                        key[key.size - 1] = tag.dataID
                        val source = VirtualFile(
                            info.storage,
                            "${if (isEntity) "Entity" else "BlockEntity"} at " +
                                    "${editor.dimension.identifier} (${pos.chunkX}, ${pos.chunkZ}) ",
                            key
                        )
                        val exists = withContext(Dispatchers.IO) { source.isPresent() }
                        if (exists) {
                            onNavigate(AppNavKey.NBTEditor(source to source))
                        } else {
                            snackbar.showSnackbar(
                                resources.getString(
                                    R.string.failed_to_load_x,
                                    resources.getString(
                                        if (isEntity) R.string.entity_chunk_data
                                        else R.string.tile_entity_chunk_data
                                    )
                                )
                            )
                        }
                    }
                }

                BasicAlertDialog(
                    onDismissRequest = onDismissRequest,
                    properties = DialogProperties()
                ) {
                    Column(
                        Modifier
                            .verticalScroll(rememberScrollState())
                            .clip(MaterialTheme.shapes.medium)
                            .background(Color(0, 0, 0, 0x40))
                    ) {
                        Text(
                            stringResource(
                                R.string.postion_2D_floats_with_chunkpos,
                                pos.worldX,
                                pos.worldZ,
                                pos.chunkX,
                                pos.chunkZ
                            ),
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        val itemPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                        val itemColors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        )
                        ListItem(
                            onClick = {
                                editor.createMarkerRequest = CreateMarkerRequest(
                                    pos.worldX.toInt(),
                                    pos.worldZ.toInt(),
                                    editor.dimension
                                )
                                onDismissRequest()
                            },
                            colors = itemColors,
                            contentPadding = itemPadding
                        ) {
                            Text(stringResource(R.string.create_custom_marker))
                        }
                        ListItem(
                            onClick = { openChunkNbt(true) },
                            colors = itemColors,
                            contentPadding = itemPadding
                        ) {
                            Text(stringResource(R.string.open_chunk_entity_nbt))
                        }
                        ListItem(
                            onClick = { openChunkNbt(false) },
                            colors = itemColors,
                            contentPadding = itemPadding
                        ) {
                            Text(stringResource(R.string.open_chunk_tile_entity_nbt))
                        }
                        ListItem(
                            onClick = {
                                val selecting = editor.selection.isSelecting
                                onDismissRequest()
                                if (selecting) {
                                    editor.selection.isSelecting = false
                                } else {
                                    scope.launch {
                                        editor.beginSelectionAt(pos.worldX, pos.worldZ)
                                    }
                                }
                            },
                            colors = itemColors,
                            contentPadding = itemPadding
                        ) {
                            Text(
                                stringResource(
                                    if (editor.selection.isSelecting) R.string.func_cancel_selection
                                    else R.string.func_begin_selection
                                )
                            )
                        }
                        androidx.compose.material3.TextButton(
                            onClick = onDismissRequest,
                            modifier = Modifier
                                .padding(horizontal = 24.dp, vertical = 16.dp)
                                .align(Alignment.End)
                        ) {
                            Text(stringResource(android.R.string.cancel))
                        }
                    }
                }
            }
        }
    }
    editor.createMarkerRequest?.let { request ->
        CreateMarkerDialog(
            request = request,
            editor = editor,
            onDismiss = { editor.createMarkerRequest = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateMarkerDialog(
    request: CreateMarkerRequest,
    editor: WorldEditorModel,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbar = editor.scaffoldState.snackbarHostState
    val nameState = rememberTextFieldState(stringResource(R.string.default_custom_marker_name))
    val iconState = rememberTextFieldState("blue_marker")
    val xState = rememberTextFieldState(request.x.toString())
    val yState = rememberTextFieldState("64")
    val zState = rememberTextFieldState(request.z.toString())

    val invalidName = stringResource(R.string.marker_invalid_name)
    val invalidIconName = stringResource(R.string.invalid_icon_name)
    val invalidX = stringResource(R.string.invalid_x_coordinate)
    val invalidY = stringResource(R.string.invalid_y_coordinate)
    val invalidZ = stringResource(R.string.invalid_z_coordinate)

    fun failThenDismiss(message: String) {
        scope.launch {
            snackbar.showSnackbar(message, duration = SnackbarDuration.Long)
        }
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_custom_marker)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(stringResource(R.string.create_marker_info))
                OutlinedTextField(
                    state = nameState,
                    modifier = Modifier.fillMaxWidth(),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    label = { Text("Name: ".todoI18n()) }
                )
                OutlinedTextField(
                    state = iconState,
                    modifier = Modifier.fillMaxWidth(),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    label = { Text("Icon: ".todoI18n()) }
                )
                OutlinedTextField(
                    state = xState,
                    modifier = Modifier.fillMaxWidth(),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberSigned),
                    label = { Text("x: ".todoI18n()) }
                )
                OutlinedTextField(
                    state = yState,
                    modifier = Modifier.fillMaxWidth(),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberSigned),
                    label = { Text("y: ".todoI18n()) }
                )
                OutlinedTextField(
                    state = zState,
                    modifier = Modifier.fillMaxWidth(),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberSigned),
                    label = { Text("z: ".todoI18n()) }
                )
            }
        },
        dismissButton = {
            TextButton(
                text = stringResource(android.R.string.cancel),
                onClick = onDismiss
            )
        },
        confirmButton = {
            val resources = LocalResources.current
            TextButton("Create marker".todoI18n()) {
                val name = nameState.text.toString()
                if (name.isEmpty() || name.contains("\"")) {
                    failThenDismiss(invalidName)
                    return@TextButton
                }
                val iconName = iconState.text.toString()
                if (iconName.isEmpty() || iconName.contains("\"")) {
                    failThenDismiss(invalidIconName)
                    return@TextButton
                }
                val x = xState.text.toString().toIntOrNull()
                if (x === null) {
                    failThenDismiss(invalidX)
                    return@TextButton
                }
                val y = yState.text.toString().toIntOrNull()
                if (y === null) {
                    failThenDismiss(invalidY)
                    return@TextButton
                }
                val z = zState.text.toString().toIntOrNull()
                if (z === null) {
                    failThenDismiss(invalidZ)
                    return@TextButton
                }
                val error = editor.createCustomMarker(name, iconName, x, y, z, request.dimension)
                if (error !== null) {
                    failThenDismiss(resources.getString(error))
                } else {
                    editor.persistMarkers()
                    editor.loadMarkers()
                    onDismiss()
                }
            }
        }
    )
}
