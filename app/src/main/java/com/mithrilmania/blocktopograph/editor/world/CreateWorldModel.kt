package com.mithrilmania.blocktopograph.editor.world

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mithrilmania.blocktopograph.flat.Layer
import com.mithrilmania.blocktopograph.map.Biome

class CreateWorldModel : ViewModel() {
    val biome = MutableLiveData(Biome.PLAINS)
    var layers: List<Layer> = emptyList()
    var name: String = ""
}