package de.unituebingen.dng.processor.demosaicingprocessor;

import java.util.stream.IntStream;

import de.unituebingen.dng.processor.util.AccelerationStrategy;

/**
 * @author Florian Kellner, Andreas Reiter
 */
public class HamiltonAdams {
    //Hamilton-Adams Demosaicing
    //adaptiert von:
    //https://www.ipol.im/pub/art/2011/g_gapd/

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
            case AUTO_BIG:
            case CPU_TILING_MT: 
                return 128 - 2 * getOverlap();
            case CPU_MT_TILING:
            case CPU_MT_TILING_MT:
            default:
                return 2048 - 2 * getOverlap();         
        }
    }

    /**
     * Demosaic with Hamilton Adams Algorithm.
     * Adapted from: https://www.ipol.im/pub/art/2011/g_gapd/
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
        /* Calculate green values first */
        int[] greens = new int[samples.length];
        for(int y = 0, i = 0; y < height; y++) {
            for(int x = 0; x < width; x++, i++) {
                int patternIdx = (x % 2) + 2 * (y % 2);
                if(patternIdx == greenRedRowIdx || patternIdx == greenBlueRowIdx) {
                    greens[i] = samples[i];
                } else {
                    int center = samples[i];
                    
                    int north1 = samples[x + Math.max(0, y - 1) * width];
                    int north2 = samples[x + Math.max(0, y - 2) * width];
                    int south1 = samples[x + Math.min(height - 1, y + 1) * width];
                    int south2 = samples[x + Math.min(height - 1, y + 2) * width];

                    int west1 = samples[Math.max(0, x - 1) + y * width];
                    int west2 = samples[Math.max(0, x - 2) + y * width];
                    int east1 = samples[Math.min(width - 1, x + 1) + y * width];
                    int east2 = samples[Math.min(width - 1, x + 2) + y * width];

                    float VariationH = Math.abs(west2 - 2 * center + east2) + Math.abs(west1 - east1);
                    float VariationV = Math.abs(north2 - 2 * center + south2) + Math.abs(north1 - south1);

                    float threshold = 2 / 255.0f;
                    if (Math.abs(VariationH - VariationV) < threshold) {
                        greens[i] = (int)((4 * center + 2 * (north1 + south1 + west1 + east1) - north2 - south2 - west2 - east2) / 8.f);
                    } else if (VariationH < VariationV) {
                        greens[i] = (int)((2 * (west1 + center + east1) - west2 - east2) / 4.f);
                    } else {
                        greens[i] = (int)((2 * (north1 + center + south1) - north2 - south2) / 4.f);
                    }
                }
            }
        }

        /* Calculate missing red and blue values */
        float[] result = new float[samples.length * 3];
        for(int y = 0, i = 0; y < height; y++) {
            for(int x = 0; x < width; x++, i++) {
                int patternIdx = (x % 2) + 2 * (y % 2);
                if(patternIdx == greenRedRowIdx) {
                    result[i * 3]     = samples[i] + rowGreenDiff(samples, greens, width, height, x, y);
                    result[i * 3 + 1] = greens[i];
                    result[i * 3 + 2] = samples[i] + columnGreenDiff(samples, greens, width, height, x, y);
                } else if (patternIdx == greenBlueRowIdx) {
                    result[i * 3]     = samples[i] + columnGreenDiff(samples, greens, width, height, x, y);
                    result[i * 3 + 1] = greens[i];
                    result[i * 3 + 2] = samples[i] + rowGreenDiff(samples, greens, width, height, x, y);
                } else if (patternIdx == redIdx) {
                    result[i * 3]     = samples[i];
                    result[i * 3 + 1] = greens[i];
                    result[i * 3 + 2] = greens[i] + diagonalGreenDiff(samples, greens, width, height, x, y);
                } else {
                    result[i * 3]     = greens[i] + diagonalGreenDiff(samples, greens, width, height, x, y);
                    result[i * 3 + 1] = greens[i];
                    result[i * 3 + 2] = samples[i];
                }
            }
        }
        return result;
    }

    /**
     * calculate the diagonal average of the difference between green and red/blue at a pixel position
     * <pre>.
     * 1 0 1
     * 0 0 0 * 0.25
     * 1 0 1
     * </pre>
     * Edge handling same as getUndemosaicedSample (repeat)
     * @param sample image in row-major order
     * @param greens calculated green channel in row-major order
     * @param width
     * @param height
     * @param x
     * @param y
     * @return
     */
    private static float diagonalGreenDiff(int[] samples, int[] greens, int width, int height, int x, int y) {
        int west = Math.max(0, x - 1);
        int east = Math.min(width - 1, x + 1);
        int north = Math.max(0, y - 1);
        int south = Math.min(height - 1, y + 1);
        return (
            (samples[west + north * width] - greens[west + north * width]) + 
            (samples[west + south * width] - greens[west + south * width]) +
            (samples[east + north * width] - greens[east + north * width]) + 
            (samples[east + south * width] - greens[east + south * width])
        ) / 4.f;
    }

    /**
     * calculate the row average of the difference between green and red/blue at a pixel position
     * <pre>.
     * 1 0 1 * 0.5
     * </pre>
     * Edge handling same as getUndemosaicedSample (repeat)
     * @param sample image in row-major order
     * @param greens calculated green channel in row-major order
     * @param width
     * @param height
     * @param x
     * @param y
     * @return
     */
    private static float rowGreenDiff(int[] samples, int[] greens, int width, int height, int x, int y) {
        int west = Math.max(0, x - 1);
        int east = Math.min(width - 1, x + 1);
        return (
            (samples[west + y * width] - greens[west + y * width]) + 
            (samples[east + y * width] - greens[east + y * width])
        ) / 2.f;
    }

    /**
     * calculate the column average of the difference between green and red/blue at a pixel position
     * <pre>.
     *   1
     *   0   * 0.5
     *   1
     * </pre>
     * Edge handling same as getUndemosaicedSample (repeat)
     * @param sample image in row-major order
     * @param greens calculated green channel in row-major order
     * @param width
     * @param height
     * @param x
     * @param y
     * @return
     */
    private static float columnGreenDiff(int[] samples, int[] greens, int width, int height, int x, int y) {
        int north = Math.max(0, y - 1);
        int south = Math.min(height - 1, y + 1);
        return (
            (samples[x + north * width] - greens[x + north * width]) + 
            (samples[x + south * width] - greens[x + south * width])
        ) / 2.f;
    }

    /**
     * Demosaic with Hamilton Adams Algorithm. Uses IntStream Multithreading in its loops.
     * Adapted from: https://www.ipol.im/pub/art/2011/g_gapd/
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
        /* Calculate green values first */
        int[] greens = new int[samples.length];
        IntStream.range(0, samples.length).parallel().forEach(i -> {
            int x = i % width;
            int y = i / width;
                int patternIdx = (x % 2) + 2 * (y % 2);
                if(patternIdx == greenRedRowIdx || patternIdx == greenBlueRowIdx) {
                    greens[i] = samples[i];
                } else {
                    int center = samples[i];
                    
                    int north1 = samples[x + Math.max(0, y - 1) * width];
                    int north2 = samples[x + Math.max(0, y - 2) * width];
                    int south1 = samples[x + Math.min(height - 1, y + 1) * width];
                    int south2 = samples[x + Math.min(height - 1, y + 2) * width];

                    int west1 = samples[Math.max(0, x - 1) + y * width];
                    int west2 = samples[Math.max(0, x - 2) + y * width];
                    int east1 = samples[Math.min(width - 1, x + 1) + y * width];
                    int east2 = samples[Math.min(width - 1, x + 2) + y * width];

                    float VariationH = Math.abs(west2 - 2 * center + east2) + Math.abs(west1 - east1);
                    float VariationV = Math.abs(north2 - 2 * center + south2) + Math.abs(north1 - south1);

                    float threshold = 2 / 255.0f;
                    if (Math.abs(VariationH - VariationV) < threshold) {
                        greens[i] = (int)((4 * center + 2 * (north1 + south1 + west1 + east1) - north2 - south2 - west2 - east2) / 8.f);
                    } else if (VariationH < VariationV) {
                        greens[i] = (int)((2 * (west1 + center + east1) - west2 - east2) / 4.f);
                    } else {
                        greens[i] = (int)((2 * (north1 + center + south1) - north2 - south2) / 4.f);
                    }
                }
            
        });

        /* Calculate missing red and blue values */
        float[] result = new float[samples.length * 3];
        IntStream.range(0, samples.length).parallel().forEach(i -> {
            int x = i % width;
            int y = i / width;
                int patternIdx = (x % 2) + 2 * (y % 2);
                if(patternIdx == greenRedRowIdx) {
                    result[i * 3]     = samples[i] + rowGreenDiff(samples, greens, width, height, x, y);
                    result[i * 3 + 1] = greens[i];
                    result[i * 3 + 2] = samples[i] + columnGreenDiff(samples, greens, width, height, x, y);
                } else if (patternIdx == greenBlueRowIdx) {
                    result[i * 3]     = samples[i] + columnGreenDiff(samples, greens, width, height, x, y);
                    result[i * 3 + 1] = greens[i];
                    result[i * 3 + 2] = samples[i] + rowGreenDiff(samples, greens, width, height, x, y);
                } else if (patternIdx == redIdx) {
                    result[i * 3]     = samples[i];
                    result[i * 3 + 1] = greens[i];
                    result[i * 3 + 2] = greens[i] + diagonalGreenDiff(samples, greens, width, height, x, y);
                } else {
                    result[i * 3]     = greens[i] + diagonalGreenDiff(samples, greens, width, height, x, y);
                    result[i * 3 + 1] = greens[i];
                    result[i * 3 + 2] = samples[i];
                }
            
        });
        return result;
    }
}

