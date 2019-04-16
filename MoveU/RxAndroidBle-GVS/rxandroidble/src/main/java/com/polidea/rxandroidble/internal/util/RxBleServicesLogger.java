package com.polidea.rxandroidble.internal.util;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import com.polidea.rxandroidble.RxBleDeviceServices;
import com.polidea.rxandroidble.internal.RxBleLog;
import com.polidea.rxandroidble.utils.StandardUUIDsParser;

import bleshadow.javax.inject.Inject;

/**
 * --------------- ====== Printing peripheral content ====== ---------------
 * PERIPHERAL ADDRESS: AA-BB-CC-DD-EE-FF
 * PERIPHERAL NAME: Device name
 * -------------------------------------------------------------------------
 * Primary Service - Weight Scale (ce029566-f9eb-11e7-8c3f-9a214cf093ae)
 * Instance ID: 1111
 * <p>
 * -> Characteristics:
 * * Weight Measurement (ce029962-f9eb-11e7-8c3f-9a214cf093ae)
 * Properties: READ, WRITE, NOTIFY, INDICATE, etc...
 * -> Descriptors
 * * Client Characteristic Configuration (ce029bc4-f9eb-11e7-8c3f-9a214cf093ae)
 * * Characteristic User Description (ce02a344-f9eb-11e7-8c3f-9a214cf093ae)
 */
public class RxBleServicesLogger {

    private final CharacteristicPropertiesParser characteristicPropertiesParser;

    @Inject
    RxBleServicesLogger(CharacteristicPropertiesParser characteristicPropertiesParser) {
        this.characteristicPropertiesParser = characteristicPropertiesParser;
    }

    public void log(RxBleDeviceServices rxBleDeviceServices, BluetoothDevice device) {
        if (RxBleLog.isAtLeast(RxBleLog.VERBOSE)) {
            RxBleLog.v("Preparing services description");
            RxBleLog.v(prepareServicesDescription(rxBleDeviceServices, device));
        }
    }

    private String prepareServicesDescription(RxBleDeviceServices rxBleDeviceServices, BluetoothDevice device) {
        StringBuilder descriptionBuilder = new StringBuilder();
        appendDeviceHeader(device, descriptionBuilder);
        for (BluetoothGattService bluetoothGattService : rxBleDeviceServices.getBluetoothGattServices()) {
            descriptionBuilder.append('\n'); // New line before each service description
            appendServiceDescription(descriptionBuilder, bluetoothGattService);
        }
        descriptionBuilder.append("\n--------------- ====== Finished peripheral content ====== ---------------");
        return descriptionBuilder.toString();
    }

    private void appendServiceDescription(StringBuilder descriptionBuilder, BluetoothGattService bluetoothGattService) {
        appendServiceHeader(descriptionBuilder, bluetoothGattService);
        descriptionBuilder.append("-> Characteristics:");
        for (BluetoothGattCharacteristic characteristic : bluetoothGattService.getCharacteristics()) {
            appendCharacteristicNameHeader(descriptionBuilder, characteristic);
            appendCharacteristicProperties(descriptionBuilder, characteristic);
            appendDescriptors(descriptionBuilder, characteristic);
        }
    }

    private void appendDescriptors(StringBuilder descriptionBuilder, BluetoothGattCharacteristic characteristic) {
        if (!characteristic.getDescriptors().isEmpty()) {
            appendDescriptorsHeader(descriptionBuilder);
            for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                appendDescriptorNameHeader(descriptionBuilder, descriptor);
            }
        }
    }

    private void appendDescriptorsHeader(StringBuilder descriptionBuilder) {
        descriptionBuilder
                .append('\n')
                .append('\t')
                .append("  ")
                .append("-> Descriptors: ");
    }

    private void appendCharacteristicNameHeader(StringBuilder descriptionBuilder, BluetoothGattCharacteristic characteristic) {
        descriptionBuilder
                .append('\n')
                .append('\t').append("* ").append(createCharacteristicName(characteristic))
                .append(" (")
                .append(characteristic.getUuid().toString())
                .append(")");
    }

    private void appendDescriptorNameHeader(StringBuilder descriptionBuilder, BluetoothGattDescriptor descriptor) {
        descriptionBuilder
                .append('\n')
                .append('\t')
                .append('\t')
                .append("* ").append(createDescriptorName(descriptor))
                .append(" (")
                .append(descriptor.getUuid().toString())
                .append(")");
    }

    private String createDescriptorName(BluetoothGattDescriptor descriptor) {
        final String descriptorName = StandardUUIDsParser.getDescriptorName(descriptor.getUuid());
        return descriptorName == null ? "Unknown descriptor" : descriptorName;
    }

    private void appendCharacteristicProperties(StringBuilder descriptionBuilder, BluetoothGattCharacteristic characteristic) {
        descriptionBuilder
                .append('\n')
                .append('\t')
                .append("  ")
                .append("Properties: ").append(characteristicPropertiesParser.propertiesIntToString(characteristic.getProperties()));
    }

    private String createCharacteristicName(BluetoothGattCharacteristic characteristic) {
        final String characteristicName = StandardUUIDsParser.getCharacteristicName(characteristic.getUuid());
        return characteristicName == null ? "Unknown characteristic" : characteristicName;
    }

    private void appendDeviceHeader(BluetoothDevice device, StringBuilder descriptionBuilder) {
        descriptionBuilder
                .append("--------------- ====== Printing peripheral content ====== ---------------\n")
                .append("PERIPHERAL ADDRESS: ").append(device.getAddress()).append('\n')
                .append("PERIPHERAL NAME: ").append(device.getName()).append('\n')
                .append("-------------------------------------------------------------------------");
    }

    private void appendServiceHeader(StringBuilder descriptionBuilder, BluetoothGattService bluetoothGattService) {
        descriptionBuilder
                .append("\n")
                .append(createServiceType(bluetoothGattService))
                .append(" - ")
                .append(createServiceName(bluetoothGattService))
                .append(" (")
                .append(bluetoothGattService.getUuid().toString())
                .append(")\n")
                .append("Instance ID: ").append(bluetoothGattService.getInstanceId())
                .append('\n');
    }

    private String createServiceName(BluetoothGattService bluetoothGattService) {
        final String serviceName = StandardUUIDsParser.getServiceName(bluetoothGattService.getUuid());
        return serviceName == null ? "Unknown service" : serviceName;
    }

    private String createServiceType(BluetoothGattService bluetoothGattService) {
        if (bluetoothGattService.getType() == BluetoothGattService.SERVICE_TYPE_PRIMARY) {
            return "Primary Service";
        } else {
            return "Secondary Service";
        }
    }
}
