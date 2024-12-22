package com.mithrilmania.blocktopograph.world

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.mithrilmania.blocktopograph.worldlist.WorldItemAdapter

interface IWorldLoader<T> {
    suspend fun loadWorlds(
        adapter: WorldItemAdapter,
        context: Context,
        location: T,
        state: MutableLiveData<Boolean>,
        tag: String = ""
    )
}