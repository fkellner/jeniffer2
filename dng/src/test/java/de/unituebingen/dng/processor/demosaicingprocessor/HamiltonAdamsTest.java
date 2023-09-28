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

public class HamiltonAdamsTest
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public HamiltonAdamsTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( HamiltonAdamsTest.class );
    }

    public void testHamiltonAdams() throws IOException, DNGReadException, CompressionDecoderException
    {
        System.out.println("### HamiltonAdams");
        Processor<BufferedImage> processor = new DemosaicingProcessor(InterpolationMethod.HAMILTON_ADAMS, CFAPattern.RGGB, AccelerationStrategy.NONE, new NopLogger());
        assertTrue(TestUtils.testProcessor(processor, "preprocessed-rggb.png", "hamilton-adams.png", false));

        System.out.println("### HamiltonAdams MT");
        processor = new DemosaicingProcessor(InterpolationMethod.HAMILTON_ADAMS, CFAPattern.RGGB, AccelerationStrategy.MULTITHREADING, new NopLogger());
        assertTrue(TestUtils.testProcessor(processor, "preprocessed-rggb.png", "hamilton-adams.png", false));
        
    }
}



