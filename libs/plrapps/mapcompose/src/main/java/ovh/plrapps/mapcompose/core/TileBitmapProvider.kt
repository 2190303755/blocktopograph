package ovh.plrapps.mapcompose.core

import android.graphics.Bitmap

/**
 * Defines how tiles should be fetched. It must be supplied as part of the configuration of
 * MapCompose.
 *
 * The [getTileBitmap] method implementation may suspend, but it isn't required (e.g, it isn't
 * required to switch context using withContext(Dispatcher.IO) { ... }) as MapCompose does that
 * already. The [getTileBitmap] method is declared using the suspend modifier, as it is sometimes
 * useful to provide an implementation which suspends.
 *
 * MapCompose leverages bitmap pooling to reduce the pressure on the garbage collector. However,
 * there's no tile caching by default - this is an implementation detail of the supplied
 * [TileBitmapProvider].
 *
 * If [getTileBitmap] returns null, the tile won't be rendered.
 * The library does not handle exceptions thrown from [getTileBitmap]. Such errors are treated as
 * unrecoverable failures.
 */
fun interface TileBitmapProvider {
    suspend fun getTileBitmap(row: Int, col: Int, zoomLvl: Int): Bitmap?
}