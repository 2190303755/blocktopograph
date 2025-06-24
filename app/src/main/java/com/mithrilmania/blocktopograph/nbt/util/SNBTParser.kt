package com.mithrilmania.blocktopograph.nbt.util

import com.mithrilmania.blocktopograph.nbt.BinaryTag
import com.mithrilmania.blocktopograph.nbt.ByteArrayTag
import com.mithrilmania.blocktopograph.nbt.ByteTag
import com.mithrilmania.blocktopograph.nbt.CollectionTag
import com.mithrilmania.blocktopograph.nbt.CompoundTag
import com.mithrilmania.blocktopograph.nbt.DoubleTag
import com.mithrilmania.blocktopograph.nbt.FloatTag
import com.mithrilmania.blocktopograph.nbt.IntArrayTag
import com.mithrilmania.blocktopograph.nbt.IntTag
import com.mithrilmania.blocktopograph.nbt.ListTag
import com.mithrilmania.blocktopograph.nbt.LongArrayTag
import com.mithrilmania.blocktopograph.nbt.LongTag
import com.mithrilmania.blocktopograph.nbt.NumericTag
import com.mithrilmania.blocktopograph.nbt.ShortTag
import com.mithrilmania.blocktopograph.nbt.StringTag
import com.mithrilmania.blocktopograph.nbt.TagType
import java.util.regex.Pattern


class SNBTParser private constructor(
    val snbt: String
) {
    private val length = snbt.length
    private var cursor: Int = 0

    fun reset() {
        this.cursor = 0
    }

    fun skipWhitespace() {
        val input = this.snbt
        while (this.cursor < this.length && input[this.cursor].isWhitespace()) {
            ++this.cursor
        }
    }

    private inline fun <T, R> readArray(
        converter: NumericTag<*>.() -> T,
        factory: (ArrayList<T>) -> R
    ): R {
        this.cursor += 2
        this.skipWhitespace()
        if (this.cursor >= this.length) throw NBTFormatException("Expect value at $cursor")
        val list = ArrayList<T>()
        val input = this.snbt
        while (input[this.cursor] != ']') {
            val index = this.cursor
            val value = this.readValue()
            if (value is NumericTag<*>) {
                list += value.converter()
            } else {
                throw NBTFormatException("Can't insert ${value.type} into numeric array at $index")
            }
            if (this.missesSeparator()) break
            if (this.cursor >= this.length) throw NBTFormatException("Expect value at $cursor")
        }
        this.expect(']')
        return factory(list)
    }

    fun expect(char: Char) {
        this.skipWhitespace()
        if (this.cursor < this.length && this.snbt[this.cursor] == char) {
            ++this.cursor
            return
        }
        throw NBTFormatException("Expect $char at $cursor")
    }

    fun missesSeparator(): Boolean {
        this.skipWhitespace()
        if (this.cursor < this.length && this.snbt[this.cursor] == ',') {
            ++this.cursor
            this.skipWhitespace()
            return false
        }
        return true
    }

    fun readString(): String {
        if (this.cursor >= this.length) return ""
        val char = this.snbt[this.cursor]
        return if (char.isQuote()) {
            this.readStringUntil(char)
        } else {
            this.readUnquotedString()
        }
    }

    fun readStringUntil(terminator: Char): String {
        val builder = StringBuilder()
        var escaped = false
        val input = this.snbt
        while (++this.cursor < this.length) {//skip promoter
            val char = input[this.cursor]
            if (escaped) {
                if (char == terminator || char == ESCAPE) {
                    builder.append(char)
                    escaped = false
                    continue
                }
                throw NBTFormatException("Invalid escape at position ${--this.cursor}")
            } else if (char == ESCAPE) {
                escaped = true
            } else if (char == terminator) {
                ++this.cursor // consume this
                return builder.toString()
            } else {
                builder.append(char)
            }
        }
        throw NBTFormatException("Expect $terminator")
    }

    fun readUnquotedString(): String {
        val start = this.cursor
        val input = this.snbt
        while (this.cursor < this.length && input[this.cursor].isSafeLiteral()) {
            ++this.cursor
        }
        return this.snbt.substring(start, this.cursor)
    }

    fun readCompound(): CompoundTag {
        val compound = CompoundTag()
        val input = this.snbt
        if (this.cursor >= this.length) throw NBTFormatException("Expect } at $cursor")
        while (input[this.cursor] != '}') {
            val index = this.cursor
            this.skipWhitespace()
            val key = this.readString()
            if (key.isEmpty()) throw NBTFormatException("Expect key at $index")
            this.expect(':')
            compound[key] = this.readValue()
            if (this.missesSeparator()) break
            if (this.cursor >= this.length) throw NBTFormatException("Expect key at $index")
        }
        this.expect('}')
        return compound
    }

    fun readList(): CollectionTag<out BinaryTag<*>> {
        val input = this.snbt
        if (this.cursor + 1 < this.length && input[this.cursor + 1] == ';') {
            val type = input[this.cursor]
            when (type) {
                'S', 's', 'I', 'i' -> return this.readArray(NumericTag<*>::getAsInt) {
                    IntArrayTag(
                        it.toIntArray()
                    )
                }

                'B', 'b' -> return this.readArray(NumericTag<*>::getAsByte) { ByteArrayTag(it.toByteArray()) }
                'L', 'l' -> return this.readArray(NumericTag<*>::getAsLong) { LongArrayTag(it.toLongArray()) }
                SINGLE_QUOTE, DOUBLE_QUOTE -> {}
                else -> throw NBTFormatException("Invalid array with type $type at $cursor")
            }
        }
        this.skipWhitespace()
        if (this.cursor >= this.length) throw NBTFormatException("Expect value at $cursor")
        val list = ListTag()
        var type: TagType<*>? = null
        while (input[this.cursor] != ']') {
            val index = this.cursor
            val value = this.readValue()
            if (type === null) {
                type = value.type
            } else if (type !== value.type) {
                throw NBTFormatException("Can't insert ${value.type} into list of $type at $index")
            }
            list += value
            if (this.missesSeparator()) break
            if (this.cursor >= this.length) throw NBTFormatException("Expect value at $cursor")
        }
        this.expect(']')
        return list
    }

    fun readValue(): BinaryTag<*> {
        this.skipWhitespace()
        if (this.cursor >= this.length) throw NBTFormatException("Expect value at $cursor")
        return when (this.snbt[this.cursor]) {
            DOUBLE_QUOTE -> StringTag.of(this.readStringUntil(DOUBLE_QUOTE))
            SINGLE_QUOTE -> StringTag.of(this.readStringUntil(SINGLE_QUOTE))
            '{' -> {
                ++this.cursor
                this.readCompound()
            }

            '[' -> {
                ++this.cursor
                this.readList()
            }

            else -> {
                val value = this.readUnquotedString()
                if ("true".equals(value, true)) return ByteTag(1)
                if ("false".equals(value, true)) return ByteTag(0)
                try {
                    if (INTEGER_LIKE_PATTERN.matcher(value).matches()) return when (value.last()) {
                        'b', 'B' -> ByteTag(value.substring(0, value.length - 1).toByte())
                        's', 'S' -> ShortTag(value.substring(0, value.length - 1).toShort())
                        'l', 'L' -> LongTag(value.substring(0, value.length - 1).toLong())
                        else -> IntTag(value.toInt())
                    }
                    if (FLOAT_LIKE_PATTERN.matcher(value).matches()) return when (value.last()) {
                        'f', 'F' -> FloatTag(value.substring(0, value.length - 1).toFloat())
                        'd', 'D' -> DoubleTag(value.substring(0, value.length - 1).toDouble())
                        else -> DoubleTag(value.toDouble())
                    }
                } catch (_: NumberFormatException) {
                }
                return StringTag.of(value)
            }
        }
    }

    companion object {
        const val DOUBLE_QUOTE = '"'
        const val SINGLE_QUOTE = '\''
        const val ESCAPE = '\\'
        val INTEGER_LIKE_PATTERN: Pattern =
            Pattern.compile("^[-+]?(?:0|[1-9][0-9]*)[bBlLsS]?$")
        val FLOAT_LIKE_PATTERN: Pattern =
            Pattern.compile("^[-+]?(?:[0-9]+[.]?|[0-9]*[.][0-9]+)(?:e[-+]?[0-9]+)?[dDfF]?$")

        @JvmStatic
        fun String.toBinaryTag() = SNBTParser(this).readValue()

        @JvmStatic
        fun String.toCompound() = SNBTParser(this).apply {
            expect('{')
        }.readCompound()

        @JvmStatic
        fun String.toNamedTag(): Pair<String, BinaryTag<*>> {
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
    }
}