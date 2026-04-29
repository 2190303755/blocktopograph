package com.mithrilmania.blocktopograph.nbt.util

import com.mithrilmania.blocktopograph.BuildConfig
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
import com.mithrilmania.blocktopograph.nbt.NumericTag
import com.mithrilmania.blocktopograph.nbt.ShortTag
import com.mithrilmania.blocktopograph.nbt.StringTag
import com.mithrilmania.blocktopograph.nbt.io.SNBTReader
import com.mithrilmania.blocktopograph.util.toChar
import java.util.UUID

typealias NBTFunction = (List<BinaryTag>) -> BinaryTag

val INTEGER_LIKE_PATTERN = Regex(
    "^([+-]?)(?:0b([01_]+)|0o([0-7_]+)|0x([0-9a-f_]+)|([0-9_]+))([us])?([bsil])?$",
    RegexOption.IGNORE_CASE
)
val FLOAT_LIKE_PATTERN = Regex(
    "^([+-]?(?:[0-9_]+(?:\\.[0-9_]*)?|\\.[0-9_]+)(?:e[+-]?[0-9_]+)?)([fd]?)$",
    RegexOption.IGNORE_CASE
)
val INVALID_NUMERIC_LITERAL = Regex("(?<!\\d)_|_(?!\\d)")

val BUILTIN_FUNCTIONS: Map<String, NBTFunction> = mapOf(
    "bool" to {
        if (it.size != 1) throw IllegalArgumentException("Too many arguments")
        val tag = it.first()
        if (tag !is NumericTag) throw ClassCastException()
        ByteTag(tag.toInt() == 0)
    },
    "uuid" to {
        if (it.size != 1) throw IllegalArgumentException("Too many arguments")
        val tag = it.first()
        if (tag !is StringTag) throw ClassCastException()
        val uuid = UUID.fromString(tag.value)
        val mostSignificantBits = uuid.mostSignificantBits
        val leastSignificantBits = uuid.leastSignificantBits
        IntArrayTag(
            intArrayOf(
                (mostSignificantBits shr 32).toInt(),
                mostSignificantBits.toInt(),
                (leastSignificantBits shr 32).toInt(),
                leastSignificantBits.toInt()
            )
        )
    },
    "blocktopograph" to {
        CompoundTag(
            hashMapOf(
                "VersionName" to StringTag(BuildConfig.VERSION_NAME),
                "VersionCode" to IntTag(BuildConfig.VERSION_CODE),
                "BuildType" to StringTag(BuildConfig.BUILD_TYPE),
                "IsDebug" to ByteTag(BuildConfig.DEBUG),
            )
        )
    }
)

fun parseIntegerLike(literal: String): NumericTag? {
    val match = INTEGER_LIKE_PATTERN.matchEntire(literal) ?: return null
    var index = 2
    var boxed: ULong? = null
    do {
        val number = match.groupValues[index]
        if (number.isNotEmpty()) {
            boxed = number.replace("_", "").toULongOrNull(
                when (index) {
                    2 -> 2
                    3 -> 8
                    4 -> 16
                    else -> 10
                }
            )
            if (boxed !== null) break
        }
    } while (++index < 6)
    if (boxed === null) return null
    val value = if (match.groupValues[1].toChar() == '-') -boxed.toLong() else boxed.toLong()
    return when (match.groupValues[7].ifEmpty {
        match.groupValues[6] // it means short if there is only one `s`
    }.toChar()) {
        'b', 'B' -> ByteTag(value.toByte())
        's', 'S' -> ShortTag(value.toShort())
        'l', 'L' -> LongTag(value)
        else -> IntTag(value.toInt())
    }
}

fun parseFloatLike(literal: String): NumericTag? {
    val match = FLOAT_LIKE_PATTERN.matchEntire(literal) ?: return null
    val value = match.groupValues[1].replace("_", "").toDoubleOrNull()
        ?: return null
    return when (match.groupValues[2].toChar()) {
        'f', 'F' -> FloatTag(value.toFloat())
        else -> DoubleTag(value)
    }

}

fun parseLiteral(unquoted: String): BinaryTag {
    if (unquoted.isEmpty()) return StringTag("")
    if (unquoted.equals("true", true)) return ByteTag(1.toByte())
    if (unquoted.equals("false", true)) return ByteTag(0.toByte())
    if (INVALID_NUMERIC_LITERAL.matches(unquoted)) return StringTag(unquoted)
    return parseIntegerLike(unquoted) ?: parseFloatLike(unquoted) ?: StringTag(unquoted)
}

class SNBTParser(
    val reader: SNBTReader,
    val functions: Map<String, NBTFunction> = BUILTIN_FUNCTIONS
) {
    var current: Token = reader.nextToken()
    var pos: Long = reader.pos()
    var next: Token = reader.nextToken()

    fun expect(token: Token) {
        if (this.current !== token) {
            throw IllegalArgumentException("Expected $token, got $current")
        }
        this.advance()
    }

    fun advance() {
        this.current = this.next
        this.pos = this.reader.pos()
        this.next = this.reader.nextToken()
    }

    fun fetch() {
        this.current = this.reader.nextToken()
        this.pos = this.reader.pos()
        this.next = this.reader.nextToken()
    }

    fun advanceIfHasNext(terminator: Token): Boolean {
        if (this.current === Token.Comma && this.next !== terminator) {
            this.advance()
            return true
        }
        return false
    }

    fun consume(token: Token): Boolean {
        if (this.current === token) {
            this.advance()
            return true
        }
        return false
    }

    fun parseRoot(): Pair<String, BinaryTag> {
        val pair = (this.parseKey() ?: "") to this.parseValue()
        if (this.current === Token.EOF) return pair
        throw IllegalArgumentException("Expect EOF at $pos")
    }

    fun parseKey(): String? {
        val current = this.current
        if (current === Token.Colon) {
            this.advance()
            return ""
        } else if (this.next === Token.Colon) {
            if (current is Token.Literal) {
                this.fetch()
                return current.value
            }
            throw IllegalArgumentException("Expected string literal")
        }
        return null
    }

    fun parseValue(): BinaryTag {
        when (val token = this.current) {
            Token.LBrace -> return this.parseCompound()
            Token.LBracket -> {
                this.advance()
                val pos = this.pos
                if (this.next === Token.Semicolon) {
                    val current = this.current
                    if (current is Token.Literal.Unquoted) {
                        this.fetch()
                        when (current.value.toChar()) {
                            'b', 'B' -> return this.parseArray(NumericTag::toByte) {
                                ByteArrayTag(it.toByteArray())
                            }

                            'i', 'I' -> return this.parseArray(NumericTag::toInt) {
                                IntArrayTag(it.toIntArray())
                            }

                            'l', 'L' -> return this.parseArray(NumericTag::toLong) {
                                LongArrayTag(it.toLongArray())
                            }

                            's', 'S' -> return this.parseArray(
                                ::ShortTag,
                                ::ListTag
                            )
                        }
                    }
                    throw IllegalArgumentException("Unexpected $current at $pos")
                }
                return this.parseList(Token.RBracket)
            }

            Token.LParen -> {
                this.advance()
                return this.parseList(Token.RParen)
            }

            is Token.Literal.Quoted -> {
                this.advance()
                return StringTag(token.value)
            }

            is Token.Literal.Unquoted -> {
                if (this.next === Token.LParen) {
                    val function = this.functions[token.value]
                    if (function !== null) {
                        this.fetch()
                        return function(this.parseList(Token.RParen).tags)
                    }
                    throw IllegalArgumentException("Unknown operation named ${token.value} at $pos")
                }
                this.advance()
                return parseLiteral(token.value)
            }

            else -> throw IllegalArgumentException("Unexpected $token at $pos")
        }
    }


    fun parseCompound(): CompoundTag {
        this.advance()
        val tags = hashMapOf<String, BinaryTag>()
        if (this.consume(Token.RBrace)) return CompoundTag(tags)
        do {
            tags[
                this.parseKey() ?: throw IllegalArgumentException(
                    "Expect string literal at $pos"
                )
            ] = this.parseValue()
        } while (this.advanceIfHasNext(Token.RBrace))
        if (this.current === Token.Comma) {
            this.advance()
        }
        this.expect(Token.RBrace)
        return CompoundTag(tags)
    }

    fun parseList(terminator: Token): ListTag {
        val tags = mutableListOf<BinaryTag>()
        if (this.consume(terminator)) return ListTag(tags)
        do {
            tags.add(this.parseValue())
        } while (this.advanceIfHasNext(terminator))
        if (this.current === Token.Comma) {
            this.advance()
        }
        this.expect(terminator)
        return ListTag(tags)
    }

    fun <T, R> parseArray(
        converter: (NumericTag) -> T,
        factory: (MutableList<T>) -> R
    ): R {
        val tags = mutableListOf<T>()
        if (this.consume(Token.RBracket)) return factory(tags)
        do {
            val pos = this.pos
            val tag = this.parseValue()
            if (tag is NumericTag) {
                tags.add(converter(tag))
            } else {
                throw IllegalArgumentException("Expect numeric literal at $pos")
            }
        } while (this.advanceIfHasNext(Token.RBracket))
        if (this.current === Token.Comma) {
            this.advance()
        }
        this.expect(Token.RBracket)
        return factory(tags)
    }

}