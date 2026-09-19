package com.mithrilmania.blocktopograph

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.widget.Toast
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.request.crossfade
import com.google.android.material.color.DynamicColors
import com.mithrilmania.blocktopograph.storage.FileService
import com.mithrilmania.blocktopograph.storage.ShizukuFileSystem
import com.mithrilmania.blocktopograph.util.ShizukuConnector
import com.mithrilmania.blocktopograph.util.error
import rikka.shizuku.Shizuku
import java.io.File

class Blocktopograph : Application(),
    Shizuku.OnRequestPermissionResultListener,
    Thread.UncaughtExceptionHandler,
    SingletonImageLoader.Factory {
    companion object {
        lateinit var instance: Blocktopograph
            private set

        lateinit var fileService: ShizukuConnector<IFileService>
            private set

        fun getShizukuStatus(): ShizukuStatus {
            if (Shizuku.isPreV11()) return ShizukuStatus.UNSUPPORTED
            try {
                return if (Shizuku.checkSelfPermission() == PERMISSION_GRANTED) ShizukuStatus.AVAILABLE else ShizukuStatus.UNAUTHORIZED
            } catch (e: Throwable) {
                e.error("Failed to query Shizuku status")
            }
            return ShizukuStatus.UNKNOWN
        }
    }

    var exceptionHandler: Thread.UncaughtExceptionHandler? = null
        private set

    override fun onRequestPermissionResult(code: Int, result: Int) {
        if ((code and 1) == 1) {
            if (result == PERMISSION_GRANTED) {
                Toast.makeText(this, "已授权", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "已拒绝", Toast.LENGTH_SHORT).show()
            }
        }
    }

    init {
        instance = this
        fileService = ShizukuConnector(
            Shizuku.UserServiceArgs(
                ComponentName(
                    BuildConfig.APPLICATION_ID,
                    FileService::class.java.name
                )
            ).daemon(false)
                .processNameSuffix("service")
                .debuggable(BuildConfig.DEBUG)
                .version(BuildConfig.VERSION_CODE),
            IFileService.Stub::asInterface
        )
        Shizuku.addRequestPermissionResultListener(this)
    }

    override fun newImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .crossfade(true)
            .fileSystem(ShizukuFileSystem)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        this.exceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, exception: Throwable) {
        val builder = StringBuilder(exception.toString())
        for (element in exception.stackTrace) {
            builder.append('\n').append(element.toString())
        }
        File(this.externalCacheDir, "report.txt").writeText(builder.toString())
        this.exceptionHandler?.uncaughtException(thread, exception)
    }
}