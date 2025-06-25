package com.mithrilmania.blocktopograph.nbt.util

import com.mithrilmania.blocktopograph.EMPTY_CHAR
import com.mithrilmania.blocktopograph.nbt.util.SNBTParser.Companion.DOUBLE_QUOTE
import com.mithrilmania.blocktopograph.nbt.util.SNBTParser.Companion.SINGLE_QUOTE
import java.util.regex.Pattern

val SIMPLE_VALUE: Pattern = Pattern.compile("[A-Za-z0-9._+-]+")

fun StringBuilder.appendQuoted(text: String): StringBuilder {
    var quote = EMPTY_CHAR
    val length = this.length
    this.append(' ')
    text.forEach { char ->
        if (char == '\\') {
            this.append('\\')
        } else if (char == '"' || char == '\'') {
            if (quote == EMPTY_CHAR) {
                quote = if (char == '"') '\'' else '"'
            }
            if (quote == char) {
                this.append('\\')
            }
        }
        this.append(char)
    }
    if (quote == EMPTY_CHAR) {
        quote = '"'
    }
    this.setCharAt(length, quote)
    return this.append(quote)
}

fun StringBuilder.appendSafeLiteral(
    text: String
): StringBuilder = if (SIMPLE_VALUE.matcher(text).matches()) {
    this.append(text)
} else {
    this.appendQuoted(text)
}

fun StringBuilder.indent(unit: String, depth: Int): StringBuilder {
    repeat(depth) { this.append(unit) }
    return this
}

inline fun <reified T> StringBuilder.append(
    iterable: Iterable<T>,
    indent: StringBuilder.() -> Unit,
    action: (T) -> Unit
) {
    val iterator = iterable.iterator()
    if (iterator.hasNext()) {
        this.indent()
        action(iterator.next())
        while (iterator.hasNext()) {
            this.append(',')
            this.indent()
            action(iterator.next())
        }
    }
}

fun Char.isQuote() = when (this) {
    DOUBLE_QUOTE, SINGLE_QUOTE -> true
    else -> false
}

fun Char.isSafeLiteral() = when (this) {
    in '0'..'9',
    in 'A'..'Z',
    in 'a'..'z',
    '_', '-', '.', '+' -> true

    else -> false
}