package de.unituebingen.dng;

import de.unituebingen.dng.processor.Pipeline;
import de.unituebingen.dng.processor.Processor;
import de.unituebingen.dng.processor.demosaicingprocessor.*;
import de.unituebingen.dng.processor.demosaicingprocessor.DemosaicingProcessor.InterpolationMethod;
import de.unituebingen.dng.processor.gpudemosaicing.GPUBilinearMean;
import de.unituebingen.dng.processor.gpudemosaicing.GPURCD;
import de.unituebingen.dng.processor.log.ConsoleLogger;
import de.unituebingen.dng.processor.log.CsvAndConsoleLogger;
import de.unituebingen.dng.processor.log.CsvLogger;
import de.unituebingen.dng.processor.log.NopLogger;
import de.unituebingen.dng.processor.log.Timer;
import de.unituebingen.dng.processor.postprocessor.*;
import de.unituebingen.dng.processor.otherprocessor.ImageCroppingProcessor;
import de.unituebingen.dng.processor.preprocessor.PreProcessor;
import de.unituebingen.dng.processor.preprocessor.RawMapping;
import de.unituebingen.dng.processor.preprocessor.WhiteBalancing;
import de.unituebingen.dng.processor.util.AccelerationStrategy;
import de.unituebingen.dng.reader.ImageFileDirectory;
import de.unituebingen.dng.reader.DNGReadException;
import de.unituebingen.dng.reader.DNGFile;
import de.unituebingen.dng.reader.DNGTag;
import de.unituebingen.dng.reader.compression.CompressionDecoderException;
import de.unituebingen.dng.reader.dng.util.CFAPattern;
import de.unituebingen.opengl.GPUImage;
import de.unituebingen.opengl.OpenGLContext;
import de.unituebingen.opengl.TransformableOnGPU;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * LEGAL NOTICE
 *
 * This product includes DNG technology under license by Adobe.
 * Dieses Produkt enthaelt die bei Adobe lizenzierte DNG-Technologie.
 */
public class DNGProcessor {

    private boolean performWhiteBalance = true;
    private InterpolationMethod interpolationMethod = InterpolationMethod.NONE;
    private AccelerationStrategy accelerationStrategy = AccelerationStrategy.NONE;

    private DNGFile DNGFile;
    private ImageFileDirectory ifd0;
    private ImageFileDirectory hrIFD;

    private boolean performHSVMapping;
    private boolean performExposureCorrection;
    private boolean performProfileToneCurveMapping;
    private boolean performProfileLookTableMapping;
    private String subStep;

    private Pipeline<BufferedImage> cpuPipeline;
    private Pipeline<TransformableOnGPU> gpuPipeline;
    private int demosaicingOverlap = 0;
    private boolean gpuSwitchBeforeDemosaicing = false;
    private ImageCroppingProcessor afterGpuCropper;

    private Timer pTimer;


    public DNGProcessor(File file) throws IOException, DNGReadException {
        this.DNGFile = new DNGFile(file);
        this.pTimer = new NopLogger(); // default: none
        configure();
        buildProcessor();
    }

    public DNGProcessor(String pathName) throws IOException, DNGReadException {
        this(new File(pathName));
    }

    private void configure() throws EOFException, DNGReadException {
        ifd0 = DNGFile.get0thImageFileDirectory();
        hrIFD = DNGFile.getRAWImageFileDirectory();

        if (ifd0.hasEntry(DNGTag.PROFILE_HUE_SAT_MAP_DATA_1) || ifd0.hasEntry(DNGTag.PROFILE_HUE_SAT_MAP_DATA_2)) {
            performHSVMapping = true;
        }
        if (ifd0.hasEntry(DNGTag.PROFILE_LOOK_TABLE_DATA)) {
            performProfileLookTableMapping = true;
        }
        if (ifd0.hasEntry(DNGTag.BASELINE_EXPOSURE) || ifd0.hasEntry(DNGTag.BASELINE_EXPOSURE_OFFSET)) {
            performExposureCorrection = true;
        }
        if (ifd0.hasEntry(DNGTag.PROFILE_TONE_CURVE)) {
            performProfileToneCurveMapping = true;
        }
    }


    private void buildProcessor() {
        cpuPipeline = new Pipeline<BufferedImage>(pTimer);
        gpuPipeline = new Pipeline<TransformableOnGPU>(pTimer);

        Processor<BufferedImage> preProcessor = new PreProcessor(pTimer, new RawMapping(ifd0, hrIFD));
        if (performWhiteBalance) {
            ((PreProcessor) preProcessor).addOperation(new WhiteBalancing(ifd0, hrIFD));
        }
        ((PreProcessor) preProcessor).setAccelerationStrategy(accelerationStrategy);
        cpuPipeline.add(preProcessor);

        gpuSwitchBeforeDemosaicing = false;
        demosaicingOverlap = 0;
        int width = (int) hrIFD.getImageWidth();
        int height = (int) hrIFD.getImageLength();
        CFAPattern cfaPattern = hrIFD.getCFAPattern();
        switch(accelerationStrategy) {
            case GPU_OPERATION_WISE:
            case GPU_TILE_WISE:
                switch(interpolationMethod) {
                    case RCD:
                        gpuPipeline.add(new GPURCD(cfaPattern));
                        demosaicingOverlap = 12;
                        gpuSwitchBeforeDemosaicing = true;
                        break;
                    case BILINEAR_MEAN:
                        gpuPipeline.add(new GPUBilinearMean(cfaPattern));
                        demosaicingOverlap = 2;
                        gpuSwitchBeforeDemosaicing = true;
                        break;
                    default:
                }
                if(gpuSwitchBeforeDemosaicing) break;
            default:
                cpuPipeline.add(new DemosaicingProcessor(interpolationMethod, cfaPattern, accelerationStrategy, pTimer));
        }
        
        ImageCroppingProcessor cropper = new ImageCroppingProcessor(hrIFD);
        // non-power-of-two-textures cannot be correctly downloaded and cause weird behaviour/segfaults
        // (processing seems to work fine)
        boolean nonPOTTextureDownloadBug = 
            cropper.isNonPOTCrop() && (
                accelerationStrategy == AccelerationStrategy.GPU_OPERATION_WISE ||
                accelerationStrategy == AccelerationStrategy.GPU_TILE_WISE
            );
        if (gpuSwitchBeforeDemosaicing  // cropping not implemented on GPU
            || nonPOTTextureDownloadBug) {
            afterGpuCropper = cropper;
        } else {
            cpuPipeline.add(cropper);
        }

        if (subStep != null && subStep != "") {
            // no postprocessing!
            return;
        } else if(accelerationStrategy == AccelerationStrategy.GPU_OPERATION_WISE ||
                accelerationStrategy == AccelerationStrategy.GPU_TILE_WISE) {
            // postprocessing on GPU
            gpuPipeline.add(new ColorSpaceTransformation(width, height, ifd0));

            if (performHSVMapping) {
                throw new IllegalArgumentException("HSV Mapping not yet implemented on GPU");
            }
            if (performExposureCorrection) {
                gpuPipeline.add(new ExposureCorrection(width, height, ifd0));
            }
            if (performProfileLookTableMapping) {
                throw new IllegalArgumentException("Profile Look Table Mapping not yet implemented on GPU");
            }
            if (performProfileToneCurveMapping) {
                throw new IllegalArgumentException("Profile Tone Mapping not yet implemented on GPU");
            }
            gpuPipeline.add(new XYZD50ToSRGBMapping(width, height));     
        } else {
            // postprocessing on CPU
            Processor<BufferedImage> postProcessor = new PostProcessor(pTimer, new ColorSpaceTransformation(width, height, ifd0));

            if (performHSVMapping) {
                ((PostProcessor) postProcessor).addOperation(new HSVMapping(width, height, ifd0));
            }
            if (performExposureCorrection) {
                ((PostProcessor) postProcessor).addOperation(new ExposureCorrection(width, height, ifd0));
            }
            if (performProfileLookTableMapping) {
                ((PostProcessor) postProcessor).addOperation(new ProfileLookTableMapping(width, height, ifd0));
            }
            if (performProfileToneCurveMapping) {
                ((PostProcessor) postProcessor).addOperation(new ProfileToneCurveMapping(width, height, ifd0));
            }
            ((PostProcessor) postProcessor).addOperation(new XYZD50ToSRGBMapping(width, height));
            ((PostProcessor) postProcessor).setAccelerationStrategy(accelerationStrategy);
            cpuPipeline.add(postProcessor);
        }

    }

    public BufferedImage process() throws CompressionDecoderException, DNGReadException, EOFException {
        
        System.out.println();
        System.out.println("Interpolation Method: " + interpolationMethod.getLabel());
        System.out.println("Acceleration Strategy: " + accelerationStrategy.getLabel());

        System.gc();
        int[] imageData = DNGFile.parseRasterOfImageFileDirectory(hrIFD);
        int imageWidth = (int) hrIFD.getImageWidth();
        int imageLength = (int) hrIFD.getImageLength();
        pTimer.startRun(DNGFile.getFile().getName(),imageWidth,imageLength,accelerationStrategy.getLabel());

        ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        ColorModel colorModel = new ComponentColorModel(colorSpace, false, false, ColorModel.OPAQUE, DataBuffer.TYPE_USHORT);
        WritableRaster writableRaster = Raster.createInterleavedRaster(DataBuffer.TYPE_USHORT, imageWidth, imageLength, 1, new Point(0, 0));
        BufferedImage rawImage = new BufferedImage(colorModel, writableRaster, false, new Properties());
        writableRaster.setPixels(0, 0, imageWidth, imageLength, imageData);

        BufferedImage result = cpuPipeline.process(rawImage);
        if(accelerationStrategy == AccelerationStrategy.GPU_OPERATION_WISE) {
            OpenGLContext ogl = new OpenGLContext(pTimer);
            GPUImage gpuImg = ogl.uploadImage(result, demosaicingOverlap);
            GPUImage gpuResult = (GPUImage) gpuPipeline.process(gpuImg);
            if (gpuSwitchBeforeDemosaicing) {
                result = createRGBImage(result.getWidth(), result.getHeight());
            }
            gpuResult.downloadTo(result);
            gpuImg.delete();
            gpuResult.delete();
            ogl.delete();
            if (afterGpuCropper != null) {                
                String cropTaskName = "ImageCroppingProcessor";
                pTimer.startTask(cropTaskName, "after GPU");
                result = afterGpuCropper.process(result);
                pTimer.endTask(cropTaskName);
            }
        } else if(accelerationStrategy == AccelerationStrategy.GPU_TILE_WISE) {
            BufferedImage source = result;
            if (gpuSwitchBeforeDemosaicing) {
                result = createRGBImage(result.getWidth(), result.getHeight());
            }
            OpenGLContext ogl = new OpenGLContext(pTimer);
            ogl.transformTilewise(source, result, gpuPipeline, demosaicingOverlap);
            ogl.delete();
            if (afterGpuCropper != null) {                
                String cropTaskName = "ImageCroppingProcessor";
                pTimer.startTask(cropTaskName, "after GPU");
                result = afterGpuCropper.process(result);
                pTimer.endTask(cropTaskName);
            }
        }
        pTimer.endRun();
        return result;
    }

    public void setPerformWhiteBalance(boolean performWhiteBalance) {
        this.performWhiteBalance = performWhiteBalance;
    }
    public void setInterpolationMethod(InterpolationMethod interpolationMethod) {
        this.interpolationMethod = interpolationMethod;
        validateSubStep();
        buildProcessor();
    }

    public void setAccelerationStrategy(AccelerationStrategy accelerationStrategy) {
        this.accelerationStrategy = accelerationStrategy;
        validateSubStep();
        buildProcessor();
    }

    public void setLoggingMethod(DNGProcessor.LoggingMethod method) {
        switch (method) {
            case CONSOLE:
                this.pTimer = ConsoleLogger.getInstance();
                break;
            case CSV:
                this.pTimer = CsvLogger.getInstance();
                break;
            case CSV_AND_CONSOLE:
                this.pTimer = CsvAndConsoleLogger.getInstance();
                break;
            case NOP:
                this.pTimer = new NopLogger();
                break;
        }
        buildProcessor();
    }

    public enum LoggingMethod {
        CONSOLE, CSV, NOP, CSV_AND_CONSOLE
    }

    private static BufferedImage createRGBImage(int width, int height) {
        ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        ColorModel colorModel = new ComponentColorModel(colorSpace, false, false, ColorModel.OPAQUE,
                DataBuffer.TYPE_USHORT);
        WritableRaster writableRaster = Raster.createInterleavedRaster(DataBuffer.TYPE_USHORT, width, height, 3 * width,
                3, new int[] { 0, 1, 2 }, new Point(0, 0));
        BufferedImage rgbImage = new BufferedImage(colorModel, writableRaster, false, new Properties());

        return rgbImage;
    }

    public String[] getAvailableSubsteps() {
        return getAvailableSubsteps(interpolationMethod, accelerationStrategy);
    }

    public static String[] getAvailableSubsteps(
        InterpolationMethod interpolationMethod,
        AccelerationStrategy accelerationStrategy
    ) {
        switch (interpolationMethod) {
            case RCD:
                if(accelerationStrategy == AccelerationStrategy.GPU_OPERATION_WISE ||
                   accelerationStrategy == AccelerationStrategy.GPU_TILE_WISE) {
                    GPURCD.RCDStep[] steps = GPURCD.RCDStep.values();
                    String[] names = new String[steps.length];
                    for(int i = 0; i < steps.length; i++) {
                        names[i] = steps[i].getLabel();
                    }
                    return names;
                }
                RatioCorrectedDemosaicing.RCDStep[] rsteps = RatioCorrectedDemosaicing.RCDStep.values();
                String[] rnames = new String[rsteps.length];
                for(int i = 0; i < rsteps.length; i++) {
                    rnames[i] = rsteps[i].getLabel();
                }
                return rnames;
            default:
                return new String[0];
        }
    }

    public void setSubstep(String s) {
        subStep = s;
        buildProcessor();
    }

    public void validateSubStep() {
        String[] availableSubsteps = getAvailableSubsteps();
        boolean valid = false;
        for (int i = 0; i < availableSubsteps.length; i++) {
            if (availableSubsteps[i] == subStep) {
                valid = true;
                break;
            }
        }
        if (!valid)
            subStep = "";
    }

    /**
     * Helper for benchmarking performance of demosaicing algorithms, only performs demosaicing step and nothing else.
     * Used e.g. in CLI for PNG demosaicing.
     * @param img
     * @param interpolationMethod
     * @param accelerationStrategy
     * @param cfaPattern
     * @param loggingMethod
     * @return
     */
    public static BufferedImage performDemosaicing(
        BufferedImage img, 
        InterpolationMethod interpolationMethod, 
        AccelerationStrategy accelerationStrategy, 
        CFAPattern cfaPattern,
        LoggingMethod loggingMethod) {

        Timer logger = new NopLogger();
        switch (loggingMethod) {
            case CONSOLE:
                logger = ConsoleLogger.getInstance();
                break;
            case CSV:
                logger = CsvLogger.getInstance();
                break;
            case CSV_AND_CONSOLE:
                logger = CsvAndConsoleLogger.getInstance();
                break;
            default:
        }

        if(accelerationStrategy == AccelerationStrategy.GPU_TILE_WISE ||
           accelerationStrategy == AccelerationStrategy.GPU_OPERATION_WISE) {
            Pipeline<TransformableOnGPU> gpuPipeline = new Pipeline<TransformableOnGPU>(logger);
            int demosaicingOverlap = 0;
            switch(interpolationMethod) {
                case RCD:
                    gpuPipeline.add(new GPURCD(cfaPattern));
                    demosaicingOverlap = 12;
                    break;
                case BILINEAR_MEAN:
                    gpuPipeline.add(new GPUBilinearMean(cfaPattern));
                    demosaicingOverlap = 2;
                    break;
                default:
                    throw new IllegalArgumentException("Algorithm not yet implemented on GPU");
            }
            BufferedImage result = createRGBImage(img.getWidth(), img.getHeight());
            if(accelerationStrategy == AccelerationStrategy.GPU_OPERATION_WISE) {
                OpenGLContext ogl = new OpenGLContext(logger);
                GPUImage gpuImg = ogl.uploadImage(img, demosaicingOverlap);
                GPUImage gpuResult = (GPUImage) gpuPipeline.process(gpuImg);
                gpuResult.downloadTo(result);
                gpuImg.delete();
                gpuResult.delete();
                ogl.delete();
            } else if(accelerationStrategy == AccelerationStrategy.GPU_TILE_WISE) {
                OpenGLContext ogl = new OpenGLContext(logger);
                ogl.transformTilewise(img, result, gpuPipeline, demosaicingOverlap);
                ogl.delete();
            }
            return result;
        }
        Processor<BufferedImage> processor = new DemosaicingProcessor(interpolationMethod, cfaPattern, accelerationStrategy, logger);        
        return processor.process(img);
    }

    public static int getMargin(InterpolationMethod interpolationMethod) {
        switch(interpolationMethod) {
            /* rewrite and original match, take rewrite because it's faster */
            case NONE:
                return None.getOverlap();
            case NEAREST_NEIGHBOR:
                return NearestNeighbor.getOverlap();
            case BILINEAR_MEAN:
                return BilinearMean.getOverlap();
            case BILINEAR_MEDIAN:
                return BilinearMedian.getOverlap();
            case BICUBIC:
                return BiCubic.getOverlap();
            case MALVAR_HE_CUTLER:
                return MalvarHeCutler.getOverlap();
            case HAMILTON_ADAMS:
                return HamiltonAdams.getOverlap();
            case PPG:
                return PatternedPixelGrouping.getOverlap();
            /* rewrite and original differ */
            case RCD:
                return RatioCorrectedDemosaicing.getOverlap();
            case DLMMSE_CODE:
            case DLMMSE_PAPER:
                return DLMMSE.getOverlap();
            case DLMMSE_RCD_CODE:
            case DLMMSE_RCD_PAPER:
                return DLMMSE_RCD.getOverlap();
        }
        return 0;
    }
}
