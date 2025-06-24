package com.mithrilmania.blocktopograph.editor.nbt

import com.mithrilmania.blocktopograph.nbt.util.appendSafeLiteral
import com.mithrilmania.blocktopograph.nbt.util.wrap
import com.mithrilmania.blocktopograph.util.BYTE_0
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encodeToString
import net.benwoodworth.knbt.Nbt
import net.benwoodworth.knbt.NbtCompound
import net.benwoodworth.knbt.NbtCompression
import net.benwoodworth.knbt.NbtVariant
import net.benwoodworth.knbt.StringifiedNbt
import net.benwoodworth.knbt.encodeToStream
import java.io.OutputStream

interface Exporter {
    fun write(stream: OutputStream, data: NbtCompound, label: String = "")
}

interface ExporterFactory {
    fun createExporter(): Exporter
}

class NBTOptions(
    variant: NbtVariant = NbtVariant.Bedrock,
    compression: NbtCompression = NbtCompression.None,
    val version: Byte? = null,
) : Exporter {
    val encoder = Nbt {
        this.variant = variant
        this.compression = compression
    }

    override fun write(stream: OutputStream, data: NbtCompound, label: String) {
        if (this.version === null) {
            this.encoder.encodeToStream(data.wrap(label), stream)
        } else {
            stream.write(this.encoder.encodeToByteArray(data.wrap(label)).let { nbt ->
                byteArrayOf(
                    version,
                    BYTE_0,
                    BYTE_0,
                    BYTE_0,
                    (nbt.size and 255).toByte(),
                    (nbt.size ushr 8 and 255).toByte(),
                    (nbt.size ushr 16 and 255).toByte(),
                    (nbt.size ushr 24 and 255).toByte()
                ).plus(nbt)
            })
        }
    }
}

class SNBTOptions(prettify: Boolean) : Exporter {
    val encoder = StringifiedNbt {
        prettyPrint = prettify
    }

    override fun write(stream: OutputStream, data: NbtCompound, label: String) {
        val value = this.encoder.encodeToString(data)
        stream.write(
            StringBuilder(16 + value.length + label.length).apply {
                appendSafeLiteral(label)
            }.append(": ")
                .append(value)
                .toString()
                .toByteArray()
        )
    }
}