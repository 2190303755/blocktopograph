package com.mithrilmania.blocktopograph.editor.world

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.map.MapFragment
import com.mithrilmania.blocktopograph.map.marker.AbstractMarker
import com.mithrilmania.blocktopograph.ui.component.TextButton
import com.mithrilmania.blocktopograph.ui.screen.world.LevelDBKey
import com.mithrilmania.blocktopograph.util.ByteArrayMatcher
import com.mithrilmania.blocktopograph.util.NamedBitmapProvider
import com.mithrilmania.blocktopograph.util.tryOrNull
import com.mithrilmania.blocktopograph.world.Dimension
import com.mithrilmania.blocktopograph.world.NO_CACHE_OPTION
import com.mithrilmania.blocktopograph.world.WorldStorage
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.chunked
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.iq80.leveldb.DB

/**
 * A request to show the "create custom marker" dialog at the given map position.
 */
data class CreateMarkerRequest(
    val x: Int,
    val z: Int,
    val dimension: Dimension
)

/**
 * Hosts every [MapFragment] dialog that used to be created with a view-based AlertDialog.
 *
 * Visibility is driven by [viewer] state so the dialogs survive configuration changes
 * and follow the same pattern as [LongPressDialog] and the close-world confirmation.
 */
@Composable
fun MapFragmentDialogs(
    viewer: WorldViewerModel,
    fragment: MapFragment
) {
    if (viewer.showTrySelectionNotice) {
        TrySelectionNoticeDialog { viewer.showTrySelectionNotice = false }
    }
    viewer.tappedMarker?.let { marker ->
        MarkerTapDialog(
            marker = marker,
            onDismiss = { viewer.tappedMarker = null },
            onTeleport = {
                fragment.onChooseTeleportPlayer(
                    marker.x.toFloat(),
                    marker.z.toFloat(),
                    marker.dimension
                )
                viewer.tappedMarker = null
            },
            onRemove = {
                fragment.requestRemoveMarker(marker)
                viewer.tappedMarker = null
            }
        )
    }
    if (viewer.showTooManyMarkersNotice) {
        TooManyMarkersDialog(
            onDismiss = { viewer.showTooManyMarkersNotice = false },
            onOpenDrawer = {
                viewer.showTooManyMarkersNotice = false
                viewer.showDrawer.tryEmit(Unit)
            }
        )
    }
    viewer.createMarkerRequest?.let { request ->
        CreateMarkerDialog(
            request = request,
            viewer = viewer,
            fragment = fragment,
            onDismiss = { viewer.createMarkerRequest = null }
        )
    }
    if (viewer.markerFilterVisible) {
        MarkerFilterDialog(
            fragment = fragment,
            onDismiss = { viewer.markerFilterVisible = false }
        )
    }
}

@Composable
private fun TrySelectionNoticeDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(stringResource(R.string.map_notice_try_selection)) },
        confirmButton = {
            TextButton(stringResource(android.R.string.ok), onClick = onDismiss)
        }
    )
}

@Composable
private fun MarkerTapDialog(
    marker: AbstractMarker,
    onDismiss: () -> Unit,
    onTeleport: () -> Unit,
    onRemove: () -> Unit
) {
    val displayName = marker.namedBitmapProvider.bitmapDisplayName
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    R.string.marker_info,
                    displayName,
                    marker.x,
                    marker.y,
                    marker.z
                )
            )
        },
        text = {
            Column {
                DialogListItem(
                    text = stringResource(R.string.teleport_local_player),
                    onClick = onTeleport
                )
                DialogListItem(
                    text = stringResource(R.string.remove_custom_marker),
                    onClick = onRemove
                )
            }
        },
        dismissButton = {
            TextButton(stringResource(android.R.string.cancel), onClick = onDismiss)
        },
        confirmButton = {}
    )
}

@Composable
private fun DialogListItem(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    )
}

@Composable
private fun TooManyMarkersDialog(
    onDismiss: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false),
        title = { Text(stringResource(R.string.map_smart_notice_too_many_markers)) },
        text = { Text(stringResource(R.string.map_smart_notice_too_many_markers_message)) },
        confirmButton = {
            TextButton(
                stringResource(R.string.map_uioption_open_drawer),
                onClick = onOpenDrawer
            )
        },
        dismissButton = {
            TextButton(stringResource(R.string.general_got_it), onClick = onDismiss)
        }
    )
}

@Composable
private fun CreateMarkerDialog(
    request: CreateMarkerRequest,
    viewer: WorldViewerModel,
    fragment: MapFragment,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
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
            viewer.snackbar.showSnackbar(message, duration = SnackbarDuration.Long)
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
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(stringResource(R.string.create_marker_info))
                OutlinedTextField(
                    state = nameState,
                    modifier = Modifier.fillMaxWidth(),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    label = { Text("Name: ") }
                )
                OutlinedTextField(
                    state = iconState,
                    modifier = Modifier.fillMaxWidth(),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    label = { Text("Icon: ") }
                )
                OutlinedTextField(
                    state = xState,
                    modifier = Modifier.fillMaxWidth(),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberSigned),
                    label = { Text("x: ") }
                )
                OutlinedTextField(
                    state = yState,
                    modifier = Modifier.fillMaxWidth(),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberSigned),
                    label = { Text("y: ") }
                )
                OutlinedTextField(
                    state = zState,
                    modifier = Modifier.fillMaxWidth(),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberSigned),
                    label = { Text("z: ") }
                )
            }
        },
        confirmButton = {
            val resources = LocalResources.current
            TextButton("Create marker") {
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
                val error = fragment.createCustomMarker(name, iconName, x, y, z, request.dimension)
                if (error !== null) {
                    failThenDismiss(resources.getString(error))
                } else {
                    onDismiss()
                }
            }
        },
        dismissButton = {
            TextButton(stringResource(android.R.string.cancel), onClick = onDismiss)
        }
    )
}

@Composable
private fun MarkerFilterDialog(
    fragment: MapFragment,
    onDismiss: () -> Unit
) {
    val choices = remember {
        MapFragment.getMarkerFilterChoices().entries.toList()
    }
    val sandbox = remember {
        MapFragment.getMarkerFilterChoices().values.toMutableStateList()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.filter_markers)) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
            ) {
                itemsIndexed(items = choices, key = { index, _ -> index }) { index, choice ->
                    MarkerFilterRow(sandbox, index, choice.key)
                }
            }
        },
        confirmButton = {
            TextButton(stringResource(android.R.string.ok)) {
                choices.forEachIndexed { index, choice -> choice.setValue(sandbox[index]) }
                fragment.updateMarkerFilter()
                onDismiss()
            }
        },
        dismissButton = {
            TextButton(stringResource(android.R.string.cancel), onClick = onDismiss)
        }
    )
}

@Composable
private fun MarkerFilterRow(
    sandbox: MutableList<Boolean>,
    index: Int,
    provider: NamedBitmapProvider
) {
    val interactionSource = remember { MutableInteractionSource() }
    val checked = sandbox[index]
    val onCheckedChanged: (Boolean) -> Unit = {
        sandbox[index] = it
    }
    SegmentedListItem(
        checked = checked,
        onCheckedChange = onCheckedChanged,
        shapes = ListItemDefaults.segmentedShapes(index, sandbox.size),
        leadingContent = {
            val bitmap = remember(provider) {
                runCatching { provider.bitmap }.getOrNull()?.asImageBitmap()
            }
            if (bitmap !== null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Spacer(Modifier.size(24.dp))
            }
        },
        content = {
            Text(
                text = provider.bitmapDisplayName,
                modifier = Modifier.padding(end = 8.dp)
            )
        },
        interactionSource = interactionSource,
        trailingContent = {
            Checkbox(
                checked = checked,
                onCheckedChange = { sandbox[index] = it },
                interactionSource = interactionSource
            )
        }
    )
}


@OptIn(ExperimentalCoroutinesApi::class)
suspend fun DB.collectMatches(
    pattern: ByteArray,
    destination: MutableList<LevelDBKey>
) {
    flow {
        iterator(NO_CACHE_OPTION).use { iterator ->
            iterator.seekToFirst()
            if (pattern.isEmpty()) {
                while (iterator.hasNext()) {
                    emit(LevelDBKey(iterator.next().key))
                }
            } else {
                val failure = ByteArrayMatcher.computeFailure(pattern)
                while (iterator.hasNext()) {
                    val key = iterator.next().key
                    if (ByteArrayMatcher.contains(key, pattern, failure)) {
                        emit(LevelDBKey(key))
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)
        .chunked(5)
        .collect(destination::addAll)
}

suspend fun Deferred<WorldStorage?>.collectMatches(
    pattern: ByteArray,
    destination: MutableList<LevelDBKey>
) {
    tryOrNull {
        this.await()
    }?.db?.collectMatches(pattern, destination)
}
