package com.polidea.rxandroidble2.internal.operations;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.DeadObjectException;
import android.util.Log;

import com.polidea.rxandroidble2.exceptions.BleException;
import com.polidea.rxandroidble2.internal.DeviceScope;
import com.polidea.rxandroidble2.internal.QueueOperation;
import com.polidea.rxandroidble2.internal.serialization.QueueReleaseInterface;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import bleshadow.javax.inject.Inject;
import io.reactivex.ObservableEmitter;

@DeviceScope
public class BondOperation extends QueueOperation<Boolean> {
    public final Context context;
    public final BluetoothDevice device;

    @Inject
    public BondOperation(Context context, BluetoothDevice device) {
        this.device = device;
        this.context = context;
    }

    /*
    Bond Operation Description:
    whenever this interface is called, try to check bond state after a
    short delay in case [removeBond] is not yet taking effect.
     */
    @Override
    protected void protectedRun(ObservableEmitter<Boolean> emitter, QueueReleaseInterface queueReleaseInterface) throws Throwable {
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.schedule(() -> {
            Log.d("BondOperation", "starting bond: " + device.getAddress());
            if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                device.createBond();
            } else if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                emitter.onNext(true);
                emitter.onComplete();
                queueReleaseInterface.release();
                return;
            }
        }, 500, TimeUnit.MILLISECONDS);
        DeviceBondBroadcastReceiver mRecv = new DeviceBondBroadcastReceiver();
        mRecv.i = new DevcieBondBroadcastReceiverInterface() {
            boolean triedBond = false;
            @Override
            public void onReceive(int n, int p, BluetoothDevice d) {
                if (!Objects.equals(d.getAddress(), device.getAddress())) {
                    return;
                }
                if (n == BluetoothDevice.BOND_BONDING) {
                    triedBond = true;
                    return;
                }
                if (triedBond) {
                    if (n == BluetoothDevice.BOND_NONE) {
                        emitter.onNext(false);
                        emitter.onComplete();
                        queueReleaseInterface.release();
                        context.unregisterReceiver(mRecv);
                    } else if (n == BluetoothDevice.BOND_BONDED) {
                        emitter.onNext(true);
                        emitter.onComplete();
                        queueReleaseInterface.release();
                        context.unregisterReceiver(mRecv);
                    }
                }
            }
        };
        IntentFilter mFilter = new IntentFilter();
        mFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        context.registerReceiver(mRecv, mFilter);
    }

    @Override
    protected BleException provideException(DeadObjectException deadObjectException) {
        return null;
    }

    interface DevcieBondBroadcastReceiverInterface {
        void onReceive(int n, int p, BluetoothDevice d);
    }

    class DeviceBondBroadcastReceiver extends BroadcastReceiver {
        public DevcieBondBroadcastReceiverInterface i;
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
//                    BOND_NONE: 10; BOND_BONDING: 11; BOND_BONDED: 12
                    int newBondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
                    int prevBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.BOND_NONE);
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    i.onReceive(newBondState, prevBondState, device);
                default:
            }
        }
    }
}
