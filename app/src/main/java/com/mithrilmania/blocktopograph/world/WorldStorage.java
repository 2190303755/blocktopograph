package com.mithrilmania.blocktopograph.world;

import static com.mithrilmania.blocktopograph.util.IOUtilKt.writeIntLE;
import static com.mithrilmania.blocktopograph.util.StorageUtilKt.toLDBKey;

import androidx.annotation.Nullable;
import androidx.collection.LruCache;

import com.mithrilmania.blocktopograph.LogUtil;
import com.mithrilmania.blocktopograph.chunk.Chunk;
import com.mithrilmania.blocktopograph.chunk.Version;
import com.mithrilmania.blocktopograph.world.chunk.ChunkTag;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBException;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.ReadOptions;
import org.iq80.leveldb.env.Env;
import org.iq80.leveldb.fileenv.EnvImpl;
import org.iq80.leveldb.impl.DbImpl;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import kotlin.io.FilesKt;

/**
 * Wrapper around level.dat world spec en levelDB database.
 */
public class WorldStorage implements Closeable {
    private static final Env LEVEL_DB_ENV = EnvImpl.createEnv();//TODO: redirect temp dir
    private final LruCache<Key, Chunk> chunks = new ChunkCache(this, 256);
    public final DB db;
    public final String path;

    public WorldStorage(String path, Options options) throws IOException {
        LogUtil.d(this, "[Open DB]" + path);
        this.path = path;
        this.db = new DbImpl(options, path, LEVEL_DB_ENV);
    }

    public static byte[] makeChunkKey(int x, int z, int dimension, ChunkTag type) {
        byte[] key;
        if (dimension == 0) {
            key = new byte[9];
            key[8] = type.dataID;
        } else {
            key = new byte[13];
            writeIntLE(key, dimension, 8);
            key[12] = type.dataID;
        }
        writeIntLE(key, x, 0);
        writeIntLE(key, z, 4);
        return key;
    }

    public static byte[] makeChunkKey(int x, int z, int dimension, byte subChunk) {
        byte[] key;
        if (dimension == 0) {
            key = new byte[10];
            key[8] = ChunkTag.SUB_CHUNK_PREFIX.dataID;
            key[9] = subChunk;
        } else {
            key = new byte[14];
            writeIntLE(key, dimension, 8);
            key[12] = ChunkTag.SUB_CHUNK_PREFIX.dataID;
            key[13] = subChunk;
        }
        writeIntLE(key, x, 0);
        writeIntLE(key, z, 4);
        return key;
    }

    public byte[] getChunkData(int x, int z, Dimension dimension, byte subChunk) throws DBException {
        return this.db.get(makeChunkKey(x, z, dimension.getRuntimeId(), subChunk));
    }

    public byte[] getChunkData(int x, int z, ChunkTag type, Dimension dimension) throws DBException {
        return this.db.get(makeChunkKey(x, z, dimension.getRuntimeId(), type));
    }

    public void writeChunkData(int x, int z, Dimension dimension, ChunkTag type, byte[] chunkData) throws DBException {
        this.db.put(makeChunkKey(x, z, dimension.getRuntimeId(), type), chunkData);
    }

    public void writeChunkData(int x, int z, Dimension dimension, byte subChunk, byte[] chunkData) throws DBException {
        this.db.put(makeChunkKey(x, z, dimension.getRuntimeId(), subChunk), chunkData);
    }

    public void removeChunkData(int x, int z, ChunkTag type, Dimension dimension) throws DBException {
        this.db.delete(makeChunkKey(x, z, dimension.getRuntimeId(), type));
    }

    public void removeFullChunk(int x, int z, Dimension dimension) throws DBException {
        var it = this.db.iterator();
        int count = 0;
        var compareKey = makeChunkKey(x, z, dimension.getRuntimeId(), ChunkTag.DATA_2D);
        int baseKeyLength = dimension.getRuntimeId() == 0 ? 8 : 12;
        for (it.seekToFirst(); count < 800 && it.hasNext(); count++) {
            byte[] key = it.next().getKey();
            if (key.length > baseKeyLength && key.length <= baseKeyLength + 3 &&
                    IntStream.range(0, baseKeyLength).allMatch(i -> key[i] == compareKey[i]))
                this.db.delete(key);
        }
        it.close();
    }

    public Chunk getChunk(int cX, int cZ, Dimension dimension, boolean createIfMissing, Version createOfVersion) {
        Key key = new Key(cX, cZ, dimension);
        key.createIfMissng = createIfMissing;
        key.createOfVersion = createOfVersion;
        return chunks.get(key);
    }

    public Chunk getChunk(int cX, int cZ, Dimension dimension) {
        Key key = new Key(cX, cZ, dimension);
        return chunks.get(key);
    }

    // Avoid using cache for stream like operations.
    // Caller shall lock cache before operation and invalidate cache afterwards.
    public Chunk getChunkStreaming(int cx, int cz, Dimension dimension, boolean createIfMissing, Version createOfVersion) {
        return Chunk.create(this, cx, cz, dimension, createIfMissing, createOfVersion);
    }

    public void resetCache() {
        this.chunks.evictAll();
    }

    public List<String> getNetworkPlayerNameList() {
        return this.getDBKeysStartingWith("player_");
    }

    public List<String> getDBKeysStartingWith(String startWith) {
        DBIterator it = this.db.iterator(new ReadOptions().fillCache(false));
        it.seek(toLDBKey(startWith));
        ArrayList<String> items = new ArrayList<>();
        while (it.hasNext()) {
            byte[] key = it.next().getKey();
            if (key == null) continue;
            String keyStr = new String(key);
            if (!keyStr.startsWith(startWith)) break;
            items.add(keyStr);
        }
        it.close();
        return items;
    }

    private static class ChunkCache extends LruCache<Key, Chunk> {

        private WeakReference<WorldStorage> storage;

        ChunkCache(WorldStorage storage, int maxSize) {
            super(maxSize);
            this.storage = new WeakReference<>(storage);
        }

        @Override
        protected void entryRemoved(boolean evicted, Key key, Chunk oldValue, Chunk newValue) {
            try {
                oldValue.save();
            } catch (Exception e) {
                LogUtil.d(this, e);
            }
        }

        @Nullable
        @Override
        protected Chunk create(Key key) {
            WorldStorage storage = this.storage.get();
            if (storage == null) return null;
            return Chunk.create(storage, key.x, key.z, key.dim, key.createIfMissng, key.createOfVersion);
        }
    }

    static class Key {
        public int x, z;
        public Dimension dim;
        public boolean createIfMissng;
        public Version createOfVersion;

        Key(int x, int z, Dimension dim) {
            this.x = x;
            this.z = z;
            this.dim = dim;
        }

        @Override
        public int hashCode() {
            return (x * 31 + z) * 31 + dim.getRuntimeId();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Key another && ((x == another.x) && (z == another.z) && (dim != null)
                    && (another.dim != null) && (dim.getRuntimeId() == another.dim.getRuntimeId()));
        }
    }

    @Override
    public void close() {
        try {
            this.db.close();
        } catch (Throwable ignored) {
        }
        FilesKt.deleteRecursively(new File(this.path));
    }
}
