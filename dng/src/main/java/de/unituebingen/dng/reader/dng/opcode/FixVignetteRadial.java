package de.unituebingen.dng.reader.dng.opcode;

public class FixVignetteRadial extends Opcode {

    private double[] ks = new double[5];
    private double cx;
    private double cy;

    public FixVignetteRadial(long id, long dngSpecVersion, long flags, long numberOfBytes, byte[] data) {
        super(id, dngSpecVersion, flags, numberOfBytes, data);
        init();
    }

    private void init() {
        for (int i = 0; i < ks.length; i++) {
            ks[i] = data.getDouble();
        }
        cx = data.getDouble();
        cy = data.getDouble();
    }

    public double[] getKs() {
        return ks;
    }

    public double getCx() {
        return cx;
    }

    public double getCy() {
        return cy;
    }
}
