package de.unituebingen.dng.processor.demosaicingprocessor;

import java.util.stream.IntStream;

import de.unituebingen.dng.processor.util.AccelerationStrategy;
import uk.ac.manchester.tornado.api.annotations.Parallel;

public class RatioCorrectedDemosaicing {
    // Ratio Corrected Demosaicing, adapted from:
    // https://github.com/LuisSR/RCD-Demosaicing/blob/master/rcd_demosaicing.c

    /**
     * How far Tiles need to overlap in order for the algorithm to function properly
     * @return the width of the margin in pixels
     */
    public static int getOverlap() {
        return 10;
    }

    /**
     * How big to make tiles. Can be tuned to make all pixels, possibly of all threads fit into e.g. L3 or L2
     * cache. It may also be beneficial if tileSize + 2*12 is a power of two.
     * @return
     */
    public static int getTileSize(AccelerationStrategy accelerationStrategy) {
        switch(accelerationStrategy) {
            case AUTO_SMALL:
                return 1024 - 2 * getOverlap();
            case CPU_TILING:
                return 256 - 2 * getOverlap();    
            case CPU_TILING_MT: 
                return 128 - 2 * getOverlap();
            case CPU_MT_TILING:
            case AUTO_BIG:
            case CPU_MT_TILING_MT:
            default:
                return 2048 - 2 * getOverlap();         
        }
    }

    /**
     * Perform Ratio Corrected Demosaicing.<br>
     * Dataflow and space in multiples of N = undemosaiced pixels:
     * <pre>
     *  1   samples
     *      |
     *  2   O-----> xyGradient (overlap: 4)
     *      |       |
     *  3   O------------------> lowPassAtRB (overlap: 2)
     *      |       |            |
     *  4   O-------O------------O-----------> greens (overlap: 2 on low-pass, 2 on xyGradient, 4 on samples -> 6)
     *      |       |                          |
     *  4   O-----------------------------------------> pqGradient (overlap: 4)
     *      |       |                          |        |
     *  6   O----------------------------------O--------O----------> greenEverywhereRBinPlace (overlap: 2 pq, 2 greens, 3 samples -> 8)
     *              |                                                |
     *  4           O----------------------------------------------> endresult (overlap: xy: 1, endres: 3 -> 12)
     * </pre>
     * @param samples image in row-major order
     * @param width
     * @param height
     * @param redIdx
     * @param greenRedRowIdx
     * @param greenBlueRowIdx
     * @param blueIdx
     * @return demosaiced image in row-major order, rgb values interleaved ([r g b r g b ...])
     */
    public static float[] process(
        int[] samples, int width, int height, int redIdx, int green1Idx, int green2Idx, int blueIdx) {

        float[] xyGradient = new float[samples.length];
        
        calcXYGradient(samples, xyGradient, width, height);
        // if (stopAt == RCDStep.XY_GRADIENT) {
        //     abort(xyGradient, demosaicedSamples);
        //     return;
        // }

        // calculate low pass at RB
        float[] lowPassAtRB = new float[samples.length];
        calcLowpassAtRB(samples, lowPassAtRB, width, height, green1Idx, green2Idx);
        // if (stopAt == RCDStep.LOW_PASS) {
        //     abort(lowPassAtRB, demosaicedSamples);
        //     return;
        // }

        // calculate greens
        float[] greens = new float[samples.length];
        calcGreens(samples, xyGradient, lowPassAtRB, greens, width, height, green1Idx, green2Idx);
        // if (stopAt == RCDStep.GREENS) {
        //     abort(greens, demosaicedSamples);
        //     return;
        // }
        // ## pq-Gradient
        // needs only samples
        float[] pqAtRB = new float[samples.length]; //lowPassAtRB; // lowPassAtRB no longer needed
        calcPQGradient(samples, pqAtRB, width, height, green1Idx, green2Idx);
        // if (stopAt == RCDStep.PQ_GRADIENT) {
        //     abort(pqAtRB, demosaicedSamples);
        //     return;
        // }
        // ## calculate red at blue pixels and vice versa
        // needs pqAtRB, samples and greens
        float[] endresult = new float[samples.length * 3];
        calcRBAtG(samples, pqAtRB, greens, endresult, width, height, green1Idx, green2Idx, redIdx, blueIdx);
        // if (stopAt == RCDStep.RB_AT_BR) {
        //     abort(endresult, demosaicedSamples);
        //     return;
        // }

        //## calculate R and B at G locations
        calcEndresult(xyGradient, endresult, width, height, green1Idx, green2Idx);

        return endresult;
    }

    public static void intifySamples(short[] samplesIn, int[] samples) {
        for(@Parallel int i = 0; i < samples.length; i++) {
            samples[i] = samplesIn[i] & 0xFFFF; // / 65535.0f;
        }
    }
    public static void calcXYGradient(int[] samples, float[] xyGradient, int width, int height) {
        float eps = 0.1f / (255 * 255);
        float epssq = eps; // in order to mirror exactly the existing implementation
        for(@Parallel int x = 0; x < width; x++) {
            for(@Parallel int y = 0; y < height; y++) {
                float vM4 = samples[x + Math.max(0, y - 4) * width];
                float vM3 = samples[x + Math.max(0, y - 3) * width];
                float vM2 = samples[x + Math.max(0, y - 2) * width];
                float vM1 = samples[x + Math.max(0, y - 1) * width];
                float cur = samples[x + y * width];
                float vP1 = samples[x +Math.min(height - 1, y + 1) * width];
                float vP2 = samples[x +Math.min(height - 1, y + 2) * width];
                float vP3 = samples[x +Math.min(height - 1, y + 3) * width];
                float vP4 = samples[x +Math.min(height - 1, y + 4) * width];

                // @formatter:off
                float deltaY = Math.max(
                         1.f * vM4 * vM4 +
                        -6.f * vM4 * vM3 +  10.f * vM3 * vM3 +
                        -2.f * vM4 * vM2 +                      11.f * vM2 * vM2 +
                        12.f * vM4 * vM1 + -38.f * vM3 * vM1 + -12.f * vM2 * vM1 +  46.f * vM1 * vM1 +
                        -2.f * vM4 * cur +  18.f * vM3 * cur + -36.f * vM2 * cur + -18.f * vM1 * cur +  38.f * cur * cur +
                        -6.f * vM4 * vP1 +  16.f * vM3 * vP1 +  24.f * vM2 * vP1 + -70.f * vM1 * vP1 + -18.f * cur * vP1 +  46.f * vP1 * vP1 +
                         2.f * vM4 * vP2 + -12.f * vM3 * vP2 +  14.f * vM2 * vP2 +  24.f * vM1 * vP2 + -36.f * cur * vP2 + -12.f * vP1 * vP2 + 11.f * vP2 * vP2 +
                                             2.f * vM3 * vP3 + -12.f * vM2 * vP3 +  16.f * vM1 * vP3 +  18.f * cur * vP3 + -38.f * vP1 * vP3 +                    10.f * vP3 * vP3 +
                                                                 2.f * vM2 * vP4 +  -6.f * vM1 * vP4 +  -2.f * cur * vP4 +  12.f * vP1 * vP4 + -2.f * vP2 * vP4 + -6.f * vP3 * vP4 + 1.f * vP4 * vP4,
                        epssq);
                // @formatter:on

                float hM4 = samples[Math.max(0, x - 4) + y * width];
                float hM3 = samples[Math.max(0, x - 3) + y * width];
                float hM2 = samples[Math.max(0, x - 2) + y * width];
                float hM1 = samples[Math.max(0, x - 1) + y * width];

                float hP1 = samples[Math.min(width - 1, x + 1) + y * width];
                float hP2 = samples[Math.min(width - 1, x + 2) + y * width];
                float hP3 = samples[Math.min(width - 1, x + 3) + y * width];
                float hP4 = samples[Math.min(width - 1, x + 4) + y * width];

                // @formatter:off
                float deltaX = Math.max(
                         1.f * hM4 * hM4 +
                        -6.f * hM4 * hM3 +  10.f * hM3 * hM3 +
                        -2.f * hM4 * hM2 +                      11.f * hM2 * hM2 +
                        12.f * hM4 * hM1 + -38.f * hM3 * hM1 + -12.f * hM2 * hM1 +  46.f * hM1 * hM1 +
                        -2.f * hM4 * cur +  18.f * hM3 * cur + -36.f * hM2 * cur + -18.f * hM1 * cur +  38.f * cur * cur +
                        -6.f * hM4 * hP1 +  16.f * hM3 * hP1 +  24.f * hM2 * hP1 + -70.f * hM1 * hP1 + -18.f * cur * hP1 +  46.f * hP1 * hP1 +
                         2.f * hM4 * hP2 + -12.f * hM3 * hP2 +  14.f * hM2 * hP2 +  24.f * hM1 * hP2 + -36.f * cur * hP2 + -12.f * hP1 * hP2 + 11.f * hP2 * hP2 +
                                             2.f * hM3 * hP3 + -12.f * hM2 * hP3 +  16.f * hM1 * hP3 +  18.f * cur * hP3 + -38.f * hP1 * hP3 +                    10.f * hP3 * hP3 +
                                                                 2.f * hM2 * hP4 +  -6.f * hM1 * hP4 +  -2.f * cur * hP4 +  12.f * hP1 * hP4 + -2.f * hP2 * hP4 + -6.f * hP3 * hP4 + 1.f * hP4 * hP4,
                        epssq);
                // @formatter:on

                xyGradient[x + y * width] = // Math.min(1.0f, Math.max(0.0f,
                    deltaY / (deltaY + deltaX) ; //));
            }
        }
    }

    public static void calcLowpassAtRB(int[] samples, float[] lowPassAtRB, int width, int height, int green1Idx, int green2Idx) {
        for(@Parallel int x = 0; x < width; x++) {
            for(@Parallel int y = 0; y < height; y++) {
                int patternIdx = (x % 2) + 2 * (y % 2);
                // because of multiple conditionals code gen bug
                // see https://github.com/beehive-lab/TornadoVM/issues/247
                if (patternIdx == green1Idx) {
                    lowPassAtRB[x + y * width] = 0.0f;
                } else if (patternIdx == green2Idx) {
                    lowPassAtRB[x + y * width] = 0.0f;
                } else {
                    // 3x3 low-pass filter

                    float middle = samples[x + y * width];

                    float top =         samples[x + Math.max(0, y - 1) * width];
                    float bottom =      samples[x + Math.min(height - 1, y + 1) * width];
                    float left =        samples[Math.max(0, x - 1) + y * width];
                    float right =       samples[Math.min(width - 1, x + 1) + y * width];

                    float topLeft =     samples[Math.max(0, x - 1) + Math.max(0, y - 1) * width];
                    float topRight =    samples[Math.min(width - 1, x + 1) + Math.max(0, y - 1) * width];
                    float bottomLeft =  samples[Math.max(0, x - 1) + Math.min(height - 1, y + 1) * width];
                    float bottomRight = samples[Math.min(width - 1, x + 1) + Math.min(height - 1, y + 1) * width];

                    lowPassAtRB[x + y * width] = 0.25f * middle +
                            0.125f * (top + bottom + left + right) +
                            0.0625f * (topLeft + topRight + bottomLeft + bottomRight);
                }
            }
        }
    }

    public static void calcGreens(int[] samples, float[] xyGradient, float[] lowPassAtRB, float[] greens, int width, int height, int green1Idx, int green2Idx) {
        float eps = 0.1f / (255 * 255);
        for(@Parallel int x = 0; x < width; x++) {
            for(@Parallel int y = 0; y < height; y++) {
                int patternIdx = (x % 2) + 2 * (y % 2);
                // because of multiple conditionals code gen bug
                // see https://github.com/beehive-lab/TornadoVM/issues/247
                if (patternIdx == green1Idx) {
                    // we already have green values
                    greens[x + y * width] = samples[x + y * width];
                } else if (patternIdx == green2Idx) {
                    // we already have green values
                    greens[x + y * width] = samples[x + y * width];
                } else {
                    // ## greens at red and blue pixels
                    float centerGradient = xyGradient[x + y * width];
                    // X-shaped to get gradient for greens in neighbourhood
                    float neighbourhoodGradient = 0.25f * (xyGradient[Math.max(0, x - 1) + Math.max(0, y - 1) * width] +
                            xyGradient[Math.min(width - 1, x + 1) + Math.max(0, y - 1) * width] +
                            xyGradient[Math.max(0, x - 1) + Math.min(height - 1, y + 1) * width] +
                            xyGradient[Math.min(width - 1, x + 1) + Math.min(height - 1, y + 1) * width]);
                    // take the bigger one
                    float vhDisc = Math.abs(centerGradient - 0.5f) < Math.abs(neighbourhoodGradient - 0.5f)
                            ? neighbourhoodGradient
                            : centerGradient;

                    // cardinal gradients
                    float vM4 = samples[x + Math.max(0, y - 4) * width];
                    float vM3 = samples[x + Math.max(0, y - 3) * width];
                    float vM2 = samples[x + Math.max(0, y - 2) * width];
                    float vM1 = samples[x + Math.max(0, y - 1) * width];
                    float cur = samples[x + y * width];
                    float vP1 = samples[x + Math.min(height - 1, y + 1) * width];
                    float vP2 = samples[x + Math.min(height - 1, y + 2) * width];
                    float vP3 = samples[x + Math.min(height - 1, y + 3) * width];
                    float vP4 = samples[x + Math.min(height - 1, y + 4) * width];

                    float hM4 = samples[Math.max(0, x - 4) + y * width];
                    float hM3 = samples[Math.max(0, x - 3) + y * width];
                    float hM2 = samples[Math.max(0, x - 2) + y * width];
                    float hM1 = samples[Math.max(0, x - 1) + y * width];

                    float hP1 = samples[Math.min(width - 1, x + 1) + y * width];
                    float hP2 = samples[Math.min(width - 1, x + 2) + y * width];
                    float hP3 = samples[Math.min(width - 1, x + 3) + y * width];
                    float hP4 = samples[Math.min(width - 1, x + 4) + y * width];

                    float nGrad = eps +
                            Math.abs(vM1 - vP1) +
                            Math.abs(cur - vM2) +
                            Math.abs(vM1 - vM3) +
                            Math.abs(vM2 - vM4);
                    float sGrad = eps +
                            Math.abs(vP1 - vM1) +
                            Math.abs(cur - vP2) +
                            Math.abs(vP1 - vP3) +
                            Math.abs(vP2 - vP4);
                    float wGrad = eps +
                            Math.abs(hM1 - hP1) +
                            Math.abs(cur - hM2) +
                            Math.abs(hM1 - hM3) +
                            Math.abs(hM2 - hM4);
                    float eGrad = eps +
                            Math.abs(hP1 - hM1) +
                            Math.abs(cur - hP2) +
                            Math.abs(hP1 - hP3) +
                            Math.abs(hP2 - hP4);

                    float lpfCur = lowPassAtRB[x + y * width];
                    float lpfN = lowPassAtRB[x + Math.max(0, y - 2) * width];
                    float lpfS = lowPassAtRB[x + Math.min(height - 1, y + 2) * width];
                    float lpfW = lowPassAtRB[Math.max(0, x - 2) + y * width];
                    float lpfE = lowPassAtRB[Math.min(width - 1, x + 2) + y * width];

                    // cardinal pixel estimations
                    float nEst = vM1 * (1.0f + (lpfCur - lpfN) / (eps + lpfCur + lpfN));
                    float sEst = vP1 * (1.0f + (lpfCur - lpfS) / (eps + lpfCur + lpfS));
                    float wEst = hM1 * (1.0f + (lpfCur - lpfW) / (eps + lpfCur + lpfW));
                    float eEst = hP1 * (1.0f + (lpfCur - lpfE) / (eps + lpfCur + lpfE));

                    // vertical and horizontal estimations
                    float vEst = (sGrad * nEst + nGrad * sEst) / (nGrad + sGrad);
                    float hEst = (wGrad * eEst + eGrad * wEst) / (eGrad + wGrad);

                    // interpolation
                    greens[x + y * width] = 
                        //min(1.0f, Math.max(0.0f, 
                        vhDisc * hEst + (1.0f - vhDisc) * vEst; //));
                }
            }
        }
    }

    public static void calcPQGradient(int[] samples, float[] pqAtRB, int width, int height, int green1Idx, int green2Idx) {
        float eps = 0.1f / (255 * 255);
        float epssq = eps; // in order to mirror exactly the existing implementation
        for(@Parallel int x = 0; x < width; x++) {
            for(@Parallel int y = 0; y < height; y++) {
                int patternIdx = (x % 2) + 2 * (y % 2);
                // because of multiple conditionals code gen bug
                // see https://github.com/beehive-lab/TornadoVM/issues/247
                if (patternIdx == green1Idx) {
                    pqAtRB[x + y * width] = 0.0f;
                } else if (patternIdx == green2Idx) {
                    pqAtRB[x + y * width] = 0.0f;
                } else {
                    // pq-gradient

                    float vM4 = samples[Math.max(0, x - 4) + Math.max(0, y - 4) * width];
                    float vM3 = samples[Math.max(0, x - 3) + Math.max(0, y - 3) * width];
                    float vM2 = samples[Math.max(0, x - 2) + Math.max(0, y - 2) * width];
                    float vM1 = samples[Math.max(0, x - 1) + Math.max(0, y - 1) * width];
                    float cur = samples[x + y * width];
                    float vP1 = samples[Math.min(width - 1, x + 1) + Math.min(height - 1, y + 1) * width];
                    float vP2 = samples[Math.min(width - 1, x + 2) + Math.min(height - 1, y + 2) * width];
                    float vP3 = samples[Math.min(width - 1, x + 3) + Math.min(height - 1, y + 3) * width];
                    float vP4 = samples[Math.min(width - 1, x + 4) + Math.min(height - 1, y + 4) * width];

                    // @formatter:off
                    float pStat = Math.max(
                         1.f * vM4 * vM4 +
                        -6.f * vM4 * vM3 +  10.f * vM3 * vM3 +
                        -2.f * vM4 * vM2 +                      11.f * vM2 * vM2 +
                        12.f * vM4 * vM1 + -38.f * vM3 * vM1 + -12.f * vM2 * vM1 +  46.f * vM1 * vM1 +
                        -2.f * vM4 * cur +  18.f * vM3 * cur + -36.f * vM2 * cur + -18.f * vM1 * cur +  38.f * cur * cur +
                        -6.f * vM4 * vP1 +  16.f * vM3 * vP1 +  24.f * vM2 * vP1 + -70.f * vM1 * vP1 + -18.f * cur * vP1 +  46.f * vP1 * vP1 +
                         2.f * vM4 * vP2 + -12.f * vM3 * vP2 +  14.f * vM2 * vP2 +  24.f * vM1 * vP2 + -36.f * cur * vP2 + -12.f * vP1 * vP2 + 11.f * vP2 * vP2 +
                                             2.f * vM3 * vP3 + -12.f * vM2 * vP3 +  16.f * vM1 * vP3 +  18.f * cur * vP3 + -38.f * vP1 * vP3 +                    10.f * vP3 * vP3 +
                                                                 2.f * vM2 * vP4 +  -6.f * vM1 * vP4 +  -2.f * cur * vP4 +  12.f * vP1 * vP4 + -2.f * vP2 * vP4 + -6.f * vP3 * vP4 + 1.f * vP4 * vP4,
                        epssq);
                    // @formatter:on

                    float hM4 = samples[Math.max(0, x - 4) + Math.min(height - 1, y + 4) * width];
                    float hM3 = samples[Math.max(0, x - 3) + Math.min(height - 1, y + 3) * width];
                    float hM2 = samples[Math.max(0, x - 2) + Math.min(height - 1, y + 2) * width];
                    float hM1 = samples[Math.max(0, x - 1) + Math.min(height - 1, y + 1) * width];

                    float hP1 = samples[Math.min(width - 1, x + 1) + Math.max(0, y - 1) * width];
                    float hP2 = samples[Math.min(width - 1, x + 2) + Math.max(0, y - 2) * width];
                    float hP3 = samples[Math.min(width - 1, x + 3) + Math.max(0, y - 3) * width];
                    float hP4 = samples[Math.min(width - 1, x + 4) + Math.max(0, y - 4) * width];

                    // @formatter:off
                    float qStat = Math.max(
                         1.f * hM4 * hM4 +
                        -6.f * hM4 * hM3 +  10.f * hM3 * hM3 +
                        -2.f * hM4 * hM2 +                      11.f * hM2 * hM2 +
                        12.f * hM4 * hM1 + -38.f * hM3 * hM1 + -12.f * hM2 * hM1 +  46.f * hM1 * hM1 +
                        -2.f * hM4 * cur +  18.f * hM3 * cur + -36.f * hM2 * cur + -18.f * hM1 * cur +  38.f * cur * cur +
                        -6.f * hM4 * hP1 +  16.f * hM3 * hP1 +  24.f * hM2 * hP1 + -70.f * hM1 * hP1 + -18.f * cur * hP1 +  46.f * hP1 * hP1 +
                         2.f * hM4 * hP2 + -12.f * hM3 * hP2 +  14.f * hM2 * hP2 +  24.f * hM1 * hP2 + -36.f * cur * hP2 + -12.f * hP1 * hP2 + 11.f * hP2 * hP2 +
                                             2.f * hM3 * hP3 + -12.f * hM2 * hP3 +  16.f * hM1 * hP3 +  18.f * cur * hP3 + -38.f * hP1 * hP3 +                    10.f * hP3 * hP3 +
                                                                 2.f * hM2 * hP4 +  -6.f * hM1 * hP4 +  -2.f * cur * hP4 +  12.f * hP1 * hP4 + -2.f * hP2 * hP4 + -6.f * hP3 * hP4 + 1.f * hP4 * hP4,
                        epssq);
                    // @formatter:on

                    pqAtRB[x + y * width] = //Math.min(1.0f, Math.max(0.0f,
                        pStat / (pStat + qStat); //));
                }
            }
        }
    }

    public static void calcRBAtG(int[] samples, float[] pqAtRB, float[] greens, float[] endresult,
                                 int width, int height, int green1Idx, int green2Idx, int redIdx, int blueIdx) {
        float eps = 0.1f / (255 * 255);
        for(@Parallel int x = 0; x < width; x++) {
            for(@Parallel int y = 0; y < height; y++) {
                int patternIdx = (x % 2) + 2 * (y % 2);
                // because of multiple conditionals code gen bug
                // see https://github.com/beehive-lab/TornadoVM/issues/247
                if (patternIdx == green1Idx) {
                    endresult[(x + y * width) * 3] = 0.0f;
                    endresult[(x + y * width) * 3 + 1] = greens[x + y * width];
                    endresult[(x + y * width) * 3 + 2] = 0.0f;
                } else if (patternIdx == green2Idx) {
                    endresult[(x + y * width) * 3] = 0.0f;
                    endresult[(x + y * width) * 3 + 1] = greens[x + y * width];
                    endresult[(x + y * width) * 3 + 2] = 0.0f;
                } else {
                    // ## red and blue at blue and red pixels

                    float centerGradient = pqAtRB[x + y * width];
                    // X-shaped to get gradient for reds/blues in neighbourhood
                    float neighbourhoodGradient = 0.25f * (pqAtRB[Math.max(0, x - 1) + Math.max(0, y - 1) * width] +
                            pqAtRB[Math.min(width - 1, x + 1) + Math.max(0, y - 1) * width] +
                            pqAtRB[Math.max(0, x - 1) + Math.min(height - 1, y + 1) * width] +
                            pqAtRB[Math.min(width - 1, x + 1) + Math.min(height - 1, y + 1) * width]);
                    // take the bigger one
                    float pqDisc = Math.abs(centerGradient - 0.5f) < Math.abs(neighbourhoodGradient - 0.5f)
                            ? neighbourhoodGradient
                            : centerGradient;

                    // diagonal gradients
                    float center = greens[x + y * width];
                    float northWest1 = samples[Math.max(0, x - 1) + Math.max(0, y - 1) * width];
                    float northWest2 = greens[Math.max(0, x - 2) + Math.max(0, y - 2) * width];
                    float northWest3 = samples[Math.max(0, x - 3) + Math.max(0, y - 3) * width];

                    float southEast1 = samples[Math.min(width - 1, x + 1) + Math.min(height - 1, y + 1) * width];
                    float southEast2 = greens[Math.min(width - 1, x + 2) + Math.min(height - 1, y + 2) * width];
                    float southEast3 = samples[Math.min(width - 1, x + 3) + Math.min(height - 1, y + 3) * width];

                    float southWest1 = samples[Math.max(0, x - 1) + Math.min(height - 1, y + 1) * width];
                    float southWest2 = greens[Math.max(0, x - 2) + Math.min(height - 1, y + 2) * width];
                    float southWest3 = samples[Math.max(0, x - 3) + Math.min(height - 1, y + 3) * width];

                    float northEast1 = samples[Math.min(width - 1, x + 1) + Math.max(0, y - 1) * width];
                    float northEast2 = greens[Math.min(width - 1, x + 2) + Math.max(0, y - 2) * width];
                    float northEast3 = samples[Math.min(width - 1, x + 3) + Math.max(0, y - 3) * width];

                    float nwGrad = eps + Math.abs(northWest1 - southEast1) + Math.abs(northWest1 - northWest3)
                            + Math.abs(center - northWest2);
                    float neGrad = eps + Math.abs(northEast1 - southWest1) + Math.abs(northEast1 - northEast3)
                            + Math.abs(center - northEast2);
                    float swGrad = eps + Math.abs(southWest1 - northEast1) + Math.abs(southWest1 - southWest3)
                            + Math.abs(center - southWest2);
                    float seGrad = eps + Math.abs(southEast1 - northWest1) + Math.abs(southEast1 - southEast3)
                            + Math.abs(center - southEast2);

                    // diagonal color differences
                    float nwEst = northWest1 -
                            greens[Math.max(0, x - 1) + Math.max(0, y - 1) * width];
                    float neEst = northEast1 -
                            greens[Math.min(width - 1, x + 1) + Math.max(0, y - 1) * width];
                    float swEst = southWest1 -
                            greens[Math.max(0, x - 1) + Math.min(height - 1, y + 1) * width];
                    float seEst = southEast1 -
                            greens[Math.min(width - 1, x + 1) + Math.min(height - 1, y + 1) * width];

                    // p and q estimations
                    float pEst = (nwGrad * seEst + seGrad * nwEst) / (nwGrad + seGrad);
                    float qEst = (neGrad * swEst + swGrad * neEst) / (neGrad + swGrad);

                    // interpolation
                    endresult[(x + y * width) * 3 + 1] = center; // g
                    float interp = // Math.min(1.0f, Math.max(0.0f,
                            center + (1.0f - pqDisc) * pEst + pqDisc * qEst; //));
                    float cur = samples[x + y * width];
                    endresult[(x + y * width) * 3 + 0] = patternIdx == redIdx ? cur : interp; // r
                    endresult[(x + y * width) * 3 + 2] = patternIdx == blueIdx ? cur : interp; // b
                }
            }
        }
    }

    public static void calcEndresult(float[] xyGradient, float[] endresult, int width, int height, int green1Idx, int green2Idx) {
        float eps = 0.1f / (255 * 255);
        for(@Parallel int x = 0; x < width; x++) {
            for(@Parallel int y = 0; y < height; y++) {
                int patternIdx = (x % 2) + 2 * (y % 2);
                // because of multiple conditionals code gen bug
                // see https://github.com/beehive-lab/TornadoVM/issues/247
                boolean isGreen1 = patternIdx == green1Idx;
                boolean isGreen2 = patternIdx == green2Idx;
                boolean isGreen = isGreen1 || isGreen2;
                if(isGreen) {
                    // Refined vertical and horizontal local discrimination
                    float centerGradient = xyGradient[x + y * width];
                    // X-shaped to get gradient for greens in neighbourhood
                    float neighbourhoodGradient = 0.25f * (xyGradient[Math.max(0, x - 1) + Math.max(0, y - 1) * width] +
                            xyGradient[Math.min(width - 1, x + 1) + Math.max(0, y - 1) * width] +
                            xyGradient[Math.max(0, x - 1) + Math.min(height - 1, y + 1) * width] +
                            xyGradient[Math.min(width - 1, x + 1) + Math.min(height - 1, y + 1) * width]);
                    // take the bigger one
                    float vhDisc = Math.abs(centerGradient - 0.5f) < Math.abs(neighbourhoodGradient - 0.5f)
                            ? neighbourhoodGradient
                            : centerGradient;

                    // Cardinal gradients
                    int i = (x + y * width) * 3;
                    float[] center = copyRGB(endresult, i);
                    i = (x + Math.max(0, y - 1) * width) * 3;
                    float[] north1 = copyRGB(endresult, i);
                    i = (x + Math.max(0, y - 2) * width) * 3;
                    float[] north2 = copyRGB(endresult, i);
                    i = (x + Math.max(0, y - 3) * width) * 3;
                    float[] north3 = copyRGB(endresult, i);
                    i = (x + Math.min(height - 1, y + 1) * width) * 3;
                    float[] south1 = copyRGB(endresult, i);
                    i = (x + Math.min(height - 1, y + 2) * width) * 3;
                    float[] south2 = copyRGB(endresult, i);
                    i = (x + Math.min(height - 1, y + 3) * width) * 3;
                    float[] south3 = copyRGB(endresult, i);
                    i = (Math.max(0, x - 1) + y * width) * 3;
                    float[] west1 = copyRGB(endresult, i);
                    i = (Math.max(0, x - 2) + y * width) * 3;
                    float[] west2 = copyRGB(endresult, i);
                    i = (Math.max(0, x - 3) + y * width) * 3;
                    float[] west3 = copyRGB(endresult, i);
                    i = (Math.min(width - 1, x + 1) + y * width) * 3;
                    float[] east1 = copyRGB(endresult, i);
                    i = (Math.min(width - 1, x + 2) + y * width) * 3;
                    float[] east2 = copyRGB(endresult, i);
                    i = (Math.min(width - 1, x + 3) + y * width) * 3;
                    float[] east3 = copyRGB(endresult, i);

                    float nGradR = eps + Math.abs(center[1] - north2[1]) + Math.abs(north1[0] - south1[0])
                            + Math.abs(north1[0] - north3[0]);
                    float sGradR = eps + Math.abs(center[1] - south2[1]) + Math.abs(south1[0] - north1[0])
                            + Math.abs(south1[0] - south3[0]);
                    float wGradR = eps + Math.abs(center[1] - west2[1]) + Math.abs(west1[0] - east1[0])
                            + Math.abs(west1[0] - west3[0]);
                    float eGradR = eps + Math.abs(center[1] - east2[1]) + Math.abs(east1[0] - west1[0])
                            + Math.abs(east1[0] - east3[0]);

                    float nGradB = eps + Math.abs(center[1] - north2[1]) + Math.abs(north1[2] - south1[2])
                            + Math.abs(north1[2] - north3[2]);
                    float sGradB = eps + Math.abs(center[1] - south2[1]) + Math.abs(south1[2] - north1[2])
                            + Math.abs(south1[2] - south3[2]);
                    float wGradB = eps + Math.abs(center[1] - west2[1]) + Math.abs(west1[2] - east1[2])
                            + Math.abs(west1[2] - west3[2]);
                    float eGradB = eps + Math.abs(center[1] - east2[1]) + Math.abs(east1[2] - west1[2])
                            + Math.abs(east1[2] - east3[2]);

                    // cardinal color differences
                    float nEstR = north1[0] - north1[1];
                    float sEstR = south1[0] - south1[1];
                    float wEstR = west1[0] - west1[1];
                    float eEstR = east1[0] - east1[1];

                    float nEstB = north1[2] - north1[1];
                    float sEstB = south1[2] - south1[1];
                    float wEstB = west1[2] - west1[1];
                    float eEstB = east1[2] - east1[1];

                    // Vertical and horizontal estimations
                    float vEstR = (nGradR * sEstR + sGradR * nEstR) / (nGradR + sGradR);
                    float hEstR = (eGradR * wEstR + wGradR * eEstR) / (eGradR + wGradR);
                    float vEstB = (nGradB * sEstB + sGradB * nEstB) / (nGradB + sGradB);
                    float hEstB = (eGradB * wEstB + wGradB * eEstB) / (eGradB + wGradB);

                    // interpolation
                    endresult[(x + y * width) * 3] = //min(1.0f, Math.max(0.0f,
                            center[1] + (1.0f - vhDisc) * vEstR + vhDisc * hEstR; //));
                    endresult[(x + y * width) * 3 + 2] = // Math.min(1.0f, Math.max(0.0f,
                            center[1] + (1.0f - vhDisc) * vEstB + vhDisc * hEstB; //));
                } 
            }
        }
    }

    public static float[] copyRGB(float[] from, int at) {
        float[] res = new float[3];
        res[0] = from[at];
        res[1] = from[at + 1];
        res[2] = from[at + 2];
        return res;
    }

    public static enum RCDStep {
        RAW_DATA("0. Raw data (do nothing)"),
        XY_GRADIENT("1. XY-Gradient"),
        LOW_PASS("2. Low Pass Filter at Red and Blue (Green=bilinear interp.)"),
        GREENS("3. Finished Interpolation of Green Pixels"),
        PQ_GRADIENT("4. PQ-Gradient at Red and Blue (Green=bilinear interp.)"),
        RB_AT_BR("5. All done except RB at G"),
        DONE("6. Finished (but no post-processing)");


        private String label;

        RCDStep(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    // public RCDStep getStopAt() {
    //     return stopAt;
    // }

    // public void setStopAt(RCDStep step) {
    //     stopAt = step;
    // }

    // public void setStopAt(String s) {
    //     RCDStep[] options = RCDStep.values();
    //     for(int i = 0; i < options.length; i++) {
    //         if (options[i].getLabel() == s) {
    //             stopAt = options[i];
    //             break;
    //         }
    //     }
    // }

    /**
     * Perform Ratio Corrected Demosaicing.<br>
     * Dataflow and space in multiples of N = undemosaiced pixels:
     * <pre>.
     *  1   samples
     *      |
     *  2   O-----> xyGradient (overlap: 4)
     *      |       |
     *  3   O------------------> lowPassAtRB (overlap: 2)
     *      |       |            |
     *  4   O-------O------------O-----------> greens (overlap: 2 on low-pass, 2 on xyGradient, 4 on samples -> 6)
     *      |       |                          |
     *  4   O-----------------------------------------> pqGradient (overlap: 4)
     *      |       |                          |        |
     *  6   O----------------------------------O--------O----------> greenEverywhereRBinPlace (overlap: 2 pq, 2 greens, 3 samples -> 8)
     *              |                                                |
     *  7           O------------------------------------------------O------------------------> endresult (overlap: xy: 1, endres: 3 -> 12)
     * </pre>
     * This version uses IntStream Multithreading in all its loops.
     * @param samples image in row-major order
     * @param width
     * @param height
     * @param redIdx
     * @param greenRedRowIdx
     * @param greenBlueRowIdx
     * @param blueIdx
     * @return demosaiced image in row-major order, rgb values interleaved ([r g b r g b ...])
     */
    public static float[] processMT(
        int[] samples, int width, int height, int redIdx, int green1Idx, int green2Idx, int blueIdx) {

        float[] xyGradient = new float[samples.length];
        
        float eps = 0.1f / (255 * 255);
        float epssq = eps; // in order to mirror exactly the existing implementation
        IntStream.range(0, samples.length).parallel().forEach(i -> {
            int x = i % width;
            int y = i / width;
                float vM4 = samples[x + Math.max(0, y - 4) * width];
                float vM3 = samples[x + Math.max(0, y - 3) * width];
                float vM2 = samples[x + Math.max(0, y - 2) * width];
                float vM1 = samples[x + Math.max(0, y - 1) * width];
                float cur = samples[x + y * width];
                float vP1 = samples[x +Math.min(height - 1, y + 1) * width];
                float vP2 = samples[x +Math.min(height - 1, y + 2) * width];
                float vP3 = samples[x +Math.min(height - 1, y + 3) * width];
                float vP4 = samples[x +Math.min(height - 1, y + 4) * width];

                // @formatter:off
                float deltaY = Math.max(
                         1.f * vM4 * vM4 +
                        -6.f * vM4 * vM3 +  10.f * vM3 * vM3 +
                        -2.f * vM4 * vM2 +                      11.f * vM2 * vM2 +
                        12.f * vM4 * vM1 + -38.f * vM3 * vM1 + -12.f * vM2 * vM1 +  46.f * vM1 * vM1 +
                        -2.f * vM4 * cur +  18.f * vM3 * cur + -36.f * vM2 * cur + -18.f * vM1 * cur +  38.f * cur * cur +
                        -6.f * vM4 * vP1 +  16.f * vM3 * vP1 +  24.f * vM2 * vP1 + -70.f * vM1 * vP1 + -18.f * cur * vP1 +  46.f * vP1 * vP1 +
                         2.f * vM4 * vP2 + -12.f * vM3 * vP2 +  14.f * vM2 * vP2 +  24.f * vM1 * vP2 + -36.f * cur * vP2 + -12.f * vP1 * vP2 + 11.f * vP2 * vP2 +
                                             2.f * vM3 * vP3 + -12.f * vM2 * vP3 +  16.f * vM1 * vP3 +  18.f * cur * vP3 + -38.f * vP1 * vP3 +                    10.f * vP3 * vP3 +
                                                                 2.f * vM2 * vP4 +  -6.f * vM1 * vP4 +  -2.f * cur * vP4 +  12.f * vP1 * vP4 + -2.f * vP2 * vP4 + -6.f * vP3 * vP4 + 1.f * vP4 * vP4,
                        epssq);
                // @formatter:on

                float hM4 = samples[Math.max(0, x - 4) + y * width];
                float hM3 = samples[Math.max(0, x - 3) + y * width];
                float hM2 = samples[Math.max(0, x - 2) + y * width];
                float hM1 = samples[Math.max(0, x - 1) + y * width];

                float hP1 = samples[Math.min(width - 1, x + 1) + y * width];
                float hP2 = samples[Math.min(width - 1, x + 2) + y * width];
                float hP3 = samples[Math.min(width - 1, x + 3) + y * width];
                float hP4 = samples[Math.min(width - 1, x + 4) + y * width];

                // @formatter:off
                float deltaX = Math.max(
                         1.f * hM4 * hM4 +
                        -6.f * hM4 * hM3 +  10.f * hM3 * hM3 +
                        -2.f * hM4 * hM2 +                      11.f * hM2 * hM2 +
                        12.f * hM4 * hM1 + -38.f * hM3 * hM1 + -12.f * hM2 * hM1 +  46.f * hM1 * hM1 +
                        -2.f * hM4 * cur +  18.f * hM3 * cur + -36.f * hM2 * cur + -18.f * hM1 * cur +  38.f * cur * cur +
                        -6.f * hM4 * hP1 +  16.f * hM3 * hP1 +  24.f * hM2 * hP1 + -70.f * hM1 * hP1 + -18.f * cur * hP1 +  46.f * hP1 * hP1 +
                         2.f * hM4 * hP2 + -12.f * hM3 * hP2 +  14.f * hM2 * hP2 +  24.f * hM1 * hP2 + -36.f * cur * hP2 + -12.f * hP1 * hP2 + 11.f * hP2 * hP2 +
                                             2.f * hM3 * hP3 + -12.f * hM2 * hP3 +  16.f * hM1 * hP3 +  18.f * cur * hP3 + -38.f * hP1 * hP3 +                    10.f * hP3 * hP3 +
                                                                 2.f * hM2 * hP4 +  -6.f * hM1 * hP4 +  -2.f * cur * hP4 +  12.f * hP1 * hP4 + -2.f * hP2 * hP4 + -6.f * hP3 * hP4 + 1.f * hP4 * hP4,
                        epssq);
                // @formatter:on

                xyGradient[x + y * width] = // Math.min(1.0f, Math.max(0.0f,
                    deltaY / (deltaY + deltaX) ; //));
            
        });

        // calculate low pass at RB
        float[] lowPassAtRB = new float[samples.length];
        IntStream.range(0, samples.length).parallel().forEach(i -> {
            int x = i % width;
            int y = i / width;
                int patternIdx = (x % 2) + 2 * (y % 2);
                // because of multiple conditionals code gen bug
                // see https://github.com/beehive-lab/TornadoVM/issues/247
                if (patternIdx == green1Idx) {
                    lowPassAtRB[x + y * width] = 0.0f;
                } else if (patternIdx == green2Idx) {
                    lowPassAtRB[x + y * width] = 0.0f;
                } else {
                    // 3x3 low-pass filter

                    float middle = samples[x + y * width];

                    float top =         samples[x + Math.max(0, y - 1) * width];
                    float bottom =      samples[x + Math.min(height - 1, y + 1) * width];
                    float left =        samples[Math.max(0, x - 1) + y * width];
                    float right =       samples[Math.min(width - 1, x + 1) + y * width];

                    float topLeft =     samples[Math.max(0, x - 1) + Math.max(0, y - 1) * width];
                    float topRight =    samples[Math.min(width - 1, x + 1) + Math.max(0, y - 1) * width];
                    float bottomLeft =  samples[Math.max(0, x - 1) + Math.min(height - 1, y + 1) * width];
                    float bottomRight = samples[Math.min(width - 1, x + 1) + Math.min(height - 1, y + 1) * width];

                    lowPassAtRB[x + y * width] = 0.25f * middle +
                            0.125f * (top + bottom + left + right) +
                            0.0625f * (topLeft + topRight + bottomLeft + bottomRight);
                }
            
        });

        // calculate greens
        float[] greens = new float[samples.length];
        IntStream.range(0, samples.length).parallel().forEach(i -> {
            int x = i % width;
            int y = i / width;
                int patternIdx = (x % 2) + 2 * (y % 2);
                // because of multiple conditionals code gen bug
                // see https://github.com/beehive-lab/TornadoVM/issues/247
                if (patternIdx == green1Idx) {
                    // we already have green values
                    greens[x + y * width] = samples[x + y * width];
                } else if (patternIdx == green2Idx) {
                    // we already have green values
                    greens[x + y * width] = samples[x + y * width];
                } else {
                    // ## greens at red and blue pixels
                    float centerGradient = xyGradient[x + y * width];
                    // X-shaped to get gradient for greens in neighbourhood
                    float neighbourhoodGradient = 0.25f * (xyGradient[Math.max(0, x - 1) + Math.max(0, y - 1) * width] +
                            xyGradient[Math.min(width - 1, x + 1) + Math.max(0, y - 1) * width] +
                            xyGradient[Math.max(0, x - 1) + Math.min(height - 1, y + 1) * width] +
                            xyGradient[Math.min(width - 1, x + 1) + Math.min(height - 1, y + 1) * width]);
                    // take the bigger one
                    float vhDisc = Math.abs(centerGradient - 0.5f) < Math.abs(neighbourhoodGradient - 0.5f)
                            ? neighbourhoodGradient
                            : centerGradient;

                    // cardinal gradients
                    float vM4 = samples[x + Math.max(0, y - 4) * width];
                    float vM3 = samples[x + Math.max(0, y - 3) * width];
                    float vM2 = samples[x + Math.max(0, y - 2) * width];
                    float vM1 = samples[x + Math.max(0, y - 1) * width];
                    float cur = samples[x + y * width];
                    float vP1 = samples[x + Math.min(height - 1, y + 1) * width];
                    float vP2 = samples[x + Math.min(height - 1, y + 2) * width];
                    float vP3 = samples[x + Math.min(height - 1, y + 3) * width];
                    float vP4 = samples[x + Math.min(height - 1, y + 4) * width];

                    float hM4 = samples[Math.max(0, x - 4) + y * width];
                    float hM3 = samples[Math.max(0, x - 3) + y * width];
                    float hM2 = samples[Math.max(0, x - 2) + y * width];
                    float hM1 = samples[Math.max(0, x - 1) + y * width];

                    float hP1 = samples[Math.min(width - 1, x + 1) + y * width];
                    float hP2 = samples[Math.min(width - 1, x + 2) + y * width];
                    float hP3 = samples[Math.min(width - 1, x + 3) + y * width];
                    float hP4 = samples[Math.min(width - 1, x + 4) + y * width];

                    float nGrad = eps +
                            Math.abs(vM1 - vP1) +
                            Math.abs(cur - vM2) +
                            Math.abs(vM1 - vM3) +
                            Math.abs(vM2 - vM4);
                    float sGrad = eps +
                            Math.abs(vP1 - vM1) +
                            Math.abs(cur - vP2) +
                            Math.abs(vP1 - vP3) +
                            Math.abs(vP2 - vP4);
                    float wGrad = eps +
                            Math.abs(hM1 - hP1) +
                            Math.abs(cur - hM2) +
                            Math.abs(hM1 - hM3) +
                            Math.abs(hM2 - hM4);
                    float eGrad = eps +
                            Math.abs(hP1 - hM1) +
                            Math.abs(cur - hP2) +
                            Math.abs(hP1 - hP3) +
                            Math.abs(hP2 - hP4);

                    float lpfCur = lowPassAtRB[x + y * width];
                    float lpfN = lowPassAtRB[x + Math.max(0, y - 2) * width];
                    float lpfS = lowPassAtRB[x + Math.min(height - 1, y + 2) * width];
                    float lpfW = lowPassAtRB[Math.max(0, x - 2) + y * width];
                    float lpfE = lowPassAtRB[Math.min(width - 1, x + 2) + y * width];

                    // cardinal pixel estimations
                    float nEst = vM1 * (1.0f + (lpfCur - lpfN) / (eps + lpfCur + lpfN));
                    float sEst = vP1 * (1.0f + (lpfCur - lpfS) / (eps + lpfCur + lpfS));
                    float wEst = hM1 * (1.0f + (lpfCur - lpfW) / (eps + lpfCur + lpfW));
                    float eEst = hP1 * (1.0f + (lpfCur - lpfE) / (eps + lpfCur + lpfE));

                    // vertical and horizontal estimations
                    float vEst = (sGrad * nEst + nGrad * sEst) / (nGrad + sGrad);
                    float hEst = (wGrad * eEst + eGrad * wEst) / (eGrad + wGrad);

                    // interpolation
                    greens[x + y * width] = 
                        //min(1.0f, Math.max(0.0f, 
                        vhDisc * hEst + (1.0f - vhDisc) * vEst; //));
                }
            
        });
        // ## pq-Gradient
        // needs only samples
        float[] pqAtRB = new float[samples.length]; //lowPassAtRB; // lowPassAtRB no longer needed
        IntStream.range(0, samples.length).parallel().forEach(i -> {
            int x = i % width;
            int y = i / width;
                int patternIdx = (x % 2) + 2 * (y % 2);
                // because of multiple conditionals code gen bug
                // see https://github.com/beehive-lab/TornadoVM/issues/247
                if (patternIdx == green1Idx) {
                    pqAtRB[x + y * width] = 0.0f;
                } else if (patternIdx == green2Idx) {
                    pqAtRB[x + y * width] = 0.0f;
                } else {
                    // pq-gradient

                    float vM4 = samples[Math.max(0, x - 4) + Math.max(0, y - 4) * width];
                    float vM3 = samples[Math.max(0, x - 3) + Math.max(0, y - 3) * width];
                    float vM2 = samples[Math.max(0, x - 2) + Math.max(0, y - 2) * width];
                    float vM1 = samples[Math.max(0, x - 1) + Math.max(0, y - 1) * width];
                    float cur = samples[x + y * width];
                    float vP1 = samples[Math.min(width - 1, x + 1) + Math.min(height - 1, y + 1) * width];
                    float vP2 = samples[Math.min(width - 1, x + 2) + Math.min(height - 1, y + 2) * width];
                    float vP3 = samples[Math.min(width - 1, x + 3) + Math.min(height - 1, y + 3) * width];
                    float vP4 = samples[Math.min(width - 1, x + 4) + Math.min(height - 1, y + 4) * width];

                    // @formatter:off
                    float pStat = Math.max(
                         1.f * vM4 * vM4 +
                        -6.f * vM4 * vM3 +  10.f * vM3 * vM3 +
                        -2.f * vM4 * vM2 +                      11.f * vM2 * vM2 +
                        12.f * vM4 * vM1 + -38.f * vM3 * vM1 + -12.f * vM2 * vM1 +  46.f * vM1 * vM1 +
                        -2.f * vM4 * cur +  18.f * vM3 * cur + -36.f * vM2 * cur + -18.f * vM1 * cur +  38.f * cur * cur +
                        -6.f * vM4 * vP1 +  16.f * vM3 * vP1 +  24.f * vM2 * vP1 + -70.f * vM1 * vP1 + -18.f * cur * vP1 +  46.f * vP1 * vP1 +
                         2.f * vM4 * vP2 + -12.f * vM3 * vP2 +  14.f * vM2 * vP2 +  24.f * vM1 * vP2 + -36.f * cur * vP2 + -12.f * vP1 * vP2 + 11.f * vP2 * vP2 +
                                             2.f * vM3 * vP3 + -12.f * vM2 * vP3 +  16.f * vM1 * vP3 +  18.f * cur * vP3 + -38.f * vP1 * vP3 +                    10.f * vP3 * vP3 +
                                                                 2.f * vM2 * vP4 +  -6.f * vM1 * vP4 +  -2.f * cur * vP4 +  12.f * vP1 * vP4 + -2.f * vP2 * vP4 + -6.f * vP3 * vP4 + 1.f * vP4 * vP4,
                        epssq);
                    // @formatter:on

                    float hM4 = samples[Math.max(0, x - 4) + Math.min(height - 1, y + 4) * width];
                    float hM3 = samples[Math.max(0, x - 3) + Math.min(height - 1, y + 3) * width];
                    float hM2 = samples[Math.max(0, x - 2) + Math.min(height - 1, y + 2) * width];
                    float hM1 = samples[Math.max(0, x - 1) + Math.min(height - 1, y + 1) * width];

                    float hP1 = samples[Math.min(width - 1, x + 1) + Math.max(0, y - 1) * width];
                    float hP2 = samples[Math.min(width - 1, x + 2) + Math.max(0, y - 2) * width];
                    float hP3 = samples[Math.min(width - 1, x + 3) + Math.max(0, y - 3) * width];
                    float hP4 = samples[Math.min(width - 1, x + 4) + Math.max(0, y - 4) * width];

                    // @formatter:off
                    float qStat = Math.max(
                         1.f * hM4 * hM4 +
                        -6.f * hM4 * hM3 +  10.f * hM3 * hM3 +
                        -2.f * hM4 * hM2 +                      11.f * hM2 * hM2 +
                        12.f * hM4 * hM1 + -38.f * hM3 * hM1 + -12.f * hM2 * hM1 +  46.f * hM1 * hM1 +
                        -2.f * hM4 * cur +  18.f * hM3 * cur + -36.f * hM2 * cur + -18.f * hM1 * cur +  38.f * cur * cur +
                        -6.f * hM4 * hP1 +  16.f * hM3 * hP1 +  24.f * hM2 * hP1 + -70.f * hM1 * hP1 + -18.f * cur * hP1 +  46.f * hP1 * hP1 +
                         2.f * hM4 * hP2 + -12.f * hM3 * hP2 +  14.f * hM2 * hP2 +  24.f * hM1 * hP2 + -36.f * cur * hP2 + -12.f * hP1 * hP2 + 11.f * hP2 * hP2 +
                                             2.f * hM3 * hP3 + -12.f * hM2 * hP3 +  16.f * hM1 * hP3 +  18.f * cur * hP3 + -38.f * hP1 * hP3 +                    10.f * hP3 * hP3 +
                                                                 2.f * hM2 * hP4 +  -6.f * hM1 * hP4 +  -2.f * cur * hP4 +  12.f * hP1 * hP4 + -2.f * hP2 * hP4 + -6.f * hP3 * hP4 + 1.f * hP4 * hP4,
                        epssq);
                    // @formatter:on

                    pqAtRB[x + y * width] = //Math.min(1.0f, Math.max(0.0f,
                        pStat / (pStat + qStat); //));
                }
            
        });
        // ## calculate red at blue pixels and vice versa
        // needs pqAtRB, samples and greens
        float[] endresult = new float[samples.length * 3];
        IntStream.range(0, samples.length).parallel().forEach(i -> {
            int x = i % width;
            int y = i / width;
                int patternIdx = (x % 2) + 2 * (y % 2);
                // because of multiple conditionals code gen bug
                // see https://github.com/beehive-lab/TornadoVM/issues/247
                if (patternIdx == green1Idx) {
                    endresult[(x + y * width) * 3] = 0.0f;
                    endresult[(x + y * width) * 3 + 1] = greens[x + y * width];
                    endresult[(x + y * width) * 3 + 2] = 0.0f;
                } else if (patternIdx == green2Idx) {
                    endresult[(x + y * width) * 3] = 0.0f;
                    endresult[(x + y * width) * 3 + 1] = greens[x + y * width];
                    endresult[(x + y * width) * 3 + 2] = 0.0f;
                } else {
                    // ## red and blue at blue and red pixels

                    float centerGradient = pqAtRB[x + y * width];
                    // X-shaped to get gradient for reds/blues in neighbourhood
                    float neighbourhoodGradient = 0.25f * (pqAtRB[Math.max(0, x - 1) + Math.max(0, y - 1) * width] +
                            pqAtRB[Math.min(width - 1, x + 1) + Math.max(0, y - 1) * width] +
                            pqAtRB[Math.max(0, x - 1) + Math.min(height - 1, y + 1) * width] +
                            pqAtRB[Math.min(width - 1, x + 1) + Math.min(height - 1, y + 1) * width]);
                    // take the bigger one
                    float pqDisc = Math.abs(centerGradient - 0.5f) < Math.abs(neighbourhoodGradient - 0.5f)
                            ? neighbourhoodGradient
                            : centerGradient;

                    // diagonal gradients
                    float center = greens[x + y * width];
                    float northWest1 = samples[Math.max(0, x - 1) + Math.max(0, y - 1) * width];
                    float northWest2 = greens[Math.max(0, x - 2) + Math.max(0, y - 2) * width];
                    float northWest3 = samples[Math.max(0, x - 3) + Math.max(0, y - 3) * width];

                    float southEast1 = samples[Math.min(width - 1, x + 1) + Math.min(height - 1, y + 1) * width];
                    float southEast2 = greens[Math.min(width - 1, x + 2) + Math.min(height - 1, y + 2) * width];
                    float southEast3 = samples[Math.min(width - 1, x + 3) + Math.min(height - 1, y + 3) * width];

                    float southWest1 = samples[Math.max(0, x - 1) + Math.min(height - 1, y + 1) * width];
                    float southWest2 = greens[Math.max(0, x - 2) + Math.min(height - 1, y + 2) * width];
                    float southWest3 = samples[Math.max(0, x - 3) + Math.min(height - 1, y + 3) * width];

                    float northEast1 = samples[Math.min(width - 1, x + 1) + Math.max(0, y - 1) * width];
                    float northEast2 = greens[Math.min(width - 1, x + 2) + Math.max(0, y - 2) * width];
                    float northEast3 = samples[Math.min(width - 1, x + 3) + Math.max(0, y - 3) * width];

                    float nwGrad = eps + Math.abs(northWest1 - southEast1) + Math.abs(northWest1 - northWest3)
                            + Math.abs(center - northWest2);
                    float neGrad = eps + Math.abs(northEast1 - southWest1) + Math.abs(northEast1 - northEast3)
                            + Math.abs(center - northEast2);
                    float swGrad = eps + Math.abs(southWest1 - northEast1) + Math.abs(southWest1 - southWest3)
                            + Math.abs(center - southWest2);
                    float seGrad = eps + Math.abs(southEast1 - northWest1) + Math.abs(southEast1 - southEast3)
                            + Math.abs(center - southEast2);

                    // diagonal color differences
                    float nwEst = northWest1 -
                            greens[Math.max(0, x - 1) + Math.max(0, y - 1) * width];
                    float neEst = northEast1 -
                            greens[Math.min(width - 1, x + 1) + Math.max(0, y - 1) * width];
                    float swEst = southWest1 -
                            greens[Math.max(0, x - 1) + Math.min(height - 1, y + 1) * width];
                    float seEst = southEast1 -
                            greens[Math.min(width - 1, x + 1) + Math.min(height - 1, y + 1) * width];

                    // p and q estimations
                    float pEst = (nwGrad * seEst + seGrad * nwEst) / (nwGrad + seGrad);
                    float qEst = (neGrad * swEst + swGrad * neEst) / (neGrad + swGrad);

                    // interpolation
                    endresult[(x + y * width) * 3 + 1] = center; // g
                    float interp = // Math.min(1.0f, Math.max(0.0f,
                            center + (1.0f - pqDisc) * pEst + pqDisc * qEst; //));
                    float cur = samples[x + y * width];
                    endresult[(x + y * width) * 3 + 0] = patternIdx == redIdx ? cur : interp; // r
                    endresult[(x + y * width) * 3 + 2] = patternIdx == blueIdx ? cur : interp; // b
                }
            
        });

        //## calculate R and B at G locations
        IntStream.range(0, samples.length).parallel().forEach(p -> {
            int x = p % width;
            int y = p / width;
                int patternIdx = (x % 2) + 2 * (y % 2);
                // because of multiple conditionals code gen bug
                // see https://github.com/beehive-lab/TornadoVM/issues/247
                boolean isGreen1 = patternIdx == green1Idx;
                boolean isGreen2 = patternIdx == green2Idx;
                boolean isGreen = isGreen1 || isGreen2;
                if(isGreen) {
                    // Refined vertical and horizontal local discrimination
                    float centerGradient = xyGradient[x + y * width];
                    // X-shaped to get gradient for greens in neighbourhood
                    float neighbourhoodGradient = 0.25f * (xyGradient[Math.max(0, x - 1) + Math.max(0, y - 1) * width] +
                            xyGradient[Math.min(width - 1, x + 1) + Math.max(0, y - 1) * width] +
                            xyGradient[Math.max(0, x - 1) + Math.min(height - 1, y + 1) * width] +
                            xyGradient[Math.min(width - 1, x + 1) + Math.min(height - 1, y + 1) * width]);
                    // take the bigger one
                    float vhDisc = Math.abs(centerGradient - 0.5f) < Math.abs(neighbourhoodGradient - 0.5f)
                            ? neighbourhoodGradient
                            : centerGradient;

                    // Cardinal gradients
                    int i = (x + y * width) * 3;
                    float[] center = copyRGB(endresult, i);
                    i = (x + Math.max(0, y - 1) * width) * 3;
                    float[] north1 = copyRGB(endresult, i);
                    i = (x + Math.max(0, y - 2) * width) * 3;
                    float[] north2 = copyRGB(endresult, i);
                    i = (x + Math.max(0, y - 3) * width) * 3;
                    float[] north3 = copyRGB(endresult, i);
                    i = (x + Math.min(height - 1, y + 1) * width) * 3;
                    float[] south1 = copyRGB(endresult, i);
                    i = (x + Math.min(height - 1, y + 2) * width) * 3;
                    float[] south2 = copyRGB(endresult, i);
                    i = (x + Math.min(height - 1, y + 3) * width) * 3;
                    float[] south3 = copyRGB(endresult, i);
                    i = (Math.max(0, x - 1) + y * width) * 3;
                    float[] west1 = copyRGB(endresult, i);
                    i = (Math.max(0, x - 2) + y * width) * 3;
                    float[] west2 = copyRGB(endresult, i);
                    i = (Math.max(0, x - 3) + y * width) * 3;
                    float[] west3 = copyRGB(endresult, i);
                    i = (Math.min(width - 1, x + 1) + y * width) * 3;
                    float[] east1 = copyRGB(endresult, i);
                    i = (Math.min(width - 1, x + 2) + y * width) * 3;
                    float[] east2 = copyRGB(endresult, i);
                    i = (Math.min(width - 1, x + 3) + y * width) * 3;
                    float[] east3 = copyRGB(endresult, i);

                    float nGradR = eps + Math.abs(center[1] - north2[1]) + Math.abs(north1[0] - south1[0])
                            + Math.abs(north1[0] - north3[0]);
                    float sGradR = eps + Math.abs(center[1] - south2[1]) + Math.abs(south1[0] - north1[0])
                            + Math.abs(south1[0] - south3[0]);
                    float wGradR = eps + Math.abs(center[1] - west2[1]) + Math.abs(west1[0] - east1[0])
                            + Math.abs(west1[0] - west3[0]);
                    float eGradR = eps + Math.abs(center[1] - east2[1]) + Math.abs(east1[0] - west1[0])
                            + Math.abs(east1[0] - east3[0]);

                    float nGradB = eps + Math.abs(center[1] - north2[1]) + Math.abs(north1[2] - south1[2])
                            + Math.abs(north1[2] - north3[2]);
                    float sGradB = eps + Math.abs(center[1] - south2[1]) + Math.abs(south1[2] - north1[2])
                            + Math.abs(south1[2] - south3[2]);
                    float wGradB = eps + Math.abs(center[1] - west2[1]) + Math.abs(west1[2] - east1[2])
                            + Math.abs(west1[2] - west3[2]);
                    float eGradB = eps + Math.abs(center[1] - east2[1]) + Math.abs(east1[2] - west1[2])
                            + Math.abs(east1[2] - east3[2]);

                    // cardinal color differences
                    float nEstR = north1[0] - north1[1];
                    float sEstR = south1[0] - south1[1];
                    float wEstR = west1[0] - west1[1];
                    float eEstR = east1[0] - east1[1];

                    float nEstB = north1[2] - north1[1];
                    float sEstB = south1[2] - south1[1];
                    float wEstB = west1[2] - west1[1];
                    float eEstB = east1[2] - east1[1];

                    // Vertical and horizontal estimations
                    float vEstR = (nGradR * sEstR + sGradR * nEstR) / (nGradR + sGradR);
                    float hEstR = (eGradR * wEstR + wGradR * eEstR) / (eGradR + wGradR);
                    float vEstB = (nGradB * sEstB + sGradB * nEstB) / (nGradB + sGradB);
                    float hEstB = (eGradB * wEstB + wGradB * eEstB) / (eGradB + wGradB);

                    // interpolation
                    endresult[(x + y * width) * 3] = //min(1.0f, Math.max(0.0f,
                            center[1] + (1.0f - vhDisc) * vEstR + vhDisc * hEstR; //));
                    endresult[(x + y * width) * 3 + 2] = // Math.min(1.0f, Math.max(0.0f,
                            center[1] + (1.0f - vhDisc) * vEstB + vhDisc * hEstB; //));
                } 
            
        });

        return endresult;
    }
 
}
