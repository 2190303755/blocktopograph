package com.mithrilmania.blocktopograph.util;

import java.util.ArrayList;

public class Unchecked {
    /**
     * @noinspection unchecked
     */
    public static <R, T extends R> ArrayList<R> cast(ArrayList<T> list) {
        return (ArrayList<R>) list;
    }
}
