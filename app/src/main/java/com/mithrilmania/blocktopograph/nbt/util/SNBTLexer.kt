package com.mithrilmania.blocktopograph.nbt.util

import android.os.Build
import androidx.annotation.RequiresApi
import java.io.EOFException
import java.io.PushbackReader
import java.io.Reader

const val DOUBLE_QUOTE_CHAR = '"'
const val SINGLE_QUOTE_CHAR = '\''
const val BACKSLASH_CHAR = '\\'

class SNBTLexer(reader: Reader) {
    private val reader: PushbackReader = reader as? PushbackReader ?: PushbackReader(reader)
    private val buffer: CharArray = CharArray(8)
    private var count: Long = 0L
    fun pos(): Long = this.count

    fun readIgnoringWhitespace(): Int {
        val reader = this.reader
        var read: Int
        do {
            read = reader.read()
            if (read == -1) return -1
            ++this.count
        } while (read.toChar().isWhitespace())
        return read
    }

    fun readHexedChar(size: Int): String {
        val read = this.reader.read(this.buffer, 0, size)
        if (read < size) throw EOFException(
            "Expected a char sequence of length $size, got $read"
        )
        val code = String(this.buffer, 0, size).hexToCharCode()
        this.count += size
        return Character.toString(code)
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun readNamedChar(): String {
        val reader = this.reader
        val read = reader.read()
        if (read == -1) throw EOFException("Expected { after \\N, got EOF")
        if (read != '{'.code) throw IllegalArgumentException(
            "Expected { after \\N, got 0x${Integer.toHexString(read)}"
        )
        ++this.count
        val builder = StringBuilder()
        while (true) {
            val value = reader.read()
            if (value == -1) throw EOFException("Expected }, got EOF")
            ++this.count
            val char = value.toChar()
            if (char == '}') return Character.toString(
                Character.codePointOf(builder.toString())
            )
            if (char !in '0'..'9'
                && char !in 'A'..'Z'
                && char !in 'a'..'z'
                && char != '-'
                && char != ' '
            ) throw IllegalArgumentException("Invalid character in Unicode name: $char")
            builder.append(char)
        }
    }

    fun readStringUntil(terminator: Char): String {
        val builder = StringBuilder()
        var escaped = false
        val reader = this.reader
        while (true) {
            val read = reader.read()
            if (read == -1) throw SNBTException(this.count, "Expected $terminator")
            val char = read.toChar()
            ++this.count
            if (escaped) {
                val pos = this.count - 1
                escaped = false
                try {
                    when (char) {
                        'b' -> builder.append('\b')
                        'f' -> builder.append('\u000c')
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

                        else -> throw SNBTException(pos, "Invalid escape \\$char")
                    }
                } catch (e: SNBTException) {
                    throw e
                } catch (e: Exception) {
                    throw SNBTException(pos, "Invalid escape \\$char", e)
                }
            } else if (char == BACKSLASH_CHAR) {
                escaped = true
            } else if (char == terminator) {
                return builder.toString()
            } else {
                builder.append(char)
            }
        }
    }

    fun readLiteral(): String {
        val builder = StringBuilder()
        val reader = this.reader
        var read: Int
        while (true) {
            read = reader.read()
            if (read == -1) break
            val char = read.toChar()
            if (char.isValidInLiteral()) {
                ++this.count
                builder.append(char)
            } else {
                reader.unread(read)
                break
            }
        }
        if (builder.isEmpty()) {
            val pos = this.count
            if (read == -1) {
                throw SNBTException(pos, "Unexpected EOF")
            }
            throw SNBTException(
                pos,
                "Invalid character 0x${Integer.toHexString(read)}"
            )
        }
        return builder.toString()
    }

    fun nextToken(): SNBTToken {
        val read = this.readIgnoringWhitespace()
        if (read == -1) return SNBTToken.EOF
        return when (read.toChar()) {
            DOUBLE_QUOTE_CHAR -> SNBTToken.Literal.Quoted(this.readStringUntil(DOUBLE_QUOTE_CHAR))
            SINGLE_QUOTE_CHAR -> SNBTToken.Literal.Quoted(this.readStringUntil(SINGLE_QUOTE_CHAR))
            ':' -> SNBTToken.Colon
            ';' -> SNBTToken.Semicolon
            ',' -> SNBTToken.Comma
            '{' -> SNBTToken.LBrace
            '}' -> SNBTToken.RBrace
            '[' -> SNBTToken.LBracket
            ']' -> SNBTToken.RBracket
            '(' -> SNBTToken.LParen
            ')' -> SNBTToken.RParen
            else -> {
                --this.count
                this.reader.unread(read)
                SNBTToken.Literal.Unquoted(this.readLiteral())
            }
        }
    }
}
