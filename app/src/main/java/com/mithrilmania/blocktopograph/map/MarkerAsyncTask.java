package com.mithrilmania.blocktopograph.map;

import android.os.AsyncTask;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.mithrilmania.blocktopograph.LogUtil;
import com.mithrilmania.blocktopograph.chunk.Chunk;
import com.mithrilmania.blocktopograph.chunk.NBTChunkData;
import com.mithrilmania.blocktopograph.editor.world.WorldMapModel;
import com.mithrilmania.blocktopograph.map.marker.AbstractMarker;
import com.mithrilmania.blocktopograph.nbt.BinaryTag;
import com.mithrilmania.blocktopograph.nbt.CompoundTag;
import com.mithrilmania.blocktopograph.nbt.IntTag;
import com.mithrilmania.blocktopograph.nbt.ListTag;
import com.mithrilmania.blocktopograph.nbt.NumericTag;
import com.mithrilmania.blocktopograph.nbt.StringTag;
import com.mithrilmania.blocktopograph.world.WorldModel;
import com.mithrilmania.blocktopograph.world.WorldStorage;

/**
 * Load the NBT of the chunks and output the markers, async with both map-rendering and UI
 */
public class MarkerAsyncTask extends AsyncTask<Void, AbstractMarker, Void> {

    private final WorldMapModel world;
    private final WorldModel worldModel;

    private final int minChunkX, minChunkZ, maxChunkX, maxChunkZ;
    private final Dimension dimension;


    public MarkerAsyncTask(WorldMapModel model, WorldModel worldModel, int minChunkX, int minChunkZ,
                           int maxChunkX, int maxChunkZ, Dimension dimension) {
        this.minChunkX = minChunkX;
        this.minChunkZ = minChunkZ;
        this.maxChunkX = maxChunkX;
        this.maxChunkZ = maxChunkZ;
        this.dimension = dimension;

        this.world = model;
        this.worldModel = worldModel;
    }

    @Override
    protected Void doInBackground(Void... v) {
        WorldStorage storage = this.worldModel.getWorld().getStorage();
        if (storage == null) return null;
        Dimension dimension = this.dimension;
        int cX, cZ;
        for (cZ = minChunkZ; cZ < maxChunkZ; cZ++) {
            for (cX = minChunkX; cX < maxChunkX; cX++) {
                Chunk chunk = storage.getChunk(cX, cZ, dimension);
                this.loadEntityMarkers(chunk);
                this.loadTileEntityMarkers(chunk);
                //loadCustomMarkers(chunk);
            }
        }

        return null;
    }

    private void loadEntityMarkers(Chunk chunk) {
        try {
            NBTChunkData entityData = chunk.getEntity();

            if (entityData == null) return;

            entityData.load();
            for (BinaryTag tag : entityData.tags.values()) {
                if (!(tag instanceof CompoundTag compoundTag)) continue;
                Entity e = null;
                {
                    BinaryTag idTag = compoundTag.get("id");
                    if (idTag instanceof IntTag) {
                        e = Entity.getEntity(((IntTag) idTag).toInt());
                    }
                }
                if (e == null) {
                    BinaryTag idenTag = compoundTag.get("identifier");
                    if (idenTag instanceof StringTag) {
                        e = Entity.getEntity(((StringTag) idenTag).value);
                    }
                }
                if (e == null) e = Entity.UNKNOWN;
                float xf = 0.0F;
                float yf = 0.0F;
                float zf = 0.0F;
                if (compoundTag.get("Pos") instanceof ListTag pos && pos.size() > 3) {
                    xf = ((NumericTag) pos.get(0)).toFloat();
                    yf = ((NumericTag) pos.get(1)).toFloat();
                    zf = ((NumericTag) pos.get(2)).toFloat();
                }
                this.publishProgress(new AbstractMarker(Math.round(xf), Math.round(yf), Math.round(zf), dimension, e, false));
            }

        } catch (Exception e) {
            LogUtil.d(this, e);
        }
    }

    private void loadTileEntityMarkers(Chunk chunk) {
        try {
            NBTChunkData tileEntityData = chunk.getBlockEntity();
            if (tileEntityData == null) return;
            tileEntityData.load();
            for (BinaryTag tag : tileEntityData.tags.values()) {
                if (tag instanceof CompoundTag compoundTag) {
                    var id = compoundTag.get("id");
                    if (!(id instanceof StringTag)) continue;
                    TileEntity te = TileEntity.getTileEntity(((StringTag) id).value);
                    if (te == null || te.getBitmap() == null) continue;
                    int eX = getIntOrZero(compoundTag, "x");
                    int eY = getIntOrZero(compoundTag, "y");
                    int eZ = getIntOrZero(compoundTag, "z");
                    this.publishProgress(new AbstractMarker(eX, eY, eZ, dimension, te, false));
                }
            }
        } catch (Exception e) {
            LogUtil.d(this, e);
        }
    }

    private static int getIntOrZero(CompoundTag tag, String key) {
        var value = tag.get(key);
        return value instanceof NumericTag ? ((NumericTag) value).toInt() : 0;
    }

    /*private void loadCustomMarkers(Chunk chunk) {
        WorldActivityInterface wai = worldProvider.get();
        Collection<AbstractMarker> chunk = wai.getWorld().getMarkerManager()
                .getMarkersOfChunk(chunkX, chunkZ);
        AbstractMarker[] markers = new AbstractMarker[chunk.size()];
        this.publishProgress(chunk.toArray(markers));
    }*/

    @Override
    protected void onProgressUpdate(AbstractMarker... values) {


        // Some of the marks may have been added to screen already, remove first.
        // TODO: Why not just skipping them?
        for (AbstractMarker marker : values) {
            // 2019/2/27 fixing crash here.
            // Fatal Exception: java.lang.IllegalStateException
            // The specified child already has a parent.
            // You must call removeView() on the child's parent first.
            // com.qozix.tileview.markers.MarkerLayout.addMarker
            // We found it caused by custom markers reusing issue.
            // Entity and TileEntity marks are all newly created.
            // So we're removing custom marks from parent if present.

            if (marker.view != null) {
                ViewParent par = marker.view.getParent();
                if (par instanceof ViewGroup)
                    ((ViewGroup) par).removeView(marker.view);
            }

            this.world.getMarkers().getValue().add(marker);
        }
        //TODO: trigger
    }
}
