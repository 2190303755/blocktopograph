package com.mithrilmania.blocktopograph.settings

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.flags by preferencesDataStore(name = "flags")

suspend fun Preferences.Key<Boolean>.getFlag(context: Context) =
    context.flags.data.first()[this]

suspend fun Preferences.Key<Boolean>.setFlag(context: Context, value: Boolean) {
    context.flags.edit { it[this] = value }
}

fun Preferences.Key<Boolean>.asFlagFlow(context: Context, fallback: Boolean) =
    context.flags.data.map { it[this] ?: fallback }

@Composable
fun Preferences.Key<Boolean>.asFlagState(context: Context, fallback: Boolean) =
    remember(context, fallback) {
        this.asFlagFlow(context, fallback)
    }.collectAsStateWithLifecycle(fallback)

object FlagStore {
    @JvmField
    val IS_SHIZUKU_ALLOWED: Preferences.Key<Boolean> = booleanPreferencesKey("IsShizukuAllowed")

    @JvmField
    val IS_WORKSPACE_ENABLED: Preferences.Key<Boolean> = booleanPreferencesKey("IsWorkspaceEnabled")
}