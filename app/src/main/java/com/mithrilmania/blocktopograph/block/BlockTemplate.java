package com.mithrilmania.blocktopograph.block;

import static com.mithrilmania.blocktopograph.ContractKt.ASSETS_URL_PREFIX;

import androidx.annotation.Nullable;


import java.io.Serializable;

public class BlockTemplate implements Serializable {

    public final String subName;

    public final Block block;

    public final @Nullable String icon;

    public final int color;
    public final boolean hasBiomeShading;

    public BlockTemplate(String subName, Block block, @Nullable String icon, int color, boolean hasBiomeShading) {
        this.subName = subName;
        this.block = block;
        this.icon = icon == null ? null : ASSETS_URL_PREFIX + icon;
        this.color = color;
        this.hasBiomeShading = hasBiomeShading;
    }

    @Nullable
    public String getSubName() {
        return subName;
    }

    public Block getBlock() {
        return block;
    }

    public int getColor() {
        return color;
    }

    public boolean isHasBiomeShading() {
        return hasBiomeShading;
    }
}
