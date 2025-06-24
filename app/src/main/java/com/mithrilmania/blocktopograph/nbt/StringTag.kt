package com.mithrilmania.blocktopograph.nbt

import com.mithrilmania.blocktopograph.nbt.util.TagVisitor
import java.io.DataInput
import java.io.DataOutput

@JvmInline
value class StringTag private constructor(override val value: String) : BinaryTag<String> {
    override val type get() = Type
    override fun accept(visitor: TagVisitor) = visitor.visit(this)
    override fun copy() = this
    override fun write(output: DataOutput) {
        output.writeUTF(this.value)
    }

    companion object Type : TagType<StringTag> {
        val EMPTY = StringTag("")
        override val id get() = TAG_STRING
        override fun toString() = "TAG_String"
        override fun read(input: DataInput, depth: Int) = StringTag(input.readUTF())

        @JvmStatic
        fun of(value: String) = if (value.isEmpty()) EMPTY else StringTag(value)
    }
}