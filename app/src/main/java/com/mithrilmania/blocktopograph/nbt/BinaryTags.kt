package com.mithrilmania.blocktopograph.nbt

import com.mithrilmania.blocktopograph.BYTE_0
import com.mithrilmania.blocktopograph.BYTE_1
import com.mithrilmania.blocktopograph.nbt.util.NBTStackOverflowException
import com.mithrilmania.blocktopograph.nbt.util.SNBTParser
import com.mithrilmania.blocktopograph.nbt.util.TagVisitor
import java.io.DataOutput

const val MAX_STACK_DEPTH = 512

fun Int.increaseDepthOrThrow(): Int {
    val depth = this + 1
    if (depth < MAX_STACK_DEPTH) return depth
    throw NBTStackOverflowException("Tried to read NBT tag with too high complexity, depth > $MAX_STACK_DEPTH")
}

fun String.parseSNBT() = SNBTParser(this).readValue()

fun String.parseCompound() = SNBTParser(this).apply {
    expect('{')
}.readCompound()

fun String.parseNamedTag(): Pair<String, BinaryTag<*>> {
    val parser = SNBTParser(this)
    try {
        val key = parser.readString()
        parser.expect(':')
        return Pair(key, parser.readValue())
    } catch (_: Exception) {
    }
    parser.reset()
    return Pair("", parser.readValue())
}

fun String.toBinaryTag() = if (this.isEmpty()) STRING_TAG_EMPTY else StringTag(this)
fun Boolean.toBinaryTag() = if (this) BYTE_TAG_ONE else BYTE_TAG_ZERO
fun Byte.toBinaryTag() = when (this) {
    BYTE_0 -> BYTE_TAG_ZERO
    BYTE_1 -> BYTE_TAG_ONE
    else -> ByteTag(this)
}
fun Int.toBinaryTag() = IntTag(this)
fun Long.toBinaryTag() = LongTag(this)

val STRING_TAG_EMPTY = StringTag("")
val BYTE_TAG_ZERO = ByteTag(0)
val BYTE_TAG_ONE = ByteTag(1)

sealed interface BinaryTag<T> {
    val type: TagType<*>
    val value: T
    fun copy(): BinaryTag<T>
    fun write(output: DataOutput)
    fun accept(visitor: TagVisitor)
}

sealed interface NumericTag<T : Number> : BinaryTag<T> {
    fun getAsByte(): Byte
    fun getAsShort(): Short
    fun getAsInt(): Int
    fun getAsLong(): Long
    fun getAsFloat(): Float
    fun getAsDouble(): Double
}

sealed interface CollectionTag<T : BinaryTag<*>> : MutableList<T>, BinaryTag<MutableList<T>> {
    val elementType: Int
    fun setTag(index: Int, tag: BinaryTag<*>): Boolean
    fun addTag(index: Int, tag: BinaryTag<*>): Boolean
}

sealed class ArrayTag<T : BinaryTag<out Number>> : AbstractMutableList<T>(), CollectionTag<T> {
    override fun add(index: Int, element: T) {
        if (!this.addTag(index, element)) throw UnsupportedOperationException(
            "Trying to add tag of type %d to array of %d".format(
                element.type.id,
                this.elementType
            )
        )
    }

    override fun set(index: Int, element: T): T {
        val tag = this[index]
        if (this.setTag(index, element)) return tag
        throw UnsupportedOperationException(
            "Trying to replace tag in array of %d with one of type %d".format(
                this.elementType,
                element.type.id
            )
        )
    }
}

object EndTag : BinaryTag<EndTag> {
    override val type get() = EndTagType
    override val value get() = this
    override fun copy() = this
    override fun write(output: DataOutput) {}
    override fun accept(visitor: TagVisitor) {}
}