package de.unituebingen.dng.reader.dng.opcode;

public class WarpFisheye extends Opcode {

    private static final int COEFFICIENTS = 4;

    private int amountCoefficientSets;
    private double[][] coefficientSets;
    private double cx;
    private double cy;

    public WarpFisheye(long id, long dngSpecVersion, long flags, long numberOfBytes, byte[] data) {
        super(id, dngSpecVersion, flags, numberOfBytes, data);
        init();
    }

    private void init() {
        amountCoefficientSets = (int) (data.getInt() & 0xFFFFFFFFL);
        coefficientSets = new double[amountCoefficientSets][COEFFICIENTS];
        for (int i = 0; i < amountCoefficientSets; i++) {
            for (int j = 0; j < COEFFICIENTS; j++) {
                coefficientSets[i][j] = data.getDouble();
            }
        }
        cx = data.getDouble();
        cy = data.getDouble();
    }

    public int getAmountCoefficientSets() {
        return amountCoefficientSets;
    }

    public double[][] getCoefficientSets() {
        return coefficientSets;
    }

    public double getCx() {
        return cx;
    }

    public double getCy() {
        return cy;
    }
}
