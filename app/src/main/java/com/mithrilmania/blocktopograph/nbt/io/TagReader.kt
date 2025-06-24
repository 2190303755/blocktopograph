package com.mithrilmania.blocktopograph.nbt.io

import com.mithrilmania.blocktopograph.nbt.BinaryTag
import java.io.DataInput

interface TagReader<T : BinaryTag<*>> {
    fun read(input: DataInput, depth: Int = 0): T
}