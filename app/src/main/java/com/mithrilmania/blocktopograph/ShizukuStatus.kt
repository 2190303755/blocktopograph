package com.mithrilmania.blocktopograph

import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.mithrilmania.blocktopograph.util.RPC_TAG
import rikka.shizuku.Shizuku

enum class ShizukuStatus {
    UNKNOWN,
    UNSUPPORTED,
    UNAUTHORIZED,
    AVAILABLE;
}

fun currentShizukuStatus(): ShizukuStatus {
    if (Shizuku.isPreV11()) return ShizukuStatus.UNSUPPORTED
    if (Shizuku.getBinder() === null) return ShizukuStatus.UNKNOWN
    try {
        return if (Shizuku.checkSelfPermission() == PERMISSION_GRANTED) ShizukuStatus.AVAILABLE else ShizukuStatus.UNAUTHORIZED
    } catch (e: Throwable) {
        Log.e(RPC_TAG, "Failed to query Shizuku status", e)
    }
    return ShizukuStatus.UNKNOWN
}

fun isShizukuAvailable(): Boolean = currentShizukuStatus() == ShizukuStatus.AVAILABLE

@Composable
fun rememberShizukuStatus(
    onGrantPermission: (() -> Unit)? = null
): State<ShizukuStatus> {
    val shizukuStatus = remember {
        mutableStateOf(currentShizukuStatus())
    }
    val context = LocalContext.current
    DisposableEffect(context) {
        val listener = Shizuku.OnRequestPermissionResultListener { _, result ->
            if (result == PERMISSION_GRANTED) {
                shizukuStatus.value = ShizukuStatus.AVAILABLE
                onGrantPermission?.invoke()
            } else {
                shizukuStatus.value = ShizukuStatus.UNAUTHORIZED
            }
        }
        Shizuku.addRequestPermissionResultListener(listener)
        onDispose {
            Shizuku.removeRequestPermissionResultListener(listener)
        }
    }
    return shizukuStatus
}
