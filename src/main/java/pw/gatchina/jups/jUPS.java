package pw.gatchina.jups;

import javax.usb.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author maximvarentsov
 * @since 14.04.2022
 */
public class jUPS {
    /**
     * https://github.com/networkupstools/nut/blob/v2.8.0-rc2/drivers/nutdrv_qx.c#L1755
     */
    final static byte IF_ADDR = (byte) 0x00;
    final static byte E_IN_ADDR = (byte) 0x81;
    final static short VENDOR_ID = (short) 0x0925;
    final static short PRODUCT_ID = (short) 0x1234;

    public static void main(String... args) throws Exception {
        final var rootHub = UsbHostManager.getUsbServices().getRootUsbHub();
        final var devices = findDevices(rootHub, VENDOR_ID, PRODUCT_ID);

        if (devices.isEmpty()) {
            throw new RuntimeException("devices not found");
        }

        new jUPS(devices);
    }

    public jUPS(final List<UsbDevice> devices) throws UsbException, InterruptedException {
        for (final var device : devices) {
            processDevice(device);
        }
    }

    public void processDevice(final UsbDevice device) throws UsbException, InterruptedException {
        final var configuration = device.getActiveUsbConfiguration();
        final var iface = configuration.getUsbInterface(IF_ADDR);

        iface.claim(new UsbInterfacePolicy() {
            @Override
            public boolean forceClaim(final UsbInterface usbInterface) {
                return true;
            }
        });

        try {
            final var endpoint = iface.getUsbEndpoint(E_IN_ADDR);
            final var pipe = endpoint.getUsbPipe();

            pipe.open();

            try {
                // (240.0 000.0 240.0 006 49.0 13.5 30.8 00001001
                // (IN ? OUT LOAD HZ VOLTAGE TEMP STATUS
                // OL - 00001001
                // OB - 10001001
                // (byte) 0xA3 + "Q1\r".getBytes()
                final var commandQ1 = new byte[] {
                        (byte) 0xA3, //
                        (byte) 0x51, // "Q"
                        (byte) 0x31, // "1"
                        (byte) 0x0D  // "\r"
                };
                // (byte) 0xA3 + "ID\r".getBytes()
                final var commandID = new byte[] {
                        (byte) 0xA3, //
                        (byte) 0x49, // "I"
                        (byte) 0x44, // "D"
                        (byte) 0x0D  // "\r"
                };

                final var header = List.of("In", "Unknown", "Out", "Load", "Frequency", "Battery", "Temp", "Status");

                while (true) {
                    for (final var command : List.of(commandQ1)) {
                        runCommand(device, command);
                        final var response = readResponse(pipe);
                        final var str = new String(response.getBytes());
                        final var splited = str.split("\\s+");
                        final var result = new HashMap<>();
                        if (header.size() == splited.length) {
                            for (var i = 0; i < header.size(); i++) {
                                final var key = header.get(i);
                                final var value = splited[i];
                                result.put(key, value);
                            }
                        }
                        if (result.size() > 0) {
                            System.out.println(result);
                        }
                    }
                }
            } finally {
                pipe.close();
            }
        } finally {
            iface.release();
        }
    }

    public ByteArray readResponse(final UsbPipe pipe) throws UsbException, InterruptedException {
        final var response = new ByteArray();
        final var responseSize = 6;
        final var maxReads = 32;

        for (var i = 0; i < maxReads; i++) {
            final var bytes = new byte[responseSize];
            final var ret = pipe.syncSubmit(bytes);
            final var availableBytes = ((bytes[0] & 0xFF) & 0x0F);

            if (availableBytes <= 2) {
                Thread.sleep(TimeUnit.MICROSECONDS.toMillis(15_000));
            }

            if (availableBytes == 0 || ret != responseSize) { // XXX ret check ?
                break;
            }

            final var arr2 = Arrays.copyOfRange(bytes, 1, availableBytes + 1);

            response.addAll(arr2);
        }

        return response;
    }

    public void runCommand(final UsbDevice device, final byte[] command) throws UsbException {
        final var irp = device.createUsbControlIrp(
                (byte) (UsbConst.REQUESTTYPE_DIRECTION_OUT | UsbConst.REQUESTTYPE_TYPE_CLASS | UsbConst.REQUESTTYPE_RECIPIENT_INTERFACE),
                (byte) 0x09,
                (short) 0x200,
                (short) 0
        );
        irp.setData(command);
        device.syncSubmit(irp);
    }

    public static List<UsbDevice> findDevices(final UsbHub hub, final short vendorId, final short productId) {
        final var result = new ArrayList<UsbDevice>();
        for (final var device : (List<UsbDevice>) hub.getAttachedUsbDevices()) {
            final var desc = device.getUsbDeviceDescriptor();
            if (desc.idVendor() == vendorId && desc.idProduct() == productId) {
                result.add(device);
                continue;
            }
            if (device.isUsbHub()) {
                final var devices = findDevices((UsbHub) device, vendorId, productId);
                if (devices.size() > 0) {
                    result.addAll(devices);
                }
            }
        }
        return result;
    }
}
