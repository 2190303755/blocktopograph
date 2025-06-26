package com.mithrilmania.blocktopograph.nbt

import com.mithrilmania.blocktopograph.nbt.util.NBTFormatException
import com.mithrilmania.blocktopograph.nbt.util.TagVisitor
import java.io.DataInput
import java.io.DataOutput


class ListTag private constructor(
    type: Int,
    val delegate: MutableList<BinaryTag<*>>
) : CollectionTag<BinaryTag<*>>, MutableList<BinaryTag<*>> by delegate {
    constructor() : this(0, ArrayList())

    override val type get() = Type
    override val value get() = this
    override var elementType = type
        private set

    override fun accept(visitor: TagVisitor) = visitor.visit(this)
    override fun copy() = ListTag(
        this.elementType,
        if (this.elementType.isImmutableTagType())
            ArrayList(this)
        else
            this.mapTo(ArrayList(this.size), BinaryTag<*>::copy)
    )

    override fun write(output: DataOutput) {
        this.elementType = if (this.isEmpty()) 0 else this[0].type.id.toInt()
        output.writeByte(this.elementType)
        output.writeInt(this.size)
        this.forEach {
            it.write(output)
        }
    }

    override fun setTag(index: Int, tag: BinaryTag<*>): Boolean {
        if (this.canAccept(tag)) {
            this.delegate[index] = tag
            return true
        }
        return false
    }

    override fun addTag(index: Int, tag: BinaryTag<*>): Boolean {
        if (this.canAccept(tag)) {
            this.delegate.add(index, tag)
            return true
        }
        return false
    }

    override fun set(index: Int, element: BinaryTag<*>): BinaryTag<*> {
        val tag = this[index]
        if (this.setTag(index, element)) return tag
        throw UnsupportedOperationException(
            "Trying to replace tag in list of %d with one of type %d".format(
                this.elementType,
                element.type.id
            )
        )
    }

    override fun add(element: BinaryTag<*>): Boolean {
        if (this.canAccept(element)) {
            this.delegate.add(element)
            return true
        }
        return false
    }

    override fun add(index: Int, element: BinaryTag<*>) {
        if (!this.addTag(index, element)) throw UnsupportedOperationException(
            "Trying to add tag of type %d to list of %d".format(
                element.type.id,
                this.elementType
            )
        )
    }

    override fun removeAt(index: Int): BinaryTag<*> {
        val tag = this.delegate.removeAt(index)
        if (this.isEmpty()) {
            this.elementType = 0
        }
        return tag
    }

    override fun clear() {
        this.delegate.clear()
        this.elementType = 0
    }

    private fun canAccept(tag: BinaryTag<*>): Boolean {
        val type = tag.type.id
        if (type == 0) return false
        if (this.elementType != 0) return this.elementType == type
        this.elementType = type
        return true
    }

    companion object Type : TagType<ListTag> {
        override val id get() = TAG_LIST
        override fun toString() = "TAG_List"
        override fun read(input: DataInput, depth: Int): ListTag {
            val child = depth.increaseDepthOrThrow()
            val id = input.readByte().toInt()
            val length = input.readInt()
            if (id == 0 && length > 0) throw NBTFormatException("Missing type on ListTag")
            val type = id.asTagType()
            val list = ArrayList<BinaryTag<*>>(length)
            repeat(length) {
                list += type.read(input, child)
            }
            return ListTag(id, list)
        }
    }
}