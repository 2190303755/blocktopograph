package com.protolambda.blocktopograph.map.marker;


import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import com.protolambda.blocktopograph.util.NamedBitmapProvider;

public class CustomNamedBitmapProvider implements NamedBitmapProvider {

    private final NamedBitmapProvider inner;

    private final String displayName;

    public CustomNamedBitmapProvider(NamedBitmapProvider inner, String displayName){
        this.inner = inner;
        this.displayName = displayName;
    }


    @Override
    public Bitmap getBitmap() {
        return inner.getBitmap();
    }

    @NonNull
    @Override
    public String getBitmapDisplayName() {
        return displayName;
    }

    @NonNull
    @Override
    public String getBitmapDataName() {
        return inner.getBitmapDataName();
    }
}
