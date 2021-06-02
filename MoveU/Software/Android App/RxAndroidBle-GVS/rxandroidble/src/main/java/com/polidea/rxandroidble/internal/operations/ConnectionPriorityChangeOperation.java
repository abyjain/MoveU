package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.polidea.rxandroidble.exceptions.BleGattCannotStartException;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.SingleResponseOperation;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;

import java.util.concurrent.TimeUnit;

import bleshadow.javax.inject.Inject;

import rx.Observable;
import rx.Scheduler;

public class ConnectionPriorityChangeOperation extends SingleResponseOperation<Long> {

    private final int connectionPriority;
    private final long operationTimeout;
    private final TimeUnit timeUnit;
    private final Scheduler delayScheduler;

    @Inject
    ConnectionPriorityChangeOperation(
            RxBleGattCallback rxBleGattCallback,
            BluetoothGatt bluetoothGatt,
            TimeoutConfiguration timeoutConfiguration,
            int connectionPriority,
            long operationTimeout,
            TimeUnit timeUnit,
            Scheduler delayScheduler) {
        super(bluetoothGatt, rxBleGattCallback, BleGattOperationType.CONNECTION_PRIORITY_CHANGE, timeoutConfiguration);
        this.connectionPriority = connectionPriority;
        this.operationTimeout = operationTimeout;
        this.timeUnit = timeUnit;
        this.delayScheduler = delayScheduler;
    }

    @Override
    protected Observable<Long> getCallback(RxBleGattCallback rxBleGattCallback) {
        return Observable.timer(operationTimeout, timeUnit, delayScheduler);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected boolean startOperation(BluetoothGatt bluetoothGatt) throws IllegalArgumentException, BleGattCannotStartException {
        return bluetoothGatt.requestConnectionPriority(connectionPriority);
    }
}
