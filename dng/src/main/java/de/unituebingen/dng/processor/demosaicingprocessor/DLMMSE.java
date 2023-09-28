package de.unituebingen.dng.processor.demosaicingprocessor;

import java.util.stream.IntStream;

import de.unituebingen.dng.processor.util.AccelerationStrategy;

public class DLMMSE {
    // Directional Linear Minimum Mean-Square-Error Demosaicking
    // adapted from 
    // https://www.ipol.im/pub/art/2011/g_zwld/ (C Reference)
    // and 
    // http://www4.comp.polyu.edu.hk/~cslzhang/PCA-CFA-Denoising.htm (original paper, Matlab Code)
    // the matlab code published with the original paper differs from the formula in the paper at one point, 
    // UseZhangCodeEst is used to switch between the matlab code and the paper formula versions 
    // Edge handling is changed to match getUndemosaicedSample helper used in previous implementation,
    // which was benchmarked with a large dataset

    /**
     * How far Tiles need to overlap in order for the algorithm to function properly
     * @return the width of the margin in pixels
     */
    public static int getOverlap() {
        return 12;
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
            case AUTO_BIG:
            case CPU_TILING_MT: 
                return 256 - 2 * getOverlap();
            case CPU_MT_TILING:
            case CPU_MT_TILING_MT:
            default:
                return 2048 - 2 * getOverlap();         
        }
    }

    /**
     * Demosaic image with DLMMSE algorithm.
     * Dataflow and space in multiples of N = undemosaiced pixels:
     * <pre>.
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
     *  4  O---------------->diffGR, diffGB<---O        <- (3 loops, 2 also use values already there)
     *                          |              |
     *  6                       O--------------O------> endresult
     *  </pre>
     * @param src image as array in row-major order (idx = x + y * width)
     * @param target
     * @param width
     * @param height
     * @param useZhangCodeEst see comment at top of file
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
                    //ph = ph/(2*M + 1) - mom1*mom1/((2*M + 1)*(2*M + 1));
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
                    //pv = pv/(2*M + 1) - mom1*mom1/((2*M + 1)*(2*M + 1));
                    Rv = Rv/(2*M + 1) + divEpsilon;
                    v = mv + (pv/(pv + Rv))*(diffV[i] - mv);
                    V = pv - (pv/(pv + Rv))*pv + divEpsilon;

                    /* Fuse the directional estimates to obtain 
                    the green component. */
                    greens[i] = samples[i] + (V*h + H*v) / (H + V);
                }                
            }
        }

        /* Compute the primary difference signals:
          DiffGR = Green - Red   (known at red locations)
          DiffGB = Green - Blue  (known at blue locations)   */ 
        // reuse already allocated arrays
        float[] diffGR = diffH;
        float[] diffGB = diffV;
        for(int y = 0, i = 0; y < height; y++) {
            for(int x = 0; x < width; x++, i++) {
                int patternIdx = (x % 2) + 2 * (y % 2);
                if(patternIdx == redIdx) {
                    diffGR[i] = greens[i] - samples[i];
                } else if(patternIdx == blueIdx) {
                    diffGB[i] = greens[i] - samples[i];
                }
            }
        }

        /* Interpolate DiffGR at blue locations and DiffGB at red locations */
        for(int y = 0, i = 0; y < height; y++) {
            for(int x = 0; x < width; x++, i++) {
                int patternIdx = (x % 2) + 2 * (y % 2);
                if(patternIdx == redIdx) {
                    diffGB[i] = diagonalAverage(diffGB, width, height, x, y);
                } else if(patternIdx == blueIdx) {
                    diffGR[i] = diagonalAverage(diffGR, width, height, x, y);
                }
            }
        }
        /* Interpolate DiffGR and DiffGB at green locations */
        for(int y = 0, i = 0; y < height; y++) {
            for(int x = 0; x < width; x++, i++) {
                int patternIdx = (x % 2) + 2 * (y % 2);
                if(patternIdx == green1Idx || patternIdx == green2Idx) {
                    diffGB[i] = axialAverage(diffGB, width, height, x, y);
                    diffGR[i] = axialAverage(diffGR, width, height, x, y);
                }
            }
        }
        /* Obtain the red and blue channel interpolations */
        float[] endresult = new float[greens.length * 3];
        // for(int i = 0; i < greens.length; i++) {
        //     endresult[i * 3] = greens[i] - diffGR[i];
        //     endresult[i * 3 + 1] = greens[i];
        //     endresult[i * 3 + 2] = 0; // greens[i] - diffGB[i];
        // }
        for(int y = 0, i = 0; y < height; y++) {
            for(int x = 0; x < width; x++, i++) {
                endresult[i * 3] = greens[i] - diffGR[i];
                endresult[i * 3 + 1] = greens[i];
                endresult[i * 3 + 2] = greens[i] - diffGB[i];
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

    /**
     * calculate the diagonal average at a pixel position
     * <pre>
     * 1 0 1
     * 0 0 0 * 0.25
     * 1 0 1
     * </pre>
     * Edge handling same as getUndemosaicedSample (repeat)
     * @param src image in row-major order
     * @param width
     * @param height
     * @param x
     * @param y
     * @return
     */
    private static float diagonalAverage(float[] src, int width, int height, int x, int y) {
        int west = Math.max(0, x - 1);
        int east = Math.min(width - 1, x + 1);
        int north = Math.max(0, y - 1);
        int south = Math.min(height - 1, y + 1);
        return (
            src[west + north * width] + 
            src[west + south * width] +
            src[east + north * width] + 
            src[east + south * width]
        ) / 4.f;
    }

    /**
     * calculate the diagonal average at a pixel position
     * <pre>
     * 0 1 0
     * 1 0 1 * 0.25
     * 0 1 0
     * </pre>
     * Edge handling same as getUndemosaicedSample (repeat)
     * @param src image in row-major order
     * @param width
     * @param height
     * @param x
     * @param y
     * @return
     */
    private static float axialAverage(float[] src, int width, int height, int x, int y) {
        int west = Math.max(0, x - 1);
        int east = Math.min(width - 1, x + 1);
        int north = Math.max(0, y - 1);
        int south = Math.min(height - 1, y + 1);
        return (
           src[x + north * width] +
            src[east + y * width] + 
            src[x + south * width] +
            src[west + y * width]            
        ) / 4.f;
    }

    /**
     * Same as process, but with IntStreams-based multithreading
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
        float[] filteredH = conv1DMT(samples, width, height, interpCoeff, false);        
        float[] filteredV = conv1DMT(samples, width, height, interpCoeff, true);

        /* Local noise estimation for LMMSE */
        float[] diffH = new float[samples.length];
        float[] diffV = new float[samples.length];

        IntStream.range(0, samples.length).parallel().forEach( i -> {
            int x = i % width;
            int y = i / width;
            int patternIdx = (x % 2) + 2 * (y % 2);
            if(patternIdx == green1Idx || patternIdx == green2Idx) {
                diffH[i] = samples[i] - filteredH[i];
                diffV[i] = samples[i] - filteredV[i];
            } else {
                diffH[i] = filteredH[i] - samples[i];
                diffV[i] = filteredV[i] - samples[i];
            }
        });

        /* Compute the smoothed signals for LMMSE */
        float[] filteredH2 = conv1DMT(diffH, width, height, smoothCoeff, false);
        float[] filteredV2 = conv1DMT(diffV, width, height, smoothCoeff, true);

        /* LMMSE interpolation of the green channel */
        float[] greens = new float[samples.length];
        IntStream.range(0, samples.length).parallel().forEach( i -> {
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
                        temp = filteredH2[y * width + Math.max(0, Math.min(width - 1, x + m))];
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
                        mh = filteredH2[i];
                    }
                    ph = ph/(2*M) - mom1*mom1/(2*M*(2*M + 1)); // copied from C reference code, but this is not variance?
                    //ph = ph/(2*M + 1) - mom1*mom1/((2*M + 1)*(2*M + 1));
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
                        temp = filteredV2[Math.max(0, Math.min(height - 1, y + m)) * width + x];
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
                        mv = filteredV2[i];
                    }
                    pv = pv/(2*M) - mom1*mom1/(2*M*(2*M + 1)); // copied from C reference code, but this is not variance?
                    //pv = pv/(2*M + 1) - mom1*mom1/((2*M + 1)*(2*M + 1));
                    Rv = Rv/(2*M + 1) + divEpsilon;
                    v = mv + (pv/(pv + Rv))*(diffV[i] - mv);
                    V = pv - (pv/(pv + Rv))*pv + divEpsilon;

                    /* Fuse the directional estimates to obtain 
                    the green component. */
                    greens[i] = samples[i] + (V*h + H*v) / (H + V);
                }                
            
        });

        /* Compute the primary difference signals:
          DiffGR = Green - Red   (known at red locations)
          DiffGB = Green - Blue  (known at blue locations)   */ 
        // reuse already allocated arrays
        float[] diffGR = diffH;
        float[] diffGB = diffV;
        IntStream.range(0, samples.length).parallel().forEach( i -> {
            int x = i % width;
            int y = i / width;
                int patternIdx = (x % 2) + 2 * (y % 2);
                if(patternIdx == redIdx) {
                    diffGR[i] = greens[i] - samples[i];
                } else if(patternIdx == blueIdx) {
                    diffGB[i] = greens[i] - samples[i];
                }
            
        });

        /* Interpolate DiffGR at blue locations and DiffGB at red locations */
        IntStream.range(0, samples.length).parallel().forEach( i -> {
            int x = i % width;
            int y = i / width;
                int patternIdx = (x % 2) + 2 * (y % 2);
                if(patternIdx == redIdx) {
                    diffGB[i] = diagonalAverage(diffGB, width, height, x, y);
                } else if(patternIdx == blueIdx) {
                    diffGR[i] = diagonalAverage(diffGR, width, height, x, y);
                }
            
        });
        /* Interpolate DiffGR and DiffGB at green locations */
        IntStream.range(0, samples.length).parallel().forEach( i -> {
            int x = i % width;
            int y = i / width;
                int patternIdx = (x % 2) + 2 * (y % 2);
                if(patternIdx == green1Idx || patternIdx == green2Idx) {
                    diffGB[i] = axialAverage(diffGB, width, height, x, y);
                    diffGR[i] = axialAverage(diffGR, width, height, x, y);
                }
            
        });
        /* Obtain the red and blue channel interpolations */
        float[] endresult = new float[greens.length * 3];
        // for(int i = 0; i < greens.length; i++) {
        //     endresult[i * 3] = greens[i] - diffGR[i];
        //     endresult[i * 3 + 1] = greens[i];
        //     endresult[i * 3 + 2] = 0; // greens[i] - diffGB[i];
        // }
        IntStream.range(0, samples.length).parallel().forEach( i -> {
                endresult[i * 3] = greens[i] - diffGR[i];
                endresult[i * 3 + 1] = greens[i];
                endresult[i * 3 + 2] = greens[i] - diffGB[i];
            
        });
        return endresult;
    }

    /**
     * Line or columnwise 1D Convolution. Edge handling: Repeat outermost pixel (like in getUndemosaicedSample)
     * This version uses IntStream-based Multithreading
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
            IntStream.range(0, samples.length).parallel().forEach( p -> {
                int c = p % width;
                int r = p / width;
                    acc[r * width + c] = 0;
                    for(int i = 0; i < filter.length; i++) {
                        acc[r * width + c] += samples[Math.max(0, Math.min(height - 1, r - offset + i)) * width + c] * filter[i];
                    }
                
            });
        } else {            
            IntStream.range(0, samples.length).parallel().forEach( p -> {
                int c = p % width;
                int r = p / width;
                    acc[r * width + c] = 0;
                    for(int i = 0; i < filter.length; i++) {
                        acc[r * width + c] += samples[r * width + Math.max(0, Math.min(width - 1, c - offset + i))] * filter[i];
                    }
                
            });
        }
        return acc;
    }

    /**
     * Line or columnwise 1D Convolution. Edge handling: Repeat outermost pixel (like in getUndemosaicedSample)
     * This version uses IntStream-based Multithreading
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
            IntStream.range(0, samples.length).parallel().forEach( p -> {
                int c = p % width;
                int r = p / width;
                    acc[r * width + c] = 0;
                    for(int i = 0; i < filter.length; i++) {
                        acc[r * width + c] += samples[Math.max(0, Math.min(height - 1, r - offset + i)) * width + c] * filter[i];
                    }
                
            });
        } else {            
            IntStream.range(0, samples.length).parallel().forEach( p -> {
                int c = p % width;
                int r = p / width;
                    acc[r * width + c] = 0;
                    for(int i = 0; i < filter.length; i++) {
                        acc[r * width + c] += samples[r * width + Math.max(0, Math.min(width - 1, c - offset + i))] * filter[i];
                    }
                
            });
        }
        return acc;
    }
}
