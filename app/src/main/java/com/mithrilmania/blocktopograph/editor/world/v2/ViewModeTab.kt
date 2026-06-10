package com.mithrilmania.blocktopograph.editor.world.v2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mithrilmania.blocktopograph.ui.component.Expander

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ViewModeTab(
    viewModel: WorldEditorModel,
    info: InitState.Succeed
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .windowInsetsPadding(
                WindowInsets.systemBars
                    .union(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Bottom)
            ),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
        val resources = LocalResources.current
        Expander(
            title = "维度",
            items = info.dimensions,
            supportingContent = { Text(text = viewModel.dimension.getDisplayName(resources)) },
            selectable = true,
            colors = colors
        ) { dimension, shapes ->
            val selected = viewModel.dimension.id == dimension.id
            SegmentedListItem(
                selected = selected,
                onClick = { viewModel.dimension = dimension },
                shapes = shapes,
                colors = colors,
                trailingContent = { RadioButton(selected = selected, onClick = null) },
                content = { Text(dimension.getDisplayName(resources)) },
            )
        }
        Expander(
            title = "渲染层",
            items = MapLayer.entries,
            supportingContent = {
                Text(text = stringResource(viewModel.enabledLayer.display))
            },
            selectable = true,
            colors = colors
        ) { layer, shapes ->
            val selected = viewModel.enabledLayer == layer
            SegmentedListItem(
                selected = selected,
                onClick = { viewModel.enabledLayer = layer },
                shapes = shapes,
                colors = colors,
                trailingContent = { RadioButton(selected = selected, onClick = null) },
                content = { Text(stringResource(layer.display)) },
            )
        }
    }
}

