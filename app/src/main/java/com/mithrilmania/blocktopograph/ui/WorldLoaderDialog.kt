package com.mithrilmania.blocktopograph.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mithrilmania.blocktopograph.storage.Workspace
import com.mithrilmania.blocktopograph.ui.component.AlertDialog
import com.mithrilmania.blocktopograph.ui.component.TextButton
import com.mithrilmania.blocktopograph.util.todoI18n
import com.mithrilmania.blocktopograph.world.LoadedWorld
import com.mithrilmania.blocktopograph.world.access.WorldAccess
import com.mithrilmania.blocktopograph.world.resolveLoadedWorld
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import org.iq80.leveldb.Options
import org.iq80.leveldb.fileenv.EnvImpl
import org.iq80.leveldb.impl.DbImpl

sealed interface LoaderState {
    object Waiting : LoaderState
    class Prepared(@JvmField val workspace: Workspace) : LoaderState {
        @JvmField
        val duplicate: CompletableDeferred<Boolean> = CompletableDeferred()
    }

    object Preparing : LoaderState
    class Failed(@JvmField val message: String, @JvmField val details: String?) : LoaderState
    class Loading(@JvmField val world: Deferred<LoadedWorld>) : LoaderState
}

class WorldLoaderModel : ViewModel() {
    private val mAccess = MutableStateFlow<WorldAccess?>(null)
    private val mRetry = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<LoaderState> = mRetry.onStart {
        emit(Unit)
    }.combine(mAccess) { _, access ->
        access
    }.transformLatest { access ->
        emit(LoaderState.Waiting)
        if (access === null) return@transformLatest
        var workspace = access.queryPreparedWorkspaceAndValidate()
        if (workspace === null) {
            emit(LoaderState.Preparing)
            workspace = access.prepareWorkspace()
        } else {
            val state = LoaderState.Prepared(workspace)
            emit(state)
            if (state.duplicate.await()) {
                emit(LoaderState.Preparing)
                workspace = access.prepareWorkspace()
            }
        }
        if (workspace === null) {
            emit(LoaderState.Failed("无法准备工作区", null))
            return@transformLatest
        }
        emit(LoaderState.Waiting)
        val path = workspace.file.path
        val deferred = CompletableDeferred<LoadedWorld>()
        emit(LoaderState.Loading(deferred))
        val db = try {
            DbImpl(Options.newDefaultOptions(), path, EnvImpl.createEnv())
        } catch (e: Exception) {
            deferred.completeExceptionally(e)
            emit(LoaderState.Failed("无法打开工作区数据库", e.stackTraceToString()))
            return@transformLatest
        }
        deferred.complete(resolveLoadedWorld(access, path, db))
    }.flowOn(Dispatchers.IO).stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        LoaderState.Waiting
    )

    fun load(access: WorldAccess) {
        this.mAccess.value = access
    }

    fun retry() {
        this.mRetry.tryEmit(Unit)
    }
}

@Composable
fun WorldLoaderDialog(
    access: WorldAccess,
    onCancel: () -> Unit,
    onLoading: (Deferred<LoadedWorld>) -> Unit
) {
    val model = viewModel<WorldLoaderModel>(rememberViewModelStoreOwner())
    LaunchedEffect(access) {
        model.load(access)
    }
    LaunchedEffect(Unit) {
        model.state.collect {
            if (it is LoaderState.Loading) {
                onLoading(it.world)
            }
        }
    }
    val loadingState = model.state.collectAsStateWithLifecycle()
    val dismissible = remember { derivedStateOf { loadingState.value !== LoaderState.Preparing } }
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                when (loadingState.value) {
                    is LoaderState.Failed -> "加载失败"
                    is LoaderState.Prepared -> "发现工作区"
                    else -> "准备中"
                }.todoI18n()
            )
        },
        neutralButton = {
            val state = loadingState.value
            if (state is LoaderState.Prepared) {
                TextButton("弃用".todoI18n()) {
                    state.duplicate.complete(true)
                }
            }
        },
        negativeButton = {
            TextButton(
                stringResource(android.R.string.cancel),
                enabled = dismissible.value,
                onClick = onCancel
            )
        },
        positiveButton = {
            when (val state = loadingState.value) {
                is LoaderState.Failed -> TextButton("重试".todoI18n()) {
                    model.retry()
                }

                is LoaderState.Prepared -> TextButton("复用".todoI18n()) {
                    state.duplicate.complete(false)
                }

                else -> TextButton(stringResource(android.R.string.ok), enabled = false) {}
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = dismissible.value,
            dismissOnClickOutside = dismissible.value
        )
    ) {
        when (val state = loadingState.value) {
            is LoaderState.Failed -> Column(
                Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(state.message.todoI18n())
                state.details?.let {
                    Text(it)
                }
            }

            is LoaderState.Prepared -> Text("检测到有效的工作区。复用可保留改动；丢弃将重新拷贝。".todoI18n())
            else -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}
