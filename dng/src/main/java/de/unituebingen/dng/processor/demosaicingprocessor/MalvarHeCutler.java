package de.unituebingen.dng.processor.demosaicingprocessor;

import java.util.stream.IntStream;

import de.unituebingen.dng.processor.util.AccelerationStrategy;

/**
 * @author Florian Kellner, Andreas Reiter
 */
public class MalvarHeCutler {
    //Malvar-He-Cutler Linear Demosaicking
    //adapted from
    //https://www.ipol.im/pub/art/2011/g_mhcd/

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
     * Demosaic with Malvar He Cutler Algorithm
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
                    result[i * 3]     = interpolateRBHorizontal(samples, width, height, x, y);
                    result[i * 3 + 1] = samples[i];
                    result[i * 3 + 2] = interpolateRBVertical(samples, width, height, x, y);
                } else if (patternIdx == greenBlueRowIdx) {
                    result[i * 3]     = interpolateRBVertical(samples, width, height, x, y);
                    result[i * 3 + 1] = samples[i];
                    result[i * 3 + 2] = interpolateRBHorizontal(samples, width, height, x, y);
                } else if (patternIdx == redIdx) {
                    result[i * 3]     = samples[i];
                    result[i * 3 + 1] = interpolateGreen(samples, width, height, x, y);
                    result[i * 3 + 2] = interpolateRBDiagonal(samples, width, height, x, y);
                } else { // blue
                    result[i * 3]     = interpolateRBDiagonal(samples, width, height, x, y);
                    result[i * 3 + 1] = interpolateGreen(samples, width, height, x, y);
                    result[i * 3 + 2] = samples[i];
                }
            }
        }
        return result;
    }

    /**
     * Axial Filter for green Values at red and blue locations:
     * Values with weight 2 are green pixels.
     * <pre>.
     *     -1
     *      2
     * -1 2 4 2 -1  / 8
     *      2
     *     -1
     * </pre>
     * Edge handling same as getUndemosaicedSample (repeat).
     * @param src image in row-major order
     * @param width
     * @param height
     * @param x
     * @param y
     * @return
     */
    private static int interpolateGreen(int[] src, int width, int height, int x, int y) {
        // terms linewise
        return (
            -     src[x + Math.max(0, y - 2) * width]

            + 2 * src[x + Math.max(0, y - 1) * width]

            -     src[Math.max(0, x - 2) + y * width]
            + 2 * src[Math.max(0, x - 1) + y * width]
            + 4 * src[x + y * width]
            + 2 * src[Math.min(width - 1, x + 1) + y * width]
            -     src[Math.min(width - 1, x + 2) + y * width]

            + 2 * src[x + Math.min(height - 1, y + 1) * width]

            -     src[x + Math.min(height - 1, y + 2) * width]
        ) / 8;
    }

    /**
     * Filter for missing red/blue values where the closest values are horizontally beside them.
     * Values with weight 4 are red/blue pixels.
     * <pre>.
     *       1/2
     *    -1    -1
     * -1  4  5  4  -1  / 8
     *    -1    -1
     *       1/2
     * </pre>
     * Edge handling same as getUndemosaicedSample (repeat).
     * @param src image in row-major order
     * @param width
     * @param height
     * @param x
     * @param y
     * @return
     */
    private static int interpolateRBHorizontal(int[] src, int width, int height, int x, int y) {
        // terms linewise
        return (
                  src[x + Math.max(0, y - 2) * width]

            -  2 * src[Math.max(0, x - 1) + Math.max(0, y - 1) * width]
            -  2 * src[Math.min(width - 1, x + 1) + Math.max(0, y - 1) * width]

            -  2 * src[Math.max(0, x - 2) + y * width]
            +  8 * src[Math.max(0, x - 1) + y * width]
            + 10 * src[x + y * width]
            +  8 * src[Math.min(width - 1, x + 1) + y * width]
            -  2 * src[Math.min(width - 1, x + 2) + y * width]

            -  2 * src[Math.max(0, x - 1) + Math.min(height - 1, y + 1) * width]
            -  2 * src[Math.min(width - 1, x + 1) + Math.min(height - 1, y + 1) * width]

            +      src[x + Math.min(height - 1, y + 2) * width]
        ) / 16;
    }

    /**
     * Filter for missing red/blue values where the closest values are vertically above/below them.
     * Values with weight 4 are red/blue pixels.
     * <pre>.
     *       -1
     *    -1  4 -1
     * 1/2    5    1/2  / 8
     *    -1  4 -1
     *       -1
     * </pre>
     * Edge handling same as getUndemosaicedSample (repeat).
     * @param src image in row-major order
     * @param width
     * @param height
     * @param x
     * @param y
     * @return
     */
    private static int interpolateRBVertical(int[] src, int width, int height, int x, int y) {
        // terms linewise
        return (
            -  2 * src[x + Math.max(0, y - 2) * width]

            -  2 * src[Math.max(0, x - 1) + Math.max(0, y - 1) * width]
            +  8 * src[x + Math.max(0, y - 1) * width]
            -  2 * src[Math.min(width - 1, x + 1) + Math.max(0, y - 1) * width]

            +      src[Math.max(0, x - 2) + y * width]
            + 10 * src[x + y * width]
            +      src[Math.min(width - 1, x + 2) + y * width]

            -  2 * src[Math.max(0, x - 1) + Math.min(height - 1, y + 1) * width]
            +  8 * src[x + Math.min(height - 1, y + 1) * width]
            -  2 * src[Math.min(width - 1, x + 1) + Math.min(height - 1, y + 1) * width]

            -  2 * src[x + Math.min(height - 1, y + 2) * width]
        ) / 16;
    }

    /**
     * Filter for missing red/blue values at blue/red locations.
     * Values with weight 4 are red/blue pixels.
     * <pre>.
     *      -3
     *    4    4
     * -3   12   -3  / 16
     *    4    4
     *      -3
     * </pre>
     * Edge handling same as getUndemosaicedSample (repeat).
     * @param src image in row-major order
     * @param width
     * @param height
     * @param x
     * @param y
     * @return
     */
    private static int interpolateRBDiagonal(int[] src, int width, int height, int x, int y) {
        // terms linewise
        return (
            -  3 * src[x + Math.max(0, y - 2) * width]

            +  4 * src[Math.max(0, x - 1) + Math.max(0, y - 1) * width]
            +  4 * src[Math.min(width - 1, x + 1) + Math.max(0, y - 1) * width]

            -  3 * src[Math.max(0, x - 2) + y * width]
            + 12 * src[x + y * width]
            -  3 * src[Math.min(width - 1, x + 2) + y * width]

            +  4 * src[Math.max(0, x - 1) + Math.min(height - 1, y + 1) * width]
            +  4 * src[Math.min(width - 1, x + 1) + Math.min(height - 1, y + 1) * width]

            -  3 * src[x + Math.min(height - 1, y + 2) * width]
        ) / 16;
    }

    /**
     * Demosaic with Malvar He Cutler Algorithm. With IntStream Multithreading in its main loop.
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
                    result[i * 3]     = interpolateRBHorizontal(samples, width, height, x, y);
                    result[i * 3 + 1] = samples[i];
                    result[i * 3 + 2] = interpolateRBVertical(samples, width, height, x, y);
                } else if (patternIdx == greenBlueRowIdx) {
                    result[i * 3]     = interpolateRBVertical(samples, width, height, x, y);
                    result[i * 3 + 1] = samples[i];
                    result[i * 3 + 2] = interpolateRBHorizontal(samples, width, height, x, y);
                } else if (patternIdx == redIdx) {
                    result[i * 3]     = samples[i];
                    result[i * 3 + 1] = interpolateGreen(samples, width, height, x, y);
                    result[i * 3 + 2] = interpolateRBDiagonal(samples, width, height, x, y);
                } else { // blue
                    result[i * 3]     = interpolateRBDiagonal(samples, width, height, x, y);
                    result[i * 3 + 1] = interpolateGreen(samples, width, height, x, y);
                    result[i * 3 + 2] = samples[i];
                }
        });
        return result;
    }
}

