package com.mithrilmania.blocktopograph.util;


import androidx.annotation.NonNull;

/**
 * Convert utils
 */
public class ConvertUtil {
    @NonNull
    public static String getLegalFileName(@NonNull String text) {
        return text.replaceAll("[\\\\/:*?\"<>|.]", "_");
    }

    public static float distanceSq(float x1, float y1, float x2, float y2) {
        float d1 = x2 - x1;
        float d2 = y2 - y1;
        return d1 * d1 + d2 * d2;
    }

    public static String formatSize(long size) {
        if (size < 1024) return size + " B";
        int level = 0;
        while (size >= 0x100000 && level++ < 2) {
            size >>>= 10;
        }
        return String.format(switch (level) {
            case 0 -> "%.2f KiB";
            case 1 -> "%.2f MiB";
            default -> "%.2f GiB";
        }, size / 1024F);
    }
}
