package com.mithrilmania.blocktopograph.worldlist

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mithrilmania.blocktopograph.world.World

class WorldListModel : ViewModel() {
    val selected: MutableLiveData<World<*>> = MutableLiveData()
    val worlds: ArrayList<World<*>> = arrayListOf()
    val loading: MutableLiveData<Boolean> = MutableLiveData(false)
}