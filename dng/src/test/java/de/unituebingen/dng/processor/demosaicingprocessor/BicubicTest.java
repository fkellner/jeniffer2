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

public class BicubicTest
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public BicubicTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( BicubicTest.class );
    }

    public void testBicubic() throws IOException, DNGReadException, CompressionDecoderException
    {
        System.out.println("### Bicubic");
        Processor<BufferedImage> processor = new DemosaicingProcessor(InterpolationMethod.BICUBIC, CFAPattern.RGGB, AccelerationStrategy.NONE, new NopLogger());
        assertTrue(TestUtils.testProcessor(processor, "preprocessed-rggb.png", "bicubic.png", false));

        System.out.println("### Bicubic MT");
        processor = new DemosaicingProcessor(InterpolationMethod.BICUBIC, CFAPattern.RGGB, AccelerationStrategy.MULTITHREADING, new NopLogger());
        assertTrue(TestUtils.testProcessor(processor, "preprocessed-rggb.png", "bicubic.png", false));
    }
}



