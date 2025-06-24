package com.mithrilmania.blocktopograph.nbt.old.convert;

import static com.google.common.io.ByteStreams.skipFully;

import com.mithrilmania.blocktopograph.nbt.old.tags.CompoundTag;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class LevelDataConverter {
    public static final byte[] header = {0x04, 0x00, 0x00, 0x00};

    public static CompoundTag read(InputStream stream) throws IOException {
        if (stream == null) return null;
        skipFully(stream, 8);
        // Skip the length? Yeah I know it's redundant but...
        NBTInputStream in = new NBTInputStream(stream);
        CompoundTag levelTag = (CompoundTag) in.readTag();
        in.close();
        return levelTag;
    }

    public static void write(OutputStream stream, CompoundTag levelTag) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        NBTOutputStream out = new NBTOutputStream(bos);
        out.writeTag(levelTag);
        out.close();
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(stream));
        int length = bos.size();
        dos.write(header);
        dos.writeInt(Integer.reverseBytes(length));
        bos.writeTo(dos);
        dos.close();
    }
}
