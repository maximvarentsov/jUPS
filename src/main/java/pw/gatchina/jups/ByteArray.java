package pw.gatchina.jups;

import java.util.Arrays;

/**
 * @author maximvarentsov
 * @since 20.04.2022
 */
public class ByteArray {
    private byte[] values;

    public ByteArray() {
        this(new byte[0]);
    }

    public ByteArray(final byte[] arr) {
        values = arr;
    }

    public void add(final byte value) {
        final var result = Arrays.copyOf(values, values.length + 1);
        result[result.length - 1] = value;
        values = result;
    }

    public void addAll(final byte[] arr) {
        final var tmp = Arrays.copyOf(values, values.length + arr.length);
        System.arraycopy(arr, 0, tmp, values.length, arr.length);
        /*for (var i = 0; i < arr.length; i++) {
            tmp[values.length + i] = arr[i];
        }*/
        values = tmp;
    }

    public byte[] getBytes() {
        return values;
    }
}
