package com.mithrilmania.blocktopograph.nbt.util

sealed class NBTException(message: String) : RuntimeException(message)
class NBTFormatException(message: String) : NBTException(message)
class NBTStackOverflowException(message: String) : NBTException(message)