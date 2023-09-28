package de.unituebingen.dng.reader.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @author Eugen Ljavin
 * <p>
 * A util class for byte level implementations.
 */
public class ByteUtil {

    public static int UNSIGNED_BYTE_MASK = 0xFF;
    public static int UNSIGNED_SHORT_MASK = 0xFFFF;
    public static long UNSIGNED_LONG_MASK = 0xFFFFFFFFL;

    /**
     * Converts the first two positions of the given data to an unsigned short value. <br/>
     * If more that two values are provided the other ones will be ignored.
     *
     * @param byteOrder The byte order of the data
     * @param data      The data which should be converted
     * @return The converted data as unsigned short
     */
    public static int toUnsignedShort(ByteOrder byteOrder, byte... data) {
        return ByteBuffer.wrap(data).order(byteOrder).getShort() & UNSIGNED_SHORT_MASK;
    }

    public static long toUnsignedLong(ByteOrder byteOrder, byte... data) {
        return ByteBuffer.wrap(data).order(byteOrder).getInt() & UNSIGNED_LONG_MASK;
    }

    public static byte[] toByteArray(short[] data) {
        byte[] dataAsByte = new byte[data.length];
        for (int i = 0; i < dataAsByte.length; i++) {
            dataAsByte[i] = (byte) (data[i] & 0xFF);
        }

        return dataAsByte;
    }
}
