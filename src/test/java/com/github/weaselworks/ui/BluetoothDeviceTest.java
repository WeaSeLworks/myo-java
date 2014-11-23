package com.github.weaselworks.ui;

import org.junit.Test;
import org.thingml.bglib.BDAddr;
import static org.junit.Assert.*;
/**
 * Created by paulwatson on 23/11/2014.
 */
public class BluetoothDeviceTest {

    @org.junit.Test
    public void testEquals() throws Exception {

        BDAddr baddr = new BDAddr(new byte[]{1,2,3,4,5,6});
        BDAddr baddr2 = new BDAddr(new byte[]{1,2,3,4,5,6});
        BluetoothDevice device = new BluetoothDevice(baddr);
        BluetoothDevice device2 = new BluetoothDevice(baddr2);

        assertEquals(device, device2);
    }

    @Test
    public void testHashcode() throws Exception {
        BDAddr baddr = new BDAddr(new byte[]{1,2,3,4,5,6});
        BDAddr baddr2 = new BDAddr(new byte[]{1,2,3,4,5,6});
        BluetoothDevice device = new BluetoothDevice(baddr);
        BluetoothDevice device2 = new BluetoothDevice(baddr2);

        assertEquals(device.hashCode(), device2.hashCode());

    }
}
