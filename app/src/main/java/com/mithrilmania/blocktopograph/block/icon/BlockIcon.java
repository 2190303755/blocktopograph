package com.mithrilmania.blocktopograph.block.icon;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.Nullable;

import java.io.Serializable;

public abstract class BlockIcon implements Serializable{

    @Nullable
    abstract public Bitmap getIcon(Context context);
}
