package de.unituebingen.jeniffer2;



import de.unituebingen.dng.DNGProcessor;
import de.unituebingen.dng.DNGProcessor.LoggingMethod;
import de.unituebingen.dng.reader.DNGFile;
import de.unituebingen.dng.reader.DNGReadException;
import de.unituebingen.dng.reader.ImageFileDirectory;
import de.unituebingen.dng.reader.compression.CompressionDecoderException;
import de.unituebingen.imageprocessor.ImageExporter;
import de.unituebingen.imageprocessor.ImageUtils;
import de.unituebingen.imageprocessor.ImageUtils.Orientation;
import de.unituebingen.dng.processor.demosaicingprocessor.DemosaicingProcessor.InterpolationMethod;
import de.unituebingen.dng.processor.util.AccelerationStrategy;
import de.unituebingen.jeniffer2.util.PipelineConfiguration;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.image.*;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class ApplicationData {

    private DNGFile tiffReader;
    private DNGProcessor pipeline;
    private HashMap<PipelineConfiguration, Image> configurations = new HashMap<PipelineConfiguration, Image>();

    private PipelineConfiguration currentConfig = new PipelineConfiguration(
        InterpolationMethod.RCD,
        AccelerationStrategy.GPU_TILE_WISE,
            "");
    private BooleanProperty updateImage = new SimpleBooleanProperty(false);

    public DNGFile getTiffReader() {
        return tiffReader;
    }

    public void setTiffReader(DNGFile tiffReader) {
        if (this.tiffReader == null || tiffReader.getFile().getAbsolutePath() != this.tiffReader.getFile().getAbsolutePath()) {
            resetConfigurations();
            this.tiffReader = tiffReader;
            pipeline = null;
        }
    }

    public Image getImage() throws IOException, DNGReadException, CompressionDecoderException, EOFException {
        Image cached = configurations.get(currentConfig);
        if (cached == null) {
            BufferedImage image = processImage(currentConfig);
            cached = SwingFXUtils.toFXImage(image, null);
            configurations.put(currentConfig, cached);
        }
        return cached;
    }

    private BufferedImage processImage(PipelineConfiguration config) throws IOException, DNGReadException, CompressionDecoderException, EOFException {
        if (pipeline == null) {                
            pipeline = new DNGProcessor(tiffReader.getFile());
            pipeline.setLoggingMethod(LoggingMethod.CSV_AND_CONSOLE);
        }
        pipeline.setInterpolationMethod(config.interpolationMethod());
        pipeline.setAccelerationStrategy(config.accelerationStrategy());
        pipeline.setSubstep(config.subStep());
        ImageFileDirectory baselineIFD = tiffReader.get0thImageFileDirectory();
        int orientation = baselineIFD.getOrientation();
        Orientation imageOrientation = ImageUtils.Orientation.getByTiffOrientation(orientation);
        BufferedImage image = ImageUtils.rotate(pipeline.process(),
                imageOrientation);
        return image;
    }

    public InterpolationMethod getInterpolationMethod() {
        return currentConfig.interpolationMethod();
    }

    public void setInterpolationMethod(InterpolationMethod interpolationMethod) {
        currentConfig = new PipelineConfiguration(
                interpolationMethod,
                currentConfig.accelerationStrategy(),
                currentConfig.subStep());
    }

    public AccelerationStrategy getAccelerationStrategy() {
        return currentConfig.accelerationStrategy();
    }

    public void setAccelerationStrategy(AccelerationStrategy accelerationStrategy) {
        currentConfig = new PipelineConfiguration(
                currentConfig.interpolationMethod(),
                accelerationStrategy,
                currentConfig.subStep());
    }

    public String getSubStep() {
        return currentConfig.subStep();
    }

    public void setSubstep(String step) {
        currentConfig = new PipelineConfiguration(
                currentConfig.interpolationMethod(),
                currentConfig.accelerationStrategy(),
                step);
    }

    public PipelineConfiguration getCurrentConfig() {
        return currentConfig;
    }

    public void setConfig(PipelineConfiguration config) {
        currentConfig = config;
    }

    public HashMap<PipelineConfiguration, Image> getConfigurations() {
        return configurations;
    }

    public BooleanProperty getUpdateImage() {
        return updateImage;
    }

    public void triggerImageUpdate() {
        updateImage.set(!updateImage.get()); // flip
    }

    public void resetConfigurations() {
        configurations.clear();
    }
    
    public Image getImageCached() {
        Image cached = configurations.get(currentConfig);
        if (cached == null) {
            throw new IllegalStateException("Image is not computed yet in the current configuration");
        }
        return cached;
    }
    public boolean isCurrentConfigComputed() {
        return configurations.get(currentConfig) != null;
    }

    public void saveAsTiff16(File file) throws IOException, DNGReadException, CompressionDecoderException, EOFException {
        // Information was lost when converting to FXImage, need to recalculate
        BufferedImage image = processImage(currentConfig);
        ImageExporter.saveAsTIFF(image, file);
    }

    public void saveAsTiff8(File file) throws IOException {
        // FXImage is 8 bit by default
        ImageExporter.saveAsTIFF(SwingFXUtils.fromFXImage(getImageCached(), null), file);
    }

    public void saveAsPNG16(File file) throws IOException, DNGReadException, CompressionDecoderException, EOFException {
        // Information was lost when converting to FXImage, need to recalculate
        BufferedImage image = processImage(currentConfig);
        ImageExporter.saveAsPNG(image, file);
    }

    public void saveAsPNG8(File file) throws IOException {
        // FXImage is 8 bit by default
        ImageExporter.saveAsPNG(SwingFXUtils.fromFXImage(getImageCached(), null), file);
    }

    public void saveAsJPEG(File file, float compression) throws IOException, DNGReadException, CompressionDecoderException, EOFException {
        // FXImage is 8 bit by default
        // but exporter complains about colorspace
        BufferedImage image = processImage(currentConfig);
        ImageExporter.saveAsJPEG(ImageExporter.create8BitBufferedImage(image), file, compression);
    }
}
