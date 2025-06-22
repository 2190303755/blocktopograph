package com.mithrilmania.blocktopograph.nbt;

import static com.mithrilmania.blocktopograph.util.ConvertUtil.bytesToHexStr;

import android.util.Log;

import androidx.annotation.Nullable;

import com.mithrilmania.blocktopograph.nbt.convert.DataConverter;
import com.mithrilmania.blocktopograph.nbt.tags.Tag;

import org.iq80.leveldb.DB;

import java.io.IOException;
import java.util.ArrayList;

/**
 * @noinspection rawtypes
 */
public class LevelDBNBT extends EditableNBT {
    /**
     * Load NBT data of this key from the database, converting it into structured Java Objects.
     * These objects are wrapped in a nice EditableNBT, ready for viewing and editing.
     *
     * @param key Key corresponding with NBT data in the database.
     * @return EditableNBT, NBT wrapper of NBT objects to view or to edit.
     * @throws IOException when database fails.
     */
    public static @Nullable LevelDBNBT open(DB db, String display, byte[] key) throws IOException {
        byte[] data = db.get(key);
        if (data == null) return null;
        ArrayList<Tag> tags = DataConverter.read(data);
        return new LevelDBNBT(db, display, key, tags);
    }

    public final DB db;
    public final String display;
    private final byte[] key;
    public final ArrayList<Tag> tags;

    protected LevelDBNBT(DB db, String display, byte[] key, ArrayList<Tag> tags) {
        this.db = db;
        this.display = display;
        this.key = key;
        this.tags = tags;
    }

    @Override
    public Iterable<? extends Tag> getTags() {
        return this.tags;
    }

    @Override
    public boolean save() {
        try {
            this.db.put(this.key, DataConverter.write(this.tags));
            return true;
        } catch (Exception e) {
            Log.e("LevelDB", "Failed to save data with key: " + bytesToHexStr(this.key), e);
        }
        return false;
    }

    @Override
    public String getRootTitle() {
        return this.display;
    }

    @Override
    public void addRootTag(Tag tag) {
        this.tags.add(tag);
    }

    @Override
    public void removeRootTag(Tag tag) {
        this.tags.remove(tag);
    }
}
