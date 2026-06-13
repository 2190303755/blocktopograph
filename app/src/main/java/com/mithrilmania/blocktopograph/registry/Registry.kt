package com.mithrilmania.blocktopograph.registry

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap

class Registry<T> {
    private val byId = ObjectArrayList<T>()
    private val toId = Reference2IntOpenHashMap<T>()

    init {
        this.toId.defaultReturnValue(-1)
    }

    fun register(value: T): Int {
        require(!this.toId.containsKey(value))
        val index = this.byId.size
        this.byId.add(index, value)
        this.toId.put(value, index)
        return index
    }

    operator fun get(runtimeId: Int): T? = this.byId.getOrNull(runtimeId)

    operator fun get(value: T): Int = this.toId.getInt(value)
}