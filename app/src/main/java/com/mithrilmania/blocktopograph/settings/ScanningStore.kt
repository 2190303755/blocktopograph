package com.mithrilmania.blocktopograph.settings

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.net.toUri
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mithrilmania.blocktopograph.util.tryOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.scanLocations by preferencesDataStore(name = "scan_locations")

data class ScanLocations(
    val paths: List<String>,
    val uris: List<Uri>,
)

fun Preferences.Key<Set<String>>.asPathsFlow(context: Context): Flow<Set<String>> =
    context.scanLocations.data.map { it[this].orEmpty() }

@Composable
fun Preferences.Key<Set<String>>.asPathsState(context: Context) =
    remember { this.asPathsFlow(context) }.collectAsStateWithLifecycle(emptySet())

/**
 * Persisted initial scan paths, consumed by WorldListActivity on startup.
 */
object ScanningStore {
    @JvmField
    val PATHS = stringSetPreferencesKey("scan_paths")

    @JvmField
    val URIS = stringSetPreferencesKey("scan_uris")

    suspend fun read(context: Context): ScanLocations {
        val preferences = context.scanLocations.data.first()
        val paths = preferences[PATHS].orEmpty().filter { it.isNotBlank() }
        val uris = preferences[URIS].orEmpty().filter { it.isNotBlank() }
            .mapNotNull { uri -> tryOrNull { uri.toUri() } }
        return ScanLocations(paths, uris)
    }

    suspend fun add(context: Context, path: String) {
        context.scanLocations.edit { preferences ->
            preferences[PATHS] = (preferences[PATHS] ?: emptySet()) + path
        }
    }

    suspend fun remove(context: Context, path: String) {
        context.scanLocations.edit { preferences ->
            preferences[PATHS] = (preferences[PATHS] ?: emptySet()) - path
        }
    }

    suspend fun add(context: Context, uri: Uri) {
        context.scanLocations.edit { preferences ->
            preferences[URIS] = (preferences[URIS] ?: emptySet()) + uri.toString()
        }
    }

    suspend fun remove(context: Context, uri: Uri) {
        context.scanLocations.edit { preferences ->
            preferences[URIS] = (preferences[URIS] ?: emptySet()) - uri.toString()
        }
    }
}