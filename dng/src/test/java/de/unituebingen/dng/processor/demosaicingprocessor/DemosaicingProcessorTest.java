package de.unituebingen.dng.processor.demosaicingprocessor;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import de.unituebingen.TestUtils;
import de.unituebingen.dng.processor.Processor;
import de.unituebingen.dng.processor.demosaicingprocessor.DemosaicingProcessor.InterpolationMethod;
import de.unituebingen.dng.processor.log.NopLogger;
import de.unituebingen.dng.processor.util.AccelerationStrategy;
import de.unituebingen.dng.reader.DNGReadException;
import de.unituebingen.dng.reader.compression.CompressionDecoderException;
import de.unituebingen.dng.reader.dng.util.CFAPattern;

import java.awt.image.*;
import java.io.*;

public class DemosaicingProcessorTest
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public DemosaicingProcessorTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( DemosaicingProcessorTest.class );
    }

    public void testDemosaicingProcessor() throws IOException, DNGReadException, CompressionDecoderException
    {
        Processor<BufferedImage> processor;

        System.out.println("### No Acceleration");
        processor = new DemosaicingProcessor(InterpolationMethod.BILINEAR_MEAN, CFAPattern.RGGB, AccelerationStrategy.NONE, new NopLogger());
        assertTrue(TestUtils.testProcessor(processor, "preprocessed-rggb.png", "bilinear-mean.png", false));

        System.out.println("### Multithreading");
        processor = new DemosaicingProcessor(InterpolationMethod.BILINEAR_MEAN, CFAPattern.RGGB, AccelerationStrategy.MULTITHREADING, new NopLogger());
        assertTrue(TestUtils.testProcessor(processor, "preprocessed-rggb.png", "bilinear-mean.png", false));

        System.out.println("### CPU Tiling");
        processor = new DemosaicingProcessor(InterpolationMethod.BILINEAR_MEAN, CFAPattern.RGGB, AccelerationStrategy.CPU_TILING, new NopLogger());
        assertTrue(TestUtils.testProcessor(processor, "preprocessed-rggb.png", "bilinear-mean.png", false));

        System.out.println("### CPU Tiling + Multithreading");
        processor = new DemosaicingProcessor(InterpolationMethod.BILINEAR_MEAN, CFAPattern.RGGB, AccelerationStrategy.CPU_TILING_MT, new NopLogger());
        assertTrue(TestUtils.testProcessor(processor, "preprocessed-rggb.png", "bilinear-mean.png", false));

        System.out.println("### CPU MT + Tiling");
        processor = new DemosaicingProcessor(InterpolationMethod.BILINEAR_MEAN, CFAPattern.RGGB, AccelerationStrategy.CPU_MT_TILING, new NopLogger());
        assertTrue(TestUtils.testProcessor(processor, "preprocessed-rggb.png", "bilinear-mean.png", false));
        
        System.out.println("### CPU MT + Tiling MT");
        processor = new DemosaicingProcessor(InterpolationMethod.BILINEAR_MEAN, CFAPattern.RGGB, AccelerationStrategy.CPU_MT_TILING, new NopLogger());
        assertTrue(TestUtils.testProcessor(processor, "preprocessed-rggb.png", "bilinear-mean.png", false));

    }
}




