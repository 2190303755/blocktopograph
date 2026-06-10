package com.mithrilmania.blocktopograph.editor.world.v2

import android.app.Application
import android.graphics.BitmapFactory
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.nbt.io.NBTImportConfig
import com.mithrilmania.blocktopograph.nbt.io.NBTSource
import com.mithrilmania.blocktopograph.util.APP_TAG
import com.mithrilmania.blocktopograph.world.Dimension
import com.mithrilmania.blocktopograph.world.VanillaDimension
import com.mithrilmania.blocktopograph.world.World
import com.mithrilmania.blocktopograph.world.WorldStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ovh.plrapps.mapcompose.ui.state.MapState
import java.io.IOException

const val CHUNK_DIMENSION = 16
const val RENDER_SCALE = 16
const val TILE_DIMENSION = CHUNK_DIMENSION * RENDER_SCALE
const val ZOOM_LEVELS = 4

enum class MapLayer(@JvmField @field:StringRes val display: Int) {
    SATELLITE(R.string.satellite),
    SLIME_CHUNKS(R.string.slime_chunks),
    HEIGHT_MAP(R.string.heightmap)
}

sealed interface InitState {
    object Uninitialized : InitState
    object Failed : InitState
    object Initializing : InitState
    class Succeed(
        val world: World,
        val storage: WorldStorage,
        val dimensions: List<Dimension>
    ) : InitState
}

class WorldEditorModel(app: Application) : AndroidViewModel(app) {
    val entityIcons: MutableState<ImageBitmap?> = mutableStateOf(null)
    val customIcons: MutableState<ImageBitmap?> = mutableStateOf(null)
    var initialization: InitState by mutableStateOf(InitState.Uninitialized)
    var selectedTab: Int by mutableIntStateOf(0)
    val tabPager: PagerState = PagerState(0, 0F) { 3 }
    var editing: MutableStateFlow<Pair<NBTSource, NBTImportConfig>?> = MutableStateFlow(null)
    var enabledLayer: MapLayer by mutableStateOf(MapLayer.SATELLITE)
    var dimension: Dimension by mutableStateOf(VanillaDimension.Overworld)
    var majorLayerId: String? = null

    @JvmField
    val map: MapState = MapState(
        levelCount = ZOOM_LEVELS,
        tileSize = TILE_DIMENSION,
    )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val icons = try {
                application.assets.open("entity_wiki.png").use {
                    BitmapFactory.decodeStream(it).asImageBitmap()
                }
            } catch (e: IOException) {
                Log.e(APP_TAG, "Failed to load entity icons", e)
                return@launch
            }
            withContext(Dispatchers.Main) {
                entityIcons.value = icons
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            val icons = try {
                application.assets.open("custom_icons.png").use {
                    BitmapFactory.decodeStream(it).asImageBitmap()
                }
            } catch (e: IOException) {
                Log.e(APP_TAG, "Failed to load custom icons", e)
                return@launch
            }
            withContext(Dispatchers.Main) {
                customIcons.value = icons
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        (this.initialization as? InitState.Succeed)?.storage?.close()
    }
}