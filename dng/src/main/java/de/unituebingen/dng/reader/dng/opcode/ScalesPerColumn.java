package de.unituebingen.dng.reader.dng.opcode;

public class ScalesPerColumn extends ScaleDeltaPer {

    public ScalesPerColumn(long id, long dngSpecVersion, long flags, long numberOfBytes, byte[] data) {
        super(id, dngSpecVersion, flags, numberOfBytes, data);
    }

    public float getScale(int index) {
        return scalesOrDeltas[index];
    }

    public float[] getScalesor() {
        return scalesOrDeltas;
    }
}
