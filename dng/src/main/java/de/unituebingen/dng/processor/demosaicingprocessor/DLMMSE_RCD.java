package de.unituebingen.dng.processor.demosaicingprocessor;

import java.util.stream.IntStream;

import de.unituebingen.dng.processor.util.AccelerationStrategy;

public class DLMMSE_RCD {
    // Directional Linear Minimum Mean-Square-Error Demosaicking (for green values)
    // combined with Ratio Corrected Demosaicing (for red and blue values)
    // as described by Andreas Reiter (2023)

    /**
     * How far Tiles need to overlap in order for the algorithm to function properly
     * @return the width of the margin in pixels
     */
    public static int getOverlap() {
        return 14; // FIXME needs to be 16!!
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
            case CPU_TILING_MT: 
                return 256 - 2 * getOverlap();
            case CPU_MT_TILING:   
            case AUTO_BIG:
            case CPU_MT_TILING_MT:
            default:
                return 2048 - 2 * getOverlap();         
        }
    }

    /**
     * Demosaic image with DLMMSE for green and RCD for red and blue pixels.
     * 
     * Dataflow and space in multiples of N = undemosaiced pixels:
     *  <pre>.
     *  1  samples
     *     |
     *  3  O-----> filteredH, filteredV
     *     |       |
     *  5  O-------O----------> diffH, diffV
     *     |                    |
     *  5  |       O<-----------O                       <- (same name, new values)
     *     |       |            |
     *  6  O-------O------------O-----------> greens
     *     |                                   |
     *  3  O-----> xyGradient                  |
     *     |       |                           |
     *  4  O------------------> pqGradient     | 
     *     |       |            |              |
     *  7  O--------------------O--------------O------> endresult (rb at g)
     *             |                                     |
     *  4          O----------------------------------> endresult
     *  </pre>
     * @param samples
     * @param width
     * @param height
     * @param useZhangCodeEst
     * @param green1Idx
     * @param green2Idx
     * @param redIdx
     * @param blueIdx
     * @return
     */
    public static float[] process(int[] samples, int width, int height, boolean useZhangCodeEst, int green1Idx, int green2Idx, int redIdx, int blueIdx) {
        /* Window size for estimating LMMSE statistics */
        int M = 4;
        /* Small value added in denominators to avoid divide-by-zero */
        float divEpsilon = 0.1f/(255*255);
        /* Interpolation filter used for horizontal and vertical interpolations */
        float[] interpCoeff = {-0.25f, 0.5f, 0.5f, 0.5f, -0.25f};
        /* Approximately Gaussian smoothing filter used in the LMMSE denoising */
        float[] smoothCoeff = {0.03125f, 0.0703125f, 0.1171875f, 
            0.1796875f, 0.203125f, 0.1796875f, 0.1171875f, 0.0703125f, 0.03125f};   
        
        /* Horizontal and vertical 1D interpolations */
        float[] filteredH = conv1D(samples, width, height, interpCoeff, false);        
        float[] filteredV = conv1D(samples, width, height, interpCoeff, true);

        /* Local noise estimation for LMMSE */
        float[] diffH = new float[samples.length];
        float[] diffV = new float[samples.length];

        for(int y = 0, i = 0; y < height; y++) {
            for(int x = 0; x < width; x++, i++) {
                int patternIdx = (x % 2) + 2 * (y % 2);
                if(patternIdx == green1Idx || patternIdx == green2Idx) {
                    diffH[i] = samples[i] - filteredH[i];
                    diffV[i] = samples[i] - filteredV[i];
                } else {
                    diffH[i] = filteredH[i] - samples[i];
                    diffV[i] = filteredV[i] - samples[i];
                }
            }
        }

        /* Compute the smoothed signals for LMMSE */
        filteredH = conv1D(diffH, width, height, smoothCoeff, false);
        filteredV = conv1D(diffV, width, height, smoothCoeff, true);

        /* LMMSE interpolation of the green channel */
        float[] greens = new float[samples.length];
        for(int y = 0, i = 0; y < height; y++) {
            for(int x = 0; x < width; x++, i++) {
                int patternIdx = (x % 2) + 2 * (y % 2);
                if(patternIdx == green1Idx || patternIdx == green2Idx) {
                    greens[i] = samples[i];
                } else {
                    /* The following computes
                    * ph =   var   FilteredH[i + m]
                    *      m=-M,...,M
                    * Rh =   mean  (FilteredH[i + m] - DiffH[i + m])^2 
                    *      m=-M,...,M
                    * h = LMMSE estimate
                    * H = LMMSE estimate accuracy (estimated variance of h)
                    */                    
                    float ph = 0;
                    float Rh = 0;
                    float h = 0;
                    float H = 0;

                    // do not do zero-padded boundary handling, but repeat as in getUndemosaicedSample
                    float temp;
                    float mom1 = 0;
                    for(int m = -M; m <= M; m++) {
                        temp = filteredH[y * width + Math.max(0, Math.min(width - 1, x + m))];
                        mom1 += temp;
                        ph += temp * temp;
                        temp -= diffH[y * width + Math.max(0, Math.min(width - 1, x + m))];
                        Rh += temp * temp;
                    }
                    float mh;
                    if(!useZhangCodeEst) {
                        /* Compute mh = mean_m FilteredH[i + m] */
                        mh = mom1 / (2 * M + 1);
                    } else {
                        /* Compute mh as in Zhang's MATLAB code */
                        mh = filteredH[i];
                    }
                    ph = ph/(2*M) - mom1*mom1/(2*M*(2*M + 1)); // copied from C reference code, but this is not variance?
                    Rh = Rh/(2*M + 1) + divEpsilon;
                    h = mh + (ph/(ph + Rh))*(diffH[i] - mh);
                    H = ph - (ph/(ph + Rh))*ph + divEpsilon;

                    /* The following computes
                    * pv =   var   FilteredV[i + m]
                    *      m=-M,...,M
                    * Rv =   mean  (FilteredV[i + m] - DiffV[i + m])^2 
                    *      m=-M,...,M
                    * v = LMMSE estimate
                    * V = LMMSE estimate accuracy (estimated variance of v)
                    */
                    float pv = 0;
                    float Rv = 0;
                    float v = 0;
                    float V = 0;

                    // do not do zero-padded boundary handling, but repeat as in getUndemosaicedSample
                    temp = 0;
                    mom1 = 0;
                    for(int m = -M; m <= M; m++) {
                        temp = filteredV[Math.max(0, Math.min(height - 1, y + m)) * width + x];
                        mom1 += temp;
                        pv += temp * temp;
                        temp -= diffV[Math.max(0, Math.min(height - 1, y + m)) * width + x];
                        Rv += temp * temp;
                    }
                    float mv;
                    if(!useZhangCodeEst) {
                        /* Compute mh = mean_m FilteredH[i + m] */
                        mv = mom1 / (2 * M + 1);
                    } else {
                        /* Compute mh as in Zhang's MATLAB code */
                        mv = filteredV[i];
                    }
                    pv = pv/(2*M) - mom1*mom1/(2*M*(2*M + 1)); // copied from C reference code, but this is not variance?
                    Rv = Rv/(2*M + 1) + divEpsilon;
                    v = mv + (pv/(pv + Rv))*(diffV[i] - mv);
                    V = pv - (pv/(pv + Rv))*pv + divEpsilon;

                    /* Fuse the directional estimates to obtain 
                    the green component. */
                    greens[i] = samples[i] + (V*h + H*v) / (H + V);
                }                
            }
        }

        /* RCD xy (axial) Gradient */
        float[] xyGradient = diffH;
        float eps = 0.1f / (255 * 255);
        float epssq = eps; // in order to mirror exactly the existing implementation
        for(int x = 0; x < width; x++) {
            for(int y = 0; y < height; y++) {
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

        /* RCD pq (diagonal) Gradient */
        float[] pqAtRB = diffV;
        for(int x = 0; x < width; x++) {
            for(int y = 0; y < height; y++) {
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
        /* Calculate red and blue at blue and red locations */
        float[] endresult = new float[samples.length * 3];
        for(int x = 0; x < width; x++) {
            for(int y = 0; y < height; y++) {
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

        /* calculate red and blue at green locations */
        for(int x = 0; x < width; x++) {
            for(int y = 0; y < height; y++) {
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
        return endresult;
    }

    /**
     * Line or columnwise 1D Convolution. Edge handling: Repeat outermost pixel (like in getUndemosaicedSample)
     * @param samples input image
     * @param width
     * @param height
     * @param filter filter weights
     * @param isVertical
     * @return The filtered image
     */
    public static float[] conv1D(int[] samples, int width, int height, float[] filter, boolean isVertical) {
        float[] acc = new float[samples.length];
        int offset = filter.length / 2;
        if(isVertical) {
            for(int c = 0; c < width; c++) {
                for(int r = 0; r < height; r++) {
                    acc[r * width + c] = 0;
                    for(int i = 0; i < filter.length; i++) {
                        acc[r * width + c] += samples[Math.max(0, Math.min(height - 1, r - offset + i)) * width + c] * filter[i];
                    }
                }
            }
        } else {            
            for(int r = 0; r < height; r++) {
                for(int c = 0; c < width; c++) {
                    acc[r * width + c] = 0;
                    for(int i = 0; i < filter.length; i++) {
                        acc[r * width + c] += samples[r * width + Math.max(0, Math.min(width - 1, c - offset + i))] * filter[i];
                    }
                }
            }
        }
        return acc;
    }

    /**
     * Line or columnwise 1D Convolution. Edge handling: Repeat outermost pixel (like in getUndemosaicedSample)
     * @param samples input image
     * @param width
     * @param height
     * @param filter filter weights
     * @param isVertical
     * @return The filtered image
     */
    public static float[] conv1D(float[] samples, int width, int height, float[] filter, boolean isVertical) {
        float[] acc = new float[samples.length];
        int offset = filter.length / 2;
        if(isVertical) {
            for(int c = 0; c < width; c++) {
                for(int r = 0; r < height; r++) {
                    acc[r * width + c] = 0;
                    for(int i = 0; i < filter.length; i++) {
                        acc[r * width + c] += samples[Math.max(0, Math.min(height - 1, r - offset + i)) * width + c] * filter[i];
                    }
                }
            }
        } else {            
            for(int r = 0; r < height; r++) {
                for(int c = 0; c < width; c++) {
                    acc[r * width + c] = 0;
                    for(int i = 0; i < filter.length; i++) {
                        acc[r * width + c] += samples[r * width + Math.max(0, Math.min(width - 1, c - offset + i))] * filter[i];
                    }
                }
            }
        }
        return acc;
    }

    private static float[] copyRGB(float[] from, int at) {
        float[] res = new float[3];
        res[0] = from[at];
        res[1] = from[at + 1];
        res[2] = from[at + 2];
        return res;
    }

    /**
     * Version of process which uses IntStream multithreading in all loops.
     * @param samples
     * @param width
     * @param height
     * @param useZhangCodeEst
     * @param green1Idx
     * @param green2Idx
     * @param redIdx
     * @param blueIdx
     * @return
     */
    public static float[] processMT(int[] samples, int width, int height, boolean useZhangCodeEst, int green1Idx, int green2Idx, int redIdx, int blueIdx) {
        /* Window size for estimating LMMSE statistics */
        int M = 4;
        /* Small value added in denominators to avoid divide-by-zero */
        float divEpsilon = 0.1f/(255*255);
        /* Interpolation filter used for horizontal and vertical interpolations */
        float[] interpCoeff = {-0.25f, 0.5f, 0.5f, 0.5f, -0.25f};
        /* Approximately Gaussian smoothing filter used in the LMMSE denoising */
        float[] smoothCoeff = {0.03125f, 0.0703125f, 0.1171875f, 
            0.1796875f, 0.203125f, 0.1796875f, 0.1171875f, 0.0703125f, 0.03125f};   
        
        /* Horizontal and vertical 1D interpolations */
        float[] filteredH1 = conv1DMT(samples, width, height, interpCoeff, false);        
        float[] filteredV1 = conv1DMT(samples, width, height, interpCoeff, true);

        /* Local noise estimation for LMMSE */
        float[] diffH = new float[samples.length];
        float[] diffV = new float[samples.length];

        IntStream.range(0, samples.length).parallel().forEach(i -> {
            int x = i % width;
            int y = i / width;
                int patternIdx = (x % 2) + 2 * (y % 2);
                if(patternIdx == green1Idx || patternIdx == green2Idx) {
                    diffH[i] = samples[i] - filteredH1[i];
                    diffV[i] = samples[i] - filteredV1[i];
                } else {
                    diffH[i] = filteredH1[i] - samples[i];
                    diffV[i] = filteredV1[i] - samples[i];
                }        
        });

        /* Compute the smoothed signals for LMMSE */
        float[] filteredH = conv1DMT(diffH, width, height, smoothCoeff, false);
        float[] filteredV = conv1DMT(diffV, width, height, smoothCoeff, true);

        /* LMMSE interpolation of the green channel */
        float[] greens = new float[samples.length];
        IntStream.range(0, samples.length).parallel().forEach(i -> {
            int x = i % width;
            int y = i / width;
                int patternIdx = (x % 2) + 2 * (y % 2);
                if(patternIdx == green1Idx || patternIdx == green2Idx) {
                    greens[i] = samples[i];
                } else {
                    /* The following computes
                    * ph =   var   FilteredH[i + m]
                    *      m=-M,...,M
                    * Rh =   mean  (FilteredH[i + m] - DiffH[i + m])^2 
                    *      m=-M,...,M
                    * h = LMMSE estimate
                    * H = LMMSE estimate accuracy (estimated variance of h)
                    */                    
                    float ph = 0;
                    float Rh = 0;
                    float h = 0;
                    float H = 0;

                    // do not do zero-padded boundary handling, but repeat as in getUndemosaicedSample
                    float temp;
                    float mom1 = 0;
                    for(int m = -M; m <= M; m++) {
                        temp = filteredH[y * width + Math.max(0, Math.min(width - 1, x + m))];
                        mom1 += temp;
                        ph += temp * temp;
                        temp -= diffH[y * width + Math.max(0, Math.min(width - 1, x + m))];
                        Rh += temp * temp;
                    }
                    float mh;
                    if(!useZhangCodeEst) {
                        /* Compute mh = mean_m FilteredH[i + m] */
                        mh = mom1 / (2 * M + 1);
                    } else {
                        /* Compute mh as in Zhang's MATLAB code */
                        mh = filteredH[i];
                    }
                    ph = ph/(2*M) - mom1*mom1/(2*M*(2*M + 1)); // copied from C reference code, but this is not variance?
                    Rh = Rh/(2*M + 1) + divEpsilon;
                    h = mh + (ph/(ph + Rh))*(diffH[i] - mh);
                    H = ph - (ph/(ph + Rh))*ph + divEpsilon;

                    /* The following computes
                    * pv =   var   FilteredV[i + m]
                    *      m=-M,...,M
                    * Rv =   mean  (FilteredV[i + m] - DiffV[i + m])^2 
                    *      m=-M,...,M
                    * v = LMMSE estimate
                    * V = LMMSE estimate accuracy (estimated variance of v)
                    */
                    float pv = 0;
                    float Rv = 0;
                    float v = 0;
                    float V = 0;

                    // do not do zero-padded boundary handling, but repeat as in getUndemosaicedSample
                    temp = 0;
                    mom1 = 0;
                    for(int m = -M; m <= M; m++) {
                        temp = filteredV[Math.max(0, Math.min(height - 1, y + m)) * width + x];
                        mom1 += temp;
                        pv += temp * temp;
                        temp -= diffV[Math.max(0, Math.min(height - 1, y + m)) * width + x];
                        Rv += temp * temp;
                    }
                    float mv;
                    if(!useZhangCodeEst) {
                        /* Compute mh = mean_m FilteredH[i + m] */
                        mv = mom1 / (2 * M + 1);
                    } else {
                        /* Compute mh as in Zhang's MATLAB code */
                        mv = filteredV[i];
                    }
                    pv = pv/(2*M) - mom1*mom1/(2*M*(2*M + 1)); // copied from C reference code, but this is not variance?
                    Rv = Rv/(2*M + 1) + divEpsilon;
                    v = mv + (pv/(pv + Rv))*(diffV[i] - mv);
                    V = pv - (pv/(pv + Rv))*pv + divEpsilon;

                    /* Fuse the directional estimates to obtain 
                    the green component. */
                    greens[i] = samples[i] + (V*h + H*v) / (H + V);
                }                
        });

        /* RCD xy (axial) Gradient */
        float[] xyGradient = diffH;
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

        /* RCD pq (diagonal) Gradient */
        float[] pqAtRB = diffV;
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
        /* Calculate red and blue at blue and red locations */
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

        /* calculate red and blue at green locations */
        IntStream.range(0, samples.length).parallel().forEach(pind -> {
            int x = pind % width;
            int y = pind / width;
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

    /**
     * Line or columnwise 1D Convolution with Multithreading. Edge handling: Repeat outermost pixel (like in getUndemosaicedSample)
     * @param samples input image
     * @param width
     * @param height
     * @param filter filter weights
     * @param isVertical
     * @return The filtered image
     */
    public static float[] conv1DMT(int[] samples, int width, int height, float[] filter, boolean isVertical) {
        float[] acc = new float[samples.length];
        int offset = filter.length / 2;
        if(isVertical) {
            IntStream.range(0, samples.length).parallel().forEach(pind -> {
                int c = pind % width;
                int r = pind / width;
                    acc[r * width + c] = 0;
                    for(int i = 0; i < filter.length; i++) {
                        acc[r * width + c] += samples[Math.max(0, Math.min(height - 1, r - offset + i)) * width + c] * filter[i];
                    }
                
            });
        } else {            
            IntStream.range(0, samples.length).parallel().forEach(pind -> {
                int c = pind % width;
                int r = pind / width;
                    acc[r * width + c] = 0;
                    for(int i = 0; i < filter.length; i++) {
                        acc[r * width + c] += samples[r * width + Math.max(0, Math.min(width - 1, c - offset + i))] * filter[i];
                    }
                
            });
        }
        return acc;
    }

    /**
     * Line or columnwise 1D Convolution with Multithreading. Edge handling: Repeat outermost pixel (like in getUndemosaicedSample)
     * @param samples input image
     * @param width
     * @param height
     * @param filter filter weights
     * @param isVertical
     * @return The filtered image
     */
    public static float[] conv1DMT(float[] samples, int width, int height, float[] filter, boolean isVertical) {
        float[] acc = new float[samples.length];
        int offset = filter.length / 2;
        if(isVertical) {
            IntStream.range(0, samples.length).parallel().forEach(pind -> {
                int c = pind % width;
                int r = pind / width;
                    acc[r * width + c] = 0;
                    for(int i = 0; i < filter.length; i++) {
                        acc[r * width + c] += samples[Math.max(0, Math.min(height - 1, r - offset + i)) * width + c] * filter[i];
                    }
                
            });
        } else {            
            IntStream.range(0, samples.length).parallel().forEach(pind -> {
                int c = pind % width;
                int r = pind / width;
                    acc[r * width + c] = 0;
                    for(int i = 0; i < filter.length; i++) {
                        acc[r * width + c] += samples[r * width + Math.max(0, Math.min(width - 1, c - offset + i))] * filter[i];
                    }
                
            });
        }
        return acc;
    }


}

