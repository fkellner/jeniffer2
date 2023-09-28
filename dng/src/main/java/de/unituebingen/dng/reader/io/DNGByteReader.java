package de.unituebingen.dng.reader.io;

import de.unituebingen.dng.reader.DNGFieldType;
import de.unituebingen.dng.reader.DNGReadException;
import de.unituebingen.dng.reader.util.SignedRational;
import de.unituebingen.dng.reader.util.Rational;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * @author Eugen Ljavin
 * <p>
 * A class which is used to read tiff binary level data.
 */
public class DNGByteReader {

    private static final int DEFAULT_BUFFER_SIZE = 16 * 1024;
    private RandomAccessFile randomAccessFile;
    private byte fileBuffer[];
    private long startPosition = -1;
    private long endPosition = -1;
    private int bufferSize;

    private long position;
    private long markedPosition;

    private ByteOrder byteOrder;

    public DNGByteReader(File file, int size) throws FileNotFoundException {
        Objects.requireNonNull(file);
        randomAccessFile = new RandomAccessFile(file, "r");
        bufferSize = size;
        fileBuffer = new byte[size];
    }

    public DNGByteReader(File file) throws FileNotFoundException {
        this(file, DEFAULT_BUFFER_SIZE);
    }

    public DNGByteReader(String name) throws FileNotFoundException {
        this(name, DEFAULT_BUFFER_SIZE);
    }

    public DNGByteReader(String name, int size) throws FileNotFoundException {
        this(new File(name), size);
    }

    /**
     * Reads a single byte of data from this file starting from the given position.
     * The byte is returned as an short in the range 0 to 255. <br/>
     * If the end of the file is reached {@code -1} will be returned to indicate the end of the file.
     *
     * @param position The starting position to read from
     * @return The byte of data or {@code -1} if the end of the file is reached
     * @throws EOFException If the end of file has been reached unexpectedly
     */
    public byte read(long position) throws EOFException {
        if (position < startPosition || position > endPosition) {
            long blockStart = (position / bufferSize) * bufferSize;
            int bytesRead;
            try {
                randomAccessFile.seek(blockStart);
                bytesRead = randomAccessFile.read(fileBuffer);
            } catch (IOException e) {
                throw new EOFException("The end of file has been reached unexpectedly.");
            }
            startPosition = blockStart;
            endPosition = startPosition + bytesRead - 1;
            if (position < startPosition || position > endPosition) {
                throw new EOFException("The end of file has been reached unexpectedly.");
            }
        }

        return (fileBuffer[(int) (position - startPosition)]);
    }

    /**
     * Reads a single byte of data from this file starting from the given position.
     * The byte is returned as an short in the range 0 to 255. <br/>
     * If the end of the file is reached {@code -1} will be returned to indicate the end of the file.
     *
     * @return The byte of data
     * @throws EOFException If the end of file has been reached unexpectedly
     */
    public byte read() throws EOFException {
        return read(position++);
    }

    /**
     * Reads multiple bytes of data from this file into a given buffer.
     * The byte is returned as an short in the range -128 to 127. <br/>
     * TODO: improve the performance of this method by copying the bytes instead of reading byte by byte
     *
     * @param buffer The buffer to read into
     * @return The number of bytes which were actually read
     * @throws EOFException If the end of file has been reached unexpectedly
     */
    public int read(byte[] buffer) throws EOFException {
        byte data;
        int actualNumOfReads = 0;
        for (int i = 0; i < buffer.length; i++) {
            data = read();
            actualNumOfReads = data == -1 ? actualNumOfReads : actualNumOfReads + 1;
            buffer[i] = data;
        }
        return actualNumOfReads;
    }

    /**
     * Reads multiple bytes of data from this file into a given buffer.
     * The byte is returned as an short in the range 0 to 255. <br/>
     *
     * @param buffer The buffer to read into
     * @return The number of bytes which were actually read
     * @throws EOFException If the end of file has been reached unexpectedly
     */
    public int read(short[] buffer) throws EOFException {
        byte[] byteBuffer = new byte[buffer.length];
        int actualNumOfReads = read(byteBuffer);
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = (short) (byteBuffer[i] & 0xff);
        }
        return actualNumOfReads;
    }

    public short readUnsignedByte() throws EOFException {
        return (short) (read() & 0xff);
    }

    public String readAscii() throws EOFException {
        byte[] data = new byte[DNGFieldType.ASCII.bytes];
        read(data);
        return new String(data);
    }

    public int readUnsignedShort() throws EOFException {
        return (readSignedShort() & 0xffff);
    }

    public long readUnsignedLong() throws EOFException {
        return ((long) readSignedLong() & 0xffffffffL);
    }

    public Rational readRational() throws EOFException {
        long numerator = readUnsignedLong();
        long denominator = readUnsignedLong();

        return new Rational(numerator, denominator);
    }

    public byte readSignedByte() throws EOFException {
        return (byte) read();
    }

    public short readSignedShort() throws EOFException {
        byte[] buffer = new byte[DNGFieldType.SSHORT.bytes];
        read(buffer);
        return ByteBuffer.wrap(buffer).order(byteOrder).getShort();
    }

    public int readSignedLong() throws EOFException {
        byte[] buffer = new byte[DNGFieldType.SLONG.bytes];
        read(buffer);
        return ByteBuffer.wrap(buffer).order(byteOrder).getInt();
    }

    public SignedRational readSignedRational() throws EOFException {
        int numerator = readSignedLong();
        int denominator = readSignedLong();

        return new SignedRational(numerator, denominator);
    }

    public float readFloat() throws EOFException {
        byte[] buffer = new byte[DNGFieldType.FLOAT.bytes];
        read(buffer);
        return ByteBuffer.wrap(buffer).order(byteOrder).getFloat();
    }

    public double readDouble() throws EOFException {
        byte[] buffer = new byte[DNGFieldType.DOUBLE.bytes];
        read(buffer);
        return ByteBuffer.wrap(buffer).order(byteOrder).getDouble();
    }

    public Object read(DNGFieldType fieldType) throws DNGReadException, EOFException {
        Object value;
        switch (fieldType) {
            case BYTE:
            case UNDEFINED:
                value = readUnsignedByte();
                break;
            case ASCII:
                value = readAscii();
                break;
            case SHORT:
                value = readUnsignedShort();
                break;
            case LONG:
                value = readUnsignedLong();
                break;
            case RATIONAL:
                value = readRational();
                break;
            case SBYTE:
                value = readSignedByte();
                break;
            case SSHORT:
                value = readSignedShort();
                break;
            case SLONG:
                value = readSignedLong();
                break;
            case SRATIONAL:
                value = readSignedRational();
                break;
            case FLOAT:
                value = readFloat();
                break;
            case DOUBLE:
                value = readDouble();
                break;
            default:
                throw new DNGReadException("This DNGFieldType does not exist");
        }
        return value;
    }

    /**
     * Marks the current position.
     *
     * @return The current marked position
     */
    public long markCurrentPosition() {
        this.markedPosition = position;
        return this.markedPosition;
    }

    public void close() throws IOException {
        randomAccessFile.close();
    }

    public ByteOrder getByteOrder() {
        return byteOrder;
    }

    public void reset() {
        position = 0;
    }

    public void skipNBytes(long n) {
        position += n;
    }

    public void setByteOrder(ByteOrder byteOrder) {
        this.byteOrder = Objects.requireNonNull(byteOrder);
    }
}