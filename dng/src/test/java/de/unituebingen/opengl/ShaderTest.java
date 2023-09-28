package de.unituebingen.opengl;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import de.unituebingen.TestUtils;
import de.unituebingen.dng.DNGProcessor;
import de.unituebingen.dng.processor.demosaicingprocessor.DemosaicingProcessor.InterpolationMethod;
import de.unituebingen.dng.processor.util.AccelerationStrategy;
import de.unituebingen.dng.reader.DNGReadException;
import de.unituebingen.dng.reader.compression.CompressionDecoderException;
import de.unituebingen.imageprocessor.ImageExporter;

import java.awt.image.*;
import java.io.*;

/**
 * Unit test for OpenGL wrapper classes
 */
public class ShaderTest
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public ShaderTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( ShaderTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testShader() throws IOException, DNGReadException, CompressionDecoderException
    {
        // ### load test image
        // load file
        String pathToDNGFile = "src/test/resources/RAW-CANON-S30.dng";
        System.out.println("Opening: " + pathToDNGFile);
        File file = new File(pathToDNGFile);

        // generate RGB data
        System.out.println("-------------");
        System.out.println("Testing operation wise postprocessor shaders:");

        DNGProcessor pipeline = new DNGProcessor(file);
        pipeline.setInterpolationMethod(InterpolationMethod.BILINEAR_MEAN);
        pipeline.setLoggingMethod(DNGProcessor.LoggingMethod.CONSOLE);
        BufferedImage processedImage = pipeline.process();
        ImageExporter.saveAsTIFF(processedImage, new File("testBilinearMean.tiff"));
        DataBuffer bufBefore = TestUtils.extractDataBuffer(processedImage);

        pipeline.setAccelerationStrategy(AccelerationStrategy.GPU_OPERATION_WISE);
        processedImage = pipeline.process();
        DataBuffer bufAfter = TestUtils.extractDataBuffer(processedImage);
        
        System.out.println("Comparing result with CPU version:");
        assertTrue(TestUtils.compareDataBuffers(bufBefore, bufAfter, 2.0f));

        System.out.println("-------------");
        System.out.println("Testing tile wise postprocessor shaders:");
        pipeline.setAccelerationStrategy(AccelerationStrategy.GPU_TILE_WISE);
        processedImage = pipeline.process();
        bufAfter = TestUtils.extractDataBuffer(processedImage);
        
        System.out.println("Comparing result with CPU version:");
        assertTrue(TestUtils.compareDataBuffers(bufBefore, bufAfter, 2.0f));

        // verify assumption that passing shader constants as string representation
        // loses no information
        double test = Math.sqrt(2);
        assertTrue(Double.parseDouble("" + test) == test);

        System.out.println("-------------");
        System.out.println("Testing RCD on GPU:");
        pipeline.setInterpolationMethod(InterpolationMethod.RCD);
        processedImage = pipeline.process();
        ImageExporter.saveAsTIFF(processedImage, new File("debug.tiff"));
        bufAfter = TestUtils.extractDataBuffer(processedImage);
        System.out.println("Testing RCD RB debug Shader on GPU:");
        // pipeline.setSubstep(GPURCD.RCDStep.LOW_PASS.getLabel());
        pipeline.process();

        pipeline.setAccelerationStrategy(AccelerationStrategy.MULTITHREADING);
        processedImage = pipeline.process();
        bufBefore = TestUtils.extractDataBuffer(processedImage);
        System.out.println("Comparing result with CPU version:");
        assertTrue(TestUtils.compareDataBuffers(bufBefore, bufAfter, 2.0f));

        // System.out.println("-------------");

        // System.out.println("-------------");
        // System.out.println("Testing RCD Rewrite:");
        // pipeline.setInterpolationMethod(Facade.InterpolationMethod.RCD_REWRITE);
        // processedImage = pipeline.process();
        // bufAfter = TestUtils.extractDataBuffer(processedImage);
        // System.out.println("Comparing rewrite with old version:");
        // assertTrue(TestUtils.compareDataBuffers(bufBefore, bufAfter, 1.0f, false));

        // interestingly, here all values match
        
    }
}
