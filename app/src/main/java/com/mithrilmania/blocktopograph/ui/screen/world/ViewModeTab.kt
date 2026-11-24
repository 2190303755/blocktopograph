package com.mithrilmania.blocktopograph.ui.screen.world

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.ui.component.Expander
import com.mithrilmania.blocktopograph.ui.component.InfoBox
import com.mithrilmania.blocktopograph.util.isDecimal
import com.mithrilmania.blocktopograph.util.todoI18n
import com.mithrilmania.blocktopograph.world.isOverworld
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.cameraX
import ovh.plrapps.mapcompose.api.cameraY
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.ui.state.MapModel

@Composable
fun ViewModeTab(
    mapModel: MapModel,
    worldModel: WorldEditorModel,
    padding: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
        val resources = LocalResources.current
        Expander(
            title = "维度".todoI18n(),
            items = worldModel.world.dimensions.values,
            modifier = Modifier.padding(top = 8.dp),
            supportingContent = { Text(text = worldModel.dimension.getDisplayName(resources)) },
            selectable = true,
            colors = colors
        ) { dimension, shapes ->
            val selected = worldModel.dimension.runtimeId == dimension.runtimeId
            SegmentedListItem(
                selected = selected,
                onClick = { worldModel.dimension = dimension },
                shapes = shapes,
                colors = colors,
                trailingContent = { RadioButton(selected = selected, onClick = null) },
                content = { Text(dimension.getDisplayName(resources)) },
            )
        }
        Expander(
            title = "主视图".todoI18n(),
            items = MapLayer.entries,
            supportingContent = {
                Text(text = stringResource(worldModel.enabledLayer.display))
            },
            selectable = true,
            colors = colors
        ) { layer, shapes ->
            val selected = worldModel.enabledLayer == layer
            SegmentedListItem(
                selected = selected,
                onClick = { worldModel.enabledLayer = layer },
                enabled = layer !== MapLayer.SLIME_CHUNKS || worldModel.dimension.isOverworld,
                shapes = shapes,
                colors = colors,
                trailingContent = { RadioButton(selected = selected, onClick = null) },
                content = { Text(stringResource(layer.display)) },
            )
        }
        SegmentedListItem(
            selected = false,
            onClick = { worldModel.showGrid = !worldModel.showGrid },
            shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
            colors = colors,
            trailingContent = {
                Switch(
                    checked = worldModel.showGrid,
                    onCheckedChange = null
                )
            },
            content = { Text(stringResource(R.string.toggle_grid)) }
        )
        CoordinateJumpSection(
            mapModel = mapModel,
            worldModel = worldModel,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}

@Composable
private fun CoordinateJumpSection(
    mapModel: MapModel,
    worldModel: WorldEditorModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    InfoBox(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.locator_page_coor),
                style = MaterialTheme.typography.titleSmall
            )
            val invalidX = remember { mutableStateOf(false) }
            val invalidZ = remember { mutableStateOf(false) }
            val commit: () -> Unit = {
                val x = worldModel.coordinateX.text.trim().toString().toDoubleOrNull()
                val z = worldModel.coordinateZ.text.trim().toString().toDoubleOrNull()
                if (x !== null && z !== null) {
                    coroutineScope.launch { mapModel.scrollTo(x, z) }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CoordinateJumpField(
                    state = worldModel.coordinateX,
                    label = "X".todoI18n(),
                    imeAction = ImeAction.Next,
                    isError = invalidX,
                    modifier = Modifier.weight(1F),
                    onKeyboardAction = { }
                ) {
                    mapModel.cameraX
                }
                CoordinateJumpField(
                    state = worldModel.coordinateZ,
                    label = "Z".todoI18n(),
                    imeAction = ImeAction.Go,
                    isError = invalidZ,
                    modifier = Modifier.weight(1F),
                    onKeyboardAction = commit
                ) {
                    mapModel.cameraY
                }
            }
            TextButton(
                onClick = commit,
                modifier = Modifier.align(Alignment.End),
                enabled = !invalidX.value && !invalidZ.value
            ) {
                Text(text = stringResource(R.string.go_loud))
            }
        }
    }
}

@Composable
private fun CoordinateJumpField(
    state: TextFieldState,
    label: String,
    imeAction: ImeAction,
    isError: MutableState<Boolean>,
    modifier: Modifier = Modifier,
    onKeyboardAction: () -> Unit,
    current: () -> Double
) {
    LaunchedEffect(state) {
        snapshotFlow { state.text }.collect {
            val input = it.trim()
            isError.value = input.isNotEmpty() && input.toString().toDoubleOrNull() === null
        }
    }
    OutlinedTextField(
        state = state,
        modifier = modifier,
        shape = OutlinedTextFieldDefaults.roundedShape,
        lineLimits = TextFieldLineLimits.SingleLine,
        isError = isError.value,
        label = { Text(label) },
        placeholder = { Text(current().toString()) },
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
            val input = state.text.trim()
            if (input.isNotEmpty() && input.toString().toDoubleOrNull() === null) {
                isError.value = true
            } else {
                it()
                onKeyboardAction()
            }
        }
    )
}
