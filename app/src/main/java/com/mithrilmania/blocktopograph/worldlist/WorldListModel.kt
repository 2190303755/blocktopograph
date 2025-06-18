package com.mithrilmania.blocktopograph.worldlist

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class WorldListModel : ViewModel() {
    val loading: MutableLiveData<Boolean> = MutableLiveData(false)
    val adapter = WorldItemAdapter()
}