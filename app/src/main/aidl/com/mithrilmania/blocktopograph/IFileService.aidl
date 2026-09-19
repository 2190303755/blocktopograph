package com.mithrilmania.blocktopograph;

import android.os.Messenger;
import android.os.ParcelFileDescriptor;
import com.mithrilmania.blocktopograph.IWorldCallback;
import com.mithrilmania.blocktopograph.ParcelFileMetadata;

interface IFileService {
    /** Destroy method defined by Shizuku server */
    void destroy() = 16777114;

    void exit() = 1;

    boolean loadWorlds(String path, in IWorldCallback callback) = 2;

    void copyTo(String src, String dest) = 3;

    @nullable String prepareDB(String cache, String world) = 4;

    @nullable String canonicalize(String path) = 5;

    @nullable ParcelFileMetadata metadata(String path) = 6;

    @nullable ParcelFileDescriptor openReadOnly(String path) = 7;

    @nullable ParcelFileDescriptor openReadWrite(String path, boolean mustCreate, boolean mustExist) = 8;

    boolean createDirectory(String path) = 9;

    boolean atomicMove(String source, String target) = 10;

    boolean delete(String path) = 11;
}