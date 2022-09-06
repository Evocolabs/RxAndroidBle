package com.polidea.rxandroidble2.internal;

import static com.polidea.rxandroidble2.internal.DeviceModule.IS_BREDR;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

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
    BluetoothA2dp a2dpProfile;

    @Inject
    RxBleDeviceImpl(
            BluetoothDevice bluetoothDevice,
            @Nullable BluetoothAdapter bluetoothAdapter,
            Connector connector,
            Context context,
            BehaviorRelay<RxBleConnection.RxBleConnectionState> connectionStateRelay,
            @Named(IS_BREDR) Boolean isBredr
    ) {
        this.bluetoothDevice = bluetoothDevice;
        this.connector = connector;
        this.connectionStateRelay = connectionStateRelay;
        this.isBredr = isBredr;

        bluetoothAdapter.getProfileProxy(context, new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                if (profile == BluetoothProfile.A2DP) {
                    a2dpProfile = (BluetoothA2dp) proxy;
                }
            }
            @Override
            public void onServiceDisconnected(int profile) {
                a2dpProfile = null;
            }
        }, BluetoothProfile.A2DP);
        RxBleLog.i("isBredr: %s", this.isBredr.toString());
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

    public Single<Boolean> createBond() {
        if (bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
            return Single.just(true);
        }
        return Single.defer(() -> connector.createBond()
        );
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
    public Boolean getA2dpConnected() {
        return a2dpProfile.getConnectionState(bluetoothDevice) == BluetoothA2dp.STATE_CONNECTED;
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
