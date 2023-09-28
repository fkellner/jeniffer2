package de.unituebingen.dng.processor.postprocessor;

import de.unituebingen.dng.processor.Operation;

public abstract class PostProcessorOperation extends Operation {

    public PostProcessorOperation(int width, int height) {
        super(width, height);
    }

    public abstract int[] process(int sampleR, int sampleG, int sampleB, int index);

    public abstract double[] process(double sampleR, double sampleG, double sampleB);

    public abstract String fragmentShader();
}
