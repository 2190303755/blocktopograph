package com.mithrilmania.blocktopograph.block;

import androidx.annotation.NonNull;

import com.mithrilmania.blocktopograph.nbt.old.convert.NBTConstants;
import com.mithrilmania.blocktopograph.nbt.old.tags.ByteTag;
import com.mithrilmania.blocktopograph.nbt.old.tags.CompoundTag;
import com.mithrilmania.blocktopograph.nbt.old.tags.IntTag;
import com.mithrilmania.blocktopograph.nbt.old.tags.ShortTag;
import com.mithrilmania.blocktopograph.nbt.old.tags.StringTag;
import com.mithrilmania.blocktopograph.nbt.old.tags.Tag;

import java.util.ArrayList;

public class BlockStateBuilder {

    private ArrayList<Tag> tags;

    public BlockStateBuilder() {
        tags = new ArrayList<>();
    }

    @NonNull
    public BlockStateBuilder addProperty(String name, NBTConstants.NBTType type, int value) {
        Tag tag;
        switch (type) {
            case INT:
                tag = new IntTag(name, value);
                break;
            case SHORT:
                tag = new ShortTag(name, (short) value);
                break;
            case BYTE:
                tag = new ByteTag(name, (byte) value);
                break;
            default:
                throw new RuntimeException();
        }
        tags.add(tag);
        return this;
    }

    @NonNull
    public BlockStateBuilder addInt(String name, int value) {
        tags.add(new IntTag(name, value));
        return this;
    }

    @NonNull
    public BlockStateBuilder addShort(String name, short value) {
        tags.add(new ShortTag(name, value));
        return this;
    }

    @NonNull
    public BlockStateBuilder addByte(String name, byte value) {
        tags.add(new ByteTag(name, value));
        return this;
    }

    @NonNull
    public BlockStateBuilder addProperty(String name, String value) {
        Tag tag;
        tags.add(new StringTag(name, value));
        return this;
    }

    @NonNull
    public CompoundTag build() {
        return new CompoundTag("states", tags);
    }
}
