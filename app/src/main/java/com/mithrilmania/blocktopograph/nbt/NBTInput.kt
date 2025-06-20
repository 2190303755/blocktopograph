package com.mithrilmania.blocktopograph.nbt

import net.benwoodworth.knbt.NbtCompound
import net.benwoodworth.knbt.NbtCompression
import net.benwoodworth.knbt.NbtVariant

class NBTInput(
    val string: Boolean,
    val name: String,
    val data: NbtCompound,
    val version: Byte? = null,
    val variant: NbtVariant = NbtVariant.Bedrock,
    val compression: NbtCompression = NbtCompression.None
)