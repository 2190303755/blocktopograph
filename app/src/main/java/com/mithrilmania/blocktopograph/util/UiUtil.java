package com.mithrilmania.blocktopograph.util;

import android.content.Context;

import androidx.annotation.NonNull;


public final class UiUtil {
    public static float dpToPx(@NonNull Context context, float dp) {
        return context.getResources().getDisplayMetrics().density * dp;
    }

    public static int dpToPxInt(@NonNull Context context, int dp) {
        return (int) (context.getResources().getDisplayMetrics().density * dp);
    }
}
