package com.mithrilmania.blocktopograph.nbt.io

enum class NBTFormat(
    val isHeaderAvailable: Boolean
) {
    UNKNOWN(true),
    STRINGIFIED(false),
    BIG_ENDIAN(false),
    LITTLE_ENDIAN(true)
}