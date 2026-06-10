package com.mithrilmania.blocktopograph.map;


import android.content.res.Resources;

import androidx.annotation.NonNull;

import com.mithrilmania.blocktopograph.R;
import com.mithrilmania.blocktopograph.map.renderer.MapType;

import java.util.HashMap;
import java.util.Map;

@Deprecated
public enum Dimension implements com.mithrilmania.blocktopograph.world.Dimension {

    OVERWORLD(0, "overworld", "Overworld", 16, 16, 128, 1, MapType.OVERWORLD_SATELLITE),
    NETHER(1, "nether", "Nether", 16, 16, 128, 1, MapType.NETHER),
    END(2, "end", "End", 16, 16, 128, 1, MapType.END_SATELLITE);//mcpe: SOON^TM /jk

    public final int id;
    public final int chunkW, chunkL, chunkH;
    public final int dimensionScale;
    public final String dataName, name;
    public final MapType defaultMapType;

    Dimension(int id, String dataName, String name, int chunkW, int chunkL, int chunkH, int dimensionScale, MapType defaultMapType) {
        this.id = id;
        this.dataName = dataName;
        this.name = name;
        this.chunkW = chunkW;
        this.chunkL = chunkL;
        this.chunkH = chunkH;
        this.dimensionScale = dimensionScale;
        this.defaultMapType = defaultMapType;
    }

    @NonNull
    @Override
    public String getName() {
        return this.name;
    }

    private static Map<String, Dimension> dimensionMap = new HashMap<>();

    static {
        for (Dimension dimension : Dimension.values()) {
            dimensionMap.put(dimension.dataName, dimension);
        }
    }

    public static Dimension getDimension(String dataName) {
        if (dataName == null) return null;
        return dimensionMap.get(dataName.toLowerCase());
    }

    public static Dimension getDimension(int id) {
        for (Dimension dimension : values()) {
            if (dimension.id == id) return dimension;
        }
        return null;
    }

    @Override
    public int getId() {
        return this.id;
    }

    @NonNull
    @Override
    public String getDisplayName(@NonNull Resources res) {
        return res.getString(switch (this) {
            case OVERWORLD -> R.string.overworld;
            case NETHER -> R.string.nether;
            case END -> R.string.the_end;
        });
    }
}
