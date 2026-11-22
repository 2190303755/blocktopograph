package com.mithrilmania.blocktopograph.texture

/**
 * typeId in, request out: manual binding (or the bare name as a guess), then a table lookup.
 * The ViewModel rebuilds it whenever the lookup or either binding table changes.
 */
data class TextureRequestFactory(
    val lookup: TextureLookup,
    private val blockBindings: Map<String, String> = emptyMap(),
    private val itemBindings: Map<String, String> = emptyMap(),
) {
    /** Null when the texture is unknown, which the UI shows as a placeholder. */
    fun createBlockRequest(typeId: String): TextureRequest? =
        createRequest(typeId, blockBindings, lookup.blocks)

    /** Null when the texture is unknown. */
    fun createItemRequest(typeId: String): TextureRequest? =
        createRequest(typeId, itemBindings, lookup.items)

    private fun createRequest(
        typeId: String,
        bindings: Map<String, String>,
        table: TexturePathTable,
    ): TextureRequest? {
        // manual binding wins; otherwise assume the bare name is the texture name
        val textureName = bindings[typeId] ?: typeId.substringAfter(':')
        // split the namespace, then a single map lookup
        val (namespace, name) = splitNamespace(textureName)
        val path = table.find(namespace, name) ?: return null
        return TextureRequest(path, lookup.version)
    }

    companion object {
        val EMPTY = TextureRequestFactory(TextureLookup.EMPTY)
    }
}

/** No colon means an empty namespace. */
internal fun splitNamespace(identifier: String): Pair<String, String> {
    val colon = identifier.indexOf(':')
    return if (colon >= 0) identifier.substring(0, colon) to identifier.substring(colon + 1)
    else "" to identifier
}
