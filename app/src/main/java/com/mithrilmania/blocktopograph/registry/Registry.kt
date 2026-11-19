package com.mithrilmania.blocktopograph.registry

import androidx.compose.ui.util.fastForEachIndexed
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap

fun <T : Any> RegistryFactory(builtin: List<T>): RegistryFactory<T> {
    val size = builtin.size
    val toId = Reference2IntOpenHashMap<T>(size)
    toId.defaultReturnValue(-1)
    builtin.fastForEachIndexed { numericId, entry ->
        toId.put(entry, numericId)
    }
    return RegistryFactory(builtin, toId)
}

class RegistryFactory<T : Any> internal constructor(
    private val byId: List<T>,
    private val toId: Reference2IntOpenHashMap<T>
) {
    fun create() = Registry(this.byId, this.toId)
}

open class Registry<T : Any> internal constructor(
    @JvmField val builtin: List<T>,
    private val builtinToId: Reference2IntOpenHashMap<T>
) {
    private val dynamicById: ObjectArrayList<T> = ObjectArrayList()
    private val dynamicToId: Reference2IntOpenHashMap<T> = Reference2IntOpenHashMap<T>().apply {
        defaultReturnValue(-1)
    }

    protected open fun onRegister(entry: T) {}

    fun register(entry: T): Int {
        var runtimeId = this.builtinToId.getInt(entry)
        if (runtimeId < 0) {
            val registry = this.dynamicToId
            synchronized(registry) {
                runtimeId = registry.getInt(entry)
                if (runtimeId < 0) {
                    this.onRegister(entry)
                    runtimeId = registry.size + this.builtin.size
                    registry[entry] = runtimeId
                    this.dynamicById.add(entry)
                }
            }
        }
        return runtimeId
    }

    operator fun get(entry: T): Int {
        val runtimeId = this.builtinToId.getInt(entry)
        if (runtimeId < 0) {
            val registry = this.dynamicToId
            synchronized(registry) {
                return registry.getInt(entry)
            }
        }
        return runtimeId
    }

    operator fun get(runtimeId: Int): T? {
        val builtin = this.builtin
        if (runtimeId in builtin.indices) return builtin[runtimeId]
        return synchronized(this.dynamicToId) {
            this.dynamicById.getOrNull(runtimeId - builtin.size)
        }
    }
}

fun <T : Any> Registry<T>.getOrAssignId(value: T): Int {
    val runtimeId = this[value]
    return if (runtimeId < 0) this.register(value) else runtimeId
}
