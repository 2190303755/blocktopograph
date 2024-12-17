package com.mithrilmania.blocktopograph.map.edit;

import androidx.annotation.NonNull;

import com.mithrilmania.blocktopograph.chunk.Chunk;
import com.mithrilmania.blocktopograph.world.WorldStorage;


public abstract class EditTarget {

    protected int mMaxError = 10;

    protected final boolean mIsChunkAware;
    @NonNull
    protected final WorldStorage storage;

    protected EditTarget(boolean isChunkAware, @NonNull WorldStorage storage) {
        mIsChunkAware = isChunkAware;
        this.storage = storage;
    }

    public abstract EditResultCode forEachXyz(RandomAccessEdit edit);

    public abstract EditResultCode forEachXz(RandomAccessEdit edit);

    public abstract EditResultCode forEachChunk(ChunkBasedEdit edit);

    public final void setMaxError(int maxError) {
        mMaxError = maxError;
    }


    public final boolean isChunkAware() {
        return mIsChunkAware;
    }

    public interface RandomAccessEdit {

        int edit(Chunk chunk, int x, int y, int z);

    }

    public interface ChunkBasedEdit {

        int edit(Chunk chunk, int fromX, int toX, int fromY, int toY, int fromZ, int toZ);

    }

}
