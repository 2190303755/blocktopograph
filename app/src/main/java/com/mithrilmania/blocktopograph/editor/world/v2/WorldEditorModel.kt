package com.mithrilmania.blocktopograph.editor.world.v2

import android.app.Application
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.mithrilmania.blocktopograph.map.Dimension
import com.mithrilmania.blocktopograph.nbt.io.NBTImportConfig
import com.mithrilmania.blocktopograph.nbt.io.NBTSource
import com.mithrilmania.blocktopograph.util.APP_TAG
import com.mithrilmania.blocktopograph.world.World
import com.mithrilmania.blocktopograph.world.WorldStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.ui.state.MapState
import java.io.IOException

const val CHUNK_DIMENSION = 16
const val RENDER_SCALE = 16
const val TILE_DIMENSION = CHUNK_DIMENSION * RENDER_SCALE
const val ZOOM_LEVELS = 4


sealed interface InitState {
    object Uninitialized : InitState
    object Failed : InitState
    object Initializing : InitState
    class Succeed(val world: World, val storage: WorldStorage) : InitState
}

class WorldEditorModel(app: Application) : AndroidViewModel(app) {
    val entityIcons: MutableStateFlow<ImageBitmap?> = MutableStateFlow(null)
    val customIcons: MutableStateFlow<ImageBitmap?> = MutableStateFlow(null)
    var initialization: InitState by mutableStateOf(InitState.Uninitialized)
    var editing: Pair<NBTSource, NBTImportConfig>? by mutableStateOf(null)
    var dimension: Dimension = Dimension.OVERWORLD
    var majorLayerId: String? = null

    @JvmField
    val snackbar: SnackbarHostState = SnackbarHostState()

    @JvmField
    val map: MapState = MapState(
        levelCount = ZOOM_LEVELS,
        tileSize = TILE_DIMENSION,
    )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                entityIcons.emit(
                    application.assets.open("entity_wiki.png").use {
                        BitmapFactory.decodeStream(it).asImageBitmap()
                    }
                )
            } catch (e: IOException) {
                Log.e(APP_TAG, "Failed to load entity icons", e)
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                customIcons.emit(
                    application.assets.open("custom_icons.png").use {
                        BitmapFactory.decodeStream(it).asImageBitmap()
                    }
                )
            } catch (e: IOException) {
                Log.e(APP_TAG, "Failed to load custom icons", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        (this.initialization as? InitState.Succeed)?.storage?.close()
    }
}