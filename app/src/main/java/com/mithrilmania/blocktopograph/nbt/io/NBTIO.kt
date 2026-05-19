package com.mithrilmania.blocktopograph.nbt.io

import com.google.common.primitives.Ints
import com.mithrilmania.blocktopograph.nbt.BinaryTag
import com.mithrilmania.blocktopograph.nbt.ByteTag
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
import com.mithrilmania.blocktopograph.nbt.toTagType
import com.mithrilmania.blocktopograph.nbt.util.Indentation
import com.mithrilmania.blocktopograph.nbt.util.NBTFormatException
import com.mithrilmania.blocktopograph.nbt.util.NBTStackOverflowException
import com.mithrilmania.blocktopograph.nbt.util.NBTStringifier
import com.mithrilmania.blocktopograph.nbt.util.SNBTParser
import com.mithrilmania.blocktopograph.nbt.util.parseSNBT
import com.mithrilmania.blocktopograph.util.autoDecompress
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInput
import java.io.DataOutput
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

const val MAX_STACK_DEPTH = 512

fun Int.increaseDepthOrThrow(): Int {
    val depth = this + 1
    if (depth < MAX_STACK_DEPTH) return depth
    throw NBTStackOverflowException("Tried to read NBT tag with too high complexity, depth > $MAX_STACK_DEPTH")
}

interface NBTExportConfig {
    val stringify: Boolean
    val prettify: Boolean
    val heterogeneous: Boolean
    val compressed: Boolean
    val littleEndian: Boolean
    val storageVersion: UInt?
}

interface NBTImportConfig {
    val format: NBTFormat
    val header: HeaderPresence
}

data class NBTImportConfigImpl(
    override val format: NBTFormat = NBTFormat.UNKNOWN,
    override val header: HeaderPresence = HeaderPresence.UNCERTAIN
) : NBTImportConfig

fun InputStream.readNBT(config: NBTImportConfig): TagWithMeta? {
    val wrapped = this.autoDecompress()
    when (config.format) {
        NBTFormat.STRINGIFIED -> {
            val pair = SNBTParser(SNBTStreamReader(wrapped)).parseRoot()
            return TagWithMeta(
                pair.second,
                pair.first,
                wrapped is GZIPInputStream,
                null,
                littleEndian = false,
                stringified = true
            )
        }

        NBTFormat.BIG_ENDIAN -> {
            val pair = JavaNBTInput(wrapped).readNamedTag()
            return TagWithMeta(
                pair.second,
                pair.first,
                wrapped is GZIPInputStream,
                null,
                littleEndian = false
            )
        }

        NBTFormat.LITTLE_ENDIAN -> {
            when (config.header) {
                HeaderPresence.ABSENT -> {
                    val pair = BedrockNBTInput(wrapped).readNamedTag()
                    return TagWithMeta(
                        pair.second,
                        pair.first,
                        wrapped is GZIPInputStream,
                        null,
                        littleEndian = true
                    )
                }

                HeaderPresence.PRESENT -> {
                    val input = BedrockNBTInput(wrapped)
                    val version = input.readInt()
                    input.skipBytes(4)
                    val pair = input.readNamedTag()
                    return TagWithMeta(
                        pair.second,
                        pair.first,
                        wrapped is GZIPInputStream,
                        version.toUInt(),
                        littleEndian = true
                    )
                }

                else -> {
                    val bytes = wrapped.readBytes()
                    if (bytes.size > 8 && bytes.size == 8 + Ints.fromBytes(
                            bytes[7],
                            bytes[6],
                            bytes[5],
                            bytes[4]
                        )
                    ) {
                        val pair = BedrockNBTInput(
                            ByteArrayInputStream(bytes, 8, bytes.size - 8)
                        ).readNamedTag()
                        return TagWithMeta(
                            pair.second,
                            pair.first,
                            wrapped is GZIPInputStream,
                            Ints.fromBytes(
                                bytes[3],
                                bytes[2],
                                bytes[1],
                                bytes[0]
                            ).toUInt(),
                            littleEndian = true
                        )
                    } else {
                        val pair = BedrockNBTInput(
                            ByteArrayInputStream(bytes)
                        ).readNamedTag()
                        return TagWithMeta(
                            pair.second,
                            pair.first,
                            wrapped is GZIPInputStream,
                            null,
                            littleEndian = true
                        )
                    }
                }
            }
        }

        NBTFormat.UNKNOWN -> {
            val bytes = wrapped.readBytes()
            if (config.header !== HeaderPresence.ABSENT) {
                if (bytes.size > 8 && bytes.size == 8 + Ints.fromBytes(
                        bytes[7],
                        bytes[6],
                        bytes[5],
                        bytes[4]
                    )
                ) runSuppressing {
                    val pair = BedrockNBTInput(
                        ByteArrayInputStream(bytes, 8, bytes.size - 8)
                    ).readNamedTag()
                    return TagWithMeta(
                        pair.second,
                        pair.first,
                        false,
                        Ints.fromBytes(
                            bytes[3],
                            bytes[2],
                            bytes[1],
                            bytes[0]
                        ).toUInt()
                    )
                }
            }
            runSuppressing {
                val pair = bytes.toString(Charsets.UTF_8).parseSNBT()
                if (pair !== null) return TagWithMeta(
                    pair.second,
                    pair.first,
                    false,
                    null,
                    littleEndian = false,
                    stringified = true
                )
            }
            runSuppressing {
                val pair = BedrockNBTInput(
                    ByteArrayInputStream(bytes)
                ).readNamedTag()
                return TagWithMeta(
                    pair.second,
                    pair.first,
                    wrapped is GZIPInputStream,
                    null,
                    littleEndian = true
                )
            }
            runSuppressing {
                val pair = JavaNBTInput(
                    ByteArrayInputStream(bytes)
                ).readNamedTag()
                return TagWithMeta(
                    pair.second,
                    pair.first,
                    wrapped is GZIPInputStream,
                    null,
                    littleEndian = false
                )
            }
        }
    }
    return null
}

fun OutputStream.writeNBT(name: String, tag: BinaryTag, config: NBTExportConfig) {
    if (config.stringify) {
        val builder = NBTStringifier(
            indentation = Indentation(config.prettify),
            heterogeneous = config.heterogeneous
        )
        tag.accept(builder)
        this.use {
            it.write(builder.toString().toByteArray(Charsets.UTF_8))
        }
        this.close()
    } else if (config.littleEndian) {
        if (config.compressed) {
            BedrockNBTOutput(GZIPOutputStream(this.buffered())).use {
                it.writeNBT(name, tag)
            }
        } else {
            val version = config.storageVersion
            if (version === null) {
                BedrockNBTOutput(this.buffered()).use {
                    it.writeNBT(name, tag)
                }
            } else {
                this.buffered().use {
                    val buffer = ByteArrayOutputStream()
                    BedrockNBTOutput(buffer).writeNBT(name, tag)
                    it.writeIntLE(version.toInt())
                    it.writeIntLE(buffer.size())
                    buffer.writeTo(it)
                }
            }
        }
    } else {
        JavaNBTOutput(
            if (config.compressed) GZIPOutputStream(this.buffered()) else this.buffered()
        ).use {
            it.writeNBT(name, tag)
        }
    }
}

fun OutputStream.writeIntLE(value: Int) {
    this.write(value ushr 0)
    this.write(value ushr 8)
    this.write(value ushr 16)
    this.write(value ushr 24)
}

fun OutputStream.writeNBTWithHeader(version: UInt, name: String, tag: BinaryTag) {
    this.buffered().use {
        val buffer = ByteArrayOutputStream()
        BedrockNBTOutput(buffer).writeNBT(name, tag)
        it.writeIntLE(version.toInt())
        it.writeIntLE(buffer.size())
        buffer.writeTo(it)
    }
}

inline fun DataInput.readBinaryTags(action: (Byte) -> Boolean) {
    while (action(this.readByte())) continue
}

fun DataInput.readAsCompound(
    destination: MutableMap<String, BinaryTag> = hashMapOf(),
    depth: Int = 0
): MutableMap<String, BinaryTag> {
    val child = depth.increaseDepthOrThrow()
    this.readBinaryTags loop@{
        if (it == TAG_END) return@loop false
        destination[this.readUTF()] = it.toTagType().read(this, child)
        return@loop true
    }
    return destination
}

fun DataInput.readBinaryTag(): BinaryTag {
    val type = this.readByte()
    if (type == TAG_END) return EndTag
    this.skipString()
    return type.toTagType().read(this, 0)
}

fun DataInput.readNamedTag(): Pair<String, BinaryTag> {
    val type = this.readByte()
    return if (type == TAG_END) {
        "" to EndTag
    } else {
        this.readUTF() to type.toTagType().read(this, 0)
    }
}

fun DataInput.skipBinaryTags(depth: Int = 0) {
    when (val type = this.readByte()) {
        TAG_END -> this.skipBytes(4)
        TAG_BYTE -> this.skipBytes(ByteTag.PAYLOAD_SIZE * this.readInt())
        TAG_SHORT -> this.skipBytes(ShortTag.PAYLOAD_SIZE * this.readInt())
        TAG_INT -> this.skipBytes(IntTag.PAYLOAD_SIZE * this.readInt())
        TAG_LONG -> this.skipBytes(LongTag.PAYLOAD_SIZE * this.readInt())
        TAG_FLOAT -> this.skipBytes(FloatTag.PAYLOAD_SIZE * this.readInt())
        TAG_DOUBLE -> this.skipBytes(DoubleTag.PAYLOAD_SIZE * this.readInt())
        TAG_BYTE_ARRAY -> repeat(this.readInt()) { this.skipBytes(this.readInt() * ByteTag.PAYLOAD_SIZE) }
        TAG_STRING -> repeat(this.readInt()) { this.skipString() }
        TAG_LIST -> depth.increaseDepthOrThrow().let { child ->
            repeat(this.readInt()) { this.skipBinaryTags(child) }
        }

        TAG_COMPOUND -> depth.increaseDepthOrThrow().let { child ->
            repeat(this.readInt()) { this.skipNamedTags(child) }
        }

        TAG_INT_ARRAY -> repeat(this.readInt()) { this.skipBytes(this.readInt() * IntTag.PAYLOAD_SIZE) }
        TAG_LONG_ARRAY -> repeat(this.readInt()) { this.skipBytes(this.readInt() * LongTag.PAYLOAD_SIZE) }
        else -> throw NBTFormatException("Invalid tag type: $type")
    }
}

fun DataInput.skipString() {
    this.skipBytes(this.readUnsignedShort())
}

fun DataInput.skipBinaryTag(type: Byte, depth: Int = 0) {
    when (type) {
        TAG_END -> {}
        TAG_BYTE -> this.skipBytes(ByteTag.PAYLOAD_SIZE)
        TAG_SHORT -> this.skipBytes(ShortTag.PAYLOAD_SIZE)
        TAG_INT -> this.skipBytes(IntTag.PAYLOAD_SIZE)
        TAG_LONG -> this.skipBytes(LongTag.PAYLOAD_SIZE)
        TAG_FLOAT -> this.skipBytes(FloatTag.PAYLOAD_SIZE)
        TAG_DOUBLE -> this.skipBytes(DoubleTag.PAYLOAD_SIZE)
        TAG_BYTE_ARRAY -> this.skipBytes(this.readInt() * ByteTag.PAYLOAD_SIZE)
        TAG_STRING -> this.skipString()
        TAG_LIST -> this.skipBinaryTags(depth)
        TAG_COMPOUND -> this.skipNamedTags(depth)
        TAG_INT_ARRAY -> this.skipBytes(this.readInt() * IntTag.PAYLOAD_SIZE)
        TAG_LONG_ARRAY -> this.skipBytes(this.readInt() * LongTag.PAYLOAD_SIZE)
        else -> throw NBTFormatException("Invalid tag type: $type")
    }
}

fun DataInput.skipNamedTags(depth: Int = 0) {
    val child = depth.increaseDepthOrThrow()
    this.readBinaryTags loop@{
        if (it == TAG_END) return@loop false
        this.skipString()
        this.skipBinaryTag(it, child)
        return@loop true
    }
}

fun DataOutput.writeNBT(name: String, tag: BinaryTag) {
    val id = tag.type.typeId
    this.writeByte(id.toInt())
    if (id == TAG_END) return
    this.writeUTF(name)
    tag.write(this)
}

@OptIn(ExperimentalContracts::class)
inline fun runSuppressing(action: () -> Unit) {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    try {
        action()
    } catch (_: Exception) {
    }
}
