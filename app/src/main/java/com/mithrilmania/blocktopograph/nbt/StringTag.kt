package com.mithrilmania.blocktopograph.nbt

import com.mithrilmania.blocktopograph.nbt.util.TagVisitor
import com.mithrilmania.blocktopograph.nbt.util.extracted
import java.io.DataInput
import java.io.DataOutput
import java.util.stream.IntStream

@JvmInline
value class StringTag(
    @JvmField val value: String
) : CharSequence by value, PrimitiveTag {
    override val type: Type get() = Type
    override fun chars(): IntStream = this.value.chars()
    override fun codePoints(): IntStream = this.value.codePoints()
    override fun toString(): String = this.value
    override fun write(output: DataOutput) {
        output.writeUTF(this.value)
    }

    override fun accept(visitor: TagVisitor) {
        visitor.visit(this)
    }

    companion object Type : TagType<StringTag> {
        override val typeId get() = TAG_STRING
        override fun toString() = "TAG_String"
        override fun read(input: DataInput, depth: Int) = StringTag(input.readUTF())
        override fun transform(tag: BinaryTag): StringTag = when (val content = tag.extracted()) {
            is NumericTag -> StringTag(content.toNumber().toString())
            is StringTag -> content
            else -> StringTag("")
        }
    }
}