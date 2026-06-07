package com.mithrilmania.blocktopograph.util

fun CharSequence.endsWithNumber(start: Int = 0): Boolean {
    val len: Int = this.length
    var code: Int
    var offset = start
    while (offset < len) {
        code = Character.codePointAt(this, offset)
        if (!Character.isDigit(code)) return false
        offset += Character.charCount(code)
    }
    return true
}

fun CharSequence.isNumber() = this.endsWithNumber(
    if (this.isNotEmpty() && (this[0] == '-' || this[0] == '+')) 1 else 0
)

fun CharSequence.endsWithDecimal(start: Int = 0): Boolean {
    val len: Int = this.length
    var code: Int
    var offset = start
    while (offset < len) {
        code = Character.codePointAt(this, offset)
        if (code == '.'.code) return this.endsWithNumber(offset + 1)
        if (!Character.isDigit(code)) return false
        offset += Character.charCount(code)
    }
    return true
}

fun CharSequence.isDecimal() = this.endsWithDecimal(
    if (this.isNotEmpty() && (this[0] == '-' || this[0] == '+')) 1 else 0
)


