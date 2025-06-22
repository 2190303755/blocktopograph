package com.mithrilmania.blocktopograph.nbt;

import com.mithrilmania.blocktopograph.nbt.tags.CompoundTag;
import com.mithrilmania.blocktopograph.nbt.tags.Tag;

import java.util.ArrayList;

public abstract class LevelDat extends EditableNBT {
    public final CompoundTag dat;
    public final ArrayList<Tag> work;
    public final String title;

    public LevelDat(CompoundTag dat, Tag subTag) {
        this.dat = dat;
        if (subTag == null) {
            this.work = null;
            this.title = "level.dat";
        } else {
            this.work = new ArrayList<>();
            this.work.add(subTag);
            this.title = "level.dat>" + subTag;
            this.enableRootModifications = true;
        }
    }

    @Override
    public Iterable<? extends Tag> getTags() {
        return this.work == null ? this.dat.getValue() : this.work;
    }

    @Override
    public String getRootTitle() {
        return this.title;
    }

    @Override
    public void addRootTag(Tag tag) {
        this.dat.getValue().add(tag);
        if (this.work != null) {
            this.work.add(tag);
        }
    }

    @Override
    public void removeRootTag(Tag tag) {
        this.dat.getValue().remove(tag);
        if (this.work != null) {
            this.work.remove(tag);
        }
    }
}
