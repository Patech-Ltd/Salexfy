package com.patechltd.salexfypos.print;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Lightweight holder for paired Bluetooth devices so UI code can list them
 * without depending on Android API level details.
 */
public final class BtDeviceInfo {

    public final String name;
    public final String address;

    private BtDeviceInfo(String name, String address) {
        this.name = name;
        this.address = address;
    }

    public static List<BtDeviceInfo> paired(Context context) {
        List<BtDeviceInfo> result = new ArrayList<>();
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null) return result;
            Set<BluetoothDevice> devices = adapter.getBondedDevices();
            if (devices == null) return result;
            for (BluetoothDevice d : devices) {
                String name = d.getName();
                result.add(new BtDeviceInfo(name == null ? d.getAddress() : name, d.getAddress()));
            }
        } catch (SecurityException e) {
            // Permission not granted yet; return empty list.
        }
        return result;
    }
}
