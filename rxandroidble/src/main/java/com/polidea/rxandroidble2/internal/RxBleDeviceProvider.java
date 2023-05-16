package com.polidea.rxandroidble2.internal;

import com.polidea.rxandroidble2.ClientScope;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.internal.cache.DeviceComponentCache;

import java.util.Map;

import bleshadow.javax.inject.Inject;
import bleshadow.javax.inject.Provider;

@ClientScope
public class RxBleDeviceProvider {

    private final Map<String, DeviceComponent> cachedDeviceComponents;
    private final Provider<DeviceComponent.Builder> deviceComponentBuilder;

    @Inject
    public RxBleDeviceProvider(DeviceComponentCache deviceComponentCache, Provider<DeviceComponent.Builder> deviceComponentBuilder) {
        this.cachedDeviceComponents = deviceComponentCache;
        this.deviceComponentBuilder = deviceComponentBuilder;
    }

    public RxBleDevice getBleDevice(String macAddress) {
        return getBleDevice(macAddress, false);
    }

    public RxBleDevice getBleDevice(String macAddress, Boolean isBredr) {
        String isBredrPrefix = isBredr ? "BREDR@" : "BLE@";
        String macAddressWithPrefix = isBredrPrefix + macAddress;

        final DeviceComponent cachedDeviceComponent = cachedDeviceComponents.get(macAddressWithPrefix);
        if (cachedDeviceComponent != null) {
            return cachedDeviceComponent.provideDevice();
        }

        synchronized (cachedDeviceComponents) {
            final DeviceComponent secondCheckRxBleDevice = cachedDeviceComponents.get(macAddressWithPrefix);
            if (secondCheckRxBleDevice != null) {
                return secondCheckRxBleDevice.provideDevice();
            }
            final DeviceComponent deviceComponent;
            deviceComponent = deviceComponentBuilder.get()
                    .macAddress(macAddress)
                    .isBredr(isBredr)
                    .build();
            final RxBleDevice newRxBleDevice = deviceComponent.provideDevice();
            cachedDeviceComponents.put(macAddressWithPrefix, deviceComponent);
            return newRxBleDevice;
        }
    }
}
