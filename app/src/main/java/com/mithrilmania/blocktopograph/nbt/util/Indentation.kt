package com.mithrilmania.blocktopograph.nbt.util

interface Indentation {
    fun beginStructure(appendable: Appendable)
    fun applyToElement(appendable: Appendable)
    fun endStructure(appendable: Appendable)
}

fun Indentation(
    unit: String? = "    "
): Indentation = if (unit.isNullOrEmpty()) {
    CompactIndentation
} else {
    IndentationImpl(unit)
}

fun Indentation(
    enabled: Boolean
): Indentation = if (enabled) {
    IndentationImpl()
} else {
    CompactIndentation
}

fun Appendable.indent(unit: String, depth: Int) {
    repeat(depth) {
        this.append(unit)
    }
}

object CompactIndentation : Indentation {
    override fun beginStructure(appendable: Appendable) {}
    override fun endStructure(appendable: Appendable) {}
    override fun applyToElement(appendable: Appendable) {
        appendable.append(' ')
    }
}

class IndentationImpl(
    val unit: String = "    ",
    var depth: Int = 0
) : Indentation {
    override fun beginStructure(appendable: Appendable) {
        appendable.append('\n').indent(this.unit, ++this.depth)
    }

    override fun applyToElement(appendable: Appendable) {
        appendable.append('\n').indent(this.unit, this.depth)
    }

    override fun endStructure(appendable: Appendable) {
        appendable.append('\n').indent(this.unit, --this.depth)
    }
}
