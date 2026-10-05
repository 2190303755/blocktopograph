package com.mithrilmania.blocktopograph

import android.content.pm.PackageManager.PERMISSION_GRANTED
import com.mithrilmania.blocktopograph.util.error
import rikka.shizuku.Shizuku

enum class ShizukuStatus {
    UNKNOWN,
    UNSUPPORTED,
    UNAUTHORIZED,
    AVAILABLE;
}

fun currentShizukuStatus(): ShizukuStatus {
    if (Shizuku.isPreV11()) return ShizukuStatus.UNSUPPORTED
    try {
        return if (Shizuku.checkSelfPermission() == PERMISSION_GRANTED) ShizukuStatus.AVAILABLE else ShizukuStatus.UNAUTHORIZED
    } catch (e: Throwable) {
        e.error("Failed to query Shizuku status")
    }
    return ShizukuStatus.UNKNOWN
}
