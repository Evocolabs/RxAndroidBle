package com.polidea.rxandroidble2.internal.connection;


import com.polidea.rxandroidble2.ConnectionSetup;
import com.polidea.rxandroidble2.RxBleConnection;

import io.reactivex.Observable;
import io.reactivex.Single;

public interface Connector {

    Observable<RxBleConnection> prepareConnection(ConnectionSetup autoConnect);

    Single<Boolean> createBond();
}
