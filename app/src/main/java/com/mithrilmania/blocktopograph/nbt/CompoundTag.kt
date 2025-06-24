package com.mithrilmania.blocktopograph.nbt

import com.mithrilmania.blocktopograph.nbt.io.readBinaryTags
import com.mithrilmania.blocktopograph.nbt.io.writeEntry
import com.mithrilmania.blocktopograph.nbt.util.TagVisitor
import java.io.DataInput
import java.io.DataOutput


class CompoundTag(
    tags: MutableMap<String, BinaryTag<*>>
) : BinaryTag<CompoundTag>, MutableMap<String, BinaryTag<*>> by tags {
    constructor() : this(HashMap())

    override val type get() = Type
    override val value get() = this
    override fun accept(visitor: TagVisitor) = visitor.visit(this)
    override fun copy() = CompoundTag(
        this.mapValuesTo(
            HashMap(((this.size / 0.75F) + 1.0F).toInt())
        ) { (_, tag) -> tag.copy() }
    )

    override fun write(output: DataOutput) {
        this.forEach { (name, tag) -> output.writeEntry(name, tag) }
        output.writeByte(TAG_END)
    }

    companion object Type : TagType<CompoundTag> {
        override val id get() = TAG_COMPOUND
        override fun toString() = "TAG_Compound"
        override fun read(input: DataInput, depth: Int): CompoundTag {
            val child = depth.increaseDepthOrThrow()
            val tags = HashMap<String, BinaryTag<*>>()
            input.readBinaryTags loop@{
                if (it == 0) return@loop false
                tags[input.readUTF()] = it.asTagType().read(input, child)
                return@loop true
            }
            return CompoundTag(tags)
        }
    }
}