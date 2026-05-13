package com.mithrilmania.blocktopograph.flat;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public final class PickBlockActivity extends AppCompatActivity {
    public static final String EXTRA_KEY_BLOCK = "block";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PickBlockDialogKt.setupBlockPicker(this);
    }
}
