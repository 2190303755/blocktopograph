package com.mithrilmania.blocktopograph.test

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.mithrilmania.blocktopograph.storage.VirtualFile
import com.mithrilmania.blocktopograph.util.ByteArrayMatcher
import org.iq80.leveldb.DB
import org.iq80.leveldb.DBIterator
import org.iq80.leveldb.util.Slice
import org.iq80.leveldb.util.Slices

class LDBEntry(
    val db: DB,
    val key: ByteArray
) {
    val plainText = this.key.toString(Charsets.UTF_8)
    val hexedText = "0x" + this.key.toHexString()
    fun toFile(): VirtualFile = VirtualFile(
        this.db,
        if (this.plainText.any { it.isISOControl() }) this.hexedText else this.plainText,
        this.key
    )
}

class LDBPagingSource(
    val iterator: DBIterator,
    val pattern: ByteArray
) : PagingSource<Slice, ByteArray>() {
    val failure: IntArray = ByteArrayMatcher.computeFailure(this.pattern)

    init {
        this.registerInvalidatedCallback {
            this.iterator.close()
        }
    }

    override suspend fun load(params: LoadParams<Slice>): LoadResult<Slice, ByteArray> {
        return try {
            val pattern = this.pattern
            val iterator = this.iterator
            val location = params.key?.bytes
            if (location === null) {
                iterator.seekToFirst()
            } else {
                iterator.seek(location)
            }
            val keys = mutableListOf<ByteArray>()
            if (params is LoadParams.Prepend) {
                if (pattern.isEmpty()) {
                    while (iterator.hasPrev() && keys.size < params.loadSize) {
                        keys.add(iterator.prev().key)
                    }
                } else {
                    val failure = this.failure
                    while (iterator.hasPrev() && keys.size < params.loadSize) {
                        val key = iterator.prev().key
                        if (ByteArrayMatcher.contains(key, pattern, failure)) {
                            keys.add(key)
                        }
                    }
                }
                LoadResult.Page(
                    keys.asReversed(),
                    if (iterator.hasPrev()) iterator.peekNext().key?.let { Slices.wrappedBuffer(it) } else null,  // exclusive
                    params.key // inclusive
                )
            } else {
                if (pattern.isEmpty()) {
                    while (iterator.hasNext() && keys.size < params.loadSize) {
                        keys.add(iterator.next().key)
                    }
                } else {
                    val failure = this.failure
                    while (iterator.hasNext() && keys.size < params.loadSize) {
                        val key = iterator.next().key
                        if (ByteArrayMatcher.contains(key, pattern, failure)) {
                            keys.add(key)
                        }
                    }
                }
                LoadResult.Page(
                    keys,
                    params.key, // exclusive
                    if (iterator.hasNext()) iterator.peekNext().key?.let { Slices.wrappedBuffer(it) } else null // inclusive
                )
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Slice, ByteArray>): Slice? = null
}
