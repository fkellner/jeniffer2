package de.unituebingen.dng.processor.postprocessor;

import de.unituebingen.dng.reader.ImageFileDirectory;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

public class ProfileToneCurveMapping extends PostProcessorOperation {

    private PolynomialSplineFunction toneCurveFunction;

    public ProfileToneCurveMapping(int width, int height, ImageFileDirectory baselineIFD) {
        super(width, height);

        float[] profileToneCurve = baselineIFD.getProfileToneCurve();
        double[] toneCurveInput = new double[profileToneCurve.length / 2];
        double[] toneCurveOutput = new double[profileToneCurve.length / 2];

        for (int i = 0; i < profileToneCurve.length; i = i + 2) {
            toneCurveInput[i >> 1] = profileToneCurve[i];
            toneCurveOutput[i >> 1] = profileToneCurve[i + 1];
        }

        SplineInterpolator interpolator = new SplineInterpolator();
        toneCurveFunction = interpolator.interpolate(toneCurveInput, toneCurveOutput);
    }

    @Override
    public int[] process(int sampleR, int sampleG, int sampleB, int index) {
        double r = toneCurveFunction.value(sampleR / 65535.0);
        double g = toneCurveFunction.value(sampleG / 65535.0);
        double b = toneCurveFunction.value(sampleB / 65535.0);

        return new int[]{(int) (r * 65535),(int) (g * 65535), (int) (b * 65535)};
    }

    /**
     * Perform profile tone curve mapping with a curve defined in the DNG.
     * @param sampleR between 0 and 1
     * @param sampleG between 0 and 1
     * @param sampleB between 0 and 1
     * @return gammacorrected samples between 0 and 1
     */
    public double[] process(double sampleR, double sampleG, double sampleB) {
        double r = toneCurveFunction.value(sampleR);
        double g = toneCurveFunction.value(sampleG);
        double b = toneCurveFunction.value(sampleB);

        return new double[]{r, g, b};
    }

    public String fragmentShader() {
        throw new IllegalStateException("Not yet implemented");
    }
}
