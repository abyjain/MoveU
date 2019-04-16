package com.polidea.rxandroidble.exceptions;

import java.util.UUID;

/**
 * An exception being emitted from {@link com.polidea.rxandroidble.RxBleDeviceServices#getCharacteristic(UUID)} or any
 * {@link com.polidea.rxandroidble.RxBleConnection} function that accepts {@link UUID} in case the said UUID is not found
 * in the discovered device services.
 */
public class BleCharacteristicNotFoundException extends BleException {

    private final UUID charactersisticUUID;

    public BleCharacteristicNotFoundException(UUID charactersisticUUID) {
        super("Characteristic not found with UUID " + charactersisticUUID);
        this.charactersisticUUID = charactersisticUUID;
    }

    public UUID getCharactersisticUUID() {
        return charactersisticUUID;
    }

}
