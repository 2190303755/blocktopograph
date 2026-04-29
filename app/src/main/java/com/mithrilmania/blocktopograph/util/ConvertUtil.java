package com.mithrilmania.blocktopograph.util;



import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * Convert utils
 */
public class ConvertUtil {

    @NonNull
    public static String getLegalFileName(@NonNull String text) {
        return text.replaceAll("[\\\\/:*?\"<>|.]", "_");
    }

    @Nullable
    public static String guessPictureMimeFromExtension(@NonNull String extension, boolean inLower) {
        // assert extension.length()>0;
        if (extension.charAt(0) == '.') extension = extension.substring(1);
        if (!inLower) extension = extension.toLowerCase();
        return switch (extension) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpg";
            default -> null;
        };
    }

    public static float distance(float x1, float y1, float x2, float y2) {
        float d1 = x2 - x1;
        float d2 = y2 - y1;
        return (float) Math.sqrt(d1 * d1 + d2 * d2);
    }

    public static double distance(double x1, double y1, double x2, double y2) {
        double d1 = x2 - x1;
        double d2 = y2 - y1;
        return Math.sqrt(d1 * d1 + d2 * d2);
    }

    public static String formatSize(long size) {
        if (size < 1024) return size + " B";
        int level = 0;
        while (size >= 0x100000 && level++ < 2) {
            size >>>= 10;
        }
        return String.format(Locale.getDefault(Locale.Category.FORMAT), switch (level) {
            case 0 -> "%.2f KiB";
            case 1 -> "%.2f MiB";
            default -> "%.2f GiB";
        }, size / 1024F);
    }
}
