package com.mithrilmania.blocktopograph.storage

import android.os.HandlerThread

object FileServiceClient : HandlerThread("FileServiceClient") {
    const val CODE_BASIC_WORLD_INFO = 1
    const val CODE_EXTRA_WORLD_INFO = 2
    const val CODE_RELEASE = 3
}