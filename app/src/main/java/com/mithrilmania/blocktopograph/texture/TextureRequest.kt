package com.mithrilmania.blocktopograph.texture

/**
 * Coil data type. [path] is already fully resolved, [version] exists only to make the cache key
 * change with the pack stack.
 */
data class TextureRequest(
    val path: String,
    val version: Long,
)
