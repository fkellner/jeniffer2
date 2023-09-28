package de.unituebingen.dng.reader.io;

import de.unituebingen.dng.reader.util.ByteUtil;

import java.io.EOFException;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * @author Eugel Ljavin
 * <p>
 * A class which allows you to read bits of an given byte array or {@link ByteBuffer}. <br/>
 * At some points the performance of this class could be improved e.g. by reading bytes instead of bits if you
 * want to reads n bits with n > 8.
 */
public class BitReader {

    //Currently only buffer size of 8 is supported.
    private static final byte BUFFER_SIZE = 8;

    private ByteBuffer buffer;
    private byte bits;

    private int position;
    private boolean skipZeroBytes;

    public BitReader(ByteBuffer byteBuffer) {
        this(byteBuffer, false);
    }

    public BitReader(ByteBuffer byteBuffer, boolean skipZeroBytes) {
        this.buffer = Objects.requireNonNull(byteBuffer);
        this.skipZeroBytes = skipZeroBytes;
    }

    public BitReader(byte[] byteBuffer) {
        this(ByteBuffer.wrap(byteBuffer), false);
    }

    public BitReader(byte[] byteBuffer, boolean skipZeroBytes) {
        this(ByteBuffer.wrap(Objects.requireNonNull(byteBuffer)), skipZeroBytes);
    }

    public void skipZeroBytes(boolean skip) {
        this.skipZeroBytes = skip;
    }

    /**
     * Reads a single bit. <br/>
     *
     * @return The read bit
     * @throws EOFException If the end of file has been reached unexpectedly
     */
    public int readBit() throws EOFException {
        if (position % BUFFER_SIZE == 0) {
            if (!buffer.hasRemaining()) {
                throw new EOFException();
            }
            bits = buffer.get();
            if (skipZeroBytes && (bits & ByteUtil.UNSIGNED_BYTE_MASK) == ByteUtil.UNSIGNED_BYTE_MASK) {
                buffer.position(buffer.position() + 1);
            }
        }
        position++;
        int bit = (bits & ByteUtil.UNSIGNED_BYTE_MASK) >> 7;
        bits = (byte) (bits << 1);
        return bit;
    }

    /**
     * Reads n bits at once and returns the unsigned decimal representation. <br/>
     * TODO: Improve the performance of this method by reading (multiple) bytes if n >= 8
     *
     * @param n The amount of bits which should be read
     * @return The read bits as unsigned decimal representation
     * @throws EOFException If the end of file has been reached unexpectedly
     */
    public int readUnsignedBits(int n) throws EOFException {
        int bits = 0;

        while (position + 1 % BUFFER_SIZE != 0 && n != 0) {
            bits = (bits << 1) | readBit();
            n--;
        }
        int bytes = n / BUFFER_SIZE;
        int remainingBits = n % BUFFER_SIZE;
        for (int i = 0; i < bytes; i++) {
            bits = (bits << BUFFER_SIZE) | buffer.get() & 0xFF;
            position += BUFFER_SIZE;
        }
        for (int i = 0; i < remainingBits; i++) {
            bits = (bits << 1) | readBit();
        }

        return bits;
    }

    /**
     * Reads n bits at once and returns the signed decimal representation.
     *
     * @param n The amount of bits which should be read
     * @return The read bits as signed decimal representation
     * @throws EOFException If the end of file has been reached unexpectedly
     */
    public int readSignedBits(int n) throws EOFException {
        int bits = readUnsignedBits(n);
        if ((bits & (1 << (n - 1))) == 0) {
            bits -= (1 << n) - 1;
        }

        return bits;
    }
}
