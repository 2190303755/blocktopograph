package com.mithrilmania.blocktopograph.editor.world

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mithrilmania.blocktopograph.map.marker.AbstractMarker
import com.mithrilmania.blocktopograph.map.renderer.MapType
import com.mithrilmania.blocktopograph.world.Dimension
import com.mithrilmania.blocktopograph.world.VanillaDimension
import com.mithrilmania.blocktopograph.world.defaultMapTypeCompat
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class WorldMapModel : ViewModel() {
    val markers: MutableLiveData<ArrayList<AbstractMarker>> =
        MutableLiveData<ArrayList<AbstractMarker>>(arrayListOf())

    var dimension: Dimension = VanillaDimension.OVERWORLD

    val mapType: MutableLiveData<MapType> =
        MutableLiveData<MapType>(dimension.defaultMapTypeCompat())

    val showGrid: MutableLiveData<Boolean> = MutableLiveData<Boolean>(true)
    val showMarkers: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)

    private val _showDrawerSignal = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    @JvmField
    val showDrawerSignal = _showDrawerSignal.asSharedFlow()

    fun navigateTo(dimension: Dimension, type: MapType) {
        this.dimension = dimension
        this.mapType.value = type
    }

    fun showDrawer() {
        this._showDrawerSignal.tryEmit(Unit)
    }
}