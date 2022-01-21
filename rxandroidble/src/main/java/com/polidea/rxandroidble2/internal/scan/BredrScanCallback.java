package com.polidea.rxandroidble2.internal.scan;

import android.bluetooth.BluetoothDevice;

public interface BredrScanCallback {
    void onDeviceScanned(BluetoothDevice device);
}
