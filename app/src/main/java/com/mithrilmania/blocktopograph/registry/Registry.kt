package com.mithrilmania.blocktopograph.registry

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList

class Registry<T : Any> {
    private val byId = ObjectArrayList<T>()
    private val toId = Object2IntOpenHashMap<T>()

    init {
        this.toId.defaultReturnValue(-1)
    }

    @Synchronized
    fun register(value: T): Int {
        var index = this.toId.getInt(value)
        if (index < 0) {
            index = this.byId.size
            this.byId.add(value)
            this.toId.put(value, index)
        }
        return index
    }

    operator fun get(runtimeId: Int): T? = this.byId.getOrNull(runtimeId)

    @Synchronized
    operator fun get(value: T): Int = this.toId.getInt(value)
}