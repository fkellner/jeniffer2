package de.unituebingen.dng.reader.dng.opcode;

public abstract class ScaleDeltaPer extends Opcode {

    private long[] params = new long[9];
    protected float[] scalesOrDeltas;

    public ScaleDeltaPer(long id, long dngSpecVersion, long flags, long numberOfBytes, byte[] data) {
        super(id, dngSpecVersion, flags, numberOfBytes, data);
        init();
    }

    private void init() {
        for (int i = 0; i < params.length; i++) {
            params[i] = data.getInt() & 0xFFFFFFFFL;
        }

        int count = (int) getCount();
        scalesOrDeltas = new float[count];
        for(int i = 0; i < count; i++) {
            scalesOrDeltas[i] = data.getFloat();
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

    public long getPlane() {
        return params[4];
    }

    public long getPlanes() {
        return params[5];
    }

    public long getRowPitch() {
        return params[6];
    }

    public long getColPitch() {
        return params[7];
    }

    public long getCount() {
        return params[8];
    }

    public long[] getParams() {
        return params;
    }
}
