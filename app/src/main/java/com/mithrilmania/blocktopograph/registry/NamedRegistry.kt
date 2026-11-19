package com.mithrilmania.blocktopograph.registry

import androidx.compose.ui.util.fastForEachIndexed
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap
import java.util.concurrent.ConcurrentHashMap

fun <T : NamedEntry> NamedRegistryFactory(builtin: List<T>): NamedRegistryFactory<T> {
    val size = builtin.size
    val toId = Reference2IntOpenHashMap<T>(size)
    val byName = Object2ObjectOpenHashMap<String, T>(builtin.size)
    toId.defaultReturnValue(-1)
    builtin.fastForEachIndexed { numericId, entry ->
        toId.put(entry, numericId)
        byName[entry.identifier] = entry
    }
    return NamedRegistryFactory(builtin, toId, byName)
}

class NamedRegistryFactory<T : NamedEntry> internal constructor(
    private val byId: List<T>,
    private val toId: Reference2IntOpenHashMap<T>,
    private val byName: Object2ObjectOpenHashMap<String, T>
) {
    fun create() = NamedRegistry(this.byId, this.toId, this.byName)
}


class NamedRegistry<T : NamedEntry> internal constructor(
    builtin: List<T>,
    builtinToId: Reference2IntOpenHashMap<T>,
    private val builtinByName: Object2ObjectOpenHashMap<String, T>
) : Registry<T>(builtin, builtinToId) {
    private val dynamicByName: ConcurrentHashMap<String, T> = ConcurrentHashMap()

    override fun onRegister(entry: T) {
        val old = this.dynamicByName.putIfAbsent(entry.identifier, entry)
        require(old === null || old == entry)
    }

    operator fun get(identifier: String): T? {
        return this.builtinByName[identifier] ?: this.dynamicByName[identifier]
    }
}