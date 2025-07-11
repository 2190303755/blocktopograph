package com.mithrilmania.blocktopograph.util;



import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * Convert utils
 */
public class ConvertUtil {

    public static String bytesToHexStr(byte[] in) {
        if (in == null) return "null";
        final StringBuilder builder = new StringBuilder();
        for (byte b : in) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    private static int hexCharToByte(char ch) {
        return switch (ch) {
            case '0' -> 0;
            case '1' -> 1;
            case '2' -> 2;
            case '3' -> 3;
            case '4' -> 4;
            case '5' -> 5;
            case '6' -> 6;
            case '7' -> 7;
            case '8' -> 8;
            case '9' -> 9;
            case 'a', 'A' -> 10;
            case 'b', 'B' -> 11;
            case 'c', 'C' -> 12;
            case 'd', 'D' -> 13;
            case 'e', 'E' -> 14;
            case 'f', 'F' -> 15;
            default -> -1;
        };
    }

    @Nullable
    public static byte[] hexStringToBytes(@NonNull String text) {
        byte[] ret;
        if (text.charAt(0) == '0' && (text.charAt(1) == 'x' || text.charAt(1) == 'X'))
            text = text.substring(2);
        flow:
        {
            int len = text.length();
            if ((len & 1) != 0) {
                ret = null;
                break flow;
            }
            len = len >> 1;
            ret = new byte[len];
            for (int i = 0; i < len; i++) {
                int h = hexCharToByte(text.charAt(i << 1)) << 4;
                if (h < 0) break flow;
                int l = hexCharToByte(text.charAt(i << 1 | 1));
                if (l < 0) break flow;
                ret[i] = (byte) (h | l);
            }
        }
        return ret;
    }

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
