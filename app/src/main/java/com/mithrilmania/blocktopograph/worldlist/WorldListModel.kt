package com.mithrilmania.blocktopograph.worldlist

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mithrilmania.blocktopograph.world.WorldInfo

class WorldListModel : ViewModel() {
    val selected: MutableLiveData<WorldInfo<*>> = MutableLiveData()
    val worlds: ArrayList<WorldInfo<*>> = arrayListOf()
    val loading: MutableLiveData<Boolean> = MutableLiveData(false)
}