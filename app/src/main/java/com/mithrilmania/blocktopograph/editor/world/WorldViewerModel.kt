package com.mithrilmania.blocktopograph.editor.world

import android.app.Application
import android.content.Intent
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntRect
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.mithrilmania.blocktopograph.map.edit.SearchAndReplaceRequest
import com.mithrilmania.blocktopograph.map.marker.AbstractMarker
import com.mithrilmania.blocktopograph.map.picer.PICER_MAX_AREA
import com.mithrilmania.blocktopograph.map.picer.PICER_MAX_LENGTH
import com.mithrilmania.blocktopograph.map.picer.PicerState
import com.mithrilmania.blocktopograph.map.renderer.MapType
import com.mithrilmania.blocktopograph.util.Signal
import com.mithrilmania.blocktopograph.world.Dimension
import com.mithrilmania.blocktopograph.world.VanillaDimension
import com.mithrilmania.blocktopograph.world.defaultMapTypeCompat
import kotlinx.coroutines.channels.Channel
import kotlin.math.sqrt

class WorldViewerModel(app: Application) : AndroidViewModel(app) {
    @JvmField
    val pendingIntent: Channel<Intent> = Channel()

    @JvmField
    val pendingMarkers: Channel<List<AbstractMarker>> = Channel(capacity = 4)

    var dimension: Dimension = VanillaDimension.OVERWORLD

    val mapType: MutableLiveData<MapType> =
        MutableLiveData<MapType>(dimension.defaultMapTypeCompat())

    val showGrid: MutableLiveData<Boolean> = MutableLiveData<Boolean>(true)
    val showMarkers: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)

    @JvmField
    val showDrawer = Signal<Unit>()

    @JvmField
    val longPressCenter = Signal<Unit>()
    val snackbar = SnackbarHostState()
    var picerState by mutableStateOf<PicerState?>(null)
    var replacingRequest by mutableStateOf<SearchAndReplaceRequest?>(null)

    fun navigateTo(dimension: Dimension, type: MapType) {
        this.dimension = dimension
        this.mapType.value = type
    }

    fun commitAnalyzedState(rect: IntRect, fallback: PicerState) {
        val width = rect.width
        val height = rect.height
        if (width > 0 && height > 0) {
            val maxEdgeScale = PICER_MAX_LENGTH / maxOf(width, height)
            if (maxEdgeScale >= 1) {
                val maxAreaFactor = PICER_MAX_AREA / (width * height)
                val maxScale = if (maxEdgeScale * maxEdgeScale > maxAreaFactor)
                    sqrt(maxAreaFactor.toDouble()).toInt()
                else maxEdgeScale
                if (maxScale >= 1) {
                    this.picerState = PicerState.Analyzed(rect, maxScale)
                    return
                }
            }
        }
        this.picerState = fallback
    }
}