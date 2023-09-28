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

public class DLMMSETest
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public DLMMSETest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( DLMMSETest.class );
    }

    public void testDLMMSE() throws IOException, DNGReadException, CompressionDecoderException
    {
        System.out.println("### DLMMSE");
        // edition with code
        Processor<BufferedImage> processor = new DemosaicingProcessor(InterpolationMethod.DLMMSE_CODE, CFAPattern.RGGB, AccelerationStrategy.CPU_TILING_MT, new NopLogger());
        assertTrue(TestUtils.testProcessor(processor, "preprocessed-rggb.png", "dlmmse_code.png", false));

        processor = new DemosaicingProcessor(InterpolationMethod.DLMMSE_PAPER, CFAPattern.RGGB, AccelerationStrategy.CPU_TILING_MT, new NopLogger());
        assertTrue(TestUtils.testProcessor(processor, "preprocessed-rggb.png", "dlmmse_paper.png", false));

        System.out.println("### DLMMSE MT");
        // edition with code
        processor = new DemosaicingProcessor(InterpolationMethod.DLMMSE_CODE, CFAPattern.RGGB, AccelerationStrategy.MULTITHREADING, new NopLogger());
        assertTrue(TestUtils.testProcessor(processor, "preprocessed-rggb.png", "dlmmse_code.png", false));

        processor = new DemosaicingProcessor(InterpolationMethod.DLMMSE_PAPER, CFAPattern.RGGB, AccelerationStrategy.MULTITHREADING, new NopLogger());
        assertTrue(TestUtils.testProcessor(processor, "preprocessed-rggb.png", "dlmmse_paper.png", false));
        
    }
}

