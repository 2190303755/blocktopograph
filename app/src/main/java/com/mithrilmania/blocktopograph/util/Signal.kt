package com.mithrilmania.blocktopograph.util

import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
class Signal<E>(
    private val flow: MutableSharedFlow<E> = MutableSharedFlow(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
) : SharedFlow<E> by flow {
    suspend fun emit(event: E) {
        this.flow.emit(event)
    }

    fun tryEmit(event: E) = this.flow.tryEmit(event)
}
