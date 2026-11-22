package com.mithrilmania.blocktopograph.util

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.mithrilmania.blocktopograph.texture.lenientTextureJson
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.InputStream
import java.io.OutputStream

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> jsonSerializer(
    crossinline fallback: () -> T
): Serializer<T> = object : Serializer<T> {
    override val defaultValue: T get() = fallback()

    override suspend fun readFrom(input: InputStream): T = try {
        lenientTextureJson.decodeFromStream(input)
    } catch (e: SerializationException) {
        throw CorruptionException("Failed to deserialize JSON as ${T::class}", e)
    }

    override suspend fun writeTo(t: T, output: OutputStream) {
        lenientTextureJson.encodeToStream(t, output)
    }
}
