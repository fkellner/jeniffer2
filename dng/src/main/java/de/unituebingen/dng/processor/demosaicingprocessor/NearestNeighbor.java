package de.unituebingen.dng.processor.demosaicingprocessor;

import java.util.stream.IntStream;

import de.unituebingen.dng.processor.util.AccelerationStrategy;

/**
 * @author Florian Kellner, Michael Kessler
 */
public class NearestNeighbor {
    
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
            case CPU_MT_TILING_MT:
            default:                
                return 2048 - 2 * getOverlap();         
        }
    }

    /**
     * Demosaic with bilinear mean, i.e.
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
                    result[i * 3] = samples[Math.min(width - 1, x + 1) + y * width];
                    result[i * 3 + 1] = samples[i];
                    result[i * 3 + 2] = samples[x + Math.min(height - 1, y + 1) * width];
                } else if (patternIdx == greenBlueRowIdx) {
                    result[i * 3 + 2] = samples[Math.min(width - 1, x + 1) + y * width];
                    result[i * 3 + 1] = samples[i];
                    result[i * 3] = samples[x + Math.min(height - 1, y + 1) * width];
                } else if (patternIdx == redIdx) {
                    result[i * 3] = samples[i];
                    result[i * 3 + 1] = samples[Math.min(width - 1, x + 1) + y * width];
                    result[i * 3 + 2] = samples[Math.min(width - 1, x + 1) + Math.min(height - 1, y + 1) * width];
                } else {
                    result[i * 3 + 2] = samples[i];
                    result[i * 3 + 1] = samples[Math.min(width - 1, x + 1) + y * width];
                    result[i * 3] = samples[Math.min(width - 1, x + 1) + Math.min(height - 1, y + 1) * width];
                }
            }
        }
        return result;
    }

    /**
     * Demosaic with bilinear mean, i.e.
     * axial or diagonal average where available, else average the two neighbouring pixels.
     * Version with IntStream Multithreading in the main loop.
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
                    result[i * 3] = samples[Math.min(width - 1, x + 1) + y * width];
                    result[i * 3 + 1] = samples[i];
                    result[i * 3 + 2] = samples[x + Math.min(height - 1, y + 1) * width];
                } else if (patternIdx == greenBlueRowIdx) {
                    result[i * 3 + 2] = samples[Math.min(width - 1, x + 1) + y * width];
                    result[i * 3 + 1] = samples[i];
                    result[i * 3] = samples[x + Math.min(height - 1, y + 1) * width];
                } else if (patternIdx == redIdx) {
                    result[i * 3] = samples[i];
                    result[i * 3 + 1] = samples[Math.min(width - 1, x + 1) + y * width];
                    result[i * 3 + 2] = samples[Math.min(width - 1, x + 1) + Math.min(height - 1, y + 1) * width];
                } else {
                    result[i * 3 + 2] = samples[i];
                    result[i * 3 + 1] = samples[Math.min(width - 1, x + 1) + y * width];
                    result[i * 3] = samples[Math.min(width - 1, x + 1) + Math.min(height - 1, y + 1) * width];
                }
            
        });
        return result;
    }
}

