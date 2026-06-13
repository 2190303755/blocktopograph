package com.mithrilmania.blocktopograph.editor.world

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mithrilmania.blocktopograph.map.marker.AbstractMarker
import com.mithrilmania.blocktopograph.map.renderer.MapType
import com.mithrilmania.blocktopograph.world.Dimension
import com.mithrilmania.blocktopograph.world.VanillaDimension
import com.mithrilmania.blocktopograph.world.defaultMapTypeCompat

class WorldMapModel : ViewModel() {
    val markers: MutableLiveData<ArrayList<AbstractMarker>> =
        MutableLiveData<ArrayList<AbstractMarker>>(arrayListOf())

    var dimension: Dimension = VanillaDimension.OVERWORLD

    val mapType: MutableLiveData<MapType> =
        MutableLiveData<MapType>(dimension.defaultMapTypeCompat())

    val showActionBar: MutableLiveData<Boolean> = MutableLiveData<Boolean>(true)
    val showGrid: MutableLiveData<Boolean> = MutableLiveData<Boolean>(true)
    val showDrawer: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    val showMarkers: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)

    fun navigateTo(dimension: Dimension, type: MapType) {
        this.dimension = dimension
        this.mapType.value = type
    }
}