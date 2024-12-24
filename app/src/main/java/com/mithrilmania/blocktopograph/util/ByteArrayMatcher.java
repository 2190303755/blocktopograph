package com.mithrilmania.blocktopograph.util;

public class ByteArrayMatcher {
    /**
     * <a href="https://stackoverflow.com/a/25659067/9399618">find index of a byte array within another byte array</a>
     */
    public static boolean contains(byte[] data, byte[] pattern, int[] failure) {
        int j = 0;

        for (byte datum : data) {
            while (j > 0 && pattern[j] != datum) {
                j = failure[j - 1];
            }
            if (pattern[j] == datum) {
                j++;
            }
            if (j == pattern.length) {
                return true;//i - pattern.length + 1;
            }
        }
        return false;//-1;
    }

    /**
     * Computes the failure function using a boot-strapping process,
     * where the pattern is matched against itself.
     */
    public static int[] computeFailure(byte[] pattern) {
        int[] failure = new int[pattern.length];

        int j = 0;
        for (int i = 1; i < pattern.length; i++) {
            while (j > 0 && pattern[j] != pattern[i]) {
                j = failure[j - 1];
            }
            if (pattern[j] == pattern[i]) {
                j++;
            }
            failure[i] = j;
        }

        return failure;
    }
}
