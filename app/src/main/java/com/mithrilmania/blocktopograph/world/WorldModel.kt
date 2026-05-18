package com.mithrilmania.blocktopograph.world

import android.app.Activity
import android.content.Context
import android.content.Intent.EXTRA_TITLE
import android.net.Uri
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
import com.mithrilmania.blocktopograph.util.findChild
import com.mithrilmania.blocktopograph.world.impl.SAFWorld
import com.mithrilmania.blocktopograph.world.impl.ShizukuWorld
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch


object UriKey : CreationExtras.Key<Uri>
class WorldModelFactory<W : WorldModel>(val factory: (World) -> W) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        require(modelClass.isAssignableFrom(WorldModel::class.java)) {
            "Unsupported ViewModel class"
        }
        val bundle = extras[DEFAULT_ARGS_KEY]
        val uri = extras[UriKey]
        if (uri === null) {
            val path = requireNotNull(bundle?.getString(EXTRA_PATH)) {
                "Missing path"
            }
            return this.factory(
                ShizukuWorld(
                    path,
                    bundle.getString(EXTRA_TITLE)
                        ?: extras[APPLICATION_KEY]?.getString(R.string.world_default_name)
                )
            ) as T
        }
        val context = requireNotNull(extras[APPLICATION_KEY]) {
            "Missing context"
        }
        val config = requireNotNull(uri.findChild(context.contentResolver, FILE_LEVEL_DAT)) {
            "Invalid world"
        }
        return this.factory(
            SAFWorld(
                uri,
                config,
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

open class WorldModel(val world: World) : ViewModel(world) {
    var storage: Deferred<WorldStorage?> = CompletableDeferred(value = null)
        private set

    fun open(context: Context) {
        val app = context.applicationContext
        this.viewModelScope.launch(Dispatchers.IO) {
            if (this@WorldModel.storage.await() == null) {
                this@WorldModel.storage = async {
                    this@WorldModel.world.open(app)
                }
            }
        }
    }
}