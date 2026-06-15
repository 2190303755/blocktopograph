package com.mithrilmania.blocktopograph.registry

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import java.util.concurrent.ConcurrentHashMap

class NamedRegistry<T : Any> {
    private val byId = ObjectArrayList<T>()
    private val toId = Object2IntOpenHashMap<T>()
    private val byName = ConcurrentHashMap<String, T>()

    init {
        this.toId.defaultReturnValue(-1)
    }

    @Synchronized
    operator fun set(identifier: String, value: T) {
        val old = this.byName.putIfAbsent(identifier, value)
        require(old === null || old == value)
        this.byName[identifier] = value
        val index = this.byId.size
        this.byId.add(value)
        this.toId.put(value, index)
    }

    operator fun get(identifier: String): T? = this.byName[identifier]

    operator fun get(runtimeId: Int): T? = this.byId.getOrNull(runtimeId)

    @Synchronized
    operator fun get(value: T): Int = this.toId.getInt(value)
}