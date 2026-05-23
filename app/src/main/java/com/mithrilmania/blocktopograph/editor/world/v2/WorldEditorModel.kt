package com.mithrilmania.blocktopograph.editor.world.v2

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.mithrilmania.blocktopograph.map.Dimension
import com.mithrilmania.blocktopograph.nbt.io.NBTImportConfig
import com.mithrilmania.blocktopograph.nbt.io.NBTSource
import com.mithrilmania.blocktopograph.world.World
import com.mithrilmania.blocktopograph.world.WorldStorage
import ovh.plrapps.mapcompose.ui.state.MapState

const val CHUNK_DIMENSION = 16
const val RENDER_SCALE = 16
const val TILE_DIMENSION = CHUNK_DIMENSION * RENDER_SCALE
const val ZOOM_LEVELS = 3


sealed interface InitState {
    object Uninitialized : InitState
    object Failed : InitState
    object Initializing : InitState
    class Succeed(val world: World, val storage: WorldStorage) : InitState
}

class WorldEditorModel : ViewModel() {
    var initialization: InitState by mutableStateOf(InitState.Uninitialized)
    var editing: Pair<NBTSource, NBTImportConfig>? by mutableStateOf(null)
    var dimension: Dimension = Dimension.OVERWORLD
    var majorLayerId: String? = null

    @JvmField
    val snackbar: SnackbarHostState = SnackbarHostState()

    @JvmField
    val map: MapState = MapState(
        levelCount = ZOOM_LEVELS,
        tileSize = TILE_DIMENSION
    )

    override fun onCleared() {
        super.onCleared()
        (this.initialization as? InitState.Succeed)?.storage?.close()
    }
}