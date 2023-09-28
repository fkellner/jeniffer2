package de.unituebingen.dng.processor.demosaicingprocessor;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import de.unituebingen.TestUtils;
import de.unituebingen.dng.processor.Processor;
import de.unituebingen.dng.processor.demosaicingprocessor.DemosaicingProcessor.InterpolationMethod;
import de.unituebingen.dng.processor.log.NopLogger;
import de.unituebingen.dng.processor.util.AccelerationStrategy;
import de.unituebingen.dng.reader.dng.util.CFAPattern;

import java.awt.image.*;
import java.io.*;

public class DLMMSE_RCDTest
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public DLMMSE_RCDTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( DLMMSE_RCDTest.class );
    }

    public void testDLMMSE_RCD() throws IOException
    {
        Processor<BufferedImage> processor;

        System.out.println("### DLMMSE_RCD");
        // rewrites
        processor = new DemosaicingProcessor(InterpolationMethod.DLMMSE_RCD_CODE, CFAPattern.RGGB, AccelerationStrategy.CPU_TILING_MT, new NopLogger());
        assertTrue(TestUtils.testProcessor(processor, "preprocessed-rggb.png", "dlmmse_rcd_code.png", false));

        processor = new DemosaicingProcessor(InterpolationMethod.DLMMSE_RCD_PAPER, CFAPattern.RGGB, AccelerationStrategy.CPU_TILING_MT, new NopLogger());
        assertTrue(TestUtils.testProcessor(processor, "preprocessed-rggb.png", "dlmmse_rcd_paper.png", false));

        System.out.println("### DLMMSE_RCD MT");
        // rewrites
        processor = new DemosaicingProcessor(InterpolationMethod.DLMMSE_RCD_CODE, CFAPattern.RGGB, AccelerationStrategy.MULTITHREADING, new NopLogger());
        assertTrue(TestUtils.testProcessor(processor, "preprocessed-rggb.png", "dlmmse_rcd_code.png", false, 1.1f));

        processor = new DemosaicingProcessor(InterpolationMethod.DLMMSE_RCD_PAPER, CFAPattern.RGGB, AccelerationStrategy.MULTITHREADING, new NopLogger());
        assertTrue(TestUtils.testProcessor(processor, "preprocessed-rggb.png", "dlmmse_rcd_paper.png", false, 1.1f));
        
    }
}
