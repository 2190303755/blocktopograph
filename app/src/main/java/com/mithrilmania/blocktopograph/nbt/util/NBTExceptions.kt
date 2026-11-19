package com.mithrilmania.blocktopograph.nbt.util

sealed class NBTException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)

class NBTFormatException(message: String, cause: Throwable? = null) : NBTException(message, cause)
class NBTStackOverflowException(message: String) : NBTException(message)

class SNBTException(
    val position: Long,
    val detail: String,
    cause: Throwable? = null
) : NBTException("$detail at $position", cause)
