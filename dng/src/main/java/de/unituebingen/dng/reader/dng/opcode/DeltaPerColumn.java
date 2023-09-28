package de.unituebingen.dng.reader.dng.opcode;

public class DeltaPerColumn extends ScaleDeltaPer {

    public DeltaPerColumn(long id, long dngSpecVersion, long flags, long numberOfBytes, byte[] data) {
        super(id, dngSpecVersion, flags, numberOfBytes, data);
    }

    public float getDelta(int index) {
        return scalesOrDeltas[index];
    }

    public float[] getDeltas() {
        return scalesOrDeltas;
    }
}
