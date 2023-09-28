package de.unituebingen.dng.processor.demosaicingprocessor;

import java.util.stream.IntStream;

import de.unituebingen.dng.processor.util.AccelerationStrategy;

/**
 * @author Florian Kellner, Andreas Reiter
 */
public class PatternedPixelGrouping {
    // Patterned Pixel Grouping (PPG)
    // adapted from 
    // https://github.com/Beep6581/RawTherapee/blob/3ad786745cf11fade334ff41aad0573dfcd7c04e/rtengine/dcraw.c
    // https://github.com/mashaka/Image-Recognition-and-Processing/blob/master/task1/Task_1.ipynb
    // https://www.informatik.hu-berlin.de/de/forschung/gebiete/viscom/teaching/media/cphoto10/cphoto10_03.pdf

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
     * Demosaic with Patterned Pixel Grouping Algorithm.
     * Adapted from:
     * https://github.com/Beep6581/RawTherapee/blob/3ad786745cf11fade334ff41aad0573dfcd7c04e/rtengine/dcraw.c
     * https://github.com/mashaka/Image-Recognition-and-Processing/blob/master/task1/Task_1.ipynb
     * https://www.informatik.hu-berlin.de/de/forschung/gebiete/viscom/teaching/media/cphoto10/cphoto10_03.pdf
     * 
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
        float[] greens = new float[samples.length];
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

                    float nGrad = 2 * Math.abs(center - north2) + Math.abs(south1 - north1);
                    float eGrad = 2 * Math.abs(center - east2) + Math.abs(west1 - east1);
                    float wGrad = 2 * Math.abs(center - west2) + Math.abs(east1 - west1);
                    float sGrad = 2 * Math.abs(center - south2) + Math.abs(north1 - south1);

                    float minGrad = nGrad;
                    int minDir = 0;     //0 = north, 1 = east, 2 = west, 3 = south

                    if (eGrad < minGrad) {
                        minGrad = eGrad;
                        minDir = 1;
                    }
                    if (wGrad < minGrad) {
                        minGrad = wGrad;
                        minDir = 2;
                    }
                    if (sGrad < minGrad) {
                        minGrad = sGrad;
                        minDir = 3;
                    }

                    if (minDir == 0) {
                        greens[i] = (south1 + center + 3 * north1 - north2) / 4.f;
                    } else if (minDir == 1) {
                        greens[i] = (west1 + center + 3 * east1 - east2) / 4.f;
                    } else if (minDir == 2) {
                        greens[i] = (east1 + center + 3 * west1 - west2) / 4.f;
                    } else {
                        greens[i] = (north1 + center + 3 * south1 - south2) / 4.f;
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
                    // guess it's copypasta
                    result[i * 3]     = hueTransit(
                        greens[Math.max(0, x - 1) + y * width],
                        samples[i],
                        greens[Math.min(width - 1, x + 1) + y * width],
                        samples[Math.max(0, x - 1) + y * width],
                        samples[Math.min(width - 1, x + 1) + y * width]);
                    result[i * 3 + 1] = greens[i];
                    result[i * 3 + 2] = hueTransit(
                        greens[x + Math.max(0, y - 1) * width], // was a typo in original, fixed
                        samples[i], 
                        greens[x + Math.min(height - 1, y + 1) * width], 
                        samples[x + Math.max(0, y - 1) * width],
                        samples[x + Math.min(height - 1, y + 1) * width]);
                } else if (patternIdx == greenBlueRowIdx) {
                    result[i * 3] = hueTransit(
                        greens[x + Math.max(0, y - 1) * width], // was a typo in original, fixed
                        samples[i], 
                        greens[x + Math.min(height - 1, y + 1) * width], 
                        samples[x + Math.max(0, y - 1) * width],
                        samples[x + Math.min(height - 1, y + 1) * width]);
                    result[i * 3 + 1] = greens[i];
                    result[i * 3 + 2] = hueTransit(
                        greens[Math.max(0, x - 1) + y * width],
                        samples[i],
                        greens[Math.min(width - 1, x + 1) + y * width],
                        samples[Math.max(0, x - 1) + y * width],
                        samples[Math.min(width - 1, x + 1) + y * width]);
                } else {
                    int center = samples[i];
                    float centerG = greens[i];

                    int nw2 = samples[Math.max(0, x - 2) + Math.max(0, y - 2) * width];
                    int nw1 = samples[Math.max(0, x - 1) + Math.max(0, y - 1) * width];
                    int se1 = samples[Math.min(width - 1, x + 1) + Math.min(height - 1, y + 1) * width];
                    int se2 = samples[Math.min(width - 1, x + 2) + Math.min(height - 1, y + 2) * width];

                    float nw1G = greens[Math.max(0, x - 1) + Math.max(0, y - 1) * width];
                    float se1G =  greens[Math.min(width - 1, x + 1) + Math.min(height - 1, y + 1) * width];

                    float nwGrad = 
                        Math.abs(nw1 - se1) + Math.abs(nw2 - center) + Math.abs(center - se2) 
                        + Math.abs(nw1G - centerG) + Math.abs(centerG - se1G);

                    int sw2 = samples[Math.max(0, x - 2) + Math.min(height - 1, y + 2) * width];
                    int sw1 = samples[Math.max(0, x - 1) + Math.min(height - 1, y + 1) * width];
                    int ne1 = samples[Math.min(width - 1, x + 1) + Math.max(0, y - 1) * width];
                    int ne2 = samples[Math.min(width - 1, x + 2) + Math.max(0, y - 2) * width];

                    float sw1G = greens[Math.max(0, x - 1) + Math.min(height - 1, y + 1) * width];
                    float ne1G =  greens[Math.min(width - 1, x + 1) + Math.max(0, y - 1) * width];
                    
                    float neGrad = 
                        Math.abs(ne1 - sw1) + Math.abs(ne2 - center) + Math.abs(center - sw2) 
                        + Math.abs(ne1G - centerG) + Math.abs(centerG - sw1G);

                    float interp;
                    if (nwGrad < neGrad) {
                        interp = hueTransit(nw1G, centerG, se1G, nw1, se1);
                    } else {
                        interp = hueTransit(ne1G, centerG, sw1G, ne1, sw1);
                    }
                    result[i * 3]     = patternIdx == redIdx ? center : interp;
                    result[i * 3 + 1] = centerG;
                    result[i * 3 + 2] = patternIdx == blueIdx ? center : interp;
                }
            }
        }
        return result;
    }

    public static float hueTransit(float l1, float l2, float l3, float v1, float v3) {
        if ((l1 < l2 && l2 < l3) || (l1 > l2 && l2 > l3)) {
            return v1 + ((v3 - v1) * (l2 - l1)) / (l3 - l1);
        } else {
            return (v1 + v3) / 2 + (2 * l2 - l1 - l3) / 4;
        }
    }

    /**
     * Demosaic with Patterned Pixel Grouping Algorithm.
     * Adapted from:
     * https://github.com/Beep6581/RawTherapee/blob/3ad786745cf11fade334ff41aad0573dfcd7c04e/rtengine/dcraw.c
     * https://github.com/mashaka/Image-Recognition-and-Processing/blob/master/task1/Task_1.ipynb
     * https://www.informatik.hu-berlin.de/de/forschung/gebiete/viscom/teaching/media/cphoto10/cphoto10_03.pdf
     * This version uses IntStream Multithreading in its loops.
     * 
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
        float[] greens = new float[samples.length];
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

                    float nGrad = 2 * Math.abs(center - north2) + Math.abs(south1 - north1);
                    float eGrad = 2 * Math.abs(center - east2) + Math.abs(west1 - east1);
                    float wGrad = 2 * Math.abs(center - west2) + Math.abs(east1 - west1);
                    float sGrad = 2 * Math.abs(center - south2) + Math.abs(north1 - south1);

                    float minGrad = nGrad;
                    int minDir = 0;     //0 = north, 1 = east, 2 = west, 3 = south

                    if (eGrad < minGrad) {
                        minGrad = eGrad;
                        minDir = 1;
                    }
                    if (wGrad < minGrad) {
                        minGrad = wGrad;
                        minDir = 2;
                    }
                    if (sGrad < minGrad) {
                        minGrad = sGrad;
                        minDir = 3;
                    }

                    if (minDir == 0) {
                        greens[i] = (south1 + center + 3 * north1 - north2) / 4.f;
                    } else if (minDir == 1) {
                        greens[i] = (west1 + center + 3 * east1 - east2) / 4.f;
                    } else if (minDir == 2) {
                        greens[i] = (east1 + center + 3 * west1 - west2) / 4.f;
                    } else {
                        greens[i] = (north1 + center + 3 * south1 - south2) / 4.f;
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
                    // guess it's copypasta
                    result[i * 3]     = hueTransit(
                        greens[Math.max(0, x - 1) + y * width],
                        samples[i],
                        greens[Math.min(width - 1, x + 1) + y * width],
                        samples[Math.max(0, x - 1) + y * width],
                        samples[Math.min(width - 1, x + 1) + y * width]);
                    result[i * 3 + 1] = greens[i];
                    result[i * 3 + 2] = hueTransit(
                        greens[x + Math.max(0, y - 1) * width], // was a typo in original, fixed
                        samples[i], 
                        greens[x + Math.min(height - 1, y + 1) * width], 
                        samples[x + Math.max(0, y - 1) * width],
                        samples[x + Math.min(height - 1, y + 1) * width]);
                } else if (patternIdx == greenBlueRowIdx) {
                    result[i * 3] = hueTransit(
                        greens[x + Math.max(0, y - 1) * width], // was a typo in original, fixed
                        samples[i], 
                        greens[x + Math.min(height - 1, y + 1) * width], 
                        samples[x + Math.max(0, y - 1) * width],
                        samples[x + Math.min(height - 1, y + 1) * width]);
                    result[i * 3 + 1] = greens[i];
                    result[i * 3 + 2] = hueTransit(
                        greens[Math.max(0, x - 1) + y * width],
                        samples[i],
                        greens[Math.min(width - 1, x + 1) + y * width],
                        samples[Math.max(0, x - 1) + y * width],
                        samples[Math.min(width - 1, x + 1) + y * width]);
                } else {
                    int center = samples[i];
                    float centerG = greens[i];

                    int nw2 = samples[Math.max(0, x - 2) + Math.max(0, y - 2) * width];
                    int nw1 = samples[Math.max(0, x - 1) + Math.max(0, y - 1) * width];
                    int se1 = samples[Math.min(width - 1, x + 1) + Math.min(height - 1, y + 1) * width];
                    int se2 = samples[Math.min(width - 1, x + 2) + Math.min(height - 1, y + 2) * width];

                    float nw1G = greens[Math.max(0, x - 1) + Math.max(0, y - 1) * width];
                    float se1G =  greens[Math.min(width - 1, x + 1) + Math.min(height - 1, y + 1) * width];

                    float nwGrad = 
                        Math.abs(nw1 - se1) + Math.abs(nw2 - center) + Math.abs(center - se2) 
                        + Math.abs(nw1G - centerG) + Math.abs(centerG - se1G);

                    int sw2 = samples[Math.max(0, x - 2) + Math.min(height - 1, y + 2) * width];
                    int sw1 = samples[Math.max(0, x - 1) + Math.min(height - 1, y + 1) * width];
                    int ne1 = samples[Math.min(width - 1, x + 1) + Math.max(0, y - 1) * width];
                    int ne2 = samples[Math.min(width - 1, x + 2) + Math.max(0, y - 2) * width];

                    float sw1G = greens[Math.max(0, x - 1) + Math.min(height - 1, y + 1) * width];
                    float ne1G =  greens[Math.min(width - 1, x + 1) + Math.max(0, y - 1) * width];
                    
                    float neGrad = 
                        Math.abs(ne1 - sw1) + Math.abs(ne2 - center) + Math.abs(center - sw2) 
                        + Math.abs(ne1G - centerG) + Math.abs(centerG - sw1G);

                    float interp;
                    if (nwGrad < neGrad) {
                        interp = hueTransit(nw1G, centerG, se1G, nw1, se1);
                    } else {
                        interp = hueTransit(ne1G, centerG, sw1G, ne1, sw1);
                    }
                    result[i * 3]     = patternIdx == redIdx ? center : interp;
                    result[i * 3 + 1] = centerG;
                    result[i * 3 + 2] = patternIdx == blueIdx ? center : interp;
                }
            
        });
        return result;
    }
}

