package de.unituebingen.dng.reader.dng.opcode;

public class MapPolynomial extends Opcode {

    private long[] params = new long[9];
    private double[] coefficients;

    public MapPolynomial(long id, long dngSpecVersion, long flags, long numberOfBytes, byte[] data) {
        super(id, dngSpecVersion, flags, numberOfBytes, data);
        init();
    }

    private void init() {
        for (int i = 0; i < params.length; i++) {
            params[i] = data.getInt() & 0xFFFFFFFFL;
        }

        int degrees = (int) getDegree() + 1;
        coefficients = new double[degrees];
        for (int i = 0; i < degrees; i++) {
            coefficients[i] = data.getDouble();
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

    public long getDegree() {
        return params[8];
    }

    public double getCoefficient(int index) {
        return coefficients[index];
    }

    public long[] getParams() {
        return params;
    }

    public double[] getCoefficients() {
        return coefficients;
    }
}
