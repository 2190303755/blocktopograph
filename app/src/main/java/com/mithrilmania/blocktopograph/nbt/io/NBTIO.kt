package com.mithrilmania.blocktopograph.nbt.io

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
import com.mithrilmania.blocktopograph.nbt.util.TagWithMeta
import com.mithrilmania.blocktopograph.util.autoDecompress
import com.mithrilmania.blocktopograph.util.readIntLE
import com.mithrilmania.blocktopograph.util.runSuppressing
import com.mithrilmania.blocktopograph.util.writeIntLE
import java.io.ByteArrayOutputStream
import java.io.DataInput
import java.io.DataOutput
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

const val MAX_STACK_DEPTH = 512

fun Int.increaseDepthOrThrow(): Int {
    val depth = this + 1
    if (depth < MAX_STACK_DEPTH) return depth
    throw NBTStackOverflowException("Tried to read NBT with too high complexity, depth > $MAX_STACK_DEPTH")
}

interface NBTExportConfig {
    val stringify: Boolean
    val prettify: Boolean
    val heterogeneous: Boolean
    val compress: Boolean
    val littleEndian: Boolean
    val storageVersion: Int?
}

interface NBTImportConfig {
    val format: NBTFormat
    val header: Boolean?
}

data class NBTImportConfigImpl(
    override val format: NBTFormat = NBTFormat.UNKNOWN,
    override val header: Boolean? = null
) : NBTImportConfig

fun InputStream.readSNBT(): TagWithMeta {
    val pair = SNBTParser(this.reader(Charsets.UTF_8)).parseRoot()
    return TagWithMeta(
        tag = pair.second,
        name = pair.first,
        compressed = this is GZIPInputStream,
        storageVersion = null,
        littleEndian = false,
        stringified = true
    )
}

fun InputStream.readJavaNBT(): TagWithMeta {
    val pair = JavaNBTInput(this).readNBT()
    return TagWithMeta(
        tag = pair.second,
        name = pair.first,
        compressed = this is GZIPInputStream,
        storageVersion = null,
        littleEndian = false
    )
}

fun InputStream.readNBT(config: NBTImportConfig): TagWithMeta? {
    val wrapped = this.autoDecompress()
    when (config.format) {
        NBTFormat.STRINGIFIED -> return wrapped.readSNBT()
        NBTFormat.BIG_ENDIAN -> return wrapped.readJavaNBT()
        NBTFormat.LITTLE_ENDIAN -> {
            val header = config.header
            val input: DataInput
            val version: Int?
            if (header === null) {
                val bytes = wrapped.readBytes()
                if (bytes.size > 8 && bytes.size == 8 + bytes.readIntLE(4)) {
                    input = BedrockNBTInput(bytes.inputStream(8, bytes.size - 8))
                    version = bytes.readIntLE(0)
                } else {
                    input = BedrockNBTInput(bytes)
                    version = null
                }
            } else if (header) {
                input = BedrockNBTInput(wrapped)
                version = input.readInt()
                input.skipBytes(4)
            } else {
                input = BedrockNBTInput(wrapped)
                version = null
            }
            val pair = input.readNBT()
            return TagWithMeta(
                tag = pair.second,
                name = pair.first,
                compressed = wrapped is GZIPInputStream,
                storageVersion = version,
                littleEndian = true
            )
        }

        else -> {
            val bytes = wrapped.readBytes()
            val header = config.header
            if (header != false
                && bytes.size > 8
                && (header !== null || bytes.size == 8 + bytes.readIntLE(4))
            ) runSuppressing {
                val pair = BedrockNBTInput(
                    bytes.inputStream(8, bytes.size - 8)
                ).readNBT()
                return TagWithMeta(
                    pair.second,
                    pair.first,
                    false,
                    bytes.readIntLE(0)
                )
            }
            runSuppressing { return bytes.inputStream().readSNBT() }
            runSuppressing {
                val pair = BedrockNBTInput(bytes).readNBT()
                return TagWithMeta(
                    pair.second,
                    pair.first,
                    wrapped is GZIPInputStream,
                    null,
                    littleEndian = true
                )
            }
            runSuppressing { return bytes.inputStream().readJavaNBT() }
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
    } else if (config.littleEndian) {
        if (config.compress) {
            BedrockNBTOutput(GZIPOutputStream(this, DEFAULT_BUFFER_SIZE)).use {
                it.writeNBT(name, tag)
            }
        } else {
            val version = config.storageVersion
            if (version === null) {
                BedrockNBTOutput(this.buffered()).use {
                    it.writeNBT(name, tag)
                }
            } else {
                this.writeNBTWithHeader(version, name, tag)
            }
        }
    } else {
        JavaNBTOutput(
            if (config.compress) GZIPOutputStream(this, DEFAULT_BUFFER_SIZE) else this.buffered()
        ).use {
            it.writeNBT(name, tag)
        }
    }
}

fun OutputStream.writeNBTWithHeader(version: Int, name: String, tag: BinaryTag) {
    this.buffered().use {
        val buffer = ByteArrayOutputStream(DEFAULT_BUFFER_SIZE)
        BedrockNBTOutput(buffer).writeNBT(name, tag)
        it.writeIntLE(version)
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

fun DataInput.readNBT(): Pair<String, BinaryTag> {
    val type = this.readByte()
    return if (type == TAG_END) {
        "" to EndTag
    } else {
        this.readUTF() to type.toTagType().read(this, 0)
    }
}

inline fun <reified T> DataInput.readAnonymousTypedTag(): T? = this.readNBT().second as? T

private fun DataInput.skipPayloads(unit: Int) {
    var remaining = this.readInt().toLong() * unit
    while (remaining > 0) {
        val read = this.skipBytes(
            minOf(remaining, Int.MAX_VALUE.toLong()).toInt()
        )
        if (read <= 0) throw EOFException()
        remaining -= read
    }
}

private fun DataInput.skipBinaryTags(depth: Int) {
    when (val type = this.readByte()) {
        TAG_END -> this.skipBytes(4)
        TAG_BYTE -> this.skipPayloads(ByteTag.PAYLOAD_SIZE)
        TAG_SHORT -> this.skipPayloads(ShortTag.PAYLOAD_SIZE)
        TAG_INT -> this.skipPayloads(IntTag.PAYLOAD_SIZE)
        TAG_LONG -> this.skipPayloads(LongTag.PAYLOAD_SIZE)
        TAG_FLOAT -> this.skipPayloads(FloatTag.PAYLOAD_SIZE)
        TAG_DOUBLE -> this.skipPayloads(DoubleTag.PAYLOAD_SIZE)
        TAG_BYTE_ARRAY -> repeat(this.readInt()) { this.skipPayloads(ByteTag.PAYLOAD_SIZE) }
        TAG_STRING -> repeat(this.readInt()) { this.skipString() }
        TAG_LIST -> depth.increaseDepthOrThrow().let { child ->
            repeat(this.readInt()) { this.skipBinaryTags(child) }
        }

        TAG_COMPOUND -> depth.increaseDepthOrThrow().let { child ->
            repeat(this.readInt()) { this.skipNamedTags(child) }
        }

        TAG_INT_ARRAY -> repeat(this.readInt()) { this.skipPayloads(IntTag.PAYLOAD_SIZE) }
        TAG_LONG_ARRAY -> repeat(this.readInt()) { this.skipPayloads(LongTag.PAYLOAD_SIZE) }
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
        TAG_BYTE_ARRAY -> this.skipPayloads(ByteTag.PAYLOAD_SIZE)
        TAG_STRING -> this.skipString()
        TAG_LIST -> this.skipBinaryTags(depth)
        TAG_COMPOUND -> this.skipNamedTags(depth)
        TAG_INT_ARRAY -> this.skipPayloads(IntTag.PAYLOAD_SIZE)
        TAG_LONG_ARRAY -> this.skipPayloads(LongTag.PAYLOAD_SIZE)
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

