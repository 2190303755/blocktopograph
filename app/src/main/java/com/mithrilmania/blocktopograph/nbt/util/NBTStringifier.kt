package com.mithrilmania.blocktopograph.nbt.util

import com.mithrilmania.blocktopograph.nbt.ByteArrayTag
import com.mithrilmania.blocktopograph.nbt.ByteTag
import com.mithrilmania.blocktopograph.nbt.CompoundTag
import com.mithrilmania.blocktopograph.nbt.DoubleTag
import com.mithrilmania.blocktopograph.nbt.FloatTag
import com.mithrilmania.blocktopograph.nbt.IntArrayTag
import com.mithrilmania.blocktopograph.nbt.IntTag
import com.mithrilmania.blocktopograph.nbt.ListTag
import com.mithrilmania.blocktopograph.nbt.LongArrayTag
import com.mithrilmania.blocktopograph.nbt.LongTag
import com.mithrilmania.blocktopograph.nbt.ShortTag
import com.mithrilmania.blocktopograph.nbt.StringTag
import com.mithrilmania.blocktopograph.nbt.TAG_COMPOUND

class NBTStringifier(
    val indentation: Indentation = Indentation("    "),
    val heterogeneous: Boolean = false,
    val builder: StringBuilder = StringBuilder()
) : TagVisitor {
    override fun visit(tag: StringTag) {
        this.builder.appendQuoted(tag.value)
    }

    override fun visit(tag: ByteTag) {
        this.builder.append(tag.value).append('b')
    }

    override fun visit(tag: ShortTag) {
        this.builder.append(tag.value).append('s')
    }

    override fun visit(tag: IntTag) {
        this.builder.append(tag.value)
    }

    override fun visit(tag: LongTag) {
        this.builder.append(tag.value).append('L')
    }

    override fun visit(tag: FloatTag) {
        this.builder.append(tag.value).append('f')
    }

    override fun visit(tag: DoubleTag) {
        this.builder.append(tag.value).append('d')
    }

    override fun visit(tag: ByteArrayTag) {
        val builder = this.builder.append('[').append('B').append(';')
        tag.elements.forEach {
            builder.append(' ').append(it).append('B').append(',')
        }
        builder.setCharAt(builder.length - 1, ']')
    }

    override fun visit(tag: IntArrayTag) {
        val builder = this.builder.append('[').append('I').append(';')
        tag.elements.forEach {
            builder.append(' ').append(it).append(',')
        }
        builder.setCharAt(builder.length - 1, ']')
    }

    override fun visit(tag: LongArrayTag) {
        val builder = this.builder.append('[').append('L').append(';')
        tag.elements.forEach {
            builder.append(' ').append(it).append('L').append(',')
        }
        builder.setCharAt(builder.length - 1, ']')
    }

    override fun visit(tag: ListTag) {
        if (tag.isEmpty()) {
            this.builder.append('[').append(']')
            return
        }
        val builder = this.builder.append('[')
        if (this.heterogeneous || tag.elementTypeId != TAG_COMPOUND) {
            builder.append(tag, this.indentation) {
                it.accept(this)
            }
        } else {
            builder.append(tag, this.indentation) {
                it.boxed().accept(this)
            }
        }
        builder.append(']')
    }

    override fun visit(tag: CompoundTag) {
        if (tag.isEmpty()) {
            this.builder.append('{').append('}')
            return
        }
        val builder = this.builder.append('{')
        builder.append(tag.keys.sorted(), this.indentation) {
            builder.appendSafeLiteral(it)
                .append(':')
                .append(' ')
            tag[it]!!.accept(this)
        }
        builder.append('}')
    }

    override fun toString() = this.builder.toString()
}