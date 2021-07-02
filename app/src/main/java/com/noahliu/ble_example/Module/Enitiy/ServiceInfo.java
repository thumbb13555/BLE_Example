package com.noahliu.ble_example.Module.Enitiy;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ServiceInfo {

    final private UUID uuid;
    final private List<CharacteristicInfo> characteristicInfo;

    public ServiceInfo(BluetoothGattService gattServices) {
        this.uuid = gattServices.getUuid();
        characteristicInfo = new ArrayList<>();
        for (BluetoothGattCharacteristic characteristic :gattServices.getCharacteristics()) {
            characteristicInfo.add(new CharacteristicInfo(characteristic));
        }
    }

    public String getTitle() {
        return "Service";
    }

    public UUID getUuid() {
        return uuid;
    }

    public List<CharacteristicInfo> getCharacteristicInfo() {
        return characteristicInfo;
    }

    public static class CharacteristicInfo{
        final private UUID uuid;
        final private ArrayList<String> propertiesTags;
        final private ArrayList<Integer> propertiesCode;
        final private List<DescriptorsInfo> descriptorsInfo;
        final private BluetoothGattCharacteristic characteristic;

        public CharacteristicInfo(BluetoothGattCharacteristic characteristic) {
            this.characteristic = characteristic;
            this.uuid = characteristic.getUuid();
            this.propertiesCode = getPropertiesCodeArray(characteristic.getProperties());
            this.propertiesTags = getPropertiesTagArray(characteristic.getProperties());
            descriptorsInfo = new ArrayList<>();
            for (BluetoothGattDescriptor descriptor: characteristic.getDescriptors()) {
                descriptorsInfo.add(new DescriptorsInfo(descriptor));
            }
        }

        public BluetoothGattCharacteristic getCharacteristic() {
            return characteristic;
        }

        public String getTitle() {
            return "Characteristic";
        }

        public UUID getUuid() {
            return uuid;
        }

        public ArrayList<String> getPropertiesTags() {
            return propertiesTags;
        }

        public ArrayList<Integer> getPropertiesCode() {
            return propertiesCode;
        }

        public List<DescriptorsInfo> getDescriptorsInfo() {
            return descriptorsInfo;
        }

        private ArrayList<String> getPropertiesTagArray(int properties){
            int addPro = properties;
            ArrayList<String> arrayList = new ArrayList<>();
            int[] bluetoothGattCharacteristicCodes = new int[]{
                    BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS,
                    BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE,
                    BluetoothGattCharacteristic.PROPERTY_INDICATE,
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                    BluetoothGattCharacteristic.PROPERTY_WRITE,
                    BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                    BluetoothGattCharacteristic.PROPERTY_READ,
                    BluetoothGattCharacteristic.PROPERTY_BROADCAST
            };
            String[] bluetoothGattCharacteristicName = new String[]{
                    "EXTENDED_PROPS",
                    "SIGNED_WRITE",
                    "INDICATE",
                    "NOTIFY",
                    "WRITE",
                    "WRITE_NO_RESPONSE",
                    "READ",
                    "BROADCAST"
            };
            for (int i = 0; i < bluetoothGattCharacteristicCodes.length; i++) {
                int code = bluetoothGattCharacteristicCodes[i];
                if (addPro>=code){
                    addPro -= code;
                    arrayList.add(bluetoothGattCharacteristicName[i]);
                }
            }

            return arrayList;
        }
        private ArrayList<Integer> getPropertiesCodeArray(int properties){
            int addPro = properties;
            ArrayList<Integer> arrayList = new ArrayList<>();
            int[] bluetoothGattCharacteristicCodes = new int[]{
                    BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS,
                    BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE,
                    BluetoothGattCharacteristic.PROPERTY_INDICATE,
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                    BluetoothGattCharacteristic.PROPERTY_WRITE,
                    BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                    BluetoothGattCharacteristic.PROPERTY_READ,
                    BluetoothGattCharacteristic.PROPERTY_BROADCAST
            };

            for (int i = 0; i < bluetoothGattCharacteristicCodes.length; i++) {
                int code = bluetoothGattCharacteristicCodes[i];
                if (addPro>=code){
                    addPro -= code;
                    arrayList.add(bluetoothGattCharacteristicCodes[i]);
                }
            }

            return arrayList;
        }

        public static class DescriptorsInfo{
            private UUID uuid;
            private String title;

            public DescriptorsInfo(BluetoothGattDescriptor descriptor) {
                this.uuid = descriptor.getUuid();
                this.title = "Descriptor";
            }

            public String getTitle() {
                return title;
            }

            public UUID getUuid() {
                return uuid;
            }
        }
    }
}
