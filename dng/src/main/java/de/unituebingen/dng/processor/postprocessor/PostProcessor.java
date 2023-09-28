package de.unituebingen.dng.processor.postprocessor;

import de.unituebingen.dng.processor.Processor;
import de.unituebingen.dng.processor.log.Timer;
import de.unituebingen.dng.reader.util.Math;
import de.unituebingen.dng.processor.util.AccelerationStrategy;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferUShort;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class PostProcessor implements Processor<BufferedImage> {

    private List<PostProcessorOperation> operations;

    private AccelerationStrategy accelerationStrategy = AccelerationStrategy.NONE;

    private Timer pTimer;

    public PostProcessor(Timer timer, PostProcessorOperation... operations) {
        this.pTimer = timer;
        this.operations = new ArrayList<>(Arrays.asList(operations));
    }

    @Override
    public BufferedImage process(BufferedImage bufferedImage) {

        short[] samples = ((DataBufferUShort) bufferedImage.getRaster().getDataBuffer()).getData();
        int length = samples.length;
        int BASE_SIZE;
        if(accelerationStrategy == AccelerationStrategy.AUTO_BIG) {
            BASE_SIZE = 32;
        } else {
            try {
                BASE_SIZE = Integer.parseInt(System.getenv("TILE_SIZE"));
            } catch(java.lang.NumberFormatException e) {
                // default, if env var is not set
                BASE_SIZE = 256;
            }
        }
        final int TILE_SIZE = BASE_SIZE * BASE_SIZE;
        final int pixels = (length / 3);
        final int num_tiles = (pixels / TILE_SIZE) + 1;

        String taskName = "Postprocessing";
        pTimer.startTask(taskName, num_tiles + " tiles of size " + TILE_SIZE);
        switch (accelerationStrategy) {
            case AUTO_SMALL:
            case MULTITHREADING:
                IntStream.range(0, pixels).parallel().forEach(i -> {
                    double sampleR = (samples[i * 3] & 0xFFFF) / 65535.0;
                    double sampleG = (samples[i * 3 + 1] & 0xFFFF) / 65535.0;
                    double sampleB = (samples[i * 3 + 2] & 0xFFFF) / 65535.0;
                    for (PostProcessorOperation operation : operations) {
                        double[] processedSample = operation.process(sampleR, sampleG, sampleB);
                        sampleR = processedSample[0];
                        sampleG = processedSample[1];
                        sampleB = processedSample[2];
                    }
                    samples[i * 3] = (short)Math.in(0, sampleR * 65535, 65535);
                    samples[i * 3 + 1] = (short)Math.in(0, sampleG * 65535, 65535);
                    samples[i * 3 + 2] = (short)Math.in(0, sampleB * 65535, 65535);
                });
                break;
            case NONE:
                for(int i = 0; i < pixels; i++) {
                    double sampleR = (samples[i * 3] & 0xFFFF) / 65535.0;
                    double sampleG = (samples[i * 3 + 1] & 0xFFFF) / 65535.0;
                    double sampleB = (samples[i * 3 + 2] & 0xFFFF) / 65535.0;
                    for (PostProcessorOperation operation : operations) {
                        double[] processedSample = operation.process(sampleR, sampleG, sampleB);
                        sampleR = processedSample[0];
                        sampleG = processedSample[1];
                        sampleB = processedSample[2];
                    }
                    samples[i * 3] = (short)Math.in(0, sampleR * 65535, 65535);
                    samples[i * 3 + 1] = (short)Math.in(0, sampleG * 65535, 65535);
                    samples[i * 3 + 2] = (short)Math.in(0, sampleB * 65535, 65535);
                }
                break;
            case CPU_TILING:
                for(int tile = 0; tile < num_tiles; tile++) {
                    for(int i = tile * TILE_SIZE; i < java.lang.Math.min(pixels, (tile + 1) * TILE_SIZE); i++) {
                        double sampleR = (samples[i * 3] & 0xFFFF) / 65535.0;
                        double sampleG = (samples[i * 3 + 1] & 0xFFFF) / 65535.0;
                        double sampleB = (samples[i * 3 + 2] & 0xFFFF) / 65535.0;
                        for (PostProcessorOperation operation : operations) {
                            double[] processedSample = operation.process(sampleR, sampleG, sampleB);
                            sampleR = processedSample[0];
                            sampleG = processedSample[1];
                            sampleB = processedSample[2];
                        }
                        samples[i * 3] = (short)Math.in(0, sampleR * 65535, 65535);
                        samples[i * 3 + 1] = (short)Math.in(0, sampleG * 65535, 65535);
                        samples[i * 3 + 2] = (short)Math.in(0, sampleB * 65535, 65535);
                    }
                }
                break;
            case CPU_MT_TILING:
                for(int tile = 0; tile < num_tiles; tile++) {
                    IntStream.range(tile * TILE_SIZE, java.lang.Math.min(pixels, (tile + 1) * TILE_SIZE)).parallel().forEach(i -> {
                        double sampleR = (samples[i * 3] & 0xFFFF) / 65535.0;
                        double sampleG = (samples[i * 3 + 1] & 0xFFFF) / 65535.0;
                        double sampleB = (samples[i * 3 + 2] & 0xFFFF) / 65535.0;
                        for (PostProcessorOperation operation : operations) {
                            double[] processedSample = operation.process(sampleR, sampleG, sampleB);
                            sampleR = processedSample[0];
                            sampleG = processedSample[1];
                            sampleB = processedSample[2];
                        }
                        samples[i * 3] = (short)Math.in(0, sampleR * 65535, 65535);
                        samples[i * 3 + 1] = (short)Math.in(0, sampleG * 65535, 65535);
                        samples[i * 3 + 2] = (short)Math.in(0, sampleB * 65535, 65535);
                    });
                }
                break;
            case GPU_OPERATION_WISE:
            case GPU_TILE_WISE:
            case CPU_TILING_MT:
                IntStream.range(0, num_tiles).parallel().forEach(tile -> {
                    for(int i = tile * TILE_SIZE; i < java.lang.Math.min(pixels, (tile + 1) * TILE_SIZE); i++) {
                        double sampleR = (samples[i * 3] & 0xFFFF) / 65535.0;
                        double sampleG = (samples[i * 3 + 1] & 0xFFFF) / 65535.0;
                        double sampleB = (samples[i * 3 + 2] & 0xFFFF) / 65535.0;
                        for (PostProcessorOperation operation : operations) {
                            double[] processedSample = operation.process(sampleR, sampleG, sampleB);
                            sampleR = processedSample[0];
                            sampleG = processedSample[1];
                            sampleB = processedSample[2];
                        }
                        samples[i * 3] = (short)Math.in(0, sampleR * 65535, 65535);
                        samples[i * 3 + 1] = (short)Math.in(0, sampleG * 65535, 65535);
                        samples[i * 3 + 2] = (short)Math.in(0, sampleB * 65535, 65535);
                    }
                });
                break;
            case AUTO_BIG:
            case CPU_MT_TILING_MT:
                IntStream.range(0, num_tiles).parallel().forEach(tile -> {
                    IntStream.range(tile * TILE_SIZE, java.lang.Math.min(pixels, (tile + 1) * TILE_SIZE)).parallel().forEach(i -> {
                        double sampleR = (samples[i * 3] & 0xFFFF) / 65535.0;
                        double sampleG = (samples[i * 3 + 1] & 0xFFFF) / 65535.0;
                        double sampleB = (samples[i * 3 + 2] & 0xFFFF) / 65535.0;
                        for (PostProcessorOperation operation : operations) {
                            double[] processedSample = operation.process(sampleR, sampleG, sampleB);
                            sampleR = processedSample[0];
                            sampleG = processedSample[1];
                            sampleB = processedSample[2];
                        }
                        samples[i * 3] = (short)Math.in(0, sampleR * 65535, 65535);
                        samples[i * 3 + 1] = (short)Math.in(0, sampleG * 65535, 65535);
                        samples[i * 3 + 2] = (short)Math.in(0, sampleB * 65535, 65535);
                    });
                });
                break;

        }
        pTimer.endTask(taskName);

        return bufferedImage;
    }

    public void addOperation(PostProcessorOperation op) {
        this.operations.add(op);
    }

    public void setAccelerationStrategy(AccelerationStrategy accelerationStrategy) {
        this.accelerationStrategy = accelerationStrategy;
    }
}
