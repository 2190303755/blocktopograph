package com.mithrilmania.blocktopograph;

import android.os.Messenger;
import android.os.ParcelFileDescriptor;

interface IWorldCallback {
    oneway void onWorldSubmit(String path, in ParcelFileDescriptor config) = 1;

    oneway void onStatisticsUpdate(String path, int behaviors, int resources, long size) = 2;
}