package com.mithrilmania.blocktopograph;

import android.app.Application;
import android.content.Intent;

import androidx.annotation.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Blocktopograph extends Application implements Thread.UncaughtExceptionHandler {
    public static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    private final Thread.UncaughtExceptionHandler defaultHanlder;

    private boolean mHasUnsatisfiedLinkErrorActivity;

    public Blocktopograph() {
        this.defaultHanlder = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
        Log.e(this, throwable);
        if (throwable instanceof UnsatisfiedLinkError) {
            if (!mHasUnsatisfiedLinkErrorActivity) {
                Intent intent = new Intent(this, UnsatisfiedLinkErrorActivity.class);
                mHasUnsatisfiedLinkErrorActivity = true;
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                this.startActivity(intent);
            }
        }
        this.defaultHanlder.uncaughtException(thread, throwable);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }
}
