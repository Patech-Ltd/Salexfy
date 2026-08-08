package com.patechltd.salexfypos.print;

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

import java.util.HashMap;

/**
 * Prints raw ESC/POS data over a USB connection to a thermal printer.
 * Finds the first bulk OUT endpoint on a device matching the saved vendor/product id.
 */
public final class UsbPrinter {

    private UsbPrinter() {
    }

    public static boolean hasUsbPermission(Context context, UsbDevice device) {
        UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        return manager.hasPermission(device);
    }

    public static void print(Context context, int vendorId, int productId, byte[] data) throws Exception {
        UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        UsbDevice device = findDevice(manager, vendorId, productId);
        if (device == null) {
            throw new Exception("USB printer not found");
        }
        if (!manager.hasPermission(device)) {
            throw new Exception("USB permission not granted. Re-open the printer settings and grant access.");
        }
        UsbDeviceConnection connection = manager.openDevice(device);
        if (connection == null) {
            throw new Exception("Could not open USB printer");
        }
        try {
            UsbInterface iface = findBulkOutInterface(device);
            if (iface == null) {
                throw new Exception("No bulk OUT endpoint on USB printer");
            }
            connection.claimInterface(iface, true);
            UsbEndpoint endpoint = findBulkOutEndpoint(iface);
            byte[] chunk = new byte[512];
            int offset = 0;
            while (offset < data.length) {
                int len = Math.min(512, data.length - offset);
                System.arraycopy(data, offset, chunk, 0, len);
                int transferred = connection.bulkTransfer(endpoint, chunk, len, 2000);
                if (transferred < 0) {
                    throw new Exception("USB write failed");
                }
                offset += transferred;
            }
            connection.releaseInterface(iface);
        } finally {
            try {
                connection.close();
            } catch (Exception ignored) {
            }
        }
    }

    public static java.util.List<UsbDevice> listPrinters(Context context) {
        UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> devices = manager.getDeviceList();
        java.util.List<UsbDevice> printers = new java.util.ArrayList<>();
        if (devices == null) return printers;
        for (UsbDevice d : devices.values()) {
            if (findBulkOutInterface(d) != null) printers.add(d);
        }
        return printers;
    }

    private static UsbDevice findDevice(UsbManager manager, int vendorId, int productId) {
        HashMap<String, UsbDevice> devices = manager.getDeviceList();
        if (devices == null) return null;
        for (UsbDevice d : devices.values()) {
            if (d.getVendorId() == vendorId && d.getProductId() == productId) {
                return d;
            }
        }
        return null;
    }

    private static UsbInterface findBulkOutInterface(UsbDevice device) {
        for (int i = 0; i < device.getInterfaceCount(); i++) {
            UsbInterface iface = device.getInterface(i);
            if (findBulkOutEndpoint(iface) != null) return iface;
        }
        return null;
    }

    private static UsbEndpoint findBulkOutEndpoint(UsbInterface iface) {
        for (int i = 0; i < iface.getEndpointCount(); i++) {
            UsbEndpoint ep = iface.getEndpoint(i);
            if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK
                    && ep.getDirection() == UsbConstants.USB_DIR_OUT) {
                return ep;
            }
        }
        return null;
    }
}
