package com.github.weaselworks.ui;

import org.thingml.bglib.BDAddr;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by paulwatson on 23/11/2014.
 */
public class BluetoothDevice {

    private BDAddr address;
    private String alias;

    public static Map<String,String> KNOWN_DEVICES = new HashMap<>();
    static {
        KNOWN_DEVICES.put("c3:69:40:b1:5d:f6", "Paul's Myo");
    }

    public BluetoothDevice(BDAddr address) {
        if (address == null) throw new IllegalArgumentException("Cannot create a null Device");
        this.address = address;
        this.alias = BluetoothDevice.KNOWN_DEVICES.getOrDefault(address.toString(), address.toString());;
    }

    public BluetoothDevice(BDAddr address, String alias) {
        if (address == null) throw new IllegalArgumentException("Cannot create a null Device");
        this.address = address;
        this.alias = alias;
    }

    public boolean isMyo() {
        return KNOWN_DEVICES.containsKey(address.toString());
    }

    public BDAddr getAddress() {
        return address;
    }

    public String getAlias() {
        return alias;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BluetoothDevice that = (BluetoothDevice) o;

        if (!address.toString().equals(that.address.toString())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return address.toString().hashCode();
    }
}
