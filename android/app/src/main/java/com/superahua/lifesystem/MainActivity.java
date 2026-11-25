package com.superahua.lifesystem;

import android.os.Bundle;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    static final String ACTION_WRITTEN = "com.superahua.lifesystem.WRITTEN";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(AlarmPlugin.class);
        super.onCreate(savedInstanceState);
        AlarmReceiver.ensureChannel(this);
    }
}
