package com.mithrilmania.blocktopograph.block.icon;

import static com.mithrilmania.blocktopograph.block.icon.IconCacheKt.loadIconWithCache;

import android.content.Context;
import android.graphics.Bitmap;

public class TexPathBlockIcon extends BlockIcon {
    private final String texPath;

    public TexPathBlockIcon(String texPath) {
        this.texPath = texPath;
    }

    public Bitmap getIcon(Context context) {
        return loadIconWithCache(context, this.texPath);
    }
}
