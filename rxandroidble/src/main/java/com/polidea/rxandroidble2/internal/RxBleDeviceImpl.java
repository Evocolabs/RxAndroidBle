package com.polidea.rxandroidble2.internal;

import static com.polidea.rxandroidble2.internal.DeviceModule.IS_BREDR;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;

import androidx.annotation.Nullable;

import com.jakewharton.rxrelay2.BehaviorRelay;
import com.polidea.rxandroidble2.ConnectionSetup;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.Timeout;
import com.polidea.rxandroidble2.exceptions.BleAlreadyConnectedException;
import com.polidea.rxandroidble2.internal.connection.Connector;
import com.polidea.rxandroidble2.internal.logger.LoggerUtil;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import bleshadow.javax.inject.Inject;
import bleshadow.javax.inject.Named;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.functions.Action;

@DeviceScope
class RxBleDeviceImpl implements RxBleDevice {

    final BluetoothDevice bluetoothDevice;
    final Connector connector;
    private final BehaviorRelay<RxBleConnection.RxBleConnectionState> connectionStateRelay;
    final AtomicBoolean isConnected = new AtomicBoolean(false);
    final Boolean isBredr;

    @Inject
    RxBleDeviceImpl(
            BluetoothDevice bluetoothDevice,
            Connector connector,
            BehaviorRelay<RxBleConnection.RxBleConnectionState> connectionStateRelay,
            @Named(IS_BREDR) Boolean isBredr
    ) {
        this.bluetoothDevice = bluetoothDevice;
        this.connector = connector;
        this.connectionStateRelay = connectionStateRelay;
        this.isBredr = isBredr;
    }

    @Override
    public Observable<RxBleConnection.RxBleConnectionState> observeConnectionStateChanges() {
        return connectionStateRelay.distinctUntilChanged().skip(1);
    }

    @Override
    public RxBleConnection.RxBleConnectionState getConnectionState() {
        return connectionStateRelay.getValue();
    }

    @Override
    public Observable<RxBleConnection> establishConnection(final boolean autoConnect) {
        ConnectionSetup options = new ConnectionSetup.Builder()
                .setAutoConnect(autoConnect)
                .setSuppressIllegalOperationCheck(true)
                .setIsBredr(isBredr)
                .build();
        return establishConnection(options);
    }

    @Override
    public Observable<RxBleConnection> establishConnection(final boolean autoConnect, final Timeout timeout) {
        ConnectionSetup options = new ConnectionSetup.Builder()
                .setAutoConnect(autoConnect)
                .setOperationTimeout(timeout)
                .setSuppressIllegalOperationCheck(true)
                .setIsBredr(isBredr)
                .build();
        return establishConnection(options);
    }

    @Override
    public Observable<RxBleConnection> establishConnection(final ConnectionSetup options) {
        return Observable.defer(new Callable<ObservableSource<RxBleConnection>>() {
            @Override
            public ObservableSource<RxBleConnection> call() {
                if (isConnected.compareAndSet(false, true)) {
                    return connector.prepareConnection(options)
                            .doFinally(new Action() {
                                @Override
                                public void run() {
                                    isConnected.set(false);
                                }
                            });
                } else {
                    return Observable.error(new BleAlreadyConnectedException(bluetoothDevice.getAddress()));
                }
            }
        });
    }

    @SuppressLint("MissingPermission")
    public Single<Boolean> createBond() {
//        if (bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED && !getHeadsetConnected()) {
//            // This [createBond] method is usually called when device is discovered by bredr scanner.
//            // it's weird that sometimes a already paired device didn't connect to classic bluetooth
//            // but is scanned over [BluetoothAdaper.startDiscovery]. It might happen when the device
//            // it self removed historical bonding information and thus need a re-bond.
//            try {
//                Method method = bluetoothDevice.getClass().getMethod("removeBond", (Class[]) null);
//                method.invoke(bluetoothDevice, (Object[]) null);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
        return Single.defer(() -> connector.createBond());
    }

    @Override
    @Nullable
    public String getName() {
        return bluetoothDevice.getName();
    }

    @Override
    public Boolean getIsBredr() {
        return isBredr;
    }

    @Override
    public String getMacAddress() {
        return bluetoothDevice.getAddress();
    }

    @Override
    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RxBleDeviceImpl)) {
            return false;
        }

        RxBleDeviceImpl that = (RxBleDeviceImpl) o;
        return bluetoothDevice.equals(that.bluetoothDevice);
    }

    @Override
    public int hashCode() {
        return bluetoothDevice.hashCode();
    }

    @Override
    public String toString() {
        return "RxBleDeviceImpl{"
                + LoggerUtil.commonMacMessage(bluetoothDevice.getAddress())
                + ", name=" + bluetoothDevice.getName()
                + '}';
    }
}
