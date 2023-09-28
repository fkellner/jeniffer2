package de.unituebingen.dng.processor.demosaicingprocessor;

import java.util.Arrays;
import java.util.stream.IntStream;

import de.unituebingen.dng.processor.util.AccelerationStrategy;

/**
 * @author Florian Kellner, Michael Kessler
 */
public class BilinearMedian {

    /**
     * How far Tiles need to overlap in order for the algorithm to function properly
     * @return the width of the margin in pixels
     */
    public static int getOverlap() {
        return 2;
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
            case AUTO_BIG:
            case CPU_TILING_MT: 
                return 128 - 2 * getOverlap();
            case CPU_MT_TILING:
                return 2048 - 2 * getOverlap();
            case CPU_MT_TILING_MT:
            default:
                return 256 - 2 * getOverlap();            
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
    public static float[] process(int[] samples, int width, int height, int redIdx, int greenRedRowIdx, int greenBlueRowIdx, int blueIdx) {
        float[] result = new float[samples.length * 3];
        for(int y = 0, i = 0; y < height; y++) {
            for(int x = 0; x < width; x++, i++) {
                int patternIdx = (x % 2) + 2 * (y % 2);
                if(patternIdx == greenRedRowIdx) {
                    // avg left/right because in red row
                    result[i * 3] = (
                          samples[Math.max(0, x - 1)       + y * width]
                        + samples[Math.min(width - 1, x + 1) + y * width]
                    ) * 0.5f;
                    result[i * 3 + 1] = samples[i];
                    // avg below/above
                    result[i * 3 + 2] = (
                          samples[x + Math.max(0, y - 1)        * width]
                        + samples[x + Math.min(height - 1, y + 1) * width]
                    ) * 0.5f; 
                } else if (patternIdx == greenBlueRowIdx) {
                     // avg below/above
                    result[i * 3] = (
                        samples[x + Math.max(0, y - 1)        * width]
                      + samples[x + Math.min(height - 1, y + 1) * width]
                    ) * 0.5f;
                    result[i * 3 + 1] = samples[i];
                     // avg left/right because in blue row
                    result[i * 3 + 2] = (
                        samples[Math.max(0, x - 1)       + y * width]
                      + samples[Math.min(width - 1, x + 1) + y * width]
                    ) * 0.5f;
                } else if (patternIdx == redIdx) {
                    result[i * 3] = samples[i];
                    result[i * 3 + 1] = axialMedian(samples, width, height, x, y);
                    result[i * 3 + 2] = diagonalMedian(samples, width, height, x, y);
                } else {
                    result[i * 3] = diagonalMedian(samples, width, height, x, y);
                    result[i * 3 + 1] = axialMedian(samples, width, height, x, y);
                    result[i * 3 + 2] = samples[i];
                }
            }
        }
        return result;
    }

    /**
     * calculate the diagonal median at a pixel position
     * <pre>
     * 1 0 1
     * 0 0 0  
     * 1 0 1
     * </pre>
     * Since there are 4 elements, it is the average of the 2nd and 3rd largest elements
     * Edge handling same as getUndemosaicedSample (repeat)
     * @param src image in row-major order
     * @param width
     * @param height
     * @param x
     * @param y
     * @return
     */
    private static int diagonalMedian(int[] src, int width, int height, int x, int y) {
        int west = Math.max(0, x - 1);
        int east = Math.min(width - 1, x + 1);
        int north = Math.max(0, y - 1);
        int south = Math.min(height - 1, y + 1);
        int nw = src[west + north * width];
        int ne = src[east + north * width];
        int sw = src[west + south * width];
        int se = src[east + south * width];
        int[] values = { nw, ne, sw, se };
        Arrays.sort(values);
        return (values[1] + values[2]) / 2; //* 0.5f;
    }

    /**
     * calculate the axial median at a pixel position
     * <pre>
     * 0 1 0
     * 1 0 1
     * 0 1 0
     * </pre>
     * Since there are 4 elements, it is the average of the 2nd and 3rd largest elements
     * Edge handling same as getUndemosaicedSample (repeat)
     * @param src image in row-major order
     * @param width
     * @param height
     * @param x
     * @param y
     * @return
     */
    private static int axialMedian(int[] src, int width, int height, int x, int y) {
        int west = Math.max(0, x - 1);
        int east = Math.min(width - 1, x + 1);
        int north = Math.max(0, y - 1);
        int south = Math.min(height - 1, y + 1);
        int n = src[x + north * width];
        int e = src[east + y * width];
        int s = src[x + south * width];
        int w = src[west + y * width];
        int[] values = { n, e, s, w };
        Arrays.sort(values);
        return (values[1] + values[2]) / 2; //* 0.5f;
    }

    /**
     * Demosaic with bilinear median, i.e.
     * axial or diagonal average where available, else average the two neighbouring pixels.
     * This version uses IntStream multithreading in its main loop.
     * @param samples image in row-major order
     * @param width
     * @param height
     * @param redIdx
     * @param greenRedRowIdx
     * @param greenBlueRowIdx
     * @param blueIdx
     * @return demosaiced image in row-major order, rgb values interleaved ([r g b r g b ...])
     */
    public static float[] processMT(int[] samples, int width, int height, int redIdx, int greenRedRowIdx, int greenBlueRowIdx, int blueIdx) {
        float[] result = new float[samples.length * 3];
        IntStream.range(0, samples.length).parallel().forEach(i -> {
            int x = i % width;
            int y = i / width;
                int patternIdx = (x % 2) + 2 * (y % 2);
                if(patternIdx == greenRedRowIdx) {
                    // avg left/right because in red row
                    result[i * 3] = (
                          samples[Math.max(0, x - 1)       + y * width]
                        + samples[Math.min(width - 1, x + 1) + y * width]
                    ) * 0.5f;
                    result[i * 3 + 1] = samples[i];
                    // avg below/above
                    result[i * 3 + 2] = (
                          samples[x + Math.max(0, y - 1)        * width]
                        + samples[x + Math.min(height - 1, y + 1) * width]
                    ) * 0.5f; 
                } else if (patternIdx == greenBlueRowIdx) {
                     // avg below/above
                    result[i * 3] = (
                        samples[x + Math.max(0, y - 1)        * width]
                      + samples[x + Math.min(height - 1, y + 1) * width]
                    ) * 0.5f;
                    result[i * 3 + 1] = samples[i];
                     // avg left/right because in blue row
                    result[i * 3 + 2] = (
                        samples[Math.max(0, x - 1)       + y * width]
                      + samples[Math.min(width - 1, x + 1) + y * width]
                    ) * 0.5f;
                } else if (patternIdx == redIdx) {
                    result[i * 3] = samples[i];
                    result[i * 3 + 1] = axialMedian(samples, width, height, x, y);
                    result[i * 3 + 2] = diagonalMedian(samples, width, height, x, y);
                } else {
                    result[i * 3] = diagonalMedian(samples, width, height, x, y);
                    result[i * 3 + 1] = axialMedian(samples, width, height, x, y);
                    result[i * 3 + 2] = samples[i];
                }
            
        });
        return result;
    }
}
