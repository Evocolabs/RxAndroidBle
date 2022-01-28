package com.polidea.rxandroidble2.bredr;

import com.polidea.rxandroidble2.RxBleDevice;

public interface BredrScanCallback {
    void onScanned(RxBleDevice device);
}
