package de.unituebingen;

import java.awt.image.*;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import de.unituebingen.dng.processor.Processor;

public class TestUtils {
      // compare if the content of two data buffers is exactly the same
      public static boolean compareDataBuffers(DataBuffer buf0, DataBuffer buf1, float epsilon) {
        return compareDataBuffers(buf0, buf1, epsilon, false);
      }

      public static boolean compareDataBuffers(DataBuffer buf0, DataBuffer buf1, float epsilon, boolean printdiffering) {
        if (buf0.getSize() != buf1.getSize()) {
            System.out.println("Different size");
            return false;
        }
        int differingValues = 0;
        double sumDifferences = 0;
        double squareError = 0;
        int maxDifference = 0;
        for (int i = 0; i < buf0.getSize(); i++) {
            if (buf0.getElem(i) != buf1.getElem(i)) {
                if(printdiffering) {
                    System.out.println("Pos: " + i + " Diff: " + buf0.getElem(i) + " vs " + buf1.getElem(i));
                }
                differingValues++;
                int difference = Math.abs(buf0.getElem(i) - buf1.getElem(i));
                sumDifferences += difference;
                squareError += difference * difference;
                if(difference > maxDifference) maxDifference = difference;
            }
        }
        double avgDifference = sumDifferences / (double) differingValues;
        double meanSquareError = squareError / buf0.getSize();
        float percentDiffering = ((float) differingValues / buf0.getSize()) * 100;
        if (Math.abs(avgDifference) > epsilon) {
            System.out.println("!! Found " + differingValues + " differing Values !!");
            System.out.println("Values that differ: " + percentDiffering + "%");
            System.out.println("Average difference: " + avgDifference + "(" + (avgDifference / 65535) * 100 + "%)");
            System.out.println("Maximum difference: " + maxDifference + "(" + ((float) maxDifference / 65535) * 100 + "%)");
            System.out.println("MSE: " + meanSquareError);
            return false;
        }
        if (differingValues > 0) {
            System.out.println("Found " + differingValues + " differing Values,");
            System.out.println("but average Difference " + avgDifference + "(" + (avgDifference / 65535) * 100 + "%)" + " is smaller than " + epsilon);
            System.out.println("Values that differ: " + percentDiffering + "%");
            System.out.println("Maximum difference: " + maxDifference + "(" + ((float) maxDifference / 65535) * 100 + "%)");
            System.out.println("MSE: " + meanSquareError);
        } else {
            System.out.println("All values match");
        }
        return true;
    }

    public static void differenceHistogram(DataBuffer buf0, DataBuffer buf1, int steppow) {
        if (buf0.getSize() != buf1.getSize()) {
            System.out.println("Different size");
            return;
        }
        int stepsize = (int)Math.pow(2, 16 - steppow);
        for(int min = 0; min < 65535; min += stepsize) {
            int numValues = 0;
            int differingValues = 0;
            double sumDifferences = 0;
            double squareError = 0;
            int maxDifference = 0;
            for (int i = 0; i < buf0.getSize(); i++) {
                int elem0 = buf0.getElem(i);
                if(elem0 < min || min + stepsize <= elem0) continue;
                numValues++;
                int elem1 = buf1.getElem(i);
                if(elem1 != elem0) {
                    differingValues++;
                    int diff = elem0 - elem1;
                    sumDifferences += diff;
                    squareError += diff * diff;
                    if(Math.abs(diff) > maxDifference) maxDifference = Math.abs(diff);
                }
            }
            float percentValues = (float)((int)(((float) numValues / buf0.getSize()) * 10000)) / 100;
            double avgDifference = sumDifferences / (double) differingValues;
            double meanSquareError = squareError / numValues;
            float percentDiffering = (float)((int)(((float) differingValues / numValues) * 10000)) / 100;
            System.out.println(min + "\t" + (min + stepsize) + "\t: " + percentValues + "%, " + percentDiffering + "% diff" + ", maxDiff:" + maxDifference + ", mse: " + meanSquareError + ", avgDiff: " + avgDifference);

        }
    }
      
    // create a deep copy of the DataBufferUShort underlying a BufferedImage
    // WARNING: does not check if the image in fact does use a DataBufferUShort
    public static DataBufferUShort extractDataBuffer(BufferedImage img) {
        DataBufferUShort buf = (DataBufferUShort) img.getData().getDataBuffer();
        DataBufferUShort bufCopy = new DataBufferUShort(buf.getSize());
        for(int i = 0; i < buf.getSize(); i++) {
        bufCopy.setElem(i, buf.getElem(i));
        }
        return bufCopy;
    }

    public static boolean testProcessor(Processor<BufferedImage> processor, String srcFile, String compareFile, boolean generateTruth) throws IOException {
        return testProcessor(processor, srcFile, compareFile, generateTruth, 0);
    } 

    public static boolean testProcessor(Processor<BufferedImage> processor, String srcFileName, String compareFileName, boolean generateTruth, float epsilon) throws IOException {
        // load preprocessed image
        String srcPath = "src/test/resources/" + srcFileName;
        File srcFile = new File(srcPath);
        BufferedImage img = ImageIO.read(srcFile);

        // apply processor
        long start = System.currentTimeMillis();
        BufferedImage result = processor.process(img);
        long end = System.currentTimeMillis();
        System.out.println((end - start) + "ms (" + processor.getClass().getSimpleName() + ")");

        String comparePath = "src/test/resources/" + compareFileName;
        File compareFile = new File(comparePath);  
        if(generateTruth) {
            // Writing to file taking type and path as
            ImageIO.write(result, "png", compareFile);
            return true;
        }
        DataBufferUShort resultBuf = extractDataBuffer(result);
        BufferedImage truth = ImageIO.read(compareFile);
        DataBufferUShort truthBuf = extractDataBuffer(truth);
        if(!compareDataBuffers(resultBuf, truthBuf, epsilon)) {
            differenceHistogram(resultBuf, truthBuf, 4);
            return false;
        }
        return true;     
    }
}
