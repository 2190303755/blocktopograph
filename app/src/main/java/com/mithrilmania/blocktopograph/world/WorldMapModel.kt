package com.mithrilmania.blocktopograph.world

import androidx.lifecycle.MutableLiveData
import com.mithrilmania.blocktopograph.map.Dimension
import com.mithrilmania.blocktopograph.map.marker.AbstractMarker
import com.mithrilmania.blocktopograph.map.renderer.MapType

class WorldMapModel : WorldModel() {
    val markers: MutableLiveData<ArrayList<AbstractMarker>> =
        MutableLiveData<ArrayList<AbstractMarker>>(arrayListOf())

    var dimension: Dimension = Dimension.OVERWORLD

    val worldType: MutableLiveData<MapType> =
        MutableLiveData<MapType>(Dimension.OVERWORLD.defaultMapType)

    val showActionBar: MutableLiveData<Boolean> = MutableLiveData<Boolean>(true)
    val showGrid: MutableLiveData<Boolean> = MutableLiveData<Boolean>(true)
    val showDrawer: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    val showMarkers: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
}