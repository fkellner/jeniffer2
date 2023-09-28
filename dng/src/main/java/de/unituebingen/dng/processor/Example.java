package de.unituebingen.dng.processor;

import de.unituebingen.dng.DNGProcessor;
import de.unituebingen.dng.processor.demosaicingprocessor.DemosaicingProcessor.InterpolationMethod;
//import de.unituebingen.dng.reader.ImageFileDirectory;
import de.unituebingen.dng.reader.DNGReadException;
//import de.unituebingen.dng.reader.DNGFile;
import de.unituebingen.dng.reader.compression.CompressionDecoderException;
import de.unituebingen.imageprocessor.ImageExporter;

import java.awt.image.*;
import java.io.*;

public class Example {

    public static void main(String args[]) throws IOException, DNGReadException, CompressionDecoderException {

        String pathToDNGFile = "PATH TO THE DNG FILE";
        File file = new File(pathToDNGFile);
        //DNGFile dngFile = new DNGFile(file);
        //ImageFileDirectory ifd = dngFile.get0thImageFileDirectory();
        //ImageFileDirectory rawIFD = dngFile.getRAWImageFileDirectory();
        //ImageFileDirectory exifIFD = dngFile.getExifImageFileDirectory();

        DNGProcessor pipeline = new DNGProcessor(file);
        pipeline.setInterpolationMethod(InterpolationMethod.BICUBIC);
        BufferedImage processedImage = pipeline.process();

        String pathToTIFFFile = "PATH TO TIFF FILE";
        ImageExporter.saveAsTIFF(processedImage, new File(pathToTIFFFile));
    }
}