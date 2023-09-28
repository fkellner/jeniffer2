package de.unituebingen.dng.processor.demosaicingprocessor;

import de.unituebingen.dng.processor.Processor;
import de.unituebingen.dng.processor.log.Timer;
import de.unituebingen.dng.processor.util.AccelerationStrategy;
import de.unituebingen.dng.reader.dng.util.CFAPattern;

import java.awt.Point;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.IntStream;

public class DemosaicingProcessor implements Processor<BufferedImage> {

    private AccelerationStrategy accelerationStrategy;
    //  assume square CFA pattern of side length 2. Positions are indexed as follows:
    //  +---+---+
    //  | 0 | 1 |
    //  +---+---+
    //  | 2 | 3 |
    //  +---+---+
    private final int greenRedRowIdx;
    private final int greenBlueRowIdx;
    private final int redIdx;
    private final int blueIdx;

    private final InterpolationMethod interpolationMethod;

    public enum InterpolationMethod {
        NONE("None"),
        NEAREST_NEIGHBOR("Nearest Neighbor"),
        BILINEAR_MEAN("Bilinear Mean"),
        BILINEAR_MEDIAN("Bilinear Median"),
        BICUBIC("Bicubic"),
        MALVAR_HE_CUTLER("MalvarHeCutler"),
        HAMILTON_ADAMS("Hamilton-Adams"),
        PPG("PPG"),
        RCD("RCD"),
        DLMMSE_CODE("DLMMSE (Code-Est)"),
        DLMMSE_PAPER("DLMMSE (Paper-Est)"),
        DLMMSE_RCD_CODE("DLMMSE+RCD (Code-Est)"),
        DLMMSE_RCD_PAPER("DLMMSE+RCD (Paper-Est)");

        private String label;

        InterpolationMethod(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    // parameters for tiling
    private final int OVERLAP;
    private int TILE_SIZE;
    private Timer pTimer;

    public DemosaicingProcessor(InterpolationMethod interpolationMethod, CFAPattern cfaPattern, AccelerationStrategy accelerationStrategy, Timer timer) {
        this.interpolationMethod = interpolationMethod;
        this.accelerationStrategy = accelerationStrategy;
        this.pTimer = timer;
        Objects.requireNonNull(cfaPattern);
        // precompute Indices for demosaicing Algorithms
        switch(cfaPattern) {
            case GBRG:
                greenBlueRowIdx = 0; blueIdx = 1; redIdx = 2; greenRedRowIdx = 3;
                break;
            case BGGR:
                blueIdx = 0; greenBlueRowIdx = 1; greenRedRowIdx = 2; redIdx = 3;
                break;
            case RGGB:
                redIdx = 0; greenRedRowIdx = 1; greenBlueRowIdx = 2; blueIdx = 3;
                break;
            case GRBG:
            default:
                greenRedRowIdx = 0; redIdx = 1; blueIdx = 2; greenBlueRowIdx = 3;
                break;
        }
        // set tiling parameters tuned for interpolationMethod
        switch(interpolationMethod) {
            case DLMMSE_CODE:
            case DLMMSE_PAPER:
                this.OVERLAP = DLMMSE.getOverlap();
                this.TILE_SIZE = DLMMSE.getTileSize(accelerationStrategy);
                break;
            case DLMMSE_RCD_CODE:
            case DLMMSE_RCD_PAPER:
                this.OVERLAP = DLMMSE_RCD.getOverlap();
                this.TILE_SIZE = DLMMSE_RCD.getTileSize(accelerationStrategy);
                break;
            case BILINEAR_MEAN:
                this.OVERLAP = BilinearMean.getOverlap();
                this.TILE_SIZE = BilinearMean.getTileSize(accelerationStrategy);
                break;
            case BILINEAR_MEDIAN:
                this.OVERLAP = BilinearMedian.getOverlap();
                this.TILE_SIZE = BilinearMedian.getTileSize(accelerationStrategy);
                break;
            case BICUBIC:
                this.OVERLAP = BiCubic.getOverlap();
                this.TILE_SIZE = BiCubic.getTileSize(accelerationStrategy);
                break;
            case NONE:
                this.OVERLAP = None.getOverlap();
                this.TILE_SIZE = None.getTileSize(accelerationStrategy);
                break;
            case NEAREST_NEIGHBOR:
                this.OVERLAP = NearestNeighbor.getOverlap();
                this.TILE_SIZE = NearestNeighbor.getTileSize(accelerationStrategy);
                break;
            case RCD:
                this.OVERLAP = RatioCorrectedDemosaicing.getOverlap();
                this.TILE_SIZE = RatioCorrectedDemosaicing.getTileSize(accelerationStrategy);
                break;
            case MALVAR_HE_CUTLER:
                this.OVERLAP = MalvarHeCutler.getOverlap();
                this.TILE_SIZE = MalvarHeCutler.getTileSize(accelerationStrategy);
                break;
            case HAMILTON_ADAMS:
                this.OVERLAP = HamiltonAdams.getOverlap();
                this.TILE_SIZE = HamiltonAdams.getTileSize(accelerationStrategy);
                break;
            case PPG:
                this.OVERLAP = PatternedPixelGrouping.getOverlap();
                this.TILE_SIZE = PatternedPixelGrouping.getTileSize(accelerationStrategy);
                break;
            default:
                throw new IllegalStateException("Reached unreachable code: Forgot a break statement?");
        }
        if(accelerationStrategy == AccelerationStrategy.AUTO_SMALL) {
            switch(interpolationMethod) {
                case NONE:
                case NEAREST_NEIGHBOR:
                case BILINEAR_MEAN:
                case BILINEAR_MEDIAN:
                case BICUBIC:
                case MALVAR_HE_CUTLER:
                case PPG:
                case HAMILTON_ADAMS:
                    this.accelerationStrategy = AccelerationStrategy.MULTITHREADING;
                    break;
                case RCD:
                case DLMMSE_CODE:
                case DLMMSE_PAPER:
                case DLMMSE_RCD_CODE:
                case DLMMSE_RCD_PAPER:
                    this.accelerationStrategy = AccelerationStrategy.CPU_MT_TILING_MT;
                    break;
            }
        } else if(accelerationStrategy == AccelerationStrategy.AUTO_BIG) {
            switch(interpolationMethod) {
                case NONE:
                case NEAREST_NEIGHBOR:
                case BILINEAR_MEAN:
                case BILINEAR_MEDIAN:
                case BICUBIC:
                case MALVAR_HE_CUTLER:
                case PPG:
                case HAMILTON_ADAMS:
                case DLMMSE_CODE:
                case DLMMSE_PAPER:
                    this.accelerationStrategy = AccelerationStrategy.CPU_TILING_MT;
                    break;
                case RCD:
                case DLMMSE_RCD_CODE:
                case DLMMSE_RCD_PAPER:
                    this.accelerationStrategy = AccelerationStrategy.CPU_MT_TILING_MT;
                    break;
            }
        }
        try {
            this.TILE_SIZE = Integer.parseInt(System.getenv("TILE_SIZE")) - 2 * this.OVERLAP;
        } catch(java.lang.NumberFormatException e) {

        }
        if(this.TILE_SIZE < this.OVERLAP) {
            throw new IllegalArgumentException(
                "Computed tile size " + this.TILE_SIZE + " is smaller than overlap " + this.OVERLAP + 
                "\n This would result in an overhead of more than 800% and require performance-degrading checks for special cases.");
        }
        

    }

    @Override
    public BufferedImage process(BufferedImage bufferedImage) {
        short[] samplesIn = ((DataBufferUShort) bufferedImage.getRaster().getDataBuffer()).getData();

        // create target buffered image
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();

        ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        ColorModel colorModel = new ComponentColorModel(colorSpace, false, false, ColorModel.OPAQUE, DataBuffer.TYPE_USHORT);
        WritableRaster writableRaster = Raster.createInterleavedRaster(DataBuffer.TYPE_USHORT, width, height, 3 * width, 3, new int[]{0, 1, 2}, new Point(0, 0));
        BufferedImage demosaicedImage = new BufferedImage(colorModel, writableRaster, false, new Properties());
        short[] demosaicedSamples = ((DataBufferUShort) demosaicedImage.getRaster().getDataBuffer()).getData();

        String taskName = interpolationMethod.toString();
        pTimer.startTask(taskName, (TILE_SIZE + 2 * OVERLAP) + "");

        switch(accelerationStrategy) {
            case NONE:
                int[] samples = new int[samplesIn.length];
                for(int i = 0; i < samples.length; i++) {
                    samples[i] = (int) samplesIn[i] & 0xFFFF;
                }
                float[] result = demosaic(samples, width, height);
                for(int i = 0; i < result.length; i++) {
                    demosaicedSamples[i] = (short)(Math.min(65535, Math.max(0, (int)result[i])));
                }
                break;
            case AUTO_SMALL:
            case MULTITHREADING:
                samples = new int[samplesIn.length];
                IntStream.range(0, samples.length).parallel().forEach(i -> {
                    samples[i] = (int) samplesIn[i] & 0xFFFF;
                });
                result = demosaicMT(samples, width, height);
                IntStream.range(0, result.length).parallel().forEach(i -> {
                    demosaicedSamples[i] = (short)(Math.min(65535, Math.max(0, (int)result[i])));
                });
                break;
            // this is the best strategy, so also apply it for insensible acceleration strategies
            case GPU_OPERATION_WISE:
            case GPU_TILE_WISE:
                System.out.println("Should not reach this code, because implemented on GPU, anyway doing CPU_TILING_MT");
            case AUTO_BIG:
            case CPU_TILING_MT:
                int widthInTiles = (int)Math.ceil((float)width / TILE_SIZE);
                int heightInTiles = (int)Math.ceil((float)height / TILE_SIZE);
                // special case:
                // tile borders:           |   |   | 
                // image border:                    |
                // width-2 tile + overlap:  |--i---i--|
                int clipRight = (widthInTiles - 1) * TILE_SIZE + OVERLAP > width ? widthInTiles - 2 : widthInTiles - 1;
                int clipBottom = (heightInTiles - 1) * TILE_SIZE + OVERLAP > height ? heightInTiles - 2 : heightInTiles - 1;

                IntStream.range(0, widthInTiles * heightInTiles).parallel().forEach( i -> {
                    int tileX = i % widthInTiles;
                    int tileY = i / widthInTiles;

                    // position in Image
                    int originX = tileX * TILE_SIZE;
                    int originY = tileY * TILE_SIZE;

                    // overlaps
                    int marginLeft = tileX == 0 ? 0 : OVERLAP;
                    int marginRight = tileX >= clipRight ? width - widthInTiles * TILE_SIZE : OVERLAP;
                    int marginTop = tileY == 0 ? 0 : OVERLAP;
                    int marginBottom = tileY >= clipBottom ? height - heightInTiles * TILE_SIZE : OVERLAP;

                    // extract tile with margins from databuffer
                    int effectiveWidth = TILE_SIZE + marginLeft + marginRight;
                    int effectiveHeight = TILE_SIZE + marginTop + marginBottom;
                    int effectiveOriginX = originX - marginLeft;
                    int effectiveOriginY = originY - marginTop;
                    int[] samplesR = new int[effectiveWidth * effectiveHeight];
                    for(int x = effectiveOriginX, tgtx = 0; x < effectiveOriginX + effectiveWidth; x++, tgtx++) {
                        for(int y = effectiveOriginY, tgty = 0; y < effectiveOriginY + effectiveHeight; y++, tgty++) {
                            samplesR[tgtx + tgty * effectiveWidth] = (int) samplesIn[x + y * width] & 0xFFFF;
                        }
                    }
                    // process
                    float[] resultR = demosaic(samplesR, effectiveWidth, effectiveHeight);
                    // write back only data
                    int dataEndX = marginRight <= 0 ? effectiveWidth : effectiveWidth - marginRight;
                    int dataEndY = marginBottom <= 0 ? effectiveHeight : effectiveHeight - marginBottom;
                    for(int srcx = marginLeft, tgtx = originX; srcx < dataEndX; srcx++, tgtx++) {
                        for(int srcy = marginTop, tgty = originY; srcy < dataEndY; srcy++, tgty++) {
                            int srcIdx = srcx + effectiveWidth * srcy;
                            int tgtIdx = tgtx + width * tgty;
                            demosaicedSamples[tgtIdx * 3] = (short)(Math.min(65535, Math.max(0, (int)resultR[srcIdx * 3])));
                            demosaicedSamples[tgtIdx * 3 + 1] = (short)(Math.min(65535, Math.max(0, (int)resultR[srcIdx * 3 + 1])));
                            demosaicedSamples[tgtIdx * 3 + 2] = (short)(Math.min(65535, Math.max(0, (int)resultR[srcIdx * 3 + 2])));
                        }
                    }
                });
                break;
            case CPU_TILING:
                widthInTiles = (int)Math.ceil((float)width / TILE_SIZE);
                heightInTiles = (int)Math.ceil((float)height / TILE_SIZE);
                // special case:
                // tile borders:           |   |   | 
                // image border:                    |
                // width-2 tile + overlap:  |--i---i--|
                clipRight = (widthInTiles - 1) * TILE_SIZE + OVERLAP > width ? widthInTiles - 2 : widthInTiles - 1;
                clipBottom = (heightInTiles - 1) * TILE_SIZE + OVERLAP > height ? heightInTiles - 2 : heightInTiles - 1;

                for(int tileX = 0; tileX < widthInTiles; tileX++) {
                    for(int tileY = 0; tileY < heightInTiles; tileY++) {
                        int originX = tileX * TILE_SIZE;
                        int originY = tileY * TILE_SIZE;
                        int marginLeft = tileX == 0 ? 0 : OVERLAP;
                        int marginRight = tileX >= clipRight ? width - widthInTiles * TILE_SIZE : OVERLAP;
                        int marginTop = tileY == 0 ? 0 : OVERLAP;
                        int marginBottom = tileY >= clipBottom ? height - heightInTiles * TILE_SIZE : OVERLAP;
                        // extract tile from databuffer
                        int effectiveWidth = TILE_SIZE + marginLeft + marginRight;
                        int effectiveHeight = TILE_SIZE + marginTop + marginBottom;
                        int effectiveOriginX = originX - marginLeft;
                        int effectiveOriginY = originY - marginTop;
                        int[] samples3 = new int[effectiveWidth * effectiveHeight];
                        for(int x = effectiveOriginX, tgtx = 0; x < effectiveOriginX + effectiveWidth; x++, tgtx++) {
                            for(int y = effectiveOriginY, tgty = 0; y < effectiveOriginY + effectiveHeight; y++, tgty++) {
                                samples3[tgtx + tgty * effectiveWidth] = (int) samplesIn[x + y * width] & 0xFFFF;
                            }
                        }
                        // process
                        float[] result3 = demosaic(samples3, effectiveWidth, effectiveHeight);
                        // write back only data
                        int dataEndX = marginRight <= 0 ? effectiveWidth : effectiveWidth - marginRight;
                        int dataEndY = marginBottom <= 0 ? effectiveHeight : effectiveHeight - marginBottom;
                        for(int srcx = marginLeft, tgtx = originX; srcx < dataEndX; srcx++, tgtx++) {
                            for(int srcy = marginTop, tgty = originY; srcy < dataEndY; srcy++, tgty++) {
                                int srcIdx = srcx + effectiveWidth * srcy;
                                int tgtIdx = tgtx + width * tgty;
                                demosaicedSamples[tgtIdx * 3] = (short)(Math.min(65535, Math.max(0, (int)result3[srcIdx * 3])));
                                demosaicedSamples[tgtIdx * 3 + 1] = (short)(Math.min(65535, Math.max(0, (int)result3[srcIdx * 3 + 1])));
                                demosaicedSamples[tgtIdx * 3 + 2] = (short)(Math.min(65535, Math.max(0, (int)result3[srcIdx * 3 + 2])));
                            }
                        }
                    }
                }
                break;
            case CPU_MT_TILING:
                widthInTiles = (int)Math.ceil((float)width / TILE_SIZE);
                heightInTiles = (int)Math.ceil((float)height / TILE_SIZE);
                // special case:
                // tile borders:           |   |   | 
                // image border:                    |
                // width-2 tile + overlap:  |--i---i--|
                clipRight = (widthInTiles - 1) * TILE_SIZE + OVERLAP > width ? widthInTiles - 2 : widthInTiles - 1;
                clipBottom = (heightInTiles - 1) * TILE_SIZE + OVERLAP > height ? heightInTiles - 2 : heightInTiles - 1;

                for(int tileX = 0; tileX < widthInTiles; tileX++) {
                    for(int tileY = 0; tileY < heightInTiles; tileY++) {
                        int originX = tileX * TILE_SIZE;
                        int originY = tileY * TILE_SIZE;
                        int marginLeft = tileX == 0 ? 0 : OVERLAP;
                        int marginRight = tileX >= clipRight ? width - widthInTiles * TILE_SIZE : OVERLAP;
                        int marginTop = tileY == 0 ? 0 : OVERLAP;
                        int marginBottom = tileY >= clipBottom ? height - heightInTiles * TILE_SIZE : OVERLAP;
                        // extract tile from databuffer
                        int effectiveWidth = TILE_SIZE + marginLeft + marginRight;
                        int effectiveHeight = TILE_SIZE + marginTop + marginBottom;
                        int effectiveOriginX = originX - marginLeft;
                        int effectiveOriginY = originY - marginTop;
                        int[] samples3 = new int[effectiveWidth * effectiveHeight];
                        IntStream.range(0, effectiveWidth * effectiveHeight).parallel().forEach(i -> {
                            int x = (i % effectiveWidth) + effectiveOriginX;
                            int y = (i / effectiveWidth) + effectiveOriginY;
                            samples3[i] = (int) samplesIn[x + y * width] & 0xFFFF;
                        });
                        // process
                        float[] result3 = demosaicMT(samples3, effectiveWidth, effectiveHeight);
                        // write back only data
                        int dataEndX = marginRight <= 0 ? effectiveWidth : effectiveWidth - marginRight;
                        int dataEndY = marginBottom <= 0 ? effectiveHeight : effectiveHeight - marginBottom;
                        int wData = dataEndX - marginLeft;
                        int hData = dataEndY - marginTop;
                        IntStream.range(0, wData * hData).parallel().forEach(i -> {
                            int srcx = (i % wData) + marginLeft;
                            int srcy = (i / wData) + marginTop;
                            int srcIdx = srcx + effectiveWidth * srcy;
                            int tgtx = (i % wData) + originX;
                            int tgty = (i / wData) + originY;
                            int tgtIdx = tgtx + width * tgty;
                            demosaicedSamples[tgtIdx * 3] = (short)(Math.min(65535, Math.max(0, (int)result3[srcIdx * 3])));
                            demosaicedSamples[tgtIdx * 3 + 1] = (short)(Math.min(65535, Math.max(0, (int)result3[srcIdx * 3 + 1])));
                            demosaicedSamples[tgtIdx * 3 + 2] = (short)(Math.min(65535, Math.max(0, (int)result3[srcIdx * 3 + 2])));                            
                        });
                    }
                }
                break;
            case CPU_MT_TILING_MT:
                widthInTiles = (int)Math.ceil((float)width / TILE_SIZE);
                heightInTiles = (int)Math.ceil((float)height / TILE_SIZE);
                // special case:
                // tile borders:           |   |   | 
                // image border:                    |
                // width-2 tile + overlap:  |--i---i--|
                clipRight = (widthInTiles - 1) * TILE_SIZE + OVERLAP > width ? widthInTiles - 2 : widthInTiles - 1;
                clipBottom = (heightInTiles - 1) * TILE_SIZE + OVERLAP > height ? heightInTiles - 2 : heightInTiles - 1;

                IntStream.range(0, widthInTiles * heightInTiles).parallel().forEach( t -> {
                    int tileX = t % widthInTiles;
                    int tileY = t / widthInTiles;
                        int originX = tileX * TILE_SIZE;
                        int originY = tileY * TILE_SIZE;
                        int marginLeft = tileX == 0 ? 0 : OVERLAP;
                        int marginRight = tileX >= clipRight ? width - widthInTiles * TILE_SIZE : OVERLAP;
                        int marginTop = tileY == 0 ? 0 : OVERLAP;
                        int marginBottom = tileY >= clipBottom ? height - heightInTiles * TILE_SIZE : OVERLAP;
                        // extract tile from databuffer
                        int effectiveWidth = TILE_SIZE + marginLeft + marginRight;
                        int effectiveHeight = TILE_SIZE + marginTop + marginBottom;
                        int effectiveOriginX = originX - marginLeft;
                        int effectiveOriginY = originY - marginTop;
                        int[] samples3 = new int[effectiveWidth * effectiveHeight];
                        IntStream.range(0, effectiveWidth * effectiveHeight).parallel().forEach(i -> {
                            int x = (i % effectiveWidth) + effectiveOriginX;
                            int y = (i / effectiveWidth) + effectiveOriginY;
                            samples3[i] = (int) samplesIn[x + y * width] & 0xFFFF;
                        });
                        // process
                        float[] result3 = demosaicMT(samples3, effectiveWidth, effectiveHeight);
                        // write back only data
                        int dataEndX = marginRight <= 0 ? effectiveWidth : effectiveWidth - marginRight;
                        int dataEndY = marginBottom <= 0 ? effectiveHeight : effectiveHeight - marginBottom;
                        int wData = dataEndX - marginLeft;
                        int hData = dataEndY - marginTop;
                        IntStream.range(0, wData * hData).parallel().forEach(i -> {
                            int srcx = (i % wData) + marginLeft;
                            int srcy = (i / wData) + marginTop;
                            int srcIdx = srcx + effectiveWidth * srcy;
                            int tgtx = (i % wData) + originX;
                            int tgty = (i / wData) + originY;
                            int tgtIdx = tgtx + width * tgty;
                            demosaicedSamples[tgtIdx * 3] = (short)(Math.min(65535, Math.max(0, (int)result3[srcIdx * 3])));
                            demosaicedSamples[tgtIdx * 3 + 1] = (short)(Math.min(65535, Math.max(0, (int)result3[srcIdx * 3 + 1])));
                            demosaicedSamples[tgtIdx * 3 + 2] = (short)(Math.min(65535, Math.max(0, (int)result3[srcIdx * 3 + 2])));                            
                        });
                    
                });
                break;
        }
        pTimer.endTask(taskName);
        
        return demosaicedImage;
    }

    /**
     * Actually perform demosaicing. 
     * @param samples Undemosaiced image in row-major order
     * @param width
     * @param height
     * @return a float array containing the demosaiced image in row-major order, with interleaved color components ([r, g, b, r, g, b...])
     */
    private float[] demosaic(int[] samples, int width, int height) {
        switch(interpolationMethod) {
            case DLMMSE_CODE:
                return DLMMSE.process(samples, width, height, true, greenBlueRowIdx, greenRedRowIdx, redIdx, blueIdx);
            case DLMMSE_PAPER:
                return DLMMSE.process(samples, width, height, false, greenBlueRowIdx, greenRedRowIdx, redIdx, blueIdx);
            case DLMMSE_RCD_CODE:
                return DLMMSE_RCD.process(samples, width, height, true, greenBlueRowIdx, greenRedRowIdx, redIdx, blueIdx);
            case DLMMSE_RCD_PAPER:
                return DLMMSE_RCD.process(samples, width, height, false, greenBlueRowIdx, greenRedRowIdx, redIdx, blueIdx);
            case BILINEAR_MEAN:
                return BilinearMean.process(samples, width, height, redIdx, greenRedRowIdx, greenBlueRowIdx, blueIdx);
            case BILINEAR_MEDIAN:
                return BilinearMedian.process(samples, width, height, redIdx, greenRedRowIdx, greenBlueRowIdx, blueIdx);
            case BICUBIC:
                return BiCubic.process(samples, width, height, redIdx, greenRedRowIdx, greenBlueRowIdx, blueIdx);
            case NONE:
                return None.process(samples, width, height, redIdx, greenRedRowIdx, greenBlueRowIdx, blueIdx);
            case NEAREST_NEIGHBOR:
                return NearestNeighbor.process(samples, width, height, redIdx, greenRedRowIdx, greenBlueRowIdx, blueIdx);
            case RCD:
                return RatioCorrectedDemosaicing.process(samples, width, height, redIdx, greenRedRowIdx, greenBlueRowIdx, blueIdx);
            case MALVAR_HE_CUTLER:
                return MalvarHeCutler.process(samples, width, height, redIdx, greenRedRowIdx, greenBlueRowIdx, blueIdx);
            case HAMILTON_ADAMS:
                return HamiltonAdams.process(samples, width, height, redIdx, greenRedRowIdx, greenBlueRowIdx, blueIdx);
            case PPG:
                return PatternedPixelGrouping.process(samples, width, height, redIdx, greenRedRowIdx, greenBlueRowIdx, blueIdx);
        }
        throw new IllegalStateException("Reached unreachable code. This statement is only here so IDE does not complain");
    }

    /**
     * Actually perform demosaicing, using multithreading in the implemented loops.
     * @param samples Undemosaiced image in row-major order
     * @param width
     * @param height
     * @return a float array containing the demosaiced image in row-major order, with interleaved color components ([r, g, b, r, g, b...])
     */
    private float[] demosaicMT(int[] samples, int width, int height) {
        switch(interpolationMethod) {
            case DLMMSE_CODE:
                return DLMMSE.processMT(samples, width, height, true, greenBlueRowIdx, greenRedRowIdx, redIdx, blueIdx);
            case DLMMSE_PAPER:
                return DLMMSE.processMT(samples, width, height, false, greenBlueRowIdx, greenRedRowIdx, redIdx, blueIdx);
            case DLMMSE_RCD_CODE:
                return DLMMSE_RCD.processMT(samples, width, height, true, greenBlueRowIdx, greenRedRowIdx, redIdx, blueIdx);
            case DLMMSE_RCD_PAPER:
                return DLMMSE_RCD.processMT(samples, width, height, false, greenBlueRowIdx, greenRedRowIdx, redIdx, blueIdx);
            case BILINEAR_MEAN:
                return BilinearMean.processMT(samples, width, height, redIdx, greenRedRowIdx, greenBlueRowIdx, blueIdx);
            case BILINEAR_MEDIAN:
                return BilinearMedian.processMT(samples, width, height, redIdx, greenRedRowIdx, greenBlueRowIdx, blueIdx);
            case BICUBIC:
                return BiCubic.processMT(samples, width, height, redIdx, greenRedRowIdx, greenBlueRowIdx, blueIdx);
            case NONE:
                return None.processMT(samples, width, height, redIdx, greenRedRowIdx, greenBlueRowIdx, blueIdx);
            case NEAREST_NEIGHBOR:
                return NearestNeighbor.processMT(samples, width, height, redIdx, greenRedRowIdx, greenBlueRowIdx, blueIdx);
            case RCD:
                return RatioCorrectedDemosaicing.processMT(samples, width, height, redIdx, greenRedRowIdx, greenBlueRowIdx, blueIdx);
            case MALVAR_HE_CUTLER:
                return MalvarHeCutler.processMT(samples, width, height, redIdx, greenRedRowIdx, greenBlueRowIdx, blueIdx);
            case HAMILTON_ADAMS:
                return HamiltonAdams.processMT(samples, width, height, redIdx, greenRedRowIdx, greenBlueRowIdx, blueIdx);
            case PPG:
                return PatternedPixelGrouping.processMT(samples, width, height, redIdx, greenRedRowIdx, greenBlueRowIdx, blueIdx);
        }
        throw new IllegalStateException("Reached unreachable code. This statement is only here so IDE does not complain");
    }
}
