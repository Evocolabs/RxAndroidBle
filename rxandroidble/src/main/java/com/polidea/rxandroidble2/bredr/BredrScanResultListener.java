package com.polidea.rxandroidble2.bredr;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.HashMap;

import bleshadow.javax.inject.Inject;

public class BredrScanResultListener {

    private final Context context;

    private HashMap<BredrScanCallback, BroadcastReceiver> receivers;

    @Inject
    public BredrScanResultListener(Context context) {
        this.context = context;
        this.receivers = new HashMap<>();
    }

    public void startListen(BredrScanCallback cb) {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    // Add the name and address to an array adapter to show in a Toast
                    String derp = device.getName() + " - " + device.getAddress();
                }
            }
        };
        IntentFilter ifilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        context.registerReceiver(receiver, ifilter);
    }
    public void stopListen(BredrScanCallback cb) {
        if (receivers.containsKey(cb)) {
            context.unregisterReceiver(receivers.get(cb));
            receivers.remove(cb);
        }
    }
}
