package com.mithrilmania.blocktopograph.editor.world.v2

import android.app.Application
import android.graphics.BitmapFactory
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.block.BlockTemplate
import com.mithrilmania.blocktopograph.block.BlockTemplates
import com.mithrilmania.blocktopograph.nbt.io.NBTImportConfig
import com.mithrilmania.blocktopograph.nbt.io.NBTSource
import com.mithrilmania.blocktopograph.registry.Registry
import com.mithrilmania.blocktopograph.util.APP_TAG
import com.mithrilmania.blocktopograph.util.math.DimensionVec3f
import com.mithrilmania.blocktopograph.util.math.DimensionVec3i
import com.mithrilmania.blocktopograph.world.Dimension
import com.mithrilmania.blocktopograph.world.VANILLA_DIMENSIONS
import com.mithrilmania.blocktopograph.world.VanillaDimension
import com.mithrilmania.blocktopograph.world.World
import com.mithrilmania.blocktopograph.world.WorldStorage
import com.mithrilmania.blocktopograph.world.chunk.ChunkCache
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.longs.Long2IntMap
import it.unimi.dsi.fastutil.longs.Long2IntMaps
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ovh.plrapps.mapcompose.api.setCamera
import ovh.plrapps.mapcompose.ui.state.MapState
import java.io.IOException

const val CHUNK_DIMENSION = 16
const val CHUNK_INDICES = CHUNK_DIMENSION - 1
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
        scope: CoroutineScope,
        @JvmField val world: World,
        @JvmField val storage: WorldStorage,
        @JvmField var spawnPos: DimensionVec3i, // TODO: marker
        @JvmField val dimensions: Int2ObjectMap<out Dimension>,
        @JvmField val heightBounds: Long2IntMap,
        @JvmField var localPlayer: DimensionVec3f? // TODO: marker
    ) : InitState {
        @JvmField
        val blocks: Registry<BlockTemplate> = Registry()
        @JvmField
        val chunks = ChunkCache(scope, this.storage, this.heightBounds, this.blocks)

        init {
            blocks.register(BlockTemplates.getAirTemplate())
        }
    }
}

class WorldEditorModel(app: Application) : AndroidViewModel(app) {
    val entityIcons: MutableState<ImageBitmap?> = mutableStateOf(null)
    val customIcons: MutableState<ImageBitmap?> = mutableStateOf(null)
    var initialization: InitState by mutableStateOf(InitState.Uninitialized)
    val tabPager: PagerState = PagerState(0, 0F) { 3 }
    var editing: MutableStateFlow<Pair<NBTSource, NBTImportConfig>?> = MutableStateFlow(null)
    var enabledLayer: MapLayer by mutableStateOf(MapLayer.SATELLITE)
    var dimension: Dimension by mutableStateOf(VanillaDimension.OVERWORLD)

    @JvmField
    val map: MapState = MapState(
        levelCount = ZOOM_LEVELS,
        tileSize = CHUNK_DIMENSION
    ) {
        magnifyingFactor(4)
        minScale(0.625)
        maxScale(64.0)
        scale(16.0)
    }

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

    fun loadWorld(
        world: World,
        storage: WorldStorage,
        spawnPos: DimensionVec3i, // TODO: marker
        dimensions: Int2ObjectMap<out Dimension> = VANILLA_DIMENSIONS,
        heightBounds: Long2IntMap = Long2IntMaps.EMPTY_MAP,
        localPlayer: DimensionVec3f? = null, // TODO: marker
    ) {
        // TODO: register markers
        if (localPlayer === null) {
            val dimension = dimensions[spawnPos.dimensionId]
            if (dimension !== null) {
                this.dimension = dimension
                this.map.setCamera(
                    spawnPos.x.toDouble(),
                    spawnPos.z.toDouble()
                )
            }
        } else {
            val dimension = dimensions[localPlayer.dimensionId]
            if (dimension !== null) {
                this.dimension = dimension
                this.map.setCamera(
                    localPlayer.x.toDouble(),
                    localPlayer.z.toDouble()
                )
            }
        }
        this.initialization = InitState.Succeed(
            viewModelScope,
            world,
            storage,
            spawnPos,
            dimensions,
            heightBounds,
            localPlayer
        )
    }

    override fun onCleared() {
        super.onCleared()
        (this.initialization as? InitState.Succeed)?.storage?.close()
    }
}