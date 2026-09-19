package com.mithrilmania.blocktopograph.storage

import android.os.ParcelFileDescriptor
import com.mithrilmania.blocktopograph.Blocktopograph
import com.mithrilmania.blocktopograph.IFileService
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

suspend fun awaitFileService(timeout: Duration = 5.seconds): IFileService? =
    Blocktopograph.fileService.awaitService(timeout)

fun IFileService.openFileDescriptor(path: String): ParcelFileDescriptor? =
    this.openReadWrite(path, false, false)