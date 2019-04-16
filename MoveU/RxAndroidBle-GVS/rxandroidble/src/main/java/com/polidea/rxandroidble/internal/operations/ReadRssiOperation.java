package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;

import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.SingleResponseOperation;
import com.polidea.rxandroidble.internal.connection.ConnectionModule;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;

import bleshadow.javax.inject.Inject;
import bleshadow.javax.inject.Named;

import rx.Observable;

public class ReadRssiOperation extends SingleResponseOperation<Integer> {

    @Inject
    ReadRssiOperation(RxBleGattCallback bleGattCallback, BluetoothGatt bluetoothGatt,
                      @Named(ConnectionModule.OPERATION_TIMEOUT) TimeoutConfiguration timeoutConfiguration) {
        super(bluetoothGatt, bleGattCallback, BleGattOperationType.READ_RSSI, timeoutConfiguration);
    }

    @Override
    protected Observable<Integer> getCallback(RxBleGattCallback rxBleGattCallback) {
        return rxBleGattCallback.getOnRssiRead();
    }

    @Override
    protected boolean startOperation(BluetoothGatt bluetoothGatt) {
        return bluetoothGatt.readRemoteRssi();
    }
}
