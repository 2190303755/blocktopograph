package com.mithrilmania.blocktopograph.block.icon

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.FileNotFoundException
import java.lang.ref.SoftReference
import java.util.concurrent.ConcurrentHashMap

private val CACHE = ConcurrentHashMap<String, SoftReference<Bitmap>>()

private fun Context.loadIcon(path: String): Bitmap? {
    try {
        return Bitmap.createScaledBitmap(
            BitmapFactory.decodeStream(
                this.assets.open(path)
            ), 120, 120, false
        )
    } catch (e: FileNotFoundException) {
        //TODO file-paths were generated from block names; some do not actually exist...
    } catch (e: Exception) {
        Log.w("Failed to load icon with path: $path", e)
    }
    return null
}

fun Context.loadIconWithCache(path: String): Bitmap? {
    var result: Bitmap? = CACHE[path]?.get()
    if (result === null) {
        result = this.loadIcon(path)
        if (result !== null) {
            CACHE[path] = SoftReference(result)
        }
    }
    return result
}
