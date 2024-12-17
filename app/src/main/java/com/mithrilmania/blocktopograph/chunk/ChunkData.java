package com.mithrilmania.blocktopograph.chunk;


import org.iq80.leveldb.DBException;

import java.io.IOException;
import java.lang.ref.WeakReference;

public abstract class ChunkData {

    public final WeakReference<Chunk> chunk;


    public ChunkData(Chunk chunk) {
        this.chunk = new WeakReference<>(chunk);
    }

    public abstract void createEmpty();

    public abstract void write() throws IOException, DBException;

}
