package com.polidea.rxandroidble2.bredr;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.polidea.rxandroidble2.internal.RxBleLog;

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

    public void startListen(final BredrScanCallback cb) {
        RxBleLog.i("starting Bredr Scan");
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    // Add the name and address to an array adapter to show in a Toast
                    RxBleLog.i("Bredr Scan- %s, %s", device.getName(), device.getAddress());
                    if (device.getType() != BluetoothDevice.DEVICE_TYPE_CLASSIC && device.getType() != BluetoothDevice.DEVICE_TYPE_DUAL) {
                        return;
                    }
                    // Only UnCategorized Device should be labeled as BLE device.
                    cb.onScanned(device);
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    cb.onScanStop();
                }
            }
        };

        IntentFilter mfilter = new IntentFilter();

        mfilter.addAction(BluetoothDevice.ACTION_FOUND);
        mfilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        context.registerReceiver(receiver, mfilter);
    }

    public void stopListen(BredrScanCallback cb) {
        if (receivers.containsKey(cb)) {
            context.unregisterReceiver(receivers.get(cb));
            receivers.remove(cb);
        }
    }
}
