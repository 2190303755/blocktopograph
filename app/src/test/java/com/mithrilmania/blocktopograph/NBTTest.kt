package com.mithrilmania.blocktopograph

import androidx.collection.MutableLongList
import androidx.collection.mutableLongListOf
import com.mithrilmania.blocktopograph.nbt.BinaryTag
import com.mithrilmania.blocktopograph.nbt.ByteArrayTag
import com.mithrilmania.blocktopograph.nbt.ByteTag
import com.mithrilmania.blocktopograph.nbt.CompoundTag
import com.mithrilmania.blocktopograph.nbt.DoubleTag
import com.mithrilmania.blocktopograph.nbt.FloatTag
import com.mithrilmania.blocktopograph.nbt.IntTag
import com.mithrilmania.blocktopograph.nbt.LongArrayTag
import com.mithrilmania.blocktopograph.nbt.LongTag
import com.mithrilmania.blocktopograph.nbt.ShortTag
import com.mithrilmania.blocktopograph.nbt.StringTag
import com.mithrilmania.blocktopograph.nbt.io.BedrockNBTOutput
import com.mithrilmania.blocktopograph.nbt.io.JavaNBTOutput
import com.mithrilmania.blocktopograph.nbt.io.NBTExportConfig
import com.mithrilmania.blocktopograph.nbt.io.NBTFormat
import com.mithrilmania.blocktopograph.nbt.io.NBTImportConfigImpl
import com.mithrilmania.blocktopograph.nbt.io.readJavaNBT
import com.mithrilmania.blocktopograph.nbt.io.readNBT
import com.mithrilmania.blocktopograph.nbt.io.writeNBT
import com.mithrilmania.blocktopograph.nbt.util.Indentation
import com.mithrilmania.blocktopograph.nbt.util.NBTStringifier
import com.mithrilmania.blocktopograph.nbt.util.SNBTParser
import com.mithrilmania.blocktopograph.nbt.util.TagWithMeta
import com.mithrilmania.blocktopograph.nbt.util.appendSafeLiteral
import com.mithrilmania.blocktopograph.util.readIntLE
import it.unimi.dsi.fastutil.bytes.ByteArrayList
import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class NBTTest {
    @Test
    fun testJavaOutput() {
        val tag = makeBigCompound()
        val buffer = ByteArrayOutputStream()
        JavaNBTOutput(buffer).use {
            it.writeNBT("", tag)
        }
        assertNbtRoundTrip(
            tag,
            ByteArrayInputStream(buffer.toByteArray()).readJavaNBT(),
            littleEndian = false
        )
    }

    @Test
    fun testBedrockOutput() {
        val tag = makeBigCompound()
        val buffer = ByteArrayOutputStream()
        BedrockNBTOutput(buffer).use {
            it.writeNBT("", tag)
        }
        assertNbtRoundTrip(
            tag,
            ByteArrayInputStream(buffer.toByteArray()).readNBT(
                NBTImportConfigImpl(format = NBTFormat.LITTLE_ENDIAN, header = false)
            )!!
        )
    }

    @Test
    fun testLevelDatOutput() {
        val tag = makeBigCompound()
        val buffer = ByteArrayOutputStream()
        buffer.writeNBT("", tag, object : NBTExportConfig {
            override val stringify: Boolean get() = false
            override val prettify: Boolean get() = false
            override val heterogeneous: Boolean get() = false
            override val storageVersion: Int get() = 9
            override val compress: Boolean get() = false
            override val littleEndian: Boolean get() = true
        })
        val bytes = buffer.toByteArray()
        // The header is a little-endian storage version followed by the payload length.
        Assert.assertEquals(9, bytes.readIntLE(0))
        Assert.assertEquals(bytes.size - 8, bytes.readIntLE(4))
        assertNbtRoundTrip(
            tag,
            ByteArrayInputStream(bytes).readNBT(
                NBTImportConfigImpl(format = NBTFormat.LITTLE_ENDIAN, header = true)
            )!!,
            storageVersion = 9
        )
        // Auto-detection should recognize the same header and metadata.
        assertNbtRoundTrip(
            tag,
            ByteArrayInputStream(bytes).readNBT(NBTImportConfigImpl())!!,
            storageVersion = 9
        )
    }

    @Test
    fun testEncoder() {
        makeBigCompound().apply {
            print()
            print("")
        }
    }

    @Test
    fun testDecoder() {
        arrayOf(
            "1",
            "1.",
            "1.4",
            "1f",
            "1.f",
            ".",
            ".1",
            ".f",
            ".1f",
            "[]",
            "{}",
            "{test: 1b}",
            "[i;1b,2d,3f]",
            ":value",
        ).forEachSNBT { (_, tag) ->
            print(tag::class.simpleName)
                print('\t')
            tag.print()
        }
    }

    @Test
    fun testNamedTag() {
        arrayOf(
            "key: value",
            "key",
            "key:",
            ":value",
            "tag:[]",
            "{tag:[]}",
            "123:-456",
        ).forEachSNBT {
            println(
                NBTStringifier(
                        builder = StringBuilder()
                            .appendSafeLiteral(it.first)
                            .append(':')
                            .append('\t')
                    ).apply {
                        it.second.accept(this)
                    }
                )
        }
    }

    @Test
    fun testExtension() {
        arrayOf(
            "'': EmptyKey",
            "tuple: (element)",
            "uuid: uuid('5deb88cb-3db2-5900-b10a-66e966fa3e2b')",
            "bool: bool(123456)",
            "app: blocktopograph()",
            "ShortArray: [S; 2e100, 1.23456, 1024I, 65537L]",
            ":''",
            "{'':''}",
            "{:''}",
        ).forEachSNBT {
            println(
                NBTStringifier(
                    builder = StringBuilder()
                        .appendSafeLiteral(it.first)
                        .append(':')
                        .append(' ')
                ).apply {
                    it.second.accept(this)
                }
            )
        }
    }
}

private fun assertNbtRoundTrip(
    expectedTag: BinaryTag,
    actual: TagWithMeta,
    name: String = "",
    compressed: Boolean = false,
    storageVersion: Int? = null,
    littleEndian: Boolean = true,
    stringified: Boolean = false
) {
    Assert.assertEquals(name, actual.name)
    Assert.assertEquals(expectedTag, actual.tag)
    Assert.assertEquals(compressed, actual.compressed)
    Assert.assertEquals(storageVersion, actual.storageVersion)
    Assert.assertEquals(littleEndian, actual.littleEndian)
    Assert.assertEquals(stringified, actual.stringified)
}

inline fun Array<String>.forEachSNBT(action: (Pair<String, BinaryTag>) -> Unit) {
    this.forEach {
        try {
            action(SNBTParser(it.reader()).parseRoot())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun BinaryTag.print(indentation: String = "    ") {
    println(NBTStringifier(indentation = Indentation(indentation)).also {
        this.accept(it)
    })
}

inline fun buildCompound(
    action: CompoundTag.() -> Unit
) = CompoundTag().apply(action)

operator fun CompoundTag.set(key: String, action: CompoundTag.() -> Unit) {
    this[key] = buildCompound(action)
}

operator fun CompoundTag.set(key: String, value: Long) {
    this[key] = LongTag(value)
}

operator fun CompoundTag.set(key: String, value: Short) {
    this[key] = ShortTag(value)
}

operator fun CompoundTag.set(key: String, value: Int) {
    this[key] = IntTag(value)
}

operator fun CompoundTag.set(key: String, value: Byte) {
    this[key] = ByteTag(value)
}

operator fun CompoundTag.set(key: String, value: String) {
    this[key] = StringTag(value)
}

operator fun CompoundTag.set(key: String, value: Float) {
    this[key] = FloatTag(value)
}

operator fun CompoundTag.set(key: String, value: Double) {
    this[key] = DoubleTag(value)
}

operator fun CompoundTag.set(key: String, value: ByteArrayList) {
    this[key] = ByteArrayTag(value)
}

operator fun CompoundTag.set(key: String, value: MutableLongList) {
    this[key] = LongArrayTag(value)
}

fun makeBigCompound() = buildCompound {
    this["Level"] = {
        this["longTest"] = 9223372036854775807L
        this["shortTest"] = 32767.toShort()
        this["stringTest"] = "HELLO WORLD THIS IS A TEST STRING ÅÄÖ!"
        this["floatTest"] = 0.49823147f
        this["intTest"] = 2147483647
        this["doubleTest"] = 0.4931287132182315
        this["nested compound test"] = {
            this["ham"] = {
                this["name"] = "Hampus"
                this["value"] = 0.75f
            }
            this["egg"] = {
                this["name"] = "Eggbert"
                this["value"] = 0.5f
            }
        }
        this["longTagList"] = mutableLongListOf(
            11L,
            12L,
            13L,
            14L,
            15L
        )
        this["byteTest"] = 127.toByte()
        this[
            "byteArrayTest (the first 1000 values of (n*n*255+n*7)%100, starting with n=0 (0, 62, 34, 16, 8, ...))"
        ] = ByteArrayList(1000).apply {
            repeat(1000) {
                add(((it * it * 255 + it * 7) % 100).toByte())
            }
        }
    }
}