package de.unituebingen.opengl;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import de.unituebingen.TestUtils;
import de.unituebingen.dng.DNGProcessor;
import de.unituebingen.dng.processor.demosaicingprocessor.DemosaicingProcessor.InterpolationMethod;
import de.unituebingen.dng.processor.log.ConsoleLogger;
import de.unituebingen.dng.reader.ImageFileDirectory;
import de.unituebingen.dng.reader.DNGReadException;
import de.unituebingen.dng.reader.DNGFile;
import de.unituebingen.dng.reader.compression.CompressionDecoderException;

import java.awt.color.ColorSpace;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.Properties;

/**
 * Unit test for OpenGL wrapper classes
 */
public class OpenGLWrapperTest
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public OpenGLWrapperTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( OpenGLWrapperTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testOpenGLWrapper() throws IOException, DNGReadException, CompressionDecoderException
    {
        // ### load test image
        // load file
        String pathToDNGFile = "src/test/resources/RAW-CANON-S30.dng";
        System.out.println("Opening: " + pathToDNGFile);
        File file = new File(pathToDNGFile);

        // create DNG file
        DNGFile dngFile = new DNGFile(file);
        ImageFileDirectory hrIFD = dngFile.getRAWImageFileDirectory();
        System.gc();

        int[] imageData = dngFile.parseRasterOfImageFileDirectory(hrIFD);
        int imageWidth = (int) hrIFD.getImageWidth();
        int imageLength = (int) hrIFD.getImageLength();

        ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        ColorModel colorModel = new ComponentColorModel(colorSpace, false, false, ColorModel.OPAQUE, DataBuffer.TYPE_USHORT);
        WritableRaster writableRaster = Raster.createInterleavedRaster(DataBuffer.TYPE_USHORT, imageWidth, imageLength, 1, new Point(0, 0));
        BufferedImage rawImage = new BufferedImage(colorModel, writableRaster, false, new Properties());
        writableRaster.setPixels(0, 0, imageWidth, imageLength, imageData);

        // ### Test OpenGLWrapper
        OpenGLContext ogl = new OpenGLContext(ConsoleLogger.getInstance());

        // Upload/Download test monochrome
        System.out.println("------------");
        System.out.println("Testing Upload/Download with Monochrome image:");
        
        DataBufferUShort bufBefore = TestUtils.extractDataBuffer(rawImage);
        GPUImage rawImageGPU = ogl.uploadImage(rawImage, 0);
        rawImageGPU.downloadTo(rawImage);
        DataBuffer bufAfter = TestUtils.extractDataBuffer(rawImage);

        assertTrue(TestUtils.compareDataBuffers(bufBefore, bufAfter, 0.0f));

        System.out.println("------------");
        System.out.println("Testing rendering to texture with Monochrome image:");
        rawImageGPU.applyShaderInPlace(FragmentShaderExamples.ID_MONO);
        rawImageGPU.downloadTo(rawImage);
        rawImageGPU.delete();
        bufAfter = TestUtils.extractDataBuffer(rawImage);

        assertTrue(TestUtils.compareDataBuffers(bufBefore, bufAfter, 0.0f));

        // generate RGB data
        System.out.println("-------------");
        System.out.println("Generating RGB test data:");

        DNGProcessor pipeline = new DNGProcessor(file);
        pipeline.setInterpolationMethod(InterpolationMethod.BICUBIC);
        pipeline.setLoggingMethod(DNGProcessor.LoggingMethod.CSV);
        BufferedImage processedImage = pipeline.process();

        // Upload/Download test RGB
        System.out.println("-------------");
        System.out.println("Testing Upload/Download with RGB image (also forcing split/join):");
        
        // force tiling that includes most edge cases
        ogl.setMaxTextureDimensions(600,500);
        bufBefore = TestUtils.extractDataBuffer(processedImage);
        GPUImage processedImageGPU = ogl.uploadImage(processedImage, 10);
        processedImageGPU.downloadTo(processedImage);
        bufAfter = TestUtils.extractDataBuffer(processedImage);

        assertTrue(TestUtils.compareDataBuffers(bufBefore, bufAfter, 0.0f));

        // rendering to texture test RGB
        System.out.println("-------------");
        System.out.println("Testing rendering to texture with RGB image (identity):");
        processedImageGPU.applyShaderInPlace(FragmentShaderExamples.ID_RGB);
        processedImageGPU.downloadTo(processedImage);
        processedImageGPU.delete();
        bufAfter = TestUtils.extractDataBuffer(processedImage);

        assertTrue(TestUtils.compareDataBuffers(bufBefore, bufAfter, 0.0f));

        // System.out.println("-------------");
        // System.out.println("Testing texture edge clamping:");
        // processedImageGPU.applyShader(FragmentShaderExamples.CLAMP_DEMO);
        // processedImageGPU.displayTextures();
        // processedImageGPU.delete();

        System.out.println("-------------");
        System.out.println("Testing tile-by-tile-rendering (double inversion):");
        String[] shaders = {FragmentShaderExamples.INVERT_RGB, FragmentShaderExamples.INVERT_RGB};
        ogl.applyShaders(processedImage, shaders, 10);
        bufAfter = TestUtils.extractDataBuffer(processedImage);

        assertTrue(TestUtils.compareDataBuffers(bufBefore, bufAfter, 0.0f));

        ogl.delete();

    }
}
