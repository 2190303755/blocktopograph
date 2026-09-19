package com.mithrilmania.blocktopograph;

parcelable ParcelFileMetadata {
    boolean isRegularFile;
    boolean isDirectory;
    @nullable String symlinkTarget;
    long size;
    long createdAtMillis;
    long lastModifiedAtMillis;
    long lastAccessedAtMillis;
}