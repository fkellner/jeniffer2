package de.unituebingen.dng.reader.dng.opcode;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Data has to be parsed manually for ech opcode
 */
public abstract class Opcode {

    private long id;
    private long dngSpecVersion;
    private long flags;
    private long numberOfBytes;

    protected ByteBuffer data;

    public Opcode(long id, long dngSpecVersion, long flags, long numberOfBytes, byte[] data) {
        this.id = id;
        this.dngSpecVersion = dngSpecVersion;
        this.flags = flags;
        this.numberOfBytes = numberOfBytes;
        this.data = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
    }

    public long getId() {
        return id;
    }

    public long getDngSpecVersion() {
        return dngSpecVersion;
    }

    public long getFlags() {
        return flags;
    }

    public long getNumberOfBytes() {
        return numberOfBytes;
    }

    public double getDouble() {
        return data.getDouble();
    }
}
