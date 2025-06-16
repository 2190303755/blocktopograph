package com.mithrilmania.blocktopograph;

import android.os.Messenger;
import android.os.ParcelFileDescriptor;

interface IFileService {
    /** Destroy method defined by Shizuku server */
    void destroy() = 16777114;

    void exit() = 0;

    void loadWorldsAsync(String path, in Messenger messenger) = 1;

    void copyTo(String src, String dest) = 2;

    @nullable ParcelFileDescriptor getFileDescriptor(String path) = 3;

    @nullable String prepareDB(String cache, String world) = 4;
}