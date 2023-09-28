package de.unituebingen.jeniffer2.cli;



import de.unituebingen.dng.DNGProcessor;
import de.unituebingen.dng.DNGProcessor.LoggingMethod;
import de.unituebingen.dng.reader.DNGFile;
import de.unituebingen.dng.reader.DNGReadException;
import de.unituebingen.dng.reader.compression.CompressionDecoderException;
import de.unituebingen.dng.reader.dng.util.CFAPattern;
import de.unituebingen.imageprocessor.ImageExporter;
import de.unituebingen.dng.processor.demosaicingprocessor.DemosaicingProcessor.InterpolationMethod;
import de.unituebingen.dng.processor.util.AccelerationStrategy;

import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

public class Main {
    private static String filePath = "test.dng";

    private static String outputPath;

    private static InterpolationMethod interpolationMethod = InterpolationMethod.RCD;

    private static AccelerationStrategy accelerationStrategy = AccelerationStrategy.GPU_TILE_WISE;

    private static LoggingMethod loggingMethod = LoggingMethod.CSV_AND_CONSOLE;

    public static void main(String[] args) throws IOException, CompressionDecoderException, DNGReadException {
        String receiving = "";
        String subStep = "";
        CFAPattern pattern = null;
        boolean printMargin = false;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--help":
                    printHelp();
                    System.exit(0);
                case "--output":
                case "-o":
                    receiving = "output";
                    continue;
                case "--interpolation":
                case "-i":
                    receiving = "interpolation";
                    continue;
                case "--acceleration":
                case "-a":
                    receiving = "acceleration";
                    continue;
                case "--log":
                case "-l":
                    receiving = "log";
                    continue;
                case "--substep":
                case "-s":
                    receiving = "substep";
                    continue;
                case "--pattern":
                case "-p":
                    receiving = "pattern";
                    continue;
                case "--printMargin":
                case "-m":
                    printMargin = true;
                    continue;
            }
            switch (receiving) {
                case "":
                    filePath = args[i];
                    continue;
                case "output":
                    outputPath = args[i];
                    continue;
                case "interpolation":
                    try {
                        interpolationMethod = InterpolationMethod.valueOf(args[i]);
                    } catch (IllegalArgumentException e) {
                        System.out.println(args[i] + " is no valid Interpolation Method" +
                                ", staying with previously specified or default " + interpolationMethod.toString());
                    }
                    receiving = "";
                    continue;
                case "acceleration":
                    try {
                        accelerationStrategy = AccelerationStrategy.valueOf(args[i]);
                    } catch (IllegalArgumentException e) {
                        System.out.println(args[i] + " is no valid acceleration strategy" +
                                ", staying with previously specified or default " + accelerationStrategy.toString());
                    }
                    receiving = "";
                    continue;
                case "log":
                    try {
                        loggingMethod = LoggingMethod.valueOf(args[i]);
                    } catch (IllegalArgumentException e) {
                        System.out.println(args[i] + " is no valid logger" +
                                ", staying with previously specified or default " + loggingMethod.toString());
                    }
                    receiving = "";
                    continue;
                case "substep":
                    try {
                        subStep = DNGProcessor.getAvailableSubsteps(interpolationMethod, accelerationStrategy)[Integer.parseInt(args[i])];
                    } catch (Exception e) {
                        System.out.println(subStep + " is not an available substep index");
                    }
                    receiving = ""; 
                    continue;                
                case "pattern":
                    try {
                        pattern = CFAPattern.valueOf(args[i]);
                    } catch (IllegalArgumentException e) {
                        System.out.println(args[i] + " is no valid CFA Pattern. Available patterns are:");
                        CFAPattern[] patterns = CFAPattern.values();
                        String available = "";
                        for(int p = 0; p < patterns.length; p++) available += patterns[p].toString() + " ";
                        System.out.println(available);
                        System.exit(1);
                    }
                    receiving = "";
                    continue;
            }
        }
        if(printMargin) {
            System.out.println(DNGProcessor.getMargin(interpolationMethod));
            System.exit(0);
        }
        if(filePath.endsWith(".png")) {
            // only perform Demosaicing
            File srcFile = new File(filePath);
            BufferedImage img = ImageIO.read(srcFile);
            if(pattern == null) {
                System.out.println("Need a CFA pattern to demosaic PNG image (give with e.g. --pattern RGGB )");
                System.exit(1);
            }
            BufferedImage result = DNGProcessor.performDemosaicing(
                img, interpolationMethod, accelerationStrategy, pattern, loggingMethod 
            );
            if(outputPath == null) {
                outputPath = "";
                String[] parts = filePath.split(Pattern.quote("."));
                for (int i = 0; i < parts.length - 1; i++) {
                    outputPath += parts[i];
                    if(i < parts.length - 2) outputPath += ".";
                }
                outputPath += interpolationMethod.toString() + ".png";
            }
            File outFile = new File(outputPath);
            ImageIO.write(result, "png", outFile);
            System.exit(0);
        }

        System.out.println("Opening " + filePath);
        DNGFile tiffReader = new DNGFile(filePath);
        System.out.println("Building Facade");
        DNGProcessor pipeline = new DNGProcessor(tiffReader.getFile());
        pipeline.setInterpolationMethod(interpolationMethod);
        pipeline.setAccelerationStrategy(accelerationStrategy);
        pipeline.setLoggingMethod(loggingMethod);
        if(subStep != "") pipeline.setSubstep(subStep);
        System.out.println("Processing Image");
        BufferedImage image = pipeline.process();
        // construct suitable output path if not given
        if (outputPath == null) {
            outputPath = "";
            String[] parts = filePath.split(Pattern.quote("."));
            for (int i = 0; i < parts.length - 1; i++) {
                outputPath += parts[i];
                outputPath += ".";
            }
            outputPath += "tiff";
        }
        if (!outputPath.endsWith(".tiff")) {
            outputPath += ".tiff";
        }
        System.out.println("Saving result to " + outputPath);
        ImageExporter.saveAsTIFF(ImageExporter.create8BitBufferedImage(image), new File(outputPath));

        System.out.println("--- Done");
    }
    
    private static void printHelp() {
        InterpolationMethod[] methods = InterpolationMethod.values();
        String methodsString = "";
        for (int i = 0; i < methods.length; i++) {
            methodsString += methods[i].toString() + " ";
        }
        AccelerationStrategy[] strategies = AccelerationStrategy.values();
        String strategiesString = "";
        for (int i = 0; i < strategies.length; i++) {
            strategiesString += strategies[i].toString() + " ";
        }
        LoggingMethod[] loggers = LoggingMethod.values();
        String loggersString = "";
        for (int i = 0; i < loggers.length; i++) {
            loggersString += loggers[i].toString() + " ";
        }
        String subStepsString = "";
        for(int i = 0; i < strategies.length; i++) {
            for(int j = 0; j < methods.length; j++) {
                String[] subSteps = DNGProcessor.getAvailableSubsteps(methods[j], strategies[i]);
                if(subSteps.length == 0) continue;
                subStepsString += strategies[i].toString() + " " + methods[j].toString() + ":\n";
                for(int k = 0; k < subSteps.length; k++) {
                    subStepsString += k + ": " + subSteps[k] + "\n";
                }
            }
        }
        CFAPattern[] patterns = CFAPattern.values();
        String patternsString = "";
        for(int p = 0; p < patterns.length; p++) patternsString += patterns[p].toString() + " ";
        System.out.println("""
            Process a DNG raw image file into a TIFF file or
            Demosaic a monochrome PNG file into an RGB PNG file
            Usage: CMD [<path-to-file>] [OPTIONS]
            CMD:
                - Linux and Windows: java -jar Jeniffer2-Cli-1.1-jar-with-Dependencies.jar
                - MacOS: java -XStartOnFirstThread -jar Jeniffer2-Cli-1.1-jar-with-Dependencies.jar
            <path-to-dng-file>:
                Default is 'test.dng'
            OPTIONS:
                --output FILEPATH
                -o FILEPATH
                    default: Dng File path with extension changed to .tiff or PNG file path with -<METHOD> added to file name
                --interpolation METHOD
                -i METHOD
                    Where METHOD is one of:
                            """ + methodsString + """
                
                --printMargin
                -m
                    Only print the margin in pixels that is influenced by the edge handling strategy, depending 
                    on the interpolation method, and exit

                --acceleration STRATEGY
                -a STRATEGY
                    Where STRATEGY is one of:
                        """ + strategiesString + """
                
                --log LOGGER
                -l LOGGER
                    Where LOGGER is one of:
                        """ + loggersString + """
                
                --substep STEP
                -s STEP
                    Where STEP is a numbered option whose availability depends
                    on the chosen interpolation method and acceleration strategy:
                    """ + subStepsString + """

                --pattern PATTERN
                -p PATTERN
                    CFA pattern to be used for demosaicing a PNG file
                    """ + patternsString + """
            """);

    }
}
