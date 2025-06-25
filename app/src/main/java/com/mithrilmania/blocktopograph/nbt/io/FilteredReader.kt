package com.mithrilmania.blocktopograph.nbt.io

import android.util.SparseArray
import androidx.core.util.isEmpty
import com.mithrilmania.blocktopograph.nbt.CompoundTag
import com.mithrilmania.blocktopograph.nbt.TagType
import com.mithrilmania.blocktopograph.nbt.increaseDepthOrThrow
import java.io.DataInput

typealias EntryReaders = MutableMap<String, TagReader<*>>

class FilteredReader(
    private val readers: SparseArray<EntryReaders>
) : TagReader<CompoundTag> {
    override fun read(input: DataInput, depth: Int): CompoundTag {
        val compound = CompoundTag()
        val readers = this.readers
        if (readers.isEmpty()) return compound
        val child = depth.increaseDepthOrThrow()
        input.readBinaryTags loop@{ type ->
            if (type == 0) return@loop false
            val candidates = readers[type]
            if (candidates !== null) {
                val key = input.readUTF()
                val reader = candidates[key]
                if (reader !== null) {
                    compound[key] = reader.read(input, child)
                    candidates.remove(key)
                    if (candidates.isEmpty()) {
                        readers.delete(type)
                        if (readers.isEmpty()) {
                            input.skipNamedTags(child)
                            return@loop false
                        }
                        return@loop true
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

fun <T : TagType<*>> SparseArray<EntryReaders>.putSimpleFilter(type: T, vararg keys: String) {
    val filters = HashMap<String, TagReader<*>>()
    for (key in keys) {
        filters[key] = type
    }
    this[type.id] = filters
}