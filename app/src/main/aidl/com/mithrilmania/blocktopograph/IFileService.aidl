package com.mithrilmania.blocktopograph;

import android.os.Messenger;
import android.os.ParcelFileDescriptor;
import com.mithrilmania.blocktopograph.IWorldCallback;

interface IFileService {
    /** Destroy method defined by Shizuku server */
    void destroy() = 16777114;

    void exit() = 1;

    boolean loadWorlds(String path, in IWorldCallback callback) = 2;

    void copyTo(String src, String dest) = 3;

    @nullable ParcelFileDescriptor getFileDescriptor(String path) = 4;

    @nullable String prepareDB(String cache, String world) = 5;
}