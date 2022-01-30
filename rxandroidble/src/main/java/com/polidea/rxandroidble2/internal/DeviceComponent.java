package com.polidea.rxandroidble2.internal;

import static com.polidea.rxandroidble2.internal.DeviceModule.IS_BREDR;
import static com.polidea.rxandroidble2.internal.DeviceModule.MAC_ADDRESS;

import androidx.annotation.Nullable;

import com.polidea.rxandroidble2.RxBleDevice;

import bleshadow.dagger.BindsInstance;
import bleshadow.dagger.Subcomponent;
import bleshadow.javax.inject.Named;

@DeviceScope
@Subcomponent(modules = {DeviceModule.class})
public interface DeviceComponent {

    @Subcomponent.Builder
    interface Builder {
        DeviceComponent build();

        @BindsInstance
        Builder macAddress(@Named(MAC_ADDRESS) String deviceMacAddress);

        @BindsInstance
        @Nullable
        Builder isBredr(@Named(IS_BREDR) Boolean isBredr);
    }

    @DeviceScope
    RxBleDevice provideDevice();
}
