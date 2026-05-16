package com.mithrilmania.blocktopograph.editor.world

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.mithrilmania.blocktopograph.block.BlockTemplate
import com.mithrilmania.blocktopograph.block.BlockTemplates
import com.mithrilmania.blocktopograph.map.Biome

class CreateWorldModel : ViewModel() {
    val snackbar: SnackbarHostState = SnackbarHostState()
    val name: TextFieldState = TextFieldState()
    var biome: Biome by mutableStateOf(Biome.PLAINS)
    var selected: Layer? by mutableStateOf(null)
    var picked: BlockTemplate? by mutableStateOf(null)
    val layers: MutableList<Layer> = mutableStateListOf(
        Layer(BlockTemplates.getOfType("minecraft:tallgrass")[0], 1),
        Layer(BlockTemplates.getOfType("minecraft:grass")[0], 1),
        Layer(BlockTemplates.getOfType("minecraft:dirt")[0], 29),
        Layer(BlockTemplates.getOfType("minecraft:bedrock")[0], 1)
    )
}