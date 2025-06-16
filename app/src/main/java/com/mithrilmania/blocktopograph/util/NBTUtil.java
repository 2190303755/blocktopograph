package com.mithrilmania.blocktopograph.util;

import com.mithrilmania.blocktopograph.editor.nbt.node.NBTNode;

import net.benwoodworth.knbt.NbtList;
import net.benwoodworth.knbt.NbtTag;

import java.util.ArrayList;

public class NBTUtil {
    /**
     * @noinspection unchecked, rawtypes
     */
    public static <T extends NbtTag> NbtList<T> forcedCast(ArrayList<NBTNode> list) {
        int length = list.size();
        ArrayList tags = new ArrayList(length);
        for (int i = 0; i < length; ++i) {
            //tags.add(list.get(i).makeTag());
        }
        return (NbtList<T>) NbtList.Companion.invoke$NbtCompound(tags);
    }
}
