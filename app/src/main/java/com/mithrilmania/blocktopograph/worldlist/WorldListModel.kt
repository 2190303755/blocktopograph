package com.mithrilmania.blocktopograph.worldlist

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class WorldListModel : ViewModel() {
    val selected: MutableLiveData<WorldItem> = MutableLiveData()
    val worlds: ArrayList<WorldItem> = arrayListOf()
    val loading: MutableLiveData<Boolean> = MutableLiveData(false)
}