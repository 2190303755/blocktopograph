package com.mithrilmania.blocktopograph.nbt.util

sealed interface SNBTToken {
    sealed interface Literal : SNBTToken {
        val value: String

        class Unquoted(override val value: String) : Literal {
            override fun toString() = "unquoted literal"
        }

        class Quoted(override val value: String) : Literal {
            override fun toString() = "quoted literal"
        }
    }

    object Colon : SNBTToken {
        override fun toString() = "':'"
    }

    object Semicolon : SNBTToken {
        override fun toString() = "';'"
    }

    object Comma : SNBTToken {
        override fun toString() = "','"
    }

    object LBrace : SNBTToken {
        override fun toString() = "'{'"
    }

    object RBrace : SNBTToken {
        override fun toString() = "'}'"
    }

    object LBracket : SNBTToken {
        override fun toString() = "'['"
    }

    object RBracket : SNBTToken {
        override fun toString() = "']'"
    }

    object LParen : SNBTToken {
        override fun toString() = "'('"
    }

    object RParen : SNBTToken {
        override fun toString() = "')'"
    }

    object EOF : SNBTToken {
        override fun toString() = "EOF"
    }
}