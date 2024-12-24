package com.mithrilmania.blocktopograph.map;

import android.os.AsyncTask;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.mithrilmania.blocktopograph.Log;
import com.mithrilmania.blocktopograph.chunk.Chunk;
import com.mithrilmania.blocktopograph.chunk.NBTChunkData;
import com.mithrilmania.blocktopograph.editor.world.WorldMapModel;
import com.mithrilmania.blocktopograph.map.marker.AbstractMarker;
import com.mithrilmania.blocktopograph.nbt.tags.CompoundTag;
import com.mithrilmania.blocktopograph.nbt.tags.FloatTag;
import com.mithrilmania.blocktopograph.nbt.tags.IntTag;
import com.mithrilmania.blocktopograph.nbt.tags.ListTag;
import com.mithrilmania.blocktopograph.nbt.tags.StringTag;
import com.mithrilmania.blocktopograph.nbt.tags.Tag;
import com.mithrilmania.blocktopograph.world.WorldStorage;

import java.util.List;

/**
 * Load the NBT of the chunks and output the markers, async with both map-rendering and UI
 */
public class MarkerAsyncTask extends AsyncTask<Void, AbstractMarker, Void> {

    private final WorldMapModel world;

    private final int minChunkX, minChunkZ, maxChunkX, maxChunkZ;
    private final Dimension dimension;


    public MarkerAsyncTask(WorldMapModel model, int minChunkX, int minChunkZ,
                           int maxChunkX, int maxChunkZ, Dimension dimension) {
        this.minChunkX = minChunkX;
        this.minChunkZ = minChunkZ;
        this.maxChunkX = maxChunkX;
        this.maxChunkZ = maxChunkZ;
        this.dimension = dimension;

        this.world = model;
    }

    @Override
    protected Void doInBackground(Void... v) {
        WorldStorage storage = this.world.getHandler().getStorage();
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

            if (entityData.tags == null) return;

            for (Tag tag : entityData.tags) {
                if (!(tag instanceof CompoundTag)) continue;
                CompoundTag compoundTag = (CompoundTag) tag;
                Entity e = null;
                {
                    Tag idTag = compoundTag.getChildTagByKey("id");
                    if (idTag instanceof IntTag) {
                        Integer id = ((IntTag) idTag).getValue();
                        if (id != null) e = Entity.getEntity(id);
                    }
                }
                if (e == null) {
                    Tag idenTag = compoundTag.getChildTagByKey("identifier");
                    if (idenTag instanceof StringTag) {
                        String identifier = ((StringTag) idenTag).getValue();
                        if (identifier != null) e = Entity.getEntity(identifier);
                    }
                }
                if (e == null) e = Entity.UNKNOWN;
                List<Tag> pos = ((ListTag) compoundTag.getChildTagByKey("Pos")).getValue();
                float xf = ((FloatTag) pos.get(0)).getValue();
                float yf = ((FloatTag) pos.get(1)).getValue();
                float zf = ((FloatTag) pos.get(2)).getValue();

                this.publishProgress(new AbstractMarker(Math.round(xf), Math.round(yf), Math.round(zf), dimension, e, false));
            }

        } catch (Exception e) {
            Log.d(this, e);
        }
    }

    private void loadTileEntityMarkers(Chunk chunk) {
        try {
            NBTChunkData tileEntityData = chunk.getBlockEntity();
            if (tileEntityData == null) return;
            tileEntityData.load();
            if (tileEntityData.tags == null) return;
            for (Tag tag : tileEntityData.tags) {
                if (tag instanceof CompoundTag) {
                    CompoundTag compoundTag = (CompoundTag) tag;
                    String name = ((StringTag) compoundTag.getChildTagByKey("id")).getValue();
                    TileEntity te = TileEntity.getTileEntity(name);
                    if (te != null && te.getBitmap() != null) {
                        int eX = ((IntTag) compoundTag.getChildTagByKey("x")).getValue();
                        int eY = ((IntTag) compoundTag.getChildTagByKey("y")).getValue();
                        int eZ = ((IntTag) compoundTag.getChildTagByKey("z")).getValue();

                        this.publishProgress(new AbstractMarker(Math.round(eX), Math.round(eY), Math.round(eZ), dimension, te, false));
                    }
                }
            }
        } catch (Exception e) {
            Log.d(this, e);
        }
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
