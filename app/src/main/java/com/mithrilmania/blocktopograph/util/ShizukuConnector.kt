package com.mithrilmania.blocktopograph.util

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.IBinder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import kotlin.time.Duration

class ShizukuConnector<S>(
    private val args: Shizuku.UserServiceArgs,
    private val wrapper: (IBinder) -> S,
) {
    private val waiters = mutableListOf<CompletableDeferred<S?>>()

    @Volatile
    private var _service: S? = null

    val service: S? get() = _service

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (binder == null) return
            val service = wrapper(binder)
            synchronized(waiters) {
                _service = service
                val copy = waiters.toList()
                waiters.clear()
                copy
            }.forEach { it.complete(service) }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            synchronized(waiters) { _service = null }
        }
    }

    suspend fun awaitService(timeout: Duration): S? {
        _service?.let { return it }

        val deferred = CompletableDeferred<S?>()
        if (!Shizuku.isPreV11() && Shizuku.checkSelfPermission() == PERMISSION_GRANTED) {
            synchronized(waiters) {
                _service?.let { deferred.complete(it) }
                waiters.add(deferred)
                runCatching { Shizuku.bindUserService(args, connection) }
            }
        } else {
            deferred.complete(_service)
        }
        return try {
            withTimeoutOrNull(timeout) { deferred.await() }
        } finally {
            synchronized(waiters) { waiters.remove(deferred) }
        }
    }
}
