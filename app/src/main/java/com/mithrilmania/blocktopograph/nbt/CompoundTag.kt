package com.mithrilmania.blocktopograph.nbt

import com.mithrilmania.blocktopograph.nbt.io.readAsCompound
import com.mithrilmania.blocktopograph.nbt.io.writeNBT
import com.mithrilmania.blocktopograph.nbt.util.TagVisitor
import com.mithrilmania.blocktopograph.nbt.util.boxed
import java.io.DataInput
import java.io.DataOutput

@JvmInline
value class CompoundTag(
    @JvmField val tags: MutableMap<String, BinaryTag> = hashMapOf()
) : BinaryTag, MutableMap<String, BinaryTag> by tags {
    override val type: Type get() = Type
    override fun copy(): CompoundTag = CompoundTag(
        this.mapValuesTo(HashMap()) { it.value.copy() }
    )

    override fun write(output: DataOutput) {
        this.forEach {
            output.writeNBT(it.key, it.value)
        }
        output.writeByte(TAG_END.toInt())
    }

    override fun accept(visitor: TagVisitor) {
        visitor.visit(this)
    }

    inline fun forEachSorted(action: (BinaryTag) -> Unit) {
        this.keys.sorted().forEach {
            action(this[it]!!)
        }
    }

    companion object Type : TagType<CompoundTag> {
        override val typeId get() = TAG_COMPOUND
        override fun toString() = "TAG_Compound"
        override fun read(
            input: DataInput,
            depth: Int
        ): CompoundTag = CompoundTag(input.readAsCompound(depth = depth))

        override fun transform(tag: BinaryTag) = when (tag) {
            EndTag -> CompoundTag()
            is ByteArrayTag -> {
                val tags = hashMapOf<String, BinaryTag>()
                tag.elements.forEachIndexed { index, tag ->
                    tags[index.toString()] = ByteTag(tag)
                }
                CompoundTag(tags)
            }

            is IntArrayTag -> {
                val tags = hashMapOf<String, BinaryTag>()
                tag.elements.forEachIndexed { index, tag ->
                    tags[index.toString()] = IntTag(tag)
                }
                CompoundTag(tags)
            }

            is LongArrayTag -> {
                val tags = hashMapOf<String, BinaryTag>()
                tag.elements.forEachIndexed { index, tag ->
                    tags[index.toString()] = LongTag(tag)
                }
                CompoundTag(tags)
            }

            is ListTag -> {
                val tags = hashMapOf<String, BinaryTag>()
                tag.forEachIndexed { index, tag ->
                    tags[index.toString()] = tag
                }
                CompoundTag(tags)
            }

            else -> tag.boxed()
        }
    }
}