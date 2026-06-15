package com.mithrilmania.blocktopograph.block.icon;

import static com.mithrilmania.blocktopograph.block.icon.IconCacheKt.loadIconWithCache;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;

public interface BlockIcon extends Serializable {
    @Nullable
    Bitmap getIcon(Context context);

    BlockIcon NO_ICON = context -> null;

    static BlockIcon of(@NonNull String path) {
        return context -> loadIconWithCache(context, path);
    }
}
