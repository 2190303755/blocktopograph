package com.mithrilmania.blocktopograph.world

import android.app.Activity
import android.content.Intent.EXTRA_TITLE
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import com.mithrilmania.blocktopograph.EXTRA_PATH
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.storage.SAFFile
import com.mithrilmania.blocktopograph.util.LEVEL_DB_TAG
import com.mithrilmania.blocktopograph.util.findChild
import com.mithrilmania.blocktopograph.world.access.SAFWorldAccess
import com.mithrilmania.blocktopograph.world.access.ShizukuWorldAccess
import com.mithrilmania.blocktopograph.world.access.WorldAccess
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async


object UriKey : CreationExtras.Key<Uri>
class WorldModelFactory(val factory: (WorldAccess) -> WorldModel) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        require(modelClass.isAssignableFrom(WorldModel::class.java)) {
            "Unsupported ViewModel class"
        }
        val bundle = extras[DEFAULT_ARGS_KEY]
        val uri = extras[UriKey]
        val context = requireNotNull(extras[APPLICATION_KEY]) {
            "Missing context"
        }
        if (uri === null) {
            val path = requireNotNull(bundle?.getString(EXTRA_PATH)) {
                "Missing path"
            }
            return this.factory(
                ShizukuWorldAccess(
                    path,
                    context,
                    bundle.getString(EXTRA_TITLE)
                        ?: extras[APPLICATION_KEY]?.getString(R.string.world_default_name)
                )
            ) as T
        }
        val config = requireNotNull(uri.findChild(context.contentResolver, FILE_LEVEL_DAT)) {
            "Invalid world"
        }
        return this.factory(
            SAFWorldAccess(
                uri,
                SAFFile(config),
                context,
                bundle?.getString(EXTRA_TITLE)
                    ?: context.getString(R.string.world_default_name)
            )
        ) as T
    }
}

fun ComponentActivity.getOrCreateWorldModel(): WorldModel = ViewModelProvider(
    this.viewModelStore,
    WorldModelFactory(::WorldModel),
    this.collectWorldCreationExtras()
)[WorldModel::class.java]

fun Activity.collectWorldCreationExtras(): CreationExtras {
    val extras = MutableCreationExtras()
    extras[APPLICATION_KEY] = this.application
    val intent = this.intent
    if (intent !== null) {
        intent.data?.let {
            extras[UriKey] = it
        }
        intent.extras?.let {
            extras[DEFAULT_ARGS_KEY] = it
        }
    }
    return extras
}

class WorldModel(val world: WorldAccess) : ViewModel() {
    private var current: WorldStorage? = null
    val storage: Deferred<WorldStorage?> = viewModelScope.async {
        world.open().also {
            current = it
        }
    }

    fun getCurrentStorage(): WorldStorage? {
        return this.current
    }

    suspend fun awaitStorage(): WorldStorage? = try {
        this.storage.await()
    } catch (e: Exception) {
        Log.w(LEVEL_DB_TAG, "Unable to open world", e)
        null
    }

    override fun onCleared() {
        this.current?.close()
    }
}