package com.mithrilmania.blocktopograph.map.edit

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalGridApi
import androidx.compose.foundation.layout.Grid
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.block.BlockTemplate
import com.mithrilmania.blocktopograph.block.BlockTemplates
import com.mithrilmania.blocktopograph.ui.BlockStatePreview
import com.mithrilmania.blocktopograph.ui.PickBlockDialog

enum class SearchMode(@param:StringRes val text: Int) {
    BACKGROUND(R.string.map_edit_snr_bg),
    FOREGROUND(R.string.map_edit_snr_fg),
    ANY(R.string.map_edit_snr_or),
    BOTH(R.string.map_edit_snr_both)
}

enum class PlaceMode(@param:StringRes val text: Int) {
    BACKGROUND(R.string.map_edit_snr_bg),
    FOREGROUND(R.string.map_edit_snr_fg),
    BOTH(R.string.map_edit_snr_and_long)
}

class BlockStates(major: String, minor: String) {
    var major: BlockTemplate by mutableStateOf(
        BlockTemplates.getOfType(major).first()
    )
    var minor: BlockTemplate by mutableStateOf(
        BlockTemplates.getOfType(minor).first()
    )
}

class SearchAndReplaceModel : ViewModel() {
    var searchMode: SearchMode by mutableStateOf(SearchMode.FOREGROUND)
    var placeMode: PlaceMode by mutableStateOf(PlaceMode.FOREGROUND)
    val search: BlockStates = BlockStates("minecraft:grass", "minecraft:air")
    val place: BlockStates = BlockStates("minecraft:grass", "minecraft:water")

    fun buildConfig(): SnrConfig {
        val config = SnrConfig()
        config.searchMode = this.searchMode.ordinal + 1
        config.placeMode = this.placeMode.ordinal + 1
        config.ignoreSubId = true // mBinding.cbIgsub.isChecked();
        config.searchBlockMain = SnrConfig.SearchConditionBlock(
            this.search.major.block,
            false,
            true
        )
        if (this.searchMode === SearchMode.BOTH) {
            config.searchBlockSub = SnrConfig.SearchConditionBlock(
                this.search.minor.block,
                false,
                true
            )
        }
        config.placeOldBlockMain = this.place.major.block
        if (this.placeMode === PlaceMode.BOTH) {
            config.placeOldBlockSub = this.place.minor.block
        }
        return config
    }
}

@Composable
fun BlockStatesPreview(
    states: BlockStates,
    majorOnly: Boolean
) {
    var selecting by rememberSaveable { mutableIntStateOf(0) }
    val context = LocalContext.current
    AnimatedContent(majorOnly) { mode ->
        if (mode) {
            BlockStatePreview(states.major, context = context) { selecting = 1 }
        } else {
            Column {
                Text(
                    text = stringResource(R.string.map_edit_snr_fg_slot),
                    style = MaterialTheme.typography.bodySmall
                )
                BlockStatePreview(states.major, context = context) { selecting = 1 }
                Text(
                    text = stringResource(R.string.map_edit_snr_bg_slot),
                    style = MaterialTheme.typography.bodySmall
                )
                BlockStatePreview(states.minor, context = context) { selecting = 2 }
            }
        }
    }
    if (selecting != 0) {
        PickBlockDialog(onCancel = { selecting = 0 }) {
            when (selecting) {
                1 -> states.major = it
                2 -> states.minor = it
            }
            selecting = 0
        }
    }
}


@OptIn(ExperimentalGridApi::class)
@Composable
fun SearchAndReplaceLayout(
    viewModel: SearchAndReplaceModel
) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Text(
            text = stringResource(R.string.map_edit_snr_find_in),
            style = MaterialTheme.typography.bodyMedium
        )
        Grid(
            config = {
                column(1.fr)
                column(1.fr)
            },
            modifier = Modifier.selectableGroup()
        ) {
            SearchMode.entries.forEach { mode ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .selectable(
                            selected = mode === viewModel.searchMode,
                            onClick = { viewModel.searchMode = mode },
                            role = Role.RadioButton,
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = mode === viewModel.searchMode, onClick = null)
                    Text(
                        text = stringResource(mode.text),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.map_edit_snr_find_for),
            style = MaterialTheme.typography.bodyMedium
        )
        BlockStatesPreview(viewModel.search, viewModel.searchMode !== SearchMode.BOTH)
        Text(
            text = stringResource(R.string.map_edit_snr_place_in),
            style = MaterialTheme.typography.bodyMedium
        )
        Grid(
            config = {
                column(1.fr)
                column(1.fr)
            },
            modifier = Modifier.selectableGroup()
        ) {
            PlaceMode.entries.forEach { mode ->
                val modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .selectable(
                        selected = mode === viewModel.placeMode,
                        onClick = { viewModel.placeMode = mode },
                        role = Role.RadioButton,
                    )
                    .padding(horizontal = 16.dp)
                Row(
                    if (mode === PlaceMode.BOTH) modifier.gridItem(columnSpan = 2) else modifier,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = mode === viewModel.placeMode, onClick = null)
                    Text(
                        text = stringResource(mode.text),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
            }
        }
        BlockStatesPreview(viewModel.place, viewModel.placeMode !== PlaceMode.BOTH)
    }
}