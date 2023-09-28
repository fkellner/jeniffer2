package de.unituebingen.dng.processor.preprocessor;

import de.unituebingen.dng.processor.Operation;
import de.unituebingen.dng.reader.ImageFileDirectory;

public abstract class PreProcessorOperation extends Operation {
    protected ImageFileDirectory highResolutionIFD;
    protected ImageFileDirectory baselineIFD;

    public PreProcessorOperation(ImageFileDirectory baselineIFD, ImageFileDirectory highResolutionIFD) {
        super((int) highResolutionIFD.getImageWidth(), (int) highResolutionIFD.getImageLength());
        this.highResolutionIFD = highResolutionIFD;
        this.baselineIFD = baselineIFD;
    }

    public abstract int process(int sample, int index);
}
