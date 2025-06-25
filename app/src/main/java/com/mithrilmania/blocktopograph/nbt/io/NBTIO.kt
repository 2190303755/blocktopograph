package com.mithrilmania.blocktopograph.nbt.io

import com.google.common.io.LineReader
import com.mithrilmania.blocktopograph.GZIP_HEADER
import com.mithrilmania.blocktopograph.nbt.BinaryTag
import com.mithrilmania.blocktopograph.nbt.ByteTag
import com.mithrilmania.blocktopograph.nbt.CompoundTag
import com.mithrilmania.blocktopograph.nbt.DoubleTag
import com.mithrilmania.blocktopograph.nbt.EndTag
import com.mithrilmania.blocktopograph.nbt.FloatTag
import com.mithrilmania.blocktopograph.nbt.IntTag
import com.mithrilmania.blocktopograph.nbt.LongTag
import com.mithrilmania.blocktopograph.nbt.ShortTag
import com.mithrilmania.blocktopograph.nbt.TAG_BYTE
import com.mithrilmania.blocktopograph.nbt.TAG_BYTE_ARRAY
import com.mithrilmania.blocktopograph.nbt.TAG_COMPOUND
import com.mithrilmania.blocktopograph.nbt.TAG_DOUBLE
import com.mithrilmania.blocktopograph.nbt.TAG_END
import com.mithrilmania.blocktopograph.nbt.TAG_FLOAT
import com.mithrilmania.blocktopograph.nbt.TAG_INT
import com.mithrilmania.blocktopograph.nbt.TAG_INT_ARRAY
import com.mithrilmania.blocktopograph.nbt.TAG_LIST
import com.mithrilmania.blocktopograph.nbt.TAG_LONG
import com.mithrilmania.blocktopograph.nbt.TAG_LONG_ARRAY
import com.mithrilmania.blocktopograph.nbt.TAG_SHORT
import com.mithrilmania.blocktopograph.nbt.TAG_STRING
import com.mithrilmania.blocktopograph.nbt.asTagType
import com.mithrilmania.blocktopograph.nbt.increaseDepthOrThrow
import com.mithrilmania.blocktopograph.nbt.parseSNBT
import com.mithrilmania.blocktopograph.nbt.util.NBTFormatException
import java.io.ByteArrayInputStream
import java.io.DataInput
import java.io.DataOutput
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.nio.ByteOrder
import java.util.zip.GZIPInputStream

fun interface NBTOutput {
    fun save(name: String, tag: BinaryTag<*>)
}

interface NBTOutputFactory {
    fun createOutput(stream: OutputStream): NBTOutput
}

inline fun DataInput.readBinaryTags(action: (Int) -> Boolean) {
    while (action(this.readByte().toInt())) continue
}

fun DataInput.readBinaryTag(): BinaryTag<*> {
    val type = this.readByte().toInt()
    if (type == 0) return EndTag
    this.skipString()
    return type.asTagType().read(this, 0)
}

fun DataInput.readNamedTag(): Pair<String, BinaryTag<*>> {
    val type = this.readByte().toInt()
    return if (type == 0) ("" to EndTag) else
        (this.readUTF() to type.asTagType().read(this, 0))
}

fun DataInput.skipBinaryTags(depth: Int = 0) {
    val type = this.readByte().toInt()
    when (type) {
        TAG_END -> {}
        TAG_BYTE -> this.skipBytes(ByteTag.SIZE * this.readInt())
        TAG_SHORT -> this.skipBytes(ShortTag.SIZE * this.readInt())
        TAG_INT -> this.skipBytes(IntTag.SIZE * this.readInt())
        TAG_LONG -> this.skipBytes(LongTag.SIZE * this.readInt())
        TAG_FLOAT -> this.skipBytes(FloatTag.SIZE * this.readInt())
        TAG_DOUBLE -> this.skipBytes(DoubleTag.SIZE * this.readInt())
        TAG_BYTE_ARRAY -> repeat(this.readInt()) { this.skipBytes(this.readInt() * ByteTag.SIZE) }
        TAG_STRING -> repeat(this.readInt()) { this.skipString() }
        TAG_LIST -> depth.increaseDepthOrThrow().let {
            repeat(this.readInt()) { this.skipBinaryTags(it) }
        }

        TAG_COMPOUND -> depth.increaseDepthOrThrow().let {
            repeat(this.readInt()) { this.skipNamedTags(it) }
        }

        TAG_INT_ARRAY -> repeat(this.readInt()) { this.skipBytes(this.readInt() * IntTag.SIZE) }
        TAG_LONG_ARRAY -> repeat(this.readInt()) { this.skipBytes(this.readInt() * LongTag.SIZE) }
        else -> throw NBTFormatException("Invalid tag type: $type")
    }
}

fun DataInput.skipBinaryTag(type: Int, depth: Int = 0) {
    when (type) {
        TAG_END -> {}
        TAG_BYTE -> this.skipBytes(ByteTag.SIZE)
        TAG_SHORT -> this.skipBytes(ShortTag.SIZE)
        TAG_INT -> this.skipBytes(IntTag.SIZE)
        TAG_LONG -> this.skipBytes(LongTag.SIZE)
        TAG_FLOAT -> this.skipBytes(FloatTag.SIZE)
        TAG_DOUBLE -> this.skipBytes(DoubleTag.SIZE)
        TAG_BYTE_ARRAY -> this.skipBytes(this.readInt() * ByteTag.SIZE)
        TAG_STRING -> this.skipString()
        TAG_LIST -> this.skipBinaryTags(depth)
        TAG_COMPOUND -> this.skipNamedTags(depth)
        TAG_INT_ARRAY -> this.skipBytes(this.readInt() * IntTag.SIZE)
        TAG_LONG_ARRAY -> this.skipBytes(this.readInt() * LongTag.SIZE)
        else -> throw NBTFormatException("Invalid tag type: $type")
    }
}

fun DataInput.skipNamedTags(depth: Int = 0): Boolean {
    val child = depth.increaseDepthOrThrow()
    this.readBinaryTags loop@{
        if (it == 0) return@loop false
        this.skipString()
        this.skipBinaryTag(it, child)
        return@loop true
    }
    return false
}

fun DataInput.skipString() {
    this.skipBytes(this.readUnsignedShort())
}

inline fun <reified T : BinaryTag<*>> DataOutput.writeEntry(name: String, tag: T) {
    val id = tag.type.id
    this.writeByte(id)
    if (id == 0) return
    this.writeUTF(name)
    tag.write(this)
}

inline fun runSilent(action: () -> Unit) {
    try {
        action()
    } catch (_: Exception) {
    }
}

fun InputStream.readUnknownNBT(): NBTResult {
    runSilent {
        val bytes = this.readBytes()
        // let's check if it starts with '{'
        val reader = LineReader(InputStreamReader(ByteArrayInputStream(bytes), Charsets.UTF_8))
        var flag = false
        var line: String?
        do {
            line = reader.readLine()
            if (line === null) break
            val content = line.trimStart()
            if (content.startsWith('{')) {
                // it is probably SNBT
                flag = true
                break
            }
            if (content.isNotBlank()) break
        } while (true)
        if (flag) runSilent {
            val tag = String(bytes, Charsets.UTF_8).parseSNBT()
            if (tag is CompoundTag) return SNBTResult(tag)
        }
        // let's check if contains something like header
        if (bytes.size > 8 && bytes.size == 8 + ((bytes[4].toInt() and 0xFF)
                    or (bytes[5].toInt() shl 8)
                    or (bytes[6].toInt() shl 16)
                    or (bytes[7].toInt() shl 24))
        ) runSilent {
            val pair = NBTInputBuffer(
                ByteArrayInputStream(bytes, 8, bytes.size - 8),
                ByteOrder.LITTLE_ENDIAN
            ).use {
                it.readNamedTag()
            }
            return NamedResult(
                pair.first,
                pair.second,
                false,
                (bytes[0].toUInt()
                        or (bytes[1].toUInt() shl 8)
                        or (bytes[2].toUInt() shl 16)
                        or (bytes[3].toUInt() shl 24))
            )
        }
        val compressed = GZIP_HEADER == bytes[0]
        runSilent { return bytes.readNamedTag(true, compressed) }
        runSilent { return bytes.readNamedTag(false, compressed) }
    }
    return NamedResult("", CompoundTag())
}

fun ByteArray.readNamedTag(
    littleEndian: Boolean,
    compressed: Boolean
): NamedResult {
    val stream = ByteArrayInputStream(this)
    val pair = NBTInputBuffer(
        if (compressed) GZIPInputStream(stream) else stream,
        if (littleEndian) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN
    ).use { it.readNamedTag() }
    return NamedResult(pair.first, pair.second, compressed, null, littleEndian)
}