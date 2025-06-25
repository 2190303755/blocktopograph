package com.mithrilmania.blocktopograph

import com.mithrilmania.blocktopograph.nbt.BinaryTag
import com.mithrilmania.blocktopograph.nbt.ByteArrayTag
import com.mithrilmania.blocktopograph.nbt.ByteTag
import com.mithrilmania.blocktopograph.nbt.CompoundTag
import com.mithrilmania.blocktopograph.nbt.DoubleTag
import com.mithrilmania.blocktopograph.nbt.FloatTag
import com.mithrilmania.blocktopograph.nbt.IntTag
import com.mithrilmania.blocktopograph.nbt.ListTag
import com.mithrilmania.blocktopograph.nbt.LongTag
import com.mithrilmania.blocktopograph.nbt.ShortTag
import com.mithrilmania.blocktopograph.nbt.io.BedrockOutputBuffer
import com.mithrilmania.blocktopograph.nbt.io.NBTOutputBuffer
import com.mithrilmania.blocktopograph.nbt.parseNamedTag
import com.mithrilmania.blocktopograph.nbt.parseSNBT
import com.mithrilmania.blocktopograph.nbt.toBinaryTag
import com.mithrilmania.blocktopograph.nbt.util.NBTStringifier
import com.mithrilmania.blocktopograph.nbt.util.appendSafeLiteral
import org.junit.Test
import java.io.File
import java.nio.ByteOrder

class NBTTest {
    @Test
    fun testOutput() {
        val tag = makeBigCompound()
        NBTOutputBuffer(File("./test.nbt").outputStream(), ByteOrder.LITTLE_ENDIAN).save("", tag)
        BedrockOutputBuffer(File("./level.dat").outputStream(), 9U).save("", tag)
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
        ).forEach {
            it.parseSNBT().let {
                print(it::class.simpleName)
                print('\t')
                it.print()
            }
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
        ).forEach {
            it.parseNamedTag().let {
                println(
                    NBTStringifier(
                        builder = StringBuilder()
                            .appendSafeLiteral(it.first)
                            .append(':')
                            .append('\t')
                            .append('\t')
                    ).apply {
                        it.second.accept(this)
                    }
                )
            }
        }
    }
}

fun BinaryTag<*>.print(indent: String = "    ") {
    println(NBTStringifier(indent = indent).also { this.accept(it) })
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
    this[key] = value.toBinaryTag()
}

operator fun CompoundTag.set(key: String, value: Float) {
    this[key] = FloatTag(value)
}

operator fun CompoundTag.set(key: String, value: Double) {
    this[key] = DoubleTag(value)
}

operator fun CompoundTag.set(key: String, value: ByteArray) {
    this[key] = ByteArrayTag(value)
}

fun listTagOf(vararg values: Long) = ListTag().apply {
    values.forEach {
        this += LongTag(it)
    }
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
        this["longTagList"] = listTagOf(
            11L,
            12L,
            13L,
            14L,
            15L
        )
        this["byteTest"] = 127.toByte()
        this[
            "byteArrayTest (the first 1000 values of (n*n*255+n*7)%100, starting with n=0 (0, 62, 34, 16, 8, ...))"
        ] = ByteArray(1000) { n -> ((n * n * 255 + n * 7) % 100).toByte() }
    }
}