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
import java.awt.image.*;
import java.io.*;

/**
 * Unit test for OpenGL wrapper classes
 */
public class NonPOTCropTest extends TestCase {
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public NonPOTCropTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( NonPOTCropTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testNonPOTCrop() throws IOException, DNGReadException, CompressionDecoderException
    {
        // ### load test image
        // load file
        String pathToDNGFile = "src/test/resources/RAW-NIKON-D40-SRGB.dng";
        System.out.println("Opening: " + pathToDNGFile);
        File file = new File(pathToDNGFile);

        // generate RGB data
        System.out.println("-------------");
        System.out.println("Testing non-power-of-two crops:");

        DNGProcessor pipeline = new DNGProcessor(file);
        pipeline.setInterpolationMethod(InterpolationMethod.BILINEAR_MEDIAN);
        pipeline.setLoggingMethod(DNGProcessor.LoggingMethod.CONSOLE);
        BufferedImage processedImage = pipeline.process();
        DataBuffer bufBefore = TestUtils.extractDataBuffer(processedImage);

        pipeline.setAccelerationStrategy(AccelerationStrategy.GPU_OPERATION_WISE);
        processedImage = pipeline.process();
        DataBuffer bufAfter = TestUtils.extractDataBuffer(processedImage);

        System.out.println("Comparing result with CPU version:");
        assertTrue(TestUtils.compareDataBuffers(bufBefore, bufAfter, 2.5f));

    }
    
}
