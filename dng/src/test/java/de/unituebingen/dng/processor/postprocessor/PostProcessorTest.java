package de.unituebingen.dng.processor.postprocessor;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import de.unituebingen.TestUtils;
import de.unituebingen.dng.processor.Processor;
import de.unituebingen.dng.processor.log.NopLogger;
import de.unituebingen.dng.processor.util.AccelerationStrategy;
import de.unituebingen.dng.reader.DNGFile;
import de.unituebingen.dng.reader.DNGReadException;
import de.unituebingen.dng.reader.ImageFileDirectory;
import de.unituebingen.dng.reader.compression.CompressionDecoderException;

import java.awt.image.*;
import java.io.*;

public class PostProcessorTest
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public PostProcessorTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( PostProcessorTest.class );
    }

    public void testPostProcessor() throws IOException, DNGReadException, CompressionDecoderException
    {
        System.out.println("### PostProcessor");

        // ### load test image
        // load file
        String pathToDNGFile = "src/test/resources/RAW-CANON-S30.dng";
        System.out.println("Opening: " + pathToDNGFile);
        File file = new File(pathToDNGFile);

        // create DNG file
        DNGFile dngFile = new DNGFile(file);
        ImageFileDirectory hrIFD = dngFile.getRAWImageFileDirectory();
        ImageFileDirectory ifd0 = dngFile.get0thImageFileDirectory();
        int width = (int) hrIFD.getImageWidth();
        int height = (int) hrIFD.getImageLength();

        // build postprocessor for this image specifically
        System.out.println("Testing ColorSpaceTransformation, ExposureCorrection XYZD50ToSRGBMapping");

        Processor<BufferedImage> postProcessor = new PostProcessor(new NopLogger(), new ColorSpaceTransformation(width, height, ifd0));
        ((PostProcessor) postProcessor).addOperation(new ExposureCorrection(width, height, ifd0));
        ((PostProcessor) postProcessor).addOperation(new XYZD50ToSRGBMapping(width, height));

        System.out.println("With NONE");
        ((PostProcessor) postProcessor).setAccelerationStrategy(AccelerationStrategy.NONE);        
        assertTrue(TestUtils.testProcessor(postProcessor, "rcd.png", "rcd-processed.png", false));

        System.out.println("With MULTITHREADING");
        ((PostProcessor) postProcessor).setAccelerationStrategy(AccelerationStrategy.MULTITHREADING);        
        assertTrue(TestUtils.testProcessor(postProcessor, "rcd.png", "rcd-processed.png", false));

        System.out.println("With CPU_TILING");
        ((PostProcessor) postProcessor).setAccelerationStrategy(AccelerationStrategy.CPU_TILING);        
        assertTrue(TestUtils.testProcessor(postProcessor, "rcd.png", "rcd-processed.png", false));

        System.out.println("With CPU_MT_TILING");
        ((PostProcessor) postProcessor).setAccelerationStrategy(AccelerationStrategy.CPU_TILING);        
        assertTrue(TestUtils.testProcessor(postProcessor, "rcd.png", "rcd-processed.png", false));
        
        System.out.println("With CPU_TILING_MT");
        ((PostProcessor) postProcessor).setAccelerationStrategy(AccelerationStrategy.CPU_TILING_MT);        
        assertTrue(TestUtils.testProcessor(postProcessor, "rcd.png", "rcd-processed.png", false));

        System.out.println("With CPU_MT_TILING_MT");
        ((PostProcessor) postProcessor).setAccelerationStrategy(AccelerationStrategy.CPU_MT_TILING_MT);        
        assertTrue(TestUtils.testProcessor(postProcessor, "rcd.png", "rcd-processed.png", false));
        
    }
}
