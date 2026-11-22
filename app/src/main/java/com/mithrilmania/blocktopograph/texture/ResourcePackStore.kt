package com.mithrilmania.blocktopograph.texture

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.mithrilmania.blocktopograph.util.jsonSerializer
import kotlinx.serialization.Serializable

/** List order is priority; the first enabled pack wins. */
@Serializable
data class ResourcePack(
    val id: String,
    /** Relative to filesDir; also the key the resolver opens by. */
    val zipPath: String,
    val enabled: Boolean,
)

val Context.resourcePackStore: DataStore<List<ResourcePack>> by dataStore(
    fileName = "resource_packs.json",
    serializer = jsonSerializer { emptyList() },
)
