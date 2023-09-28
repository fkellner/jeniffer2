package de.unituebingen.dng.reader.dng.opcode;

public class TrimBounds extends Opcode {

    private long[] params = new long[4];

    public TrimBounds(long id, long dngSpecVersion, long flags, long numberOfBytes, byte[] data) {
        super(id, dngSpecVersion, flags, numberOfBytes, data);
        init();
    }

    private void init() {
        for (int i = 0; i < params.length; i++) {
            params[i] = data.getInt() & 0xFFFFFFFFL;
        }
    }

    public long getTop() {
        return params[0];
    }

    public long getLeft() {
        return params[1];
    }

    public long getBottom() {
        return params[2];
    }

    public long getRight() {
        return params[3];
    }

}
