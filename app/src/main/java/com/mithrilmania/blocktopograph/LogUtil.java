package com.mithrilmania.blocktopograph;

import androidx.annotation.NonNull;

public class LogUtil {
    private static final String LOG_TAG = "Blocktopo";

    private static String prependClassName(@NonNull Object caller, @NonNull String msg) {
        Class<?> clazz = caller instanceof Class ? (Class<?>) caller : caller.getClass();
        return clazz.getSimpleName() + ": " + msg;
    }

    public static void d(@NonNull Object caller, @NonNull String msg) {
        android.util.Log.d(LOG_TAG, prependClassName(caller, msg));
    }

    public static void d(@NonNull Object caller, @NonNull Throwable throwable) {
        android.util.Log.e(LOG_TAG, caller.getClass().getSimpleName(), throwable);
    }

    public static void d(@NonNull Class<?> caller, @NonNull Throwable throwable) {
        android.util.Log.e(LOG_TAG, caller.getSimpleName(), throwable);
    }
}
