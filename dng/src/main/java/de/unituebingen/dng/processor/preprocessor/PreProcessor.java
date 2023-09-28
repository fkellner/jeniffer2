package de.unituebingen.dng.processor.preprocessor;

import de.unituebingen.dng.processor.Processor;
import de.unituebingen.dng.processor.log.Timer;
import de.unituebingen.dng.processor.util.AccelerationStrategy;
// import uk.ac.manchester.tornado.api.ImmutableTaskGraph;
// import uk.ac.manchester.tornado.api.TaskGraph;
// import uk.ac.manchester.tornado.api.TornadoExecutionPlan;
// import uk.ac.manchester.tornado.api.TornadoExecutionResult;
// import uk.ac.manchester.tornado.api.annotations.Parallel;
// import uk.ac.manchester.tornado.api.enums.DataTransferMode;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferUShort;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class PreProcessor implements Processor<BufferedImage> {

    private List<PreProcessorOperation> operations;

    private AccelerationStrategy accelerationStrategy = AccelerationStrategy.NONE;

    private Timer pTimer;

    public PreProcessor(Timer timer, PreProcessorOperation... operations) {
        this.pTimer = timer;
        this.operations = new ArrayList<>(Arrays.asList(operations));
    }

    @Override
    public BufferedImage process(BufferedImage bufferedImage) {
        short[] samples = ((DataBufferUShort) bufferedImage.getRaster().getDataBuffer()).getData();

        int BASE_SIZE;
        if(accelerationStrategy == AccelerationStrategy.AUTO_SMALL) {
            BASE_SIZE = 1024;
        } else {
            try {
                BASE_SIZE = Integer.parseInt(System.getenv("TILE_SIZE"));
            } catch(java.lang.NumberFormatException e) {
                // default, if env var is not set
                BASE_SIZE = 256;
            }
        }
        final int TILE_SIZE = BASE_SIZE * BASE_SIZE;
        int num_tiles = (samples.length / TILE_SIZE) + 1; // safely assume we are not divisible by TILE_SIZE
        String taskName = "Preprocessing";
        pTimer.startTask(taskName, num_tiles + " tiles of size " + TILE_SIZE);
        switch(accelerationStrategy) {
            case AUTO_SMALL:
            case CPU_TILING_MT:
                IntStream.range(0, num_tiles).parallel().forEach(tile -> {
                    for(int i = tile * TILE_SIZE; i < Math.min(samples.length, (tile + 1) * TILE_SIZE); i++) {
                        for (PreProcessorOperation operation : operations) {
                            samples[i] = (short) operation.process(samples[i] & 0xFFFF, i);
                        }
                    }
                });
                break;
            case CPU_TILING:
                for(int tile = 0; tile < num_tiles; tile++) {
                    for(int i = tile * TILE_SIZE; i < Math.min(samples.length, (tile + 1) * TILE_SIZE); i++) {
                        for (PreProcessorOperation operation : operations) {
                            samples[i] = (short) operation.process(samples[i] & 0xFFFF, i);
                        }
                    }
                }
                break;
            case CPU_MT_TILING:
                for(int tile = 0; tile < num_tiles; tile++) {
                    IntStream.range(tile * TILE_SIZE, java.lang.Math.min(samples.length, (tile + 1) * TILE_SIZE)).parallel().forEach(i -> {
                        for (PreProcessorOperation operation : operations) {
                            samples[i] = (short) operation.process(samples[i] & 0xFFFF, i);
                        }
                    });
                }
                break;
            case CPU_MT_TILING_MT:
                IntStream.range(0, num_tiles).parallel().forEach(tile -> {
                    IntStream.range(tile * TILE_SIZE, java.lang.Math.min(samples.length, (tile + 1) * TILE_SIZE)).parallel().forEach(i -> {
                        for (PreProcessorOperation operation : operations) {
                            samples[i] = (short) operation.process(samples[i] & 0xFFFF, i);
                        }
                    });
                });
                break;
            case GPU_OPERATION_WISE:
            case GPU_TILE_WISE:
                System.out.println("Not yet implemented on GPU, defaulting to MULTITHREADING");
            case AUTO_BIG:
            case MULTITHREADING:
                IntStream.range(0, samples.length).parallel().forEach(i -> {
                    for (PreProcessorOperation operation : operations) {
                        samples[i] = (short) operation.process(samples[i] & 0xFFFF, i);
                    }
                });
                break;
            case NONE:
                for(int i = 0; i < samples.length; i++) {
                    for (PreProcessorOperation operation : operations) {
                        samples[i] = (short) operation.process(samples[i] & 0xFFFF, i);
                    }
                }
                break;                

        }
        pTimer.endTask(taskName);

        return bufferedImage;
    }

    public void addOperation(PreProcessorOperation operation) {
        this.operations.add(operation);
    }

    public void setAccelerationStrategy(AccelerationStrategy accelerationStrategy) {
        this.accelerationStrategy = accelerationStrategy;
    }

}
