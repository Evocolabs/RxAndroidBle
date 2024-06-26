package com.polidea.rxandroidble2.internal.connection;

import android.bluetooth.BluetoothGatt;

import com.polidea.rxandroidble2.ClientComponent;
import com.polidea.rxandroidble2.ConnectionSetup;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.internal.operations.BondOperation;
import com.polidea.rxandroidble2.internal.serialization.ClientOperationQueue;

import java.util.Set;
import java.util.concurrent.Callable;

import bleshadow.javax.inject.Inject;
import bleshadow.javax.inject.Named;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;

public class ConnectorImpl implements Connector {

    private final ClientOperationQueue clientOperationQueue;
    final ConnectionComponent.Builder connectionComponentBuilder;
    final Scheduler callbacksScheduler;
    final BondOperation bondOperation;

    @Inject
    public ConnectorImpl(
            ClientOperationQueue clientOperationQueue,
            ConnectionComponent.Builder connectionComponentBuilder,
            @Named(ClientComponent.NamedSchedulers.BLUETOOTH_CALLBACKS) Scheduler callbacksScheduler,
            BondOperation bondOperation
    ) {
        this.clientOperationQueue = clientOperationQueue;
        this.connectionComponentBuilder = connectionComponentBuilder;
        this.callbacksScheduler = callbacksScheduler;
        this.bondOperation = bondOperation;
    }

    @Override
    public Observable<RxBleConnection> prepareConnection(final ConnectionSetup options) {
        return Observable.defer(new Callable<ObservableSource<RxBleConnection>>() {
            @Override
            public ObservableSource<RxBleConnection> call() {
                final ConnectionComponent connectionComponent = connectionComponentBuilder
                        .autoConnect(options.autoConnect)
                        .isBredr(options.isBredr)
                        .suppressOperationChecks(options.suppressOperationCheck)
                        .operationTimeout(options.operationTimeout)
                        .build();

                final Set<ConnectionSubscriptionWatcher> connSubWatchers = connectionComponent.connectionSubscriptionWatchers();
                return obtainRxBleConnection(connectionComponent)
                        .mergeWith(observeDisconnections(connectionComponent))
                        .delaySubscription(enqueueConnectOperation(connectionComponent))
                        .doOnSubscribe(new Consumer<Disposable>() {
                            @Override
                            public void accept(Disposable disposable) {
                                for (ConnectionSubscriptionWatcher csa : connSubWatchers) {
                                    csa.onConnectionSubscribed();
                                }
                            }
                        })
                        .doFinally(new Action() {
                            @Override
                            public void run() {
                                for (ConnectionSubscriptionWatcher csa : connSubWatchers) {
                                    csa.onConnectionUnsubscribed();
                                }
                            }
                        })
                        .subscribeOn(callbacksScheduler)
                        .unsubscribeOn(callbacksScheduler);
            }
        });
    }

    public Single<Boolean> createBond() {
        return clientOperationQueue.queue(bondOperation).firstOrError();
    }

    static Observable<RxBleConnection> obtainRxBleConnection(final ConnectionComponent connectionComponent) {
        return Observable.fromCallable(new Callable<RxBleConnection>() {
            @Override
            public RxBleConnection call() {
                // BluetoothGatt is needed for RxBleConnection
                // BluetoothGatt is produced by RxBleRadioOperationConnect
                return connectionComponent.rxBleConnection();
            }
        });
    }

    static Observable<RxBleConnection> observeDisconnections(ConnectionComponent connectionComponent) {
        return connectionComponent.gattCallback().observeDisconnect();
    }

    Observable<BluetoothGatt> enqueueConnectOperation(ConnectionComponent connectionComponent) {
        return clientOperationQueue.queue(connectionComponent.connectOperation());
    }
}
