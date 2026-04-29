package com.mithrilmania.blocktopograph.nbt.util

import com.mithrilmania.blocktopograph.nbt.io.SNBTReader

const val DOUBLE_QUOTE = '"'
const val SINGLE_QUOTE = '\''
const val ESCAPE = '\\'

sealed interface Token {
    sealed interface Literal : Token {
        val value: String

        class Unquoted(override val value: String) : Literal {
            override fun toString() = "unquoted literal"
        }

        class Quoted(override val value: String) : Literal {
            override fun toString() = "quoted literal"
        }
    }

    object Colon : Token {
        override fun toString() = "':'"
    }

    object Semicolon : Token {
        override fun toString() = "';'"
    }

    object Comma : Token {
        override fun toString() = "','"
    }

    object LBrace : Token {
        override fun toString() = "'{'"
    }

    object RBrace : Token {
        override fun toString() = "'}'"
    }

    object LBracket : Token {
        override fun toString() = "'['"
    }

    object RBracket : Token {
        override fun toString() = "']'"
    }

    object LParen : Token {
        override fun toString() = "'('"
    }

    object RParen : Token {
        override fun toString() = "')'"
    }

    object EOF : Token {
        override fun toString() = "EOF"
    }
}

fun SNBTReader.consume(token: Token): Token {
    this.advance()
    return token
}

fun SNBTReader.nextToken(): Token {
    if (this.skipWhitespace()) return Token.EOF
    return when (this.peek()) {
        DOUBLE_QUOTE -> Token.Literal.Quoted(this.readStringUntil(DOUBLE_QUOTE))
        SINGLE_QUOTE -> Token.Literal.Quoted(this.readStringUntil(SINGLE_QUOTE))
        ':' -> this.consume(Token.Colon)
        ';' -> this.consume(Token.Semicolon)
        ',' -> this.consume(Token.Comma)
        '{' -> this.consume(Token.LBrace)
        '}' -> this.consume(Token.RBrace)
        '[' -> this.consume(Token.LBracket)
        ']' -> this.consume(Token.RBracket)
        '(' -> this.consume(Token.LParen)
        ')' -> this.consume(Token.RParen)
        else -> Token.Literal.Unquoted(this.readLiteral())
    }
}