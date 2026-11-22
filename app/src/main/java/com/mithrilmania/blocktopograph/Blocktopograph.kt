package com.mithrilmania.blocktopograph

import android.app.Application
import android.content.ComponentName
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.request.crossfade
import coil3.size.Precision
import com.google.android.material.color.DynamicColors
import com.mithrilmania.blocktopograph.storage.ShizukuFileService
import com.mithrilmania.blocktopograph.storage.ShizukuFileSystem
import com.mithrilmania.blocktopograph.storage.WorldDetailMapper
import com.mithrilmania.blocktopograph.storage.isLegacyExternalStorageAvailable
import com.mithrilmania.blocktopograph.texture.TextureKeyer
import com.mithrilmania.blocktopograph.texture.TextureResolver
import com.mithrilmania.blocktopograph.util.ShizukuConnector
import okio.FileSystem
import rikka.shizuku.Shizuku
import java.io.File
import java.io.PrintWriter

class Blocktopograph : Application(),
    Thread.UncaughtExceptionHandler,
    SingletonImageLoader.Factory {
    companion object {
        lateinit var fileService: ShizukuConnector<IFileService>
            private set
    }

    var exceptionHandler: Thread.UncaughtExceptionHandler? = null
        private set

    val textures by lazy { TextureResolver(this) }

    init {
        fileService = ShizukuConnector(
            Shizuku.UserServiceArgs(
                ComponentName(
                    BuildConfig.APPLICATION_ID,
                    ShizukuFileService::class.java.name
                )
            ).daemon(false)
                .processNameSuffix("service")
                .debuggable(BuildConfig.DEBUG)
                .version(BuildConfig.VERSION_CODE),
            IFileService.Stub::asInterface
        )
    }

    override fun newImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context).components {
            add(WorldDetailMapper())
            add(textures)
            add(TextureKeyer())
        }.fileSystem(
            if (isLegacyExternalStorageAvailable()) FileSystem.SYSTEM else ShizukuFileSystem()
        ).crossfade(true)
            .precision(Precision.INEXACT)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        this.exceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, exception: Throwable) {
        File(this.externalCacheDir, "report.txt").writer().use {
            val printer = PrintWriter(it)
            printer.write(exception.toString())
            exception.printStackTrace(printer)
            printer.flush()
        }
        this.exceptionHandler?.uncaughtException(thread, exception)
    }
}