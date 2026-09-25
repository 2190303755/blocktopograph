package com.mithrilmania.blocktopograph.util

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow

class Signal {
    private val flow = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    suspend fun collect(collector: FlowCollector<Unit>) {
        this.flow.collect(collector)
    }

    fun trigger() {
        this.flow.tryEmit(Unit)
    }
}