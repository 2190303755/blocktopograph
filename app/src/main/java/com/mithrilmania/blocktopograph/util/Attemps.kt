package com.mithrilmania.blocktopograph.util

import android.os.RemoteException
import android.util.Log
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun runSuppressing(action: () -> Unit) {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    try {
        action()
    } catch (_: Exception) {
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <T> tryOrNull(action: () -> T): T? {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    return try {
        action()
    } catch (_: Exception) {
        null
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <T, R> T.rpc(action: (T) -> R?): R? {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    return try {
        action(this)
    } catch (e: RemoteException) {
        Log.e(RPC_TAG, "Error when calling remotely", e)
        null
    }
}
