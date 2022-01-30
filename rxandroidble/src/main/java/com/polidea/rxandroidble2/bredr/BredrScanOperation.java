package com.polidea.rxandroidble2.bredr;

import android.bluetooth.BluetoothDevice;
import android.os.DeadObjectException;

import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.exceptions.BleException;
import com.polidea.rxandroidble2.internal.QueueOperation;
import com.polidea.rxandroidble2.internal.RxBleDeviceProvider;
import com.polidea.rxandroidble2.internal.RxBleLog;
import com.polidea.rxandroidble2.internal.serialization.QueueReleaseInterface;
import com.polidea.rxandroidble2.internal.util.RxBleAdapterWrapper;

import io.reactivex.ObservableEmitter;
import io.reactivex.functions.Cancellable;

public class BredrScanOperation extends QueueOperation<RxBleDevice> {

    final RxBleAdapterWrapper rxBleAdapterWrapper;
    final BredrScanResultListener resultListener;
    final RxBleDeviceProvider rxBleDeviceProvider;

    public BredrScanOperation(
            RxBleAdapterWrapper rxBleAdapterWrapper,
            BredrScanResultListener resultListener,
            RxBleDeviceProvider rxBleDeviceProvider) {
        this.rxBleAdapterWrapper = rxBleAdapterWrapper;
        this.resultListener = resultListener;
        this.rxBleDeviceProvider = rxBleDeviceProvider;
    }

    @Override
    protected void protectedRun(ObservableEmitter<RxBleDevice> emitter, QueueReleaseInterface queueReleaseInterface) throws Throwable {
        final BredrScanCallback scanCallback = createScanCallback(emitter);

        try {
            emitter.setCancellable(new Cancellable() {
                @Override
                public void cancel() {
                    stopScan(scanCallback);
                }
            });
            startScan(scanCallback);
        } catch (Throwable throwable) {
            RxBleLog.w(throwable, "Error while calling the start bredr scan function");
        } finally {
            queueReleaseInterface.release();
        }

    }

    private BredrScanCallback createScanCallback(final ObservableEmitter<RxBleDevice> emitter) {
        return new BredrScanCallback() {
            @Override
            public void onScanned(BluetoothDevice device, Boolean isBredr) {
                RxBleDevice bleDevice = rxBleDeviceProvider.getBleDevice(device.getAddress(), isBredr);
                emitter.onNext(bleDevice);
            }
            @Override
            public void onScanStop() {
                emitter.onComplete();
            }
        };
    }

    private void startScan(BredrScanCallback callback) {
        rxBleAdapterWrapper.startBredrScan(callback);
    }

    private void stopScan(BredrScanCallback callback) {
        rxBleAdapterWrapper.stopBredrScan(callback);
    }

    @Override
    protected BleException provideException(DeadObjectException deadObjectException) {
        return null;
    }
}
