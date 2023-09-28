package de.unituebingen.jeniffer2.util;

import de.unituebingen.dng.processor.demosaicingprocessor.DemosaicingProcessor.InterpolationMethod;
import de.unituebingen.dng.processor.util.AccelerationStrategy;

public record PipelineConfiguration(
    InterpolationMethod interpolationMethod,
    AccelerationStrategy accelerationStrategy,
    String subStep
) {
        @Override
        public String toString() {
            return interpolationMethod.toString() + ", " + accelerationStrategy.toString() + 
                    (subStep != null && subStep != "" ? ", " + subStep : "");
        }
}
