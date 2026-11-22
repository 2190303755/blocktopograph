package com.mithrilmania.blocktopograph.texture

import coil3.key.Keyer
import coil3.request.Options

/**
 * path covers edits to bindings or atlases, version covers the same path resolving to a different pack.
 * Both ride along on the request, so nothing global is needed.
 */
class TextureKeyer : Keyer<TextureRequest> {
    override fun key(data: TextureRequest, options: Options): String =
        "${data.path}@v${data.version}"
}
