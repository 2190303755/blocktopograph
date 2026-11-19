package com.mithrilmania.blocktopograph.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.mithrilmania.blocktopograph.PreparedWorkspace
import com.mithrilmania.blocktopograph.storage.Workspace
import com.mithrilmania.blocktopograph.storage.resolve
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

typealias SourcedWorkspace = Pair<String, Workspace>

private val Context.workspaces by preferencesDataStore(name = "prepared_worlds")
private fun ByteArray.decode(): PreparedWorkspace? {
    if (this.size != 24) return null
    return runCatching {
        DataInputStream(ByteArrayInputStream(this)).use { input ->
            PreparedWorkspace().apply {
                uuidMostSignificantBits = input.readLong()
                uuidLeastSignificantBits = input.readLong()
                manifestModifiedMillis = input.readLong()
            }
        }
    }.getOrNull()
}

/**
 * Client-side mapping between a world and its prepared leveldb copy.
 * Stores the cache slot UUID and the source MANIFEST modification time so that
 * unchanged worlds can reuse their previous copy instead of re-copying.
 *
 * Record layout: |uuidMostSignificantBits|uuidLeastSignificantBits|manifestModifiedMillis|
 */
object WorkspaceStore {
    suspend fun read(context: Context, worldId: String): PreparedWorkspace? {
        val preferences = context.workspaces.data.first()
        return preferences[byteArrayPreferencesKey(worldId)]?.let(ByteArray::decode)
    }

    fun entries(context: Context): Flow<List<SourcedWorkspace>> {
        val appContext = context.applicationContext
        return appContext.workspaces.data.map {
            val cache = appContext.externalCacheDir ?: return@map emptyList()
            val valid = mutableListOf<SourcedWorkspace>()
            val stale = mutableListOf<Preferences.Key<*>>()
            it.asMap().forEach { (key, value) ->
                val workspace = (value as? ByteArray)?.decode()?.resolve(cache)
                if (workspace !== null && workspace.file.isDirectory) {
                    valid.add(key.name to workspace)
                } else {
                    stale.add(key)
                }
            }
            if (stale.isNotEmpty()) {
                appContext.workspaces.edit { prefs ->
                    stale.forEach { prefs.remove(it) }
                }
            }
            valid
        }.flowOn(Dispatchers.IO)
    }

    suspend fun remove(context: Context, worldId: String): PreparedWorkspace? {
        val preferences = context.workspaces.data.first()
        val info = preferences[byteArrayPreferencesKey(worldId)] ?: return null
        context.workspaces.edit { it.remove(byteArrayPreferencesKey(worldId)) }
        return info.decode()
    }

    suspend fun write(context: Context, worldId: String, result: PreparedWorkspace) {
        val bytes = ByteArrayOutputStream(24).apply {
            DataOutputStream(this).use { output ->
                output.writeLong(result.uuidMostSignificantBits)
                output.writeLong(result.uuidLeastSignificantBits)
                output.writeLong(result.manifestModifiedMillis)
            }
        }.toByteArray()
        context.workspaces.edit {
            it[byteArrayPreferencesKey(worldId)] = bytes
        }
    }
}
