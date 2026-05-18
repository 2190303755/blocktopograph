package com.mithrilmania.blocktopograph;

import android.os.Messenger;
import android.os.ParcelFileDescriptor;

interface IWorldCallback {
    void onWorldSubmit(String path, in ParcelFileDescriptor config, in @nullable ParcelFileDescriptor icon) = 1;

    void onStatisticsUpdate(String path, int behaviors, int resources, long size) = 2;
}