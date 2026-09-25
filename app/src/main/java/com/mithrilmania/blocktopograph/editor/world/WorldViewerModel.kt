package com.mithrilmania.blocktopograph.editor.world

import android.graphics.Rect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mithrilmania.blocktopograph.map.marker.AbstractMarker
import com.mithrilmania.blocktopograph.map.picer.PicerDialogFragment
import com.mithrilmania.blocktopograph.map.picer.PicerState
import com.mithrilmania.blocktopograph.map.renderer.MapType
import com.mithrilmania.blocktopograph.util.Signal
import com.mithrilmania.blocktopograph.world.Dimension
import com.mithrilmania.blocktopograph.world.VanillaDimension
import com.mithrilmania.blocktopograph.world.defaultMapTypeCompat
import kotlinx.coroutines.channels.Channel
import kotlin.math.sqrt

class WorldViewerModel : ViewModel() {
    val pendingMarkers: Channel<List<AbstractMarker>> = Channel(capacity = 4)

    var dimension: Dimension = VanillaDimension.OVERWORLD

    val mapType: MutableLiveData<MapType> =
        MutableLiveData<MapType>(dimension.defaultMapTypeCompat())

    val showGrid: MutableLiveData<Boolean> = MutableLiveData<Boolean>(true)
    val showMarkers: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)

    @JvmField
    val showDrawer = Signal()

    @JvmField
    val longPressCenter = Signal()

    var picerState by mutableStateOf<PicerState?>(null)

    fun navigateTo(dimension: Dimension, type: MapType) {
        this.dimension = dimension
        this.mapType.value = type
    }

    fun commitAnalyzedState(rect: Rect, fallback: PicerState) {
        val width = rect.width()
        val height = rect.height()
        if (width > 0 && height > 0) {
            val maxEdgeScale = PicerDialogFragment.MAX_LENGTH / maxOf(width, height)
            if (maxEdgeScale >= 1) {
                val maxAreaFactor = PicerDialogFragment.MAX_AREA / (width * height)
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