package de.unituebingen.dng.processor.demosaicingprocessor;

import java.util.stream.IntStream;

import de.unituebingen.dng.processor.util.AccelerationStrategy;

/**
 * @author Florian Kellner, Michael Kessler
 */
public class BiCubic {
    
        /**
     * How far Tiles need to overlap in order for the algorithm to function properly
     * @return the width of the margin in pixels
     */
    public static int getOverlap() {
        return 4;
    }

    /**
     * How big to make tiles. Can be tuned to make all pixels, possibly of all threads fit into e.g. L3 or L2
     * cache. It may also be beneficial if tileSize + 2*12 is a power of two.
     * TODO: tune
     * @return tile side length in pixels
     */
    public static int getTileSize(AccelerationStrategy accelerationStrategy) {
        switch(accelerationStrategy) {
            case CPU_TILING:
                return 256 - 2 * getOverlap();
            case CPU_TILING_MT: 
                return 128 - 2 * getOverlap();
            case AUTO_BIG:
            case CPU_MT_TILING:
            case CPU_MT_TILING_MT:
            default:
                return 2048 - 2 * getOverlap();
            
        }
    }

    /**
     * Demosaic with bilinear median, i.e.
     * axial or diagonal average where available, else average the two neighbouring pixels
     * @param samples image in row-major order
     * @param width
     * @param height
     * @param redIdx
     * @param greenRedRowIdx
     * @param greenBlueRowIdx
     * @param blueIdx
     * @return demosaiced image in row-major order, rgb values interleaved ([r g b r g b ...])
     */
    public static float[] process(int[] samples, int width, int height, int redIdx, int greenRedRowIdx,
            int greenBlueRowIdx, int blueIdx) {
        float[] result = new float[samples.length * 3];
        for (int y = 0, i = 0; y < height; y++) {
            for (int x = 0; x < width; x++, i++) {
                int patternIdx = (x % 2) + 2 * (y % 2);
                if (patternIdx == greenRedRowIdx) {
                    int v0 = samples[index(x - 3, y, width, height)];
                    int v1 = samples[index(x - 1, y, width, height)];
                    int n0 = samples[index(x + 1, y, width, height)];
                    int n1 = samples[index(x + 3, y, width, height)];
                    int P = (n1 - n0) - (v0 - v1);
                    int Q = (v0 - v1) - P;
                    int R = (n0 - v0);
                    result[3 * i] = 0.125f * P + 0.25f * Q + 0.5f * R + v1;

                    result[3 * i + 1] = samples[i];

                    v0 = samples[index(x, y - 3, width, height)];
                    v1 = samples[index(x, y - 1, width, height)];
                    n0 = samples[index(x, y + 1, width, height)];
                    n1 = samples[index(x, y + 3, width, height)];
                    P = (n1 - n0) - (v0 - v1);
                    Q = (v0 - v1) - P;
                    R = (n0 - v0);

                    result[3 * i + 2] = 0.125f * P + 0.25f * Q + 0.5f * R + v1;
                } else if (patternIdx == greenBlueRowIdx) {
                    int v0 = samples[index(x, y - 3, width, height)];
                    int v1 = samples[index(x, y - 1, width, height)];
                    int n0 = samples[index(x, y + 1, width, height)];
                    int n1 = samples[index(x, y + 3, width, height)];
                    int P = (n1 - n0) - (v0 - v1);
                    int Q = (v0 - v1) - P;
                    int R = (n0 - v0);
                    result[3 * i] = 0.125f * P + 0.25f * Q + 0.5f * R + v1;

                    result[3 * i + 1] = samples[i];

                    v0 = samples[index(x - 3, y, width, height)];
                    v1 = samples[index(x - 1, y, width, height)];
                    n0 = samples[index(x + 1, y, width, height)];
                    n1 = samples[index(x + 3, y, width, height)];
                    P = (n1 - n0) - (v0 - v1);
                    Q = (v0 - v1) - P;
                    R = (n0 - v0);

                    result[3 * i + 2] = 0.125f * P + 0.25f * Q + 0.5f * R + v1;
                } else if (patternIdx == redIdx) {
                    result[i * 3] = samples[i];
            
                    //templine1
                    int v0 = samples[index(x - 3, y    , width, height)];
                    int v1 = samples[index(x - 2, y - 1, width, height)];
                    int n0 = samples[index(x - 1, y - 2, width, height)];
                    int n1 = samples[index(x    , y - 3, width, height)];
                    int P = (n1 - n0) - (v0 - v1);
                    int Q = (v0 - v1) - P;
                    int R = (n0 - v0);
                    int templine1 = (int) (0.125f * P + 0.25f * Q + 0.5f * R + v1);
            
                    //templine2
                    v0 = samples[index(x + 1, y - 2, width, height)];
                    v1 = samples[index(x    , y - 1, width, height)];
                    n0 = samples[index(x - 1, y    , width, height)];
                    n1 = samples[index(x - 2, y + 1, width, height)];
                    P = (n1 - n0) - (v0 - v1);
                    Q = (v0 - v1) - P;
                    R = (n0 - v0);
                    int templine2 = (int) (0.125f * P + 0.25f * Q + 0.5f * R + v1);
            
                    //templine3
                    v0 = samples[index(x + 2, y - 1, width, height)];
                    v1 = samples[index(x + 1, y    , width, height)];
                    n0 = samples[index(x    , y + 1, width, height)];
                    n1 = samples[index(x - 1, y + 2, width, height)];
                    P = (n1 - n0) - (v0 - v1);
                    Q = (v0 - v1) - P;
                    R = (n0 - v0);
                    int templine3 = (int) (0.125f * P + 0.25f * Q + 0.5f * R + v1);

                    //templine4
                    v0 = samples[index(x + 3, y    , width, height)];
                    v1 = samples[index(x + 2, y + 1, width, height)];
                    n0 = samples[index(x + 1, y + 2, width, height)];
                    n1 = samples[index(x    , y + 3, width, height)];
                    P = (n1 - n0) - (v0 - v1);
                    Q = (v0 - v1) - P;
                    R = (n0 - v0);
                    int templine4 = (int) (0.125f * P + 0.25f * Q + 0.5f * R + v1);
            
                    //Grünwert aus templines berechnen
                    v0 = templine1;
                    v1 = templine2;
                    n0 = templine3;
                    n1 = templine4;
                    P = (n1 - n0) - (v0 - v1);
                    Q = (v0 - v1) - P;
                    R = (n0 - v0);
                    result[i * 3 + 1] = 0.125f * P + 0.25f * Q + 0.5f * R + v1;
            
                    //Blauwerte berechnen
                    //templine1
                    v0 = samples[index(x - 3, y - 3, width, height)];
                    v1 = samples[index(x - 1, y - 3, width, height)];
                    n0 = samples[index(x + 1, y - 3, width, height)];
                    n1 = samples[index(x + 3, y - 3, width, height)];
                    P = (n1 - n0) - (v0 - v1);
                    Q = (v0 - v1) - P;
                    R = (n0 - v0);
                    templine1 = (int) (0.125f * P + 0.25f * Q + 0.5f * R + v1);

                    //templine2
                    v0 = samples[index(x - 3, y - 1, width, height)];
                    v1 = samples[index(x - 1, y - 1, width, height)];
                    n0 = samples[index(x + 1, y - 1, width, height)];
                    n1 = samples[index(x + 3, y - 1, width, height)];
                    P = (n1 - n0) - (v0 - v1);
                    Q = (v0 - v1) - P;
                    R = (n0 - v0);
                    templine2 = (int) (0.125f * P + 0.25f * Q + 0.5f * R + v1);

                    //templine3
                    v0 = samples[index(x - 3, y + 1, width, height)];
                    v1 = samples[index(x - 1, y + 1, width, height)];
                    n0 = samples[index(x + 1, y + 1, width, height)];
                    n1 = samples[index(x + 3, y + 1, width, height)];
                    P = (n1 - n0) - (v0 - v1);
                    Q = (v0 - v1) - P;
                    R = (n0 - v0);
                    templine3 = (int) (0.125f * P + 0.25f * Q + 0.5f * R + v1);

                    //templine4
                    v0 = samples[index(x - 3, y + 3, width, height)];
                    v1 = samples[index(x - 1, y + 3, width, height)];
                    n0 = samples[index(x + 1, y + 3, width, height)];
                    n1 = samples[index(x + 3, y + 3, width, height)];
                    P = (n1 - n0) - (v0 - v1);
                    Q = (v0 - v1) - P;
                    R = (n0 - v0);
                    templine4 = (int) (0.125f * P + 0.25f * Q + 0.5f * R + v1);
            
                    //Blauwert aus templines berechnen
                    v0 = templine1;
                    v1 = templine2;
                    n0 = templine3;
                    n1 = templine4;
                    P = (n1 - n0) - (v0 - v1);
                    Q = (v0 - v1) - P;
                    R = (n0 - v0);
                    result[i * 3 + 2] = 0.125f * P + 0.25f * Q + 0.5f * R + v1;
                } else {
                    //Blauwert berechnen
                    result[i * 3 + 2] = samples[i];

                    //Grünwerte berechnen
                    //templine1
                    int v0 = samples[index(x - 3, y    , width, height)];
                    int v1 = samples[index(x - 2, y - 1, width, height)];
                    int n0 = samples[index(x - 1, y - 2, width, height)];
                    int n1 = samples[index(x    , y - 3, width, height)];
                    int P = (n1 - n0) - (v0 - v1);
                    int Q = (v0 - v1) - P;
                    int R = (n0 - v0);
                    int templine1 = (int) (0.125f * P + 0.25f * Q + 0.5f * R + v1);

                    //templine2
                    v0 = samples[index(x + 1, y - 2, width, height)];
                    v1 = samples[index(x    , y - 1, width, height)];
                    n0 = samples[index(x - 1, y    , width, height)];
                    n1 = samples[index(x - 2, y + 1, width, height)];
                    P = (n1 - n0) - (v0 - v1);
                    Q = (v0 - v1) - P;
                    R = (n0 - v0);
                    int templine2 = (int) (0.125f * P + 0.25f * Q + 0.5f * R + v1);

                    //templine3
                    v0 = samples[index(x + 2, y - 1, width, height)];
                    v1 = samples[index(x + 1, y    , width, height)];
                    n0 = samples[index(x    , y + 1, width, height)];
                    n1 = samples[index(x - 1, y + 2, width, height)];
                    P = (n1 - n0) - (v0 - v1);
                    Q = (v0 - v1) - P;
                    R = (n0 - v0);
                    int templine3 = (int) (0.125f * P + 0.25f * Q + 0.5f * R + v1);

                    //templine4
                    v0 = samples[index(x + 3, y    , width, height)];
                    v1 = samples[index(x + 2, y + 1, width, height)];
                    n0 = samples[index(x + 1, y + 2, width, height)];
                    n1 = samples[index(x    , y + 3, width, height)];
                    P = (n1 - n0) - (v0 - v1);
                    Q = (v0 - v1) - P;
                    R = (n0 - v0);
                    int templine4 = (int) (0.125f * P + 0.25f * Q + 0.5f * R + v1);

                    //Rotwert aus templines berechnen
                    v0 = templine1;
                    v1 = templine2;
                    n0 = templine3;
                    n1 = templine4;
                    P = (n1 - n0) - (v0 - v1);
                    Q = (v0 - v1) - P;
                    R = (n0 - v0);
                    result[i * 3 + 1] = 0.125f * P + 0.25f * Q + 0.5f * R + v1;

                    //Rotwerte berechnen
                    //templine1
                    v0 = samples[index(x - 3, y - 3, width, height)];
                    v1 = samples[index(x - 1, y - 3, width, height)];
                    n0 = samples[index(x + 1, y - 3, width, height)];
                    n1 = samples[index(x + 3, y - 3, width, height)];
                    P = (n1 - n0) - (v0 - v1);
                    Q = (v0 - v1) - P;
                    R = (n0 - v0);
                    templine1 = (int) (0.125f * P + 0.25f * Q + 0.5f * R + v1);

                    //templine2
                    v0 = samples[index(x - 3, y - 1, width, height)];
                    v1 = samples[index(x - 1, y - 1, width, height)];
                    n0 = samples[index(x + 1, y - 1, width, height)];
                    n1 = samples[index(x + 3, y - 1, width, height)];
                    P = (n1 - n0) - (v0 - v1);
                    Q = (v0 - v1) - P;
                    R = (n0 - v0);
                    templine2 = (int) (0.125f * P + 0.25f * Q + 0.5f * R + v1);

                    //templine3
                    v0 = samples[index(x - 3, y + 1, width, height)];
                    v1 = samples[index(x - 1, y + 1, width, height)];
                    n0 = samples[index(x + 1, y + 1, width, height)];
                    n1 = samples[index(x + 3, y + 1, width, height)];
                    P = (n1 - n0) - (v0 - v1);
                    Q = (v0 - v1) - P;
                    R = (n0 - v0);
                    templine3 = (int) (0.125f * P + 0.25f * Q + 0.5f * R + v1);

                    //templine4
                    v0 = samples[index(x - 3, y + 3, width, height)];
                    v1 = samples[index(x - 1, y + 3, width, height)];
                    n0 = samples[index(x + 1, y + 3, width, height)];
                    n1 = samples[index(x + 3, y + 3, width, height)];
                    P = (n1 - n0) - (v0 - v1);
                    Q = (v0 - v1) - P;
                    R = (n0 - v0);
                    templine4 = (int) (0.125f * P + 0.25f * Q + 0.5f * R + v1);

                    //Blauwert aus templines berechnen
                    v0 = templine1;
                    v1 = templine2;
                    n0 = templine3;
                    n1 = templine4;
                    P = (n1 - n0) - (v0 - v1);
                    Q = (v0 - v1) - P;
                    R = (n0 - v0);
                    result[i * 3] = 0.125f * P + 0.25f * Q + 0.5f * R + v1;
                }
            }
        }
        return result;
    }

    public static int index(int x, int y, int width, int height) {
        return Math.max(0, Math.min(width - 1, x)) + Math.max(0, Math.min(height - 1, y)) * width;
    }

    /**
     * Demosaic with bilinear median, i.e.
     * axial or diagonal average where available, else average the two neighbouring pixels.
     * This variant uses multithreading with IntStream in its main loop.
     * @param samples image in row-major order
     * @param width
     * @param height
     * @param redIdx
     * @param greenRedRowIdx
     * @param greenBlueRowIdx
     * @param blueIdx
     * @return demosaiced image in row-major order, rgb values interleaved ([r g b r g b ...])
     */
    public static float[] processMT(int[] samples, int width, int height, int redIdx, int greenRedRowIdx,
            int greenBlueRowIdx, int blueIdx) {
        float[] result = new float[samples.length * 3];
        IntStream.range(0, samples.length).parallel().forEach(i -> {
                int x = i % width;
                int y = i / width;
                int patternIdx = (x % 2) + 2 * (y % 2);
                if (patternIdx == greenRedRowIdx) {
                    int v0 = samples[index(x - 3, y, width, height)];
                    int v1 = samples[index(x - 1, y, width, height)];
                    int n0 = samples[index(x + 1, y, width, height)];
                    int n1 = samples[index(x + 3, y, width, height)];
                    int P = (n1 - n0) - (v0 - v1);
                    int Q = (v0 - v1) - P;
                    int R = (n0 - v0);
                    result[3 * i] = 0.125f * P + 0.25f * Q + 0.5f * R + v1;

                    result[3 * i + 1] = samples[i];

                    v0 = samples[index(x, y - 3, width, height)];
                    v1 = samples[index(x, y - 1, width, height)];
                    n0 = samples[index(x, y + 1, width, height)];
                    n1 = samples[index(x, y + 3, width, height)];
                    P = (n1 - n0) - (v0 - v1);
                    Q = (v0 - v1) - P;
                    R = (n0 - v0);

                    result[3 * i + 2] = 0.125f * P + 0.25f * Q + 0.5f * R + v1;
                } else if (patternIdx == greenBlueRowIdx) {
                    int v0 = samples[index(x, y - 3, width, height)];
                    int v1 = samples[index(x, y - 1, width, height)];
                    int n0 = samples[index(x, y + 1, width, height)];
                    int n1 = samples[index(x, y + 3, width, height)];
                    int P = (n1 - n0) - (v0 - v1);
                    int Q = (v0 - v1) - P;
                    int R = (n0 - v0);
                    result[3 * i] = 0.125f * P + 0.25f * Q + 0.5f * R + v1;

                    result[3 * i + 1] = samples[i];

                    v0 = samples[index(x - 3, y, width, height)];
                    v1 = samples[index(x - 1, y, width, height)];
                    n0 = samples[index(x + 1, y, width, height)];
                    n1 = samples[index(x + 3, y, width, height)];
                    P = (n1 - n0) - (v0 - v1);
                    Q = (v0 - v1) - P;
                    R = (n0 - v0);

                    result[3 * i + 2] = 0.125f * P + 0.25f * Q + 0.5f * R + v1;
                } else if (patternIdx == redIdx) {
                    result[i * 3] = samples[i];
            
                    //templine1
                    int v0 = samples[index(x - 3, y    , width, height)];
                    int v1 = samples[index(x - 2, y - 1, width, height)];
                    int n0 = samples[index(x - 1, y - 2, width, height)];
                    int n1 = samples[index(x    , y - 3, width, height)];
                    int P = (n1 - n0) - (v0 - v1);
                    int Q = (v0 - v1) - P;
                    int R = (n0 - v0);
                    int templine1 = (int) (0.125f * P + 0.25f * Q + 0.5f * R + v1);
            
                    //templine2
                    v0 = samples[index(x + 1, y - 2, width, height)];
                    v1 = samples[index(x    , y - 1, width, height)];
                    n0 = samples[index(x - 1, y    , width, height)];
                    n1 = samples[index(x - 2, y + 1, width, height)];
                    P = (n1 - n0) - (v0 - v1);
                    Q = (v0 - v1) - P;
                    R = (n0 - v0);
                    int templine2 = (int) (0.125f * P + 0.25f * Q + 0.5f * R + v1);
            
                    //templine3
                    v0 = samples[index(x + 2, y - 1, width, height)];
                    v1 = samples[index(x + 1, y    , width, height)];
                    n0 = samples[index(x    , y + 1, width, height)];
                    n1 = samples[index(x - 1, y + 2, width, height)];
                    P = (n1 - n0) - (v0 - v1);
                    Q = (v0 - v1) - P;
                    R = (n0 - v0);
                    int templine3 = (int) (0.125f * P + 0.25f * Q + 0.5f * R + v1);

                    //templine4
                    v0 = samples[index(x + 3, y    , width, height)];
                    v1 = samples[index(x + 2, y + 1, width, height)];
                    n0 = samples[index(x + 1, y + 2, width, height)];
                    n1 = samples[index(x    , y + 3, width, height)];
                    P = (n1 - n0) - (v0 - v1);
                    Q = (v0 - v1) - P;
                    R = (n0 - v0);
                    int templine4 = (int) (0.125f * P + 0.25f * Q + 0.5f * R + v1);
            
                    //Grünwert aus templines berechnen
                    v0 = templine1;
                    v1 = templine2;
                    n0 = templine3;
                    n1 = templine4;
                    P = (n1 - n0) - (v0 - v1);
                    Q = (v0 - v1) - P;
                    R = (n0 - v0);
                    result[i * 3 + 1] = 0.125f * P + 0.25f * Q + 0.5f * R + v1;
            
                    //Blauwerte berechnen
                    //templine1
                    v0 = samples[index(x - 3, y - 3, width, height)];
                    v1 = samples[index(x - 1, y - 3, width, height)];
                    n0 = samples[index(x + 1, y - 3, width, height)];
                    n1 = samples[index(x + 3, y - 3, width, height)];
                    P = (n1 - n0) - (v0 - v1);
                    Q = (v0 - v1) - P;
                    R = (n0 - v0);
                    templine1 = (int) (0.125f * P + 0.25f * Q + 0.5f * R + v1);

                    //templine2
                    v0 = samples[index(x - 3, y - 1, width, height)];
                    v1 = samples[index(x - 1, y - 1, width, height)];
                    n0 = samples[index(x + 1, y - 1, width, height)];
                    n1 = samples[index(x + 3, y - 1, width, height)];
                    P = (n1 - n0) - (v0 - v1);
                    Q = (v0 - v1) - P;
                    R = (n0 - v0);
                    templine2 = (int) (0.125f * P + 0.25f * Q + 0.5f * R + v1);

                    //templine3
                    v0 = samples[index(x - 3, y + 1, width, height)];
                    v1 = samples[index(x - 1, y + 1, width, height)];
                    n0 = samples[index(x + 1, y + 1, width, height)];
                    n1 = samples[index(x + 3, y + 1, width, height)];
                    P = (n1 - n0) - (v0 - v1);
                    Q = (v0 - v1) - P;
                    R = (n0 - v0);
                    templine3 = (int) (0.125f * P + 0.25f * Q + 0.5f * R + v1);

                    //templine4
                    v0 = samples[index(x - 3, y + 3, width, height)];
                    v1 = samples[index(x - 1, y + 3, width, height)];
                    n0 = samples[index(x + 1, y + 3, width, height)];
                    n1 = samples[index(x + 3, y + 3, width, height)];
                    P = (n1 - n0) - (v0 - v1);
                    Q = (v0 - v1) - P;
                    R = (n0 - v0);
                    templine4 = (int) (0.125f * P + 0.25f * Q + 0.5f * R + v1);
            
                    //Blauwert aus templines berechnen
                    v0 = templine1;
                    v1 = templine2;
                    n0 = templine3;
                    n1 = templine4;
                    P = (n1 - n0) - (v0 - v1);
                    Q = (v0 - v1) - P;
                    R = (n0 - v0);
                    result[i * 3 + 2] = 0.125f * P + 0.25f * Q + 0.5f * R + v1;
                } else {
                    //Blauwert berechnen
                    result[i * 3 + 2] = samples[i];

                    //Grünwerte berechnen
                    //templine1
                    int v0 = samples[index(x - 3, y    , width, height)];
                    int v1 = samples[index(x - 2, y - 1, width, height)];
                    int n0 = samples[index(x - 1, y - 2, width, height)];
                    int n1 = samples[index(x    , y - 3, width, height)];
                    int P = (n1 - n0) - (v0 - v1);
                    int Q = (v0 - v1) - P;
                    int R = (n0 - v0);
                    int templine1 = (int) (0.125f * P + 0.25f * Q + 0.5f * R + v1);

                    //templine2
                    v0 = samples[index(x + 1, y - 2, width, height)];
                    v1 = samples[index(x    , y - 1, width, height)];
                    n0 = samples[index(x - 1, y    , width, height)];
                    n1 = samples[index(x - 2, y + 1, width, height)];
                    P = (n1 - n0) - (v0 - v1);
                    Q = (v0 - v1) - P;
                    R = (n0 - v0);
                    int templine2 = (int) (0.125f * P + 0.25f * Q + 0.5f * R + v1);

                    //templine3
                    v0 = samples[index(x + 2, y - 1, width, height)];
                    v1 = samples[index(x + 1, y    , width, height)];
                    n0 = samples[index(x    , y + 1, width, height)];
                    n1 = samples[index(x - 1, y + 2, width, height)];
                    P = (n1 - n0) - (v0 - v1);
                    Q = (v0 - v1) - P;
                    R = (n0 - v0);
                    int templine3 = (int) (0.125f * P + 0.25f * Q + 0.5f * R + v1);

                    //templine4
                    v0 = samples[index(x + 3, y    , width, height)];
                    v1 = samples[index(x + 2, y + 1, width, height)];
                    n0 = samples[index(x + 1, y + 2, width, height)];
                    n1 = samples[index(x    , y + 3, width, height)];
                    P = (n1 - n0) - (v0 - v1);
                    Q = (v0 - v1) - P;
                    R = (n0 - v0);
                    int templine4 = (int) (0.125f * P + 0.25f * Q + 0.5f * R + v1);

                    //Rotwert aus templines berechnen
                    v0 = templine1;
                    v1 = templine2;
                    n0 = templine3;
                    n1 = templine4;
                    P = (n1 - n0) - (v0 - v1);
                    Q = (v0 - v1) - P;
                    R = (n0 - v0);
                    result[i * 3 + 1] = 0.125f * P + 0.25f * Q + 0.5f * R + v1;

                    //Rotwerte berechnen
                    //templine1
                    v0 = samples[index(x - 3, y - 3, width, height)];
                    v1 = samples[index(x - 1, y - 3, width, height)];
                    n0 = samples[index(x + 1, y - 3, width, height)];
                    n1 = samples[index(x + 3, y - 3, width, height)];
                    P = (n1 - n0) - (v0 - v1);
                    Q = (v0 - v1) - P;
                    R = (n0 - v0);
                    templine1 = (int) (0.125f * P + 0.25f * Q + 0.5f * R + v1);

                    //templine2
                    v0 = samples[index(x - 3, y - 1, width, height)];
                    v1 = samples[index(x - 1, y - 1, width, height)];
                    n0 = samples[index(x + 1, y - 1, width, height)];
                    n1 = samples[index(x + 3, y - 1, width, height)];
                    P = (n1 - n0) - (v0 - v1);
                    Q = (v0 - v1) - P;
                    R = (n0 - v0);
                    templine2 = (int) (0.125f * P + 0.25f * Q + 0.5f * R + v1);

                    //templine3
                    v0 = samples[index(x - 3, y + 1, width, height)];
                    v1 = samples[index(x - 1, y + 1, width, height)];
                    n0 = samples[index(x + 1, y + 1, width, height)];
                    n1 = samples[index(x + 3, y + 1, width, height)];
                    P = (n1 - n0) - (v0 - v1);
                    Q = (v0 - v1) - P;
                    R = (n0 - v0);
                    templine3 = (int) (0.125f * P + 0.25f * Q + 0.5f * R + v1);

                    //templine4
                    v0 = samples[index(x - 3, y + 3, width, height)];
                    v1 = samples[index(x - 1, y + 3, width, height)];
                    n0 = samples[index(x + 1, y + 3, width, height)];
                    n1 = samples[index(x + 3, y + 3, width, height)];
                    P = (n1 - n0) - (v0 - v1);
                    Q = (v0 - v1) - P;
                    R = (n0 - v0);
                    templine4 = (int) (0.125f * P + 0.25f * Q + 0.5f * R + v1);

                    //Blauwert aus templines berechnen
                    v0 = templine1;
                    v1 = templine2;
                    n0 = templine3;
                    n1 = templine4;
                    P = (n1 - n0) - (v0 - v1);
                    Q = (v0 - v1) - P;
                    R = (n0 - v0);
                    result[i * 3] = 0.125f * P + 0.25f * Q + 0.5f * R + v1;
                }
            
        });
        return result;
    }
}
