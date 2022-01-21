package com.polidea.rxandroidble2.internal.operations;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.os.DeadObjectException;
import com.polidea.rxandroidble2.exceptions.BleException;
import com.polidea.rxandroidble2.exceptions.BleScanException;
import com.polidea.rxandroidble2.internal.QueueOperation;
import com.polidea.rxandroidble2.internal.RxBleLog;
import com.polidea.rxandroidble2.internal.logger.LoggerUtil;
import com.polidea.rxandroidble2.internal.scan.BredrScanCallback;
import com.polidea.rxandroidble2.internal.scan.RxBleInternalScanResult;
import com.polidea.rxandroidble2.internal.serialization.QueueReleaseInterface;
import com.polidea.rxandroidble2.internal.util.RxBleAdapterWrapper;

import io.reactivex.ObservableEmitter;
import io.reactivex.functions.Cancellable;

public class BredrScanOperation extends QueueOperation<BluetoothDevice> {

    ObservableEmitter scanEmitter = null;
    RxBleAdapterWrapper rxBleAdapterWrapper;
    BredrScanOperation(RxBleAdapterWrapper rxBleAdapterWrapper) {
        this.rxBleAdapterWrapper = rxBleAdapterWrapper;
    }

    @Override
    final protected void protectedRun(
            final ObservableEmitter<BluetoothDevice> emitter,
            QueueReleaseInterface queueReleaseInterface
    ) {

        final BredrScanCallback scanCallback = createScanCallback(emitter);

        try {
            emitter.setCancellable(new Cancellable() {
                @Override
                public void cancel() {
                    RxBleLog.i("Scan operation is requested to stop.");
                    stopScan(rxBleAdapterWrapper);
                }
            });
            RxBleLog.i("Scan operation is requested to start.");
            boolean startLeScanStatus = startScan(rxBleAdapterWrapper, scanCallback);

            if (!startLeScanStatus) {
                emitter.tryOnError(new BleScanException(BleScanException.BLUETOOTH_CANNOT_START));
            }
        } catch (Throwable throwable) {
            RxBleLog.w(throwable, "Error while calling the start scan function");
            emitter.tryOnError(new BleScanException(BleScanException.BLUETOOTH_CANNOT_START, throwable));
        } finally {
            queueReleaseInterface.release();
        }
    }

    @Override
    protected BleException provideException(DeadObjectException deadObjectException) {
        return new BleScanException(BleScanException.BLUETOOTH_DISABLED, deadObjectException);
    }

    BredrScanCallback createScanCallback(final ObservableEmitter<BluetoothDevice> emitter) {
        return new BredrScanCallback() {
            @Override
            public void onDeviceScanned(BluetoothDevice device) {
                if (device != null) {
                    RxBleLog.d("%s, name=%s",
                            LoggerUtil.commonMacMessage(device.getAddress()),
                            device.getName());
                }
                emitter.onNext(device);
            }
        };
    }

    boolean startScan(RxBleAdapterWrapper rxBleAdapterWrapper, BredrScanCallback scanCallback) {
        rxBleAdapterWrapper.startLegacyBredrScan(scanCallback);
        return false;
    }

    void stopScan(RxBleAdapterWrapper rxBleAdapterWrapper) {

    }
}
