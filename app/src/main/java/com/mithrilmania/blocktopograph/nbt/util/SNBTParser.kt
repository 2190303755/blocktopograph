package com.mithrilmania.blocktopograph.nbt.util

import androidx.collection.mutableIntListOf
import com.google.common.primitives.UnsignedBytes
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
import com.mithrilmania.blocktopograph.nbt.io.MAX_STACK_DEPTH
import com.mithrilmania.blocktopograph.nbt.toByteList
import com.mithrilmania.blocktopograph.nbt.toIntList
import com.mithrilmania.blocktopograph.nbt.toLongList
import com.mithrilmania.blocktopograph.util.toChar
import java.io.Reader
import java.lang.Integer.parseInt
import java.lang.Integer.parseUnsignedInt
import java.lang.Long.parseLong
import java.lang.Long.parseUnsignedLong
import java.util.UUID

typealias NBTFunction = (List<BinaryTag>) -> BinaryTag

@JvmField
val BUILTIN_FUNCTIONS: Map<String, NBTFunction> = hashMapOf(
    "bool" to {
        if (it.size != 1) throw IllegalArgumentException("Argument count mismatch")
        val tag = it[0]
        if (tag !is NumericTag) throw ClassCastException("Require NumericTag")
        ByteTag(tag.toInt() != 0)
    },
    "uuid" to {
        if (it.size != 1) throw IllegalArgumentException("Argument count mismatch")
        val tag = it[0]
        if (tag !is StringTag) throw ClassCastException("Require StringTag")
        val uuid = UUID.fromString(tag.value)
        val mostSignificantBits = uuid.mostSignificantBits
        val leastSignificantBits = uuid.leastSignificantBits
        IntArrayTag(
            mutableIntListOf(
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

fun StringBuilder.appendNumber(radix: Int, literal: String, start: Int): Int {
    var cursor = start
    val maxDigitExclusive = '0' + radix
    val maxLetterOffsetExclusive = radix - 10
    var endsWithUnderscore = false
    val length = literal.length
    while (cursor < length) {
        val char = literal[cursor]
        if (char == '_') {
            if (cursor == start) return -1 // cannot start with underscore
            endsWithUnderscore = true
            ++cursor
            continue
        }
        endsWithUnderscore = false
        if (char in '0' until maxDigitExclusive) {
            this.append(char)
            ++cursor
            continue
        }
        if (maxLetterOffsetExclusive > 0) {
            val offset = if (char < 'a') char - 'A' else char - 'a'
            if (offset in 0 until maxLetterOffsetExclusive) {
                this.append(char)
                ++cursor
                continue
            }
        }
        break // invalid digit
    }
    return if (endsWithUnderscore) -1 else cursor // cannot end with underscore
}

fun parseIntegerLike(literal: String): NumericTag? {
    assert(literal.isNotEmpty())
    val length = literal.length
    val value = StringBuilder(length)
    var cursor: Int
    val isNegative = when (literal[0]) {
        '+' -> {
            cursor = 1
            false
        }

        '-' -> {
            value.append('-')
            cursor = 1
            true
        }

        else -> {
            cursor = 0
            false
        }
    }

    var isUnsigned = false
    var radix = 10
    if (length - cursor > 2 && literal[cursor] == '0') {
        when (literal[cursor + 1]) {
            'b', 'B' -> {
                cursor += 2
                isUnsigned = true
                radix = 2
            }

            'o', 'O' -> {
                cursor += 2
                isUnsigned = true
                radix = 8
            }

            'x', 'X' -> {
                cursor += 2
                isUnsigned = true
                radix = 16
            }
        }
    }

    cursor = value.appendNumber(radix, literal, cursor)
    if (cursor == -1) return null

    val type = when (length - cursor) {
        2 -> {
            when (literal[cursor]) {
                'u', 'U' -> isUnsigned = true
                's', 'S' -> {}
                else -> {
                    return null // invalid sign requirement
                }
            }
            literal[cursor + 1]
        }

        1 -> literal[cursor]
        0 -> 'i'
        else -> return null // unexpected length
    }

    if (isNegative && isUnsigned) return null // invalid sign requirement
    try {
        when (type) {
            'i', 'I' -> {
                return IntTag(
                    if (isUnsigned) {
                        parseUnsignedInt(value, 0, value.length, radix)
                    } else {
                        parseInt(value, 0, value.length, radix)
                    }
                )
            }

            'b', 'B' -> {
                return ByteTag(
                    if (isUnsigned) {
                        UnsignedBytes.parseUnsignedByte(value.toString(), radix)
                    } else {
                        value.toString().toByte(radix)
                    }
                )
            }

            'l', 'L' -> {
                return LongTag(
                    if (isUnsigned) {
                        parseUnsignedLong(value, 0, value.length, radix)
                    } else {
                        parseLong(value, 0, value.length, radix)
                    }
                )
            }

            's', 'S' -> {
                return ShortTag(
                    if (isUnsigned) {
                        val parse = parseInt(value, 0, value.length, radix)
                        if (parse shr 16 == 0) {
                            parse.toShort()
                        } else {
                            throw NumberFormatException(literal + "exceeds range of unsigned short.")
                        }
                    } else {
                        value.toString().toShort(radix)
                    }
                )
            }
        }
    } catch (_: NumberFormatException) {
    }
    return null // invalid format
}

fun parseFloatLike(literal: String): NumericTag? {
    assert(literal.isNotEmpty())
    val length = literal.length
    val value = StringBuilder(length)
    var cursor: Int
    when (literal[0]) {
        '+' -> cursor = 1
        '-' -> {
            value.append('-')
            cursor = 1
        }

        else -> cursor = 0
    }

    var tail = value.length
    cursor = value.appendNumber(10, literal, cursor)
    if (cursor == -1) return null
    val missingWholePart = tail == value.length

    if (cursor < length && literal[cursor] == '.') {
        tail = value.append('.').length
        cursor = value.appendNumber(10, literal, cursor + 1)
        if (cursor == -1) return null
        if (missingWholePart && tail == value.length) return null
    } else if (missingWholePart) return null

    if (cursor + 1 < length) {
        var character = literal[cursor]
        if (character == 'e' || character == 'E') {
            value.append('e')
            character = literal[++cursor]
            if (character == '-') {
                value.append('-')
                ++cursor
            } else if (character == '+') {
                ++cursor
            }
            cursor = value.appendNumber(10, literal, cursor)
            if (cursor == -1) return null
        }
    }

    if (cursor != length) {
        if (cursor + 1 != length) return null
        when (literal[cursor]) {
            'f', 'F' -> {
                val result = try {
                    value.toString().toFloat()
                } catch (_: NumberFormatException) {
                    return null
                }
                return if (result.isInfinite()) null else FloatTag(result)
            }

            'd', 'D' -> {}
            else -> return null
        }
    }
    val result = try {
        value.toString().toDouble()
    } catch (_: NumberFormatException) {
        return null
    }
    return if (result.isInfinite()) null else DoubleTag(result)
}

fun parseLiteral(unquoted: String): BinaryTag {
    if (unquoted.isEmpty()) return StringTag("")
    if (unquoted.equals("true", true)) return ByteTag(1.toByte())
    if (unquoted.equals("false", true)) return ByteTag(0.toByte())
    return parseIntegerLike(unquoted)
        ?: parseFloatLike(unquoted)
        ?: StringTag(unquoted)
}

class SNBTParser(
    reader: Reader,
    @JvmField val functions: Map<String, NBTFunction> = BUILTIN_FUNCTIONS
) {
    private val lexer: SNBTLexer = SNBTLexer(reader)
    private var start: Long = 0
    private var current: SNBTToken = lexer.nextToken()
    private var end: Long = lexer.pos()
    private var next: SNBTToken = lexer.nextToken()
    private var depth: Int = 0

    fun expect(token: SNBTToken) {
        if (this.current !== token) {
            throw SNBTException(this.start, "Expected $token, got $current")
        }
        this.advance()
    }

    fun advance() {
        this.start = this.end
        this.end = this.lexer.pos()
        this.current = this.next
        this.next = this.lexer.nextToken()
    }

    fun fetch() {
        val lexer = this.lexer
        this.start = lexer.pos()
        this.current = lexer.nextToken()
        this.end = lexer.pos()
        this.next = lexer.nextToken()
    }

    fun advanceIfHasNext(terminator: SNBTToken): Boolean {
        if (this.current === SNBTToken.Comma && this.next !== terminator) {
            this.advance()
            return true
        }
        return false
    }

    fun consume(token: SNBTToken): Boolean {
        if (this.current === token) {
            this.advance()
            return true
        }
        return false
    }

    private fun increaseDepthOrThrow() {
        if (++this.depth < MAX_STACK_DEPTH) return
        throw SNBTException(
            this.start,
            "Tried to parse SNBT with too high complexity, depth > $MAX_STACK_DEPTH"
        )
    }

    fun parseRoot(): Pair<String, BinaryTag> {
        val key = this.parseKey()
        val value = this.parseValue()
        if (this.current === SNBTToken.EOF) return (key ?: "") to value
        throw SNBTException(this.start, "Expected EOF, got $current")
    }

    fun parseKey(): String? {
        val current = this.current
        if (current === SNBTToken.Colon) {
            this.advance()
            return ""
        } else if (this.next === SNBTToken.Colon) {
            if (current is SNBTToken.Literal) {
                this.fetch()
                return current.value
            }
            throw SNBTException(
                this.start,
                "Invalid key, expect string literal, got $current"
            )
        }
        return null
    }

    fun parseValue(): BinaryTag {
        when (val token = this.current) {
            SNBTToken.LBrace -> return CompoundTag(this.parseCompound())
            SNBTToken.LBracket -> {
                this.advance()
                val pos = this.start
                if (this.next === SNBTToken.Semicolon) {
                    val current = this.current
                    val type = (current as? SNBTToken.Literal)?.value
                    if (type != null) {
                        this.fetch()
                        when (type.toChar()) {
                            'b', 'B' -> return ByteArrayTag(
                                this.parseNumericList().toByteList { it.toByte() }
                            )

                            'i', 'I' -> return IntArrayTag(
                                this.parseNumericList().toIntList { it.toInt() }
                            )


                            'l', 'L' -> return LongArrayTag(
                                this.parseNumericList().toLongList { it.toLong() }
                            )

                            's', 'S' -> return ListTag(
                                this.parseNumericList()
                                    .mapTo(mutableListOf(), ::ShortTag)
                            )
                        }
                    }
                    throw SNBTException(
                        pos,
                        "Invalid numeric type ${type ?: current}"
                    )
                }
                return ListTag(this.parseGenericList(SNBTToken.RBracket))
            }

            SNBTToken.LParen -> {
                this.advance()
                return ListTag(this.parseGenericList(SNBTToken.RParen))
            }

            is SNBTToken.Literal.Quoted -> {
                this.advance()
                return StringTag(token.value)
            }

            is SNBTToken.Literal.Unquoted -> {
                val content = token.value
                if (this.next === SNBTToken.LParen) {
                    val pos = this.start
                    val function = this.functions[content]
                        ?: throw SNBTException(pos, "Unknown operation named $content")
                    this.fetch()
                    val arguments = this.parseGenericList(SNBTToken.RParen)
                    return try {
                        function(arguments)
                    } catch (e: RuntimeException) {
                        throw SNBTException(pos, "Failed to execute operation named $content", e)
                    }
                }
                this.advance()
                return parseLiteral(content)
            }

            else -> throw SNBTException(
                this.start,
                if (token === SNBTToken.EOF) "Unexpected EOF" else "Invalid literal: $token"
            )
        }
    }

    fun parseCompound(): MutableMap<String, BinaryTag> {
        this.advance()
        val tags = hashMapOf<String, BinaryTag>()
        if (this.consume(SNBTToken.RBrace)) return tags
        this.increaseDepthOrThrow()
        do {
            tags[
                this.parseKey() ?: throw SNBTException(
                    this.end,
                    "Invalid key-value pair, expect ':'"
                )
            ] = this.parseValue()
        } while (this.advanceIfHasNext(SNBTToken.RBrace))
        --this.depth
        if (this.current === SNBTToken.Comma) {
            this.advance()
        }
        this.expect(SNBTToken.RBrace)
        return tags
    }

    fun parseGenericList(terminator: SNBTToken): MutableList<BinaryTag> {
        val tags = mutableListOf<BinaryTag>()
        if (this.consume(terminator)) return tags
        this.increaseDepthOrThrow()
        do {
            tags.add(this.parseValue())
        } while (this.advanceIfHasNext(terminator))
        --this.depth
        if (this.current === SNBTToken.Comma) {
            this.advance()
        }
        this.expect(terminator)
        return tags
    }

    fun parseNumericList(): MutableList<NumericTag> {
        val tags = mutableListOf<NumericTag>()
        if (this.consume(SNBTToken.RBracket)) return tags
        this.increaseDepthOrThrow()
        do {
            val pos = this.start
            val tag = this.parseValue()
            if (tag is NumericTag) {
                tags.add(tag)
            } else {
                throw SNBTException(pos, "Expected numeric expression")
            }
        } while (this.advanceIfHasNext(SNBTToken.RBracket))
        --this.depth
        if (this.current === SNBTToken.Comma) {
            this.advance()
        }
        this.expect(SNBTToken.RBracket)
        return tags
    }
}
