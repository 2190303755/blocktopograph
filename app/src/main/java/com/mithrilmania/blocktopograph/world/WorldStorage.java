package com.mithrilmania.blocktopograph.world;

import android.net.Uri;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mithrilmania.blocktopograph.Log;
import com.mithrilmania.blocktopograph.block.OldBlockRegistry;
import com.mithrilmania.blocktopograph.chunk.Chunk;
import com.mithrilmania.blocktopograph.chunk.ChunkTag;
import com.mithrilmania.blocktopograph.chunk.Version;
import com.mithrilmania.blocktopograph.map.Dimension;
import com.mithrilmania.blocktopograph.nbt.convert.DataConverter;
import com.mithrilmania.blocktopograph.nbt.convert.NBTConstants;
import com.mithrilmania.blocktopograph.nbt.tags.CompoundTag;
import com.mithrilmania.blocktopograph.nbt.tags.IntTag;
import com.mithrilmania.blocktopograph.nbt.tags.ListTag;
import com.mithrilmania.blocktopograph.util.SpecialDBEntryType;
import com.mithrilmania.blocktopograph.util.math.DimensionVector3;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBException;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.env.Env;
import org.iq80.leveldb.fileenv.EnvImpl;
import org.iq80.leveldb.impl.DbImpl;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Wrapper around level.dat world spec en levelDB database.
 */
public class WorldStorage {
    private static final Env LEVEL_DB_ENV = EnvImpl.createEnv();//TODO: redirect temp dir

    //another method for debugging, makes it easy to print a readable byte array
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    private LruCache<Key, Chunk> chunks = new ChunkCache(this, 256);
    public final OldBlockRegistry mOldBlockRegistry;
    public final DB db;
    public final Uri src;

    public WorldStorage(String path, Options options, Uri src) throws IOException {
        this.mOldBlockRegistry = new OldBlockRegistry(2048);
        Log.d(this, "Open DB " + path);
        this.db = new DbImpl(options, path, LEVEL_DB_ENV);
        this.src = src;
    }

    static String bytesToHex(byte[] bytes, int start, int end) {
        char[] hexChars = new char[(end - start) * 2];
        for (int j = start; j < end; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[(j - start) * 2] = hexArray[v >>> 4];
            hexChars[(j - start) * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static byte[] getChunkDataKey(int x, int z, ChunkTag type, Dimension dimension, byte subChunk, boolean asSubChunk) {
        if (dimension == Dimension.OVERWORLD) {
            byte[] key = new byte[asSubChunk ? 10 : 9];
            System.arraycopy(getReversedBytes(x), 0, key, 0, 4);
            System.arraycopy(getReversedBytes(z), 0, key, 4, 4);
            key[8] = type.dataID;
            if (asSubChunk) key[9] = subChunk;
            return key;
        } else {
            byte[] key = new byte[asSubChunk ? 14 : 13];
            System.arraycopy(getReversedBytes(x), 0, key, 0, 4);
            System.arraycopy(getReversedBytes(z), 0, key, 4, 4);
            System.arraycopy(getReversedBytes(dimension.id), 0, key, 8, 4);
            key[12] = type.dataID;
            if (asSubChunk) key[13] = subChunk;
            return key;
        }
    }

    private static byte[] getReversedBytes(int i) {
        return new byte[]{
                (byte) i,
                (byte) (i >> 8),
                (byte) (i >> 16),
                (byte) (i >> 24)
        };
    }

    public byte[] getChunkData(int x, int z, ChunkTag type, Dimension dimension, byte subChunk, boolean asSubChunk) throws DBException {
        byte[] chunkKey = getChunkDataKey(x, z, type, dimension, subChunk, asSubChunk);
        //Log.d("Getting cX: "+x+" cZ: "+z+ " with key: "+bytesToHex(chunkKey, 0, chunkKey.length));
        return this.db.get(chunkKey);
    }

    public byte[] getChunkData(int x, int z, ChunkTag type, Dimension dimension) throws DBException {
        return getChunkData(x, z, type, dimension, (byte) 0, false);
    }

    public void writeChunkData(int x, int z, ChunkTag type, Dimension dimension, byte subChunk, boolean asSubChunk, byte[] chunkData) throws DBException {
        this.db.put(getChunkDataKey(x, z, type, dimension, subChunk, asSubChunk), chunkData);
    }

    public void removeChunkData(int x, int z, ChunkTag type, Dimension dimension, byte subChunk, boolean asSubChunk) throws DBException {
        this.db.delete(getChunkDataKey(x, z, type, dimension, subChunk, asSubChunk));
    }

    public void removeFullChunk(int x, int z, Dimension dimension) throws DBException {
        var it = this.db.iterator();
        int count = 0;
        var compareKey = getChunkDataKey(x, z, ChunkTag.DATA_2D, dimension, (byte) 0, false);
        int baseKeyLength = dimension == Dimension.OVERWORLD ? 8 : 12;
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

    public String[] getNetworkPlayerNames() {
        List<String> players = getDBKeysStartingWith("player_");
        return players.toArray(new String[0]);
    }

    public List<String> getDBKeysStartingWith(String startWith) {
        DBIterator it = db.iterator();
        ArrayList<String> items = new ArrayList<>();
        for (it.seekToFirst(); it.hasNext(); it.next()) {
            byte[] key = it.next().getKey();
            if (key == null) continue;
            String keyStr = new String(key);
            if (keyStr.startsWith(startWith)) items.add(keyStr);
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
                Log.d(this, e);
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
            return (x * 31 + z) * 31 + dim.id;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Key another && ((x == another.x) && (z == another.z) && (dim != null)
                    && (another.dim != null) && (dim.id == another.dim.id));
        }
    }

    //function meant for debugging, not used in production
    public void logDBKeys() {
        DBIterator it = this.db.iterator();
        it.seekToFirst();
        while (it.hasNext()) {
            Map.Entry<byte[], byte[]> entry = it.next();
            byte[] key = entry.getKey();
            Log.d(this, "key: " + new String(key) + " key in Hex: " + WorldStorage.bytesToHex(key, 0, key.length) + " size: " + entry.getValue().length);
        }
        it.close();
    }

    @Nullable
    public DimensionVector3<Float> getLocalPlayerPos(CompoundTag levelDat) {
        try {
            byte[] data = this.db.get(SpecialDBEntryType.LOCAL_PLAYER.keyBytes);

            final CompoundTag player = data != null
                    ? (CompoundTag) DataConverter.read(data).get(0)
                    : (CompoundTag) levelDat.getChildTagByKey("Player");

            if (player == null) {
                Log.d(this, "No local player. A server world?");
                return null;
            }
            ListTag posVec = (ListTag) player.getChildTagByKey("Pos");
            IntTag dimensionId = (IntTag) player.getChildTagByKey("DimensionId");
            Dimension dimension = Dimension.getDimension(dimensionId.getValue());
            if (dimension == null) dimension = Dimension.OVERWORLD;

            return new DimensionVector3<>(
                    (float) posVec.getValue().get(0).getValue(),
                    (float) posVec.getValue().get(1).getValue(),
                    (float) posVec.getValue().get(2).getValue(),
                    dimension);
        } catch (Exception e) {
            Log.d(this, e);
            return null;
        }
    }

    @NonNull
    public DimensionVector3<Float> getMultiPlayerPos(String dbKey) throws Exception {
        try {
            byte[] data = this.db.get(dbKey.getBytes(NBTConstants.CHARSET));
            if (data == null) throw new Exception("no data!");
            final CompoundTag player = (CompoundTag) DataConverter.read(data).get(0);
            ListTag posVec = (ListTag) player.getChildTagByKey("Pos");
            if (posVec == null || posVec.getValue() == null)
                throw new Exception("No \"Pos\" specified");
            if (posVec.getValue().size() != 3)
                throw new Exception("\"Pos\" value is invalid. value: " + posVec.getValue().toString());
            IntTag dimensionId = (IntTag) player.getChildTagByKey("DimensionId");
            if (dimensionId == null || dimensionId.getValue() == null)
                throw new Exception("No \"DimensionId\" specified");
            Dimension dimension = Dimension.getDimension(dimensionId.getValue());
            if (dimension == null) dimension = Dimension.OVERWORLD;
            return new DimensionVector3<>(
                    (float) posVec.getValue().get(0).getValue(),
                    (float) posVec.getValue().get(1).getValue(),
                    (float) posVec.getValue().get(2).getValue(),
                    dimension);
        } catch (Exception e) {
            Log.d(this, e);
            throw new Exception("Could not find " + dbKey, e);
        }
    }

    public DimensionVector3<Integer> getSpawnPos(CompoundTag levelDat) throws Exception {
        try {
            int spawnX = ((IntTag) levelDat.getChildTagByKey("SpawnX")).getValue();
            int spawnY = ((IntTag) levelDat.getChildTagByKey("SpawnY")).getValue();
            int spawnZ = ((IntTag) levelDat.getChildTagByKey("SpawnZ")).getValue();
            if (spawnY >= 256) try {
                Chunk chunk = this.getChunk(spawnX >> 4, spawnZ >> 4, Dimension.OVERWORLD);
                if (!chunk.isError())
                    spawnY = chunk.getHeightMapValue(spawnX % 16, spawnZ % 16) + 1;
            } catch (Exception ignored) {
            }
            return new DimensionVector3<>(spawnX, spawnY, spawnZ, Dimension.OVERWORLD);
        } catch (Exception e) {
            Log.d(this, e);
            throw new Exception("Could not find spawn");
        }
    }
}