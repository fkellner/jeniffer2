package de.unituebingen.dng.reader.dng.opcode;

public class GainMap extends Opcode {

    private long[] longParams = new long[10];
    private double[] doubleParams = new double[4];
    private long mapPlanes;

    private float[][][] mapGains;

    public GainMap(long id, long dngSpecVersion, long flags, long numberOfBytes, byte[] data) {
        super(id, dngSpecVersion, flags, numberOfBytes, data);
        init();
    }

    private void init() {
        for (int i = 0; i < longParams.length; i++) {
            longParams[i] = data.getInt() & 0xFFFFFFFFL;
        }
        for (int i = 0; i < doubleParams.length; i++) {
            doubleParams[i] = data.getDouble();
        }
        mapPlanes = data.getInt() & 0xFFFFFFFFL;

        int mapPointsV = (int) getMapPointsV();
        int mapPointsH = (int) getMapPointsH();
        mapGains = new float[mapPointsV][mapPointsH][(int) mapPlanes];
        for (int i = 0; i < mapPointsV; i++) {
            for (int j = 0; j < mapPointsH; j++) {
                for (int k = 0; k < mapPlanes; k++) {
                    mapGains[i][j][k] = data.getFloat();
                }
            }
        }
    }

    public long getTop() {
        return longParams[0];
    }

    public long getLeft() {
        return longParams[1];
    }

    public long getBottom() {
        return longParams[2];
    }

    public long getRight() {
        return longParams[3];
    }

    public long getPlane() {
        return longParams[4];
    }

    public long getPlanes() {
        return longParams[5];
    }

    public long getRowPitch() {
        return longParams[6];
    }

    public long getColPitch() {
        return longParams[7];
    }

    public long getMapPointsV() {
        return longParams[8];
    }

    public long getMapPointsH() {
        return longParams[9];
    }

    public double getMapSpacingV() {
        return doubleParams[0];
    }

    public double getMapSpacingH() {
        return doubleParams[1];
    }

    public double getMapOriginV() {
        return doubleParams[2];
    }

    public double getMapOriginH() {
        return doubleParams[3];
    }

    public float getMapGain(int mapPointV, int mapPointH, int mapPlane) {
        return mapGains[mapPointV][mapPointH][mapPlane];
    }

    public long[] getLongParams() {
        return longParams;
    }

    public double[] getDoubleParams() {
        return doubleParams;
    }

    public long getMapPlanes() {
        return mapPlanes;
    }

    public float[][][] getMapGains() {
        return mapGains;
    }
}
