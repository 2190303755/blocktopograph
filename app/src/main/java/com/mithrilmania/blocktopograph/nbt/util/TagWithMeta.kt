package com.mithrilmania.blocktopograph.nbt.util

import com.mithrilmania.blocktopograph.nbt.BinaryTag
import com.mithrilmania.blocktopograph.nbt.io.NBTExportConfig

data class TagWithMeta(
    @JvmField val tag: BinaryTag,
    @JvmField val name: String,
    @JvmField val compressed: Boolean = false,
    override val storageVersion: Int? = null,
    override val littleEndian: Boolean = true,
    @JvmField val stringified: Boolean = false
) : NBTExportConfig {
    override val compress: Boolean get() = this.compressed
    override val stringify: Boolean get() = this.stringified
    override val prettify: Boolean get() = this.stringified
    override val heterogeneous: Boolean get() = false
}
