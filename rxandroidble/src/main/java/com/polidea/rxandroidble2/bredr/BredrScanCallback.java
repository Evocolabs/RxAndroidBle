package com.polidea.rxandroidble2.bredr;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;


public interface BredrScanCallback {
    void onScanned(BluetoothDevice device, BluetoothClass classes);
    void onScanStop();
}
