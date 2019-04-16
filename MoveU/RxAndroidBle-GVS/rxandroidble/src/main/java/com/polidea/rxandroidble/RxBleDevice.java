package com.polidea.rxandroidble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.polidea.rxandroidble.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble.exceptions.BleGattCallbackTimeoutException;
import com.polidea.rxandroidble.exceptions.BleGattException;

import rx.Observable;

public interface RxBleDevice {

    /**
     * Observe changes to connection state of the device's {@link android.bluetooth.BluetoothGatt}.
     * This Observable will never emit errors.
     *
     * If you would like to have the initial state as well you can use observeConnectionStateChanges().startWith(getConnectionState())
     *
     * @return observable that will emit {@link com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState} changes
     */
    Observable<RxBleConnection.RxBleConnectionState> observeConnectionStateChanges();

    /**
     * Returns current connection state of the device's {@link android.bluetooth.BluetoothGatt}
     *
     * @return the RxBleConnectionState
     */
    RxBleConnection.RxBleConnectionState getConnectionState();

    /**
     * @param context     Android's context.
     * @param autoConnect Flag related to
     *                    {@link android.bluetooth.BluetoothDevice#connectGatt(Context, boolean, BluetoothGattCallback)} autoConnect flag.
     *                    If false, the connection will fail with {@link com.polidea.rxandroidble.exceptions.BleGattException} if
     *                    the device is not in range after a 30-second timeout. If true, the connection will be pending indefinitely.
     *                    Unlike the native Android API, if set to true and the connection is lost there will NOT be any attempt to
     *                    reconnect unless explicitly resubscribed.
     * @return Observable emitting the connection.
     * @throws BleDisconnectedException        emitted when the BLE link has been disconnected either when the connection
     *                                         was already established or was in pending connection state. This occurs when the
     *                                         connection was released as a part of expected behavior
     *                                         (with {@link android.bluetooth.BluetoothGatt#GATT_SUCCESS} state).
     * @throws BleGattException                emitted when the BLE link has been interrupted as a result of an error.
     *                                         The exception contains detailed explanation of the error source (type of operation) and
     *                                         the code proxied from the Android system.
     * @throws BleGattCallbackTimeoutException emitted when an internal timeout for connection has been reached. The operation will
     *                                         timeout in direct mode (autoConnect = false) after 35 seconds.
     * @see #establishConnection(boolean). The context is no longer required.
     */
    @Deprecated
    Observable<RxBleConnection> establishConnection(Context context, boolean autoConnect);

    /**
     * Establishes connection with a given BLE device. {@link RxBleConnection} is a handle, used to process BLE operations with a connected
     * device.
     * <p>
     * The connection is automatically disconnected (and released) when resulting Observable is unsubscribed.
     * On the other hand when the connections is interrupted by the device or the system, the Observable will be unsubscribed as well
     * following BleDisconnectedException or BleGattException emission.
     * <p>
     * During the disconnect process the library automatically handles order and requirement of device disconnect and gatt close operations.
     * <p>
     * Autoconnect concept may be misleading at first glance. In cases when the BLE device is available and it is advertising constantly you
     * won't need to use autoconnect. Use autoconnect for connections where the BLE device is not advertising at
     * the moment of #establishConnection call.
     *
     * @param autoConnect Flag related to
     *                    {@link android.bluetooth.BluetoothDevice#connectGatt(Context, boolean, BluetoothGattCallback)} autoConnect flag.
     *                    If false, the connection will fail with {@link com.polidea.rxandroidble.exceptions.BleGattException} if
     *                    the device is not in range after a 30-second timeout. If true, the connection will be pending indefinitely.
     *                    Unlike the native Android API, if set to true and the connection is lost there will NOT be any attempt to
     *                    reconnect unless explicitly resubscribed.
     * @return Observable emitting the connection.
     * @throws BleDisconnectedException        emitted when the BLE link has been disconnected either when the connection
     *                                         was already established or was in pending connection state. This occurs when the
     *                                         connection was released as a part of expected behavior
     *                                         (with {@link android.bluetooth.BluetoothGatt#GATT_SUCCESS} state).
     * @throws BleGattException                emitted when the BLE link has been interrupted as a result of an error.
     *                                         The exception contains detailed explanation of the error source (type of operation) and
     *                                         the code proxied from the Android system.
     * @throws BleGattCallbackTimeoutException emitted when an internal timeout for connection has been reached. The operation will
     *                                         timeout in direct mode (autoConnect = false) after 35 seconds.
     */
    Observable<RxBleConnection> establishConnection(boolean autoConnect);

    /**
     * Establishes connection with a given BLE device. {@link RxBleConnection} is a handle, used to process BLE operations with a connected
     * device.
     * <p>
     * The connection is automatically disconnected (and released) when resulting Observable is unsubscribed.
     * On the other hand when the connections is interrupted by the device or the system, the Observable will be unsubscribed as well
     * following BleDisconnectedException or BleGattException emission.
     * <p>
     * During the disconnect process the library automatically handles order and requirement of device disconnect and gatt close operations.
     * <p>
     * Autoconnect concept may be misleading at first glance. In cases when the BLE device is available and it is advertising constantly you
     * won't need to use autoconnect. Use autoconnect for connections where the BLE device is not advertising at
     * the moment of #establishConnection call.
     *
     * @param autoConnect      Flag related to
     *                         {@link android.bluetooth.BluetoothDevice#connectGatt(Context, boolean, BluetoothGattCallback)}
     *                         autoConnect flag.
     *                         If false, the connection will fail with {@link com.polidea.rxandroidble.exceptions.BleGattException} if
     *                         the device is not in range after a 30-second timeout. If true, the connection will be pending indefinitely.
     *                         Unlike the native Android API, if set to true and the connection is lost there will NOT be any attempt to
     *                         reconnect unless explicitly resubscribed.
     * @param operationTimeout Timeout configuration after which the operation will be considered as broken. Eventually the operation
     *                         will be canceled and removed from queue. Keep in mind that it will cancel the library's operation
     *                         only and may leave Android's BLE stack in an inconsistent state.
     * @return Observable emitting the connection.
     * @throws BleDisconnectedException        emitted when the BLE link has been disconnected either when the connection
     *                                         was already established or was in pending connection state. This occurs when the
     *                                         connection was released as a part of expected behavior
     *                                         (with {@link android.bluetooth.BluetoothGatt#GATT_SUCCESS} state).
     * @throws BleGattException                emitted when the BLE link has been interrupted as a result of an error.
     *                                         The exception contains detailed explanation of the error source (type of operation) and
     *                                         the code proxied from the Android system.
     * @throws BleGattCallbackTimeoutException emitted when an internal timeout for connection has been reached. The operation will
     *                                         timeout in direct mode (autoConnect = false) after 35 seconds.
     */
    Observable<RxBleConnection> establishConnection(boolean autoConnect, @NonNull Timeout operationTimeout);

    /**
     * Name of the device. Name is optional and it's up to the device vendor if will be provided.
     *
     * @return The device name or null if device name is absent.
     */
    @Nullable
    String getName();

    /**
     * MAC address of the corresponding device.
     */
    String getMacAddress();

    /**
     * The underlying android.bluetooth.BluetoothDevice.
     *
     * NOTE: this should be used with caution and knowledge as interaction with the BluetoothDevice may interrupt the flow of this library.
     * @return the BluetoothDevice
     */
    BluetoothDevice getBluetoothDevice();
}
