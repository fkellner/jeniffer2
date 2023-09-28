package de.unituebingen.dng.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.unituebingen.dng.processor.log.Timer;

public class Pipeline<T> {

    private final List<Processor<T>> pipes;

    private final Timer pTimer;

    public Pipeline(Timer timer) {
        this.pTimer = timer;
        this.pipes = new ArrayList<>();
    }

    public Pipeline(Timer timer, Processor<T>... pipes) {
        this.pTimer = timer;
        this.pipes = Arrays.asList(pipes);
    }

    public void add(Processor<T> pipe) {
        this.pipes.add(pipe);
    }

    public T process(T input) {
        T processed = input;

        for (Processor<T> pipe : pipes) {
            String processor = pipe.getClass().getSimpleName();
            pTimer.startTask(processor, "");
            if (pipe != null) {
                processed = pipe.process(processed);
            }
            pTimer.endTask(processor);
        }

        return processed;
    }
}
