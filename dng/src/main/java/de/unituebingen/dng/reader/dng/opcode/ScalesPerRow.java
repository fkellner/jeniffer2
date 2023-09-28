package de.unituebingen.dng.reader.dng.opcode;

public class ScalesPerRow extends ScaleDeltaPer {

    public ScalesPerRow(long id, long dngSpecVersion, long flags, long numberOfBytes, byte[] data) {
        super(id, dngSpecVersion, flags, numberOfBytes, data);
    }

    public float getScale(int index) {
        return scalesOrDeltas[index];
    }

    public float[] getScales() {
        return scalesOrDeltas;
    }
}
