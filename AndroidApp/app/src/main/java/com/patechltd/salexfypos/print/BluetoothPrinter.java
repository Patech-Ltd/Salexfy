package com.patechltd.salexfypos.print;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.OutputStream;
import java.util.UUID;

/**
 * Prints raw ESC/POS data over a Bluetooth SPP (RFCOMM) connection.
 */
public final class BluetoothPrinter {

    private static final UUID SPP_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothPrinter() {
    }

    public static void print(String address, byte[] data) throws Exception {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            throw new Exception("Bluetooth is not supported on this device");
        }
        BluetoothDevice device = adapter.getRemoteDevice(address);
        BluetoothSocket socket = device.createRfcommSocketToServiceRecord(SPP_UUID);
        try {
            socket.connect();
            OutputStream os = socket.getOutputStream();
            os.write(data);
            os.flush();
        } finally {
            try {
                socket.close();
            } catch (Exception ignored) {
            }
        }
    }
}
