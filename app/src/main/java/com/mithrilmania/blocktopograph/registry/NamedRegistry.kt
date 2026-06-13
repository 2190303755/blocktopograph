package com.mithrilmania.blocktopograph.registry

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap

class NamedRegistry<T> {
    private val byId = ObjectArrayList<T>()
    private val toId = Reference2IntOpenHashMap<T>()
    private val byName = Object2ObjectOpenHashMap<String, T>()

    init {
        this.toId.defaultReturnValue(-1)
    }

    operator fun set(identifier: String, value: T) {
        require(!this.byName.containsKey(identifier))
        require(!this.toId.containsKey(value))
        this.byName[identifier] = value
        val index = this.byId.size
        this.byId.add(index, value)
        this.toId.put(value, index)
    }

    operator fun get(identifier: String): T? = this.byName[identifier]

    operator fun get(runtimeId: Int): T? = this.byId.getOrNull(runtimeId)

    operator fun get(value: T): Int = this.toId.getInt(value)
}