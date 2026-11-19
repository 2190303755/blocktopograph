package com.mithrilmania.blocktopograph

import androidx.collection.intListOf
import androidx.collection.longListOf
import com.mithrilmania.blocktopograph.nbt.BinaryTag
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
import com.mithrilmania.blocktopograph.nbt.io.MAX_STACK_DEPTH
import com.mithrilmania.blocktopograph.nbt.util.SNBTException
import com.mithrilmania.blocktopograph.nbt.util.SNBTParser
import it.unimi.dsi.fastutil.bytes.ByteArrayList
import it.unimi.dsi.fastutil.bytes.ByteList
import org.junit.Assert
import org.junit.Test
import java.io.EOFException
import java.io.IOException
import java.io.StringReader
import java.util.UUID

private fun parseValue(string: String): BinaryTag {
    return SNBTParser(StringReader(string)).parseValue()
}

private fun byteListOf(vararg elements: Byte): ByteList = ByteArrayList(elements)

private fun assertParses(expected: BinaryTag?, snbt: String) {
    Assert.assertEquals(snbt, expected, parseValue(snbt))
}

class SNBTTest {
    @Test
    fun testUUIDFunction() {
        repeat(10) {
            val original = UUID.randomUUID()
            val elements = (parseValue("uuid($original)") as IntArrayTag).elements
            val resolved = UUID(
                elements[0].toLong() shl 32 or (elements[1].toLong() and 0xFFFFFFFFL),
                elements[2].toLong() shl 32 or (elements[3].toLong() and 0xFFFFFFFFL)
            )
            Assert.assertEquals(original, resolved)
        }
    }

    @Test
    @Throws(IOException::class)
    fun testBoolean() {
        assertParses(ByteTag(1.toByte()), "true")
        assertParses(ByteTag(1.toByte()), "TRUE")
        assertParses(ByteTag(0.toByte()), "false")
        assertParses(ByteTag(0.toByte()), "FaLsE")

        assertParses(StringTag("true"), "\"true\"")
        assertParses(StringTag("false"), "'false'")
    }

    @Test
    @Throws(IOException::class)
    fun testIntegerTypes() {
        assertParses(IntTag(123), "123")
        assertParses(IntTag(123), "123i")
        assertParses(IntTag(123), "123I")
        assertParses(IntTag(123), "123si")
        assertParses(IntTag(-123), "-123")

        assertParses(ShortTag(123.toShort()), "123s")
        assertParses(ShortTag(123.toShort()), "123S")
        assertParses(ShortTag(123.toShort()), "123ss")

        assertParses(ByteTag(123.toByte()), "123b")
        assertParses(ByteTag(123.toByte()), "123B")
        assertParses(ByteTag(123.toByte()), "123sb")

        assertParses(LongTag(123L), "123L")
        assertParses(LongTag(123L), "123l")
        assertParses(LongTag(123L), "123sl")
    }

    @Test
    @Throws(IOException::class)
    fun testUnsignedTypes() {
        assertParses(ByteTag((-16).toByte()), "240ub")
        assertParses(ByteTag((-16).toByte()), "-16sb")
        Assert.assertEquals(parseValue("240ub"), parseValue("-16sb"))

        assertParses(ByteTag(17.toByte()), "0x11ub")
        assertParses(ByteTag(17.toByte()), "0x11sb")

        assertParses(ShortTag((-1).toShort()), "65535us")
        assertParses(IntTag(-1), "4294967295ui")
        assertParses(LongTag(-1L), "18446744073709551615ul")

        assertParses(StringTag("256ub"), "256ub")
        assertParses(StringTag("240sb"), "240sb")
        assertParses(StringTag("65536us"), "65536us")
    }

    @Test
    @Throws(IOException::class)
    fun testRadixNumbers() {
        assertParses(IntTag(0xbad), "0xbad")
        assertParses(IntTag(0xCAFE), "0xCAFE")
        assertParses(IntTag(5), "0b101")
        assertParses(IntTag(15), "0o17")
        assertParses(IntTag(0x11b), "0x11b")

        // 下划线分隔数位
        assertParses(IntTag(9), "0b10_01")
        assertParses(IntTag(0xABCD), "0xAB_CD")
    }

    @Test
    @Throws(IOException::class)
    fun testFloatingPoint() {
        assertParses(DoubleTag(1.2), "1.2")
        assertParses(DoubleTag(1.2), "1.2d")
        assertParses(DoubleTag(1.2), "1.2D")
        assertParses(FloatTag(1.2f), "1.2f")
        assertParses(FloatTag(1.2f), "1.2F")
        assertParses(DoubleTag(1.0), "1d")
        assertParses(FloatTag(1.0f), "1f")
        assertParses(DoubleTag(0.1), ".1")
        assertParses(DoubleTag(1.0), "1.")
        assertParses(DoubleTag(1200.0), "1.2e3")
        assertParses(DoubleTag(1200.0), "1.2E+3")
        assertParses(DoubleTag(1200.0), "12000e-1")
        assertParses(DoubleTag(1.2E35), "1_2e3_4")
        assertParses(FloatTag(12.345f), "1_2.3_4__5f")
    }

    @Test
    @Throws(IOException::class)
    fun testInvalidNumbersFallBackToString() {
        assertParses(StringTag("1e1000"), "1e1000")
        assertParses(StringTag("1e1000f"), "1e1000f")

        assertParses(StringTag("123abc"), "123abc")
        assertParses(StringTag("0xZZ"), "0xZZ")
    }

    @Test
    @Throws(IOException::class)
    fun testQuotedStrings() {
        assertParses(StringTag("test"), "\"test\"")
        assertParses(StringTag("test"), "'test'")

        assertParses(StringTag("\b"), "\"\\b\"")
        assertParses(StringTag("\u000c"), "\"\\f\"")
        assertParses(StringTag("\n"), "\"\\n\"")
        assertParses(StringTag("a\nb"), "\"a\\nb\"")
        assertParses(StringTag("\r"), "\"\\r\"")
        assertParses(StringTag(" "), "\"\\s\"")
        assertParses(StringTag("\t"), "\"\\t\"")
        assertParses(StringTag("\\"), "\"\\\\\"")
        assertParses(StringTag("'"), "'\\''")
        assertParses(StringTag("\""), "\"\\\"\"")

        assertParses(StringTag("B"), "\"\\x42\"")
        assertParses(StringTag("☄"), "\"\\u2604\"")
        assertParses(StringTag(String(Character.toChars(0x51020))), "\"\\U00051020\"")
    }

    @Test
    @Throws(IOException::class)
    fun testUnquotedStrings() {
        assertParses(StringTag("abc"), "abc")
        assertParses(StringTag("a-b_c.d+e"), "a-b_c.d+e")
        assertParses(StringTag("hello--world"), "hello--world")
    }

    @Test
    @Throws(IOException::class)
    fun testByteArray() {
        val first = parseValue("[B;1,2]") as ByteArrayTag
        Assert.assertEquals(byteListOf(1, 2), first.elements)

        val suffixed = parseValue("[B;1b,2b,]") as ByteArrayTag
        Assert.assertEquals(byteListOf(1, 2), suffixed.elements)

        val empty = parseValue("[B;]") as ByteArrayTag
        Assert.assertTrue(empty.isEmpty())
    }

    @Test
    @Throws(IOException::class)
    fun testIntArray() {
        val plain = parseValue("[I;1,2,3]") as IntArrayTag
        Assert.assertEquals(intListOf(1, 2, 3), plain.elements)

        val mixed = parseValue("[I;1b,2s,3]") as IntArrayTag
        Assert.assertEquals(intListOf(1, 2, 3), mixed.elements)

        val truncated = parseValue("[I;1.5]") as IntArrayTag
        Assert.assertEquals(intListOf(1), truncated.elements)

        val empty = parseValue("[I;]") as IntArrayTag
        Assert.assertTrue(empty.isEmpty())
    }

    @Test
    @Throws(IOException::class)
    fun testLongArray() {
        val mixed = parseValue("[L;1b,2s,3,4L]") as LongArrayTag
        Assert.assertEquals(longListOf(1, 2, 3, 4), mixed.elements)

        val empty = parseValue("[L;]") as LongArrayTag
        Assert.assertTrue(empty.isEmpty())
    }

    @Test
    @Throws(IOException::class)
    fun testShortArray() {
        assertParses(
            ListTag(mutableListOf(ShortTag(1.toShort()), ShortTag(2.toShort()))),
            "[S;1,2]"
        )
        assertParses(ListTag(mutableListOf()), "[S;]")
    }

    @Test
    @Throws(IOException::class)
    fun testTuple() {
        assertParses(
            ListTag(mutableListOf(IntTag(1), IntTag(2))),
            "(1,2)"
        )
        Assert.assertEquals(parseValue("(1,2)"), parseValue("(1,2,)"))
        assertParses(
            ListTag(
                mutableListOf(
                    IntTag(1),
                    ListTag(mutableListOf(IntTag(2), IntTag(3)))
                )
            ),
            "(1,(2,3))"
        )
        assertParses(ListTag(mutableListOf()), "()")
        assertParses(ListTag(mutableListOf(StringTag("a"))), "('a')")
    }

    @Test
    @Throws(IOException::class)
    fun testList() {
        assertParses(ListTag(mutableListOf()), "[]")
        Assert.assertEquals(parseValue("[1,2]"), parseValue("[1,2,]"))
        Assert.assertEquals(parseValue("[[1],[2,3]]"), parseValue("[[1], [2, 3]]"))

        assertParses(
            ListTag(
                mutableListOf(
                    StringTag(""),
                    CompoundTag(hashMapOf("text" to StringTag("hello"))),
                    IntTag(123)
                )
            ),
            "['', {text:\"hello\"}, 123]"
        )
    }

    @Test
    @Throws(IOException::class)
    fun testCompound() {
        assertParses(CompoundTag(), "{}")
        assertParses(
            CompoundTag(hashMapOf("a" to StringTag("b"))),
            "{a:b}"
        )
        Assert.assertEquals(parseValue("{a:b}"), parseValue("{a:b,}"))
        Assert.assertEquals(parseValue("{a:b}"), parseValue("{ a : b }"))

        assertParses(
            CompoundTag(
                hashMapOf(
                    "key1" to IntTag(123),
                    "key2" to StringTag("somevalue1"),
                    "key3" to CompoundTag(
                        hashMapOf(
                            "subkey1" to IntTag(0x1C8),
                            "subkey2" to StringTag("somevalue2")
                        )
                    )
                )
            ),
            "{key1: 123, 'key2': 'somevalue1', \"key3\": {subkey1: 0x1C8, \"subkey2\": \"somevalue2\"}}"
        )

        assertParses(
            CompoundTag(hashMapOf("" to IntTag(1))),
            "{\"\": 1}"
        )
    }

    @Test
    @Throws(IOException::class)
    fun testRootKey() {
        val root: Pair<String?, BinaryTag?> = SNBTParser(StringReader("key:123")).parseRoot()
        Assert.assertEquals("key", root.first)
        Assert.assertEquals(IntTag(123) as BinaryTag, root.second)

        val anonymous: Pair<String?, BinaryTag?> = SNBTParser(StringReader("[1,2]")).parseRoot()
        Assert.assertEquals("", anonymous.first)
        Assert.assertEquals(ListTag(mutableListOf(IntTag(1), IntTag(2))), anonymous.second)
    }

    @Test
    @Throws(IOException::class)
    fun testBooleanFunction() {
        assertParses(ByteTag(1.toByte()), "bool(1)")
        assertParses(ByteTag(0.toByte()), "bool(0)")
        assertParses(ByteTag(1.toByte()), "bool(2)")
        assertParses(ByteTag(1.toByte()), "bool(true)")
        assertParses(ByteTag(0.toByte()), "bool(false)")

        Assert.assertTrue(
            Assert.assertThrows(SNBTException::class.java) { parseValue("bool(1, 2)") }
                .cause is IllegalArgumentException
        )
        Assert.assertTrue(
            Assert.assertThrows(SNBTException::class.java) { parseValue("bool(\"text\")") }
                .cause is ClassCastException
        )
    }

    @Test
    fun testInvalidSyntax() {
        Assert.assertThrows(SNBTException::class.java) { parseValue("[,]") }
        Assert.assertThrows(SNBTException::class.java) { parseValue("[1,,]") }
        Assert.assertThrows(SNBTException::class.java) { parseValue("{,}") }
        Assert.assertThrows(SNBTException::class.java) { parseValue("{a:b,,}") }
        Assert.assertThrows(SNBTException::class.java) { parseValue("\"unterminated") }
        Assert.assertThrows(SNBTException::class.java) { parseValue("notAFunction(1)") }
    }

    @Test
    fun testErrorDetailsForDialog() {
        val exception = Assert.assertThrows(SNBTException::class.java) { parseValue("[,]") }
        Assert.assertTrue(exception.detail.isNotBlank())
        Assert.assertTrue(exception.position >= 0)
        Assert.assertEquals("${exception.detail} at ${exception.position}", exception.message)
    }

    @Test
    fun testInvalidEscapeDetail() {
        val exception = Assert.assertThrows(SNBTException::class.java) { parseValue("\"\\q\"") }
        Assert.assertTrue(exception.detail.contains("Invalid escape"))
        Assert.assertNull(exception.cause)
    }

    @Test
    fun testTruncatedHexEscapeKeepsCause() {
        val exception = Assert.assertThrows(SNBTException::class.java) { parseValue("\"\\x4") }
        Assert.assertTrue(exception.cause is EOFException)
    }

    @Test
    fun testQuotedNumericArrayType() {
        val tag = parseValue("[\"B\";1,2]") as ByteArrayTag
        Assert.assertEquals(byteListOf(1, 2), tag.elements)
    }

    @Test
    fun testNestingDepthLimit() {
        val depth = MAX_STACK_DEPTH + 1
        val deep = "[".repeat(depth) + "]".repeat(depth)
        val exception = Assert.assertThrows(SNBTException::class.java) { parseValue(deep) }
        Assert.assertTrue(exception.detail.contains("too high complexity"))
    }
}
