package com.mithrilmania.blocktopograph

import android.app.Application
import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.IBinder
import android.widget.Toast
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.material.color.DynamicColors
import com.mithrilmania.blocktopograph.storage.FileServiceClient
import com.mithrilmania.blocktopograph.storage.FileServiceServer
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.OnBinderDeadListener
import rikka.shizuku.Shizuku.OnBinderReceivedListener
import rikka.shizuku.Shizuku.OnRequestPermissionResultListener
import rikka.sui.Sui
import java.io.File

class Blocktopograph : Application(),
    DefaultLifecycleObserver,
    OnBinderReceivedListener,
    OnBinderDeadListener,
    OnRequestPermissionResultListener,
    Thread.UncaughtExceptionHandler {
    companion object {
        val isSui: Boolean = Sui.init(BuildConfig.APPLICATION_ID)
        lateinit var instance: Blocktopograph
            private set
        var fileService: IFileService? = null
            private set
        var unbound: Boolean = true
            private set

        fun getShizukuStatus(): ShizukuStatus {
            if (this.unbound) return ShizukuStatus.UNKNOWN
            if (Shizuku.isPreV11()) return ShizukuStatus.UNSUPPORTED
            try {
                return if (Shizuku.checkSelfPermission() == PERMISSION_GRANTED) ShizukuStatus.AVAILABLE else ShizukuStatus.UNAUTHORIZED
            } catch (e: Throwable) {
                Log.e(this, e)
            }
            return ShizukuStatus.UNKNOWN
        }
    }

    var exceptionHandler: Thread.UncaughtExceptionHandler? = null
        private set

    override fun onBinderReceived() {
        unbound = false
        if (Shizuku.isPreV11() || Shizuku.checkSelfPermission() != PERMISSION_GRANTED) return
        Shizuku.bindUserService(fileServiceArgs, fileServiceConnection)
    }

    override fun onBinderDead() {
        unbound = true
    }

    override fun onRequestPermissionResult(code: Int, result: Int) {
        if ((code and 1) == 1) {
            if (result == PERMISSION_GRANTED) {
                Shizuku.bindUserService(fileServiceArgs, fileServiceConnection)
                Toast.makeText(this, "正在绑定服务", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "已拒绝", Toast.LENGTH_SHORT).show()
            }
        }
    }

    init {
        instance = this
        Shizuku.addBinderReceivedListenerSticky(this)
        Shizuku.addBinderDeadListener(this)
        Shizuku.addRequestPermissionResultListener(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        FileServiceClient.start()
    }

    override fun onCreate(owner: LifecycleOwner) {
        DynamicColors.applyToActivitiesIfAvailable(this)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        Shizuku.removeBinderReceivedListener(this)
        Shizuku.removeBinderDeadListener(this)
        Shizuku.removeRequestPermissionResultListener(this)
        FileServiceClient.quitSafely()
    }

    private val fileServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (binder != null && binder.pingBinder()) {
                fileService = IFileService.Stub.asInterface(binder)
                Toast.makeText(this@Blocktopograph, "服务已绑定", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            fileService = null
        }
    }

    private val fileServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(
            BuildConfig.APPLICATION_ID,
            FileServiceServer::class.java.name
        )
    ).daemon(false)
        .processNameSuffix("service")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    override fun onCreate() {
        super<Application>.onCreate()
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