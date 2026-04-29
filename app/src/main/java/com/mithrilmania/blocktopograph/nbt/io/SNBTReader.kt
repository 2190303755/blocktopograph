package com.mithrilmania.blocktopograph.nbt.io

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceAtMost
import com.mithrilmania.blocktopograph.nbt.util.ESCAPE
import com.mithrilmania.blocktopograph.nbt.util.NBTFormatException
import com.mithrilmania.blocktopograph.nbt.util.isSafeLiteral
import java.io.Closeable
import java.io.EOFException
import java.io.InputStream
import java.io.PushbackInputStream

interface SNBTReader {
    /**
     * @return if ends
     */
    fun skipWhitespace(): Boolean
    fun pos(): Long
    fun advance()
    fun peek(): Char
    fun readStringUntil(terminator: Char): String
    fun readLiteral(): String
}

fun String.toHexedChar(offset: Int, length: Int): Int {
    for (i in offset until offset + length) {
        val char = this[i]
        if (char !in '0'..'9'
            && char !in 'A'..'Z'
            && char !in 'a'..'z'
        ) throw IllegalArgumentException()
    }
    val code = this.substring(offset, offset + length).hexToInt()
    if (Character.isValidCodePoint(code)) return code
    throw IllegalArgumentException()
}

class SNBTStringReader(
    val source: String,
    cursor: Int = 0,
    length: Int = source.length
) : SNBTReader {
    var cursor: Int = cursor.fastCoerceAtLeast(0)
    val length: Int = length.fastCoerceAtMost(source.length)
    override fun skipWhitespace(): Boolean {
        val source = this.source
        val length = this.length
        while (this.cursor < length && source[this.cursor].isWhitespace()) {
            ++this.cursor
        }
        return this.cursor >= length
    }

    override fun pos(): Long {
        return this.cursor.toLong()
    }

    override fun advance() {
        ++this.cursor
    }

    override fun peek(): Char {
        return this.source[this.cursor]
    }

    fun readHexedChar(size: Int): String {
        val cursor = ++this.cursor // skip promoter
        if (cursor + size >= this.length) throw EOFException()
        val code = this.source.toHexedChar(cursor, size)
        this.cursor += size
        return Character.toString(code)
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun readNamedChar(): String {
        var cursor = ++this.cursor // skip promoter
        val source = this.source
        val length = this.length
        if (cursor >= length || source[cursor] != '{') throw IllegalArgumentException("Expected { after \\N")
        while (++cursor < length) {
            val char = source[cursor]
            if (char == '}') {
                if (cursor == this.cursor + 1) throw IllegalArgumentException()
                return Character.toString(
                    Character.codePointOf(source.substring(this.cursor, cursor))
                )
            }
            if (char !in '0'..'9'
                && char !in 'A'..'Z'
                && char !in 'a'..'z'
                && char != '-'
                && char != ' '
            ) throw IllegalArgumentException()
        }
        throw EOFException("Expect '}'")
    }

    override fun readStringUntil(terminator: Char): String {
        val builder = StringBuilder()
        var escaped = false
        val source = this.source
        val length = this.length
        while (++this.cursor < length) { // skip promoter
            val char = source[this.cursor]
            if (escaped) {
                when (char) {
                    'b' -> builder.append('\b')
                    'f' -> builder.append('\u000C')
                    'n' -> builder.append('\n')
                    'r' -> builder.append('\r')
                    's' -> builder.append(' ')
                    't' -> builder.append('\t')
                    '\\' -> builder.append('\\')
                    '\'' -> builder.append('\'')
                    '"' -> builder.append('"')
                    'x' -> builder.append(this.readHexedChar(2))
                    'u' -> builder.append(this.readHexedChar(4))
                    'U' -> builder.append(this.readHexedChar(8))
                    'N' -> {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                            builder.append('\\').append('N')
                        } else {
                            builder.append(this.readNamedChar())
                        }
                    }

                    else -> throw NBTFormatException("Invalid escape at position $cursor")
                }
            } else if (char == ESCAPE) {
                escaped = true
            } else if (char == terminator) {
                ++this.cursor // consume this
                return builder.toString()
            } else {
                builder.append(char)
            }
        }
        throw EOFException("Expect '$terminator'")
    }

    override fun readLiteral(): String {
        val start = this.cursor
        val source = this.source
        val length = this.length
        while (this.cursor < length && source[this.cursor].isSafeLiteral()) {
            ++this.cursor
        }
        if (start == this.cursor) {
            if (start < length) {
                throw IllegalArgumentException("Invalid byte 0x${source[start].code.toHexString()} at $start")
            } else {
                throw EOFException("Expect unquoted literal")
            }
        }
        return source.substring(start, this.cursor)
    }
}

class SNBTStreamReader(
    val stream: PushbackInputStream
) : Closeable by stream, SNBTReader {
    constructor(stream: InputStream) : this(PushbackInputStream(stream, 1))

    val buffer: ByteArray = ByteArray(8)
    var count: Long = 0L
    override fun skipWhitespace(): Boolean {
        val stream = this.stream
        var char: Int
        do {
            char = stream.read()
            if (char == -1) return true
            ++this.count
        } while (char.toChar().isWhitespace())
        stream.unread(char)
        --this.count
        return false
    }

    override fun pos(): Long {
        return this.count
    }

    override fun advance() {
        this.count += this.stream.skip(1L)
    }

    override fun peek(): Char {
        val char = this.stream.read()
        this.stream.unread(char)
        return char.toChar()
    }

    fun readHexedChar(size: Int): String {
        val read = this.stream.read(this.buffer, 0, size)
        if (read < size) throw EOFException()
        val code = String(this.buffer, 0, size)
            .toHexedChar(0, size)
        this.count += size
        return Character.toString(code)
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun readNamedChar(): String {
        val stream = this.stream
        if (stream.read() != '{'.code) throw IllegalArgumentException("Expected { after \\N")
        ++this.count
        val builder = StringBuilder()
        while (true) {
            val read = stream.read()
            if (read == -1) throw EOFException("Expect '}'")
            val char = read.toChar()
            if (char == '}') {
                if (builder.isEmpty()) throw IllegalArgumentException()
                return Character.toString(
                    Character.codePointOf(builder.toString())
                )
            }
            if (char !in '0'..'9'
                && char !in 'A'..'Z'
                && char !in 'a'..'z'
                && char != '-'
                && char != ' '
            ) throw IllegalArgumentException()
            builder.append(char)
            ++this.count
        }
    }

    override fun readStringUntil(terminator: Char): String {
        this.advance() // skip promoter
        val builder = StringBuilder()
        var escaped = false
        val stream = this.stream
        while (true) {
            val read = stream.read()
            if (read == -1) throw EOFException("Expect '$terminator'")
            val char = read.toChar()
            ++this.count
            if (escaped) {
                when (read.toChar()) {
                    'b' -> builder.append('\b')
                    'f' -> builder.append('\u000C')
                    'n' -> builder.append('\n')
                    'r' -> builder.append('\r')
                    's' -> builder.append(' ')
                    't' -> builder.append('\t')
                    '\\' -> builder.append('\\')
                    '\'' -> builder.append('\'')
                    '"' -> builder.append('"')
                    'x' -> builder.append(this.readHexedChar(2))
                    'u' -> builder.append(this.readHexedChar(4))
                    'U' -> builder.append(this.readHexedChar(8))
                    'N' -> {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                            builder.append('\\').append('N')
                        } else {
                            builder.append(this.readNamedChar())
                        }
                    }

                    else -> throw NBTFormatException("Invalid escape at position ${count - 1}")
                }
            } else if (char == ESCAPE) {
                escaped = true
            } else if (char == terminator) {
                return builder.toString()
            } else {
                builder.append(char)
            }
        }
    }

    override fun readLiteral(): String {
        val builder = StringBuilder()
        val stream = this.stream
        var read: Int
        while (true) {
            read = stream.read()
            if (read == -1) break
            val char = read.toChar()
            if (char.isSafeLiteral()) {
                ++this.count
                builder.append(char)
            } else {
                stream.unread(read)
                break
            }
        }
        if (builder.isEmpty()) {
            if (read == -1) {
                throw EOFException("Expect unquoted literal")
            } else {
                throw IllegalArgumentException("Invalid byte 0x${read.toHexString()} at $count")
            }
        }
        return builder.toString()
    }
}