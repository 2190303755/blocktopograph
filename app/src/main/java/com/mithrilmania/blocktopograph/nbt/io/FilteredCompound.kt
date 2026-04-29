package com.mithrilmania.blocktopograph.nbt.io

import com.mithrilmania.blocktopograph.nbt.CompoundTag
import com.mithrilmania.blocktopograph.nbt.TAG_END
import com.mithrilmania.blocktopograph.nbt.TagType
import java.io.DataInput

typealias TagFilters = MutableMap<String, TagReader<*>>

class FilteredCompound(
    val filters: Array<MutableMap<String, TagReader<*>>?>
) : TagReader<CompoundTag> {
    override fun read(input: DataInput, depth: Int): CompoundTag {
        val compound = CompoundTag()
        val filters = this.filters
        if (filters.all { it === null }) return compound
        val child = depth.increaseDepthOrThrow()
        input.readBinaryTags loop@{ type ->
            if (type == TAG_END) return@loop false
            val candidates = filters.getOrNull(type.toInt())
            if (candidates !== null) {
                val key = input.readUTF()
                val reader = candidates[key]
                if (reader !== null) {
                    compound[key] = reader.read(input, child)
                    candidates.remove(key)
                    if (candidates.isEmpty()) {
                        filters[type.toInt()] = null
                        if (filters.all { it === null }) {
                            input.skipNamedTags(child)
                            return@loop false
                        }
                    }
                    return@loop true
                }
            } else {
                input.skipString()
            }
            input.skipBinaryTag(type)
            return@loop true
        }
        return compound
    }
}

inline fun FilteredCompound(init: Array<TagFilters?>.() -> Unit): FilteredCompound {
    val filters = arrayOfNulls<TagFilters>(13)
    filters.init()
    return FilteredCompound(filters)
}

fun Array<TagFilters?>.putSimpleFilter(type: TagType<*>, vararg keys: String) {
    val readers = HashMap<String, TagReader<*>>()
    keys.forEach {
        readers[it] = type
    }
    this[type.typeId.toInt()] = readers
}