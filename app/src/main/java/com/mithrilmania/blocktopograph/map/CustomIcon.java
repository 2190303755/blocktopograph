package com.mithrilmania.blocktopograph.map;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;
import androidx.compose.ui.unit.IntRect;

import com.mithrilmania.blocktopograph.util.NamedBitmapProvider;
import com.mithrilmania.blocktopograph.util.NamedBitmapProviderHandle;

import java.io.IOException;
import java.util.HashMap;

/**
 * CustomIcon provides an easy collection of special icons to use for markers.
 */
public enum CustomIcon implements NamedBitmapProviderHandle, NamedBitmapProvider {

    DEFAULT_MARKER("default_marker", new IntRect(0, 0, 32, 32)),
    BLUE_MARKER("blue_marker", new IntRect(0, 32, 32, 64)),
    GREEN_MARKER("green_marker", new IntRect(32, 0, 64, 32)),
    RED_MARKER("red_marker", new IntRect(32, 32, 64, 64)),
    AQUA_MARKER("aqua_marker", new IntRect(64, 0, 96, 32)),
    ORANGE_MARKER("orange_marker", new IntRect(64, 32, 96, 64)),
    YELLOW_MARKER("yellow_marker", new IntRect(96, 0, 128, 32)),
    PURPLE_MARKER("purple_marker", new IntRect(96, 32, 128, 64)),
    SPAWN_MARKER("spawn_marker", new IntRect(64, 64, 128, 128));

    public final String iconName;
    public final IntRect sprite;

    public Bitmap bitmap;

    CustomIcon(String iconName, IntRect sprite) {
        this.iconName = iconName;
        this.sprite = sprite;
    }

    @Override
    public Bitmap getBitmap(){
        return this.bitmap;
    }

    @NonNull
    @Override
    public NamedBitmapProvider getNamedBitmapProvider(){
        return this;
    }

    @NonNull
    @Override
    public String getBitmapDisplayName(){
        return this.iconName;
    }

    @NonNull
    @Override
    public String getBitmapDataName() {
        return this.iconName;
    }


    public static void loadCustomBitmaps(AssetManager assetManager) throws IOException {

        Bitmap sheet = BitmapFactory.decodeStream(assetManager.open("custom_icons.png"));
        for(CustomIcon icon : CustomIcon.values()){
            if (icon.bitmap == null) {
                var spec = icon.sprite;
                if (spec == null) continue;
                icon.bitmap = Bitmap.createBitmap(
                        sheet,
                        spec.getLeft(),
                        spec.getTop(),
                        spec.getWidth(),
                        spec.getHeight(),
                        null,
                        false
                );
            }
        }
    }

    private static HashMap<String, CustomIcon> iconsByName;

    static {
        iconsByName = new HashMap<>();

        for(CustomIcon icon : CustomIcon.values()){
            iconsByName.put(icon.iconName, icon);
        }
    }

    public static CustomIcon getCustomIcon(String iconName){
        return iconsByName.get(iconName);
    }

}
