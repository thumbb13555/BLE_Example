package com.noahliu.ble_example.Module.Enitiy;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;

public class ScannedData implements Serializable {
    /**這邊是拿取掃描到的所有資訊*/
    private String deviceName;
    private String rssi;
    private String deviceByteInfo;
    private String address;

    public ScannedData(String deviceName, String rssi, String deviceByteInfo, String address) {
        this.deviceName = deviceName;
        this.rssi = rssi;
        this.deviceByteInfo = deviceByteInfo;
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public String getRssi() {
        return rssi;
    }

    public String getDeviceByteInfo() {
        return deviceByteInfo;
    }

    public String getDeviceName() {
        return deviceName;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        ScannedData p = (ScannedData)obj;

        return this.address.equals(p.address);
    }

    @NonNull
    @Override
    public String toString() {
        return this.address;
    }
}
