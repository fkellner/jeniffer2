package de.unituebingen.dng.processor.postprocessor;

import de.unituebingen.dng.processor.Processor;
import de.unituebingen.dng.reader.ImageFileDirectory;
import de.unituebingen.dng.reader.util.Rational;
import de.unituebingen.dng.reader.util.SignedRational;
import de.unituebingen.opengl.TransformableOnGPU;

public class ExposureCorrection extends PostProcessorOperation implements Processor<TransformableOnGPU> {

    private double defaultExposureValue;

    public ExposureCorrection(int width, int height, ImageFileDirectory baselineIFD) {
        super(width, height);

        SignedRational baselineExposure = baselineIFD.getBaselineExposure();
        Rational baselineExposureOffset = baselineIFD.getBaselineExposureOffset();

        this.defaultExposureValue = baselineExposure.doubleValue();
        if (baselineExposureOffset != null) {
            this.defaultExposureValue += baselineExposureOffset.doubleValue();
        }
        this.defaultExposureValue = Math.pow(2, defaultExposureValue);
    }

    @Override
    public int[] process(int sampleR, int sampleG, int sampleB, int index) {
        double r = (sampleR / 65535.0) * defaultExposureValue;
        double g = (sampleG / 65535.0) * defaultExposureValue;
        double b = (sampleB / 65535.0) * defaultExposureValue;

        return new int[]{(int) (r * 65535), (int) (g * 65535), (int) (b * 65535)};
    }

    /**
     * Perform exposure correction by multiplying each channel with the same fixed scalar value
     * @param sampleR
     * @param sampleG
     * @param sampleB
     * @return
     */
    public double[] process(double sampleR, double sampleG, double sampleB) {
        double r = sampleR * defaultExposureValue;
        double g = sampleG * defaultExposureValue;
        double b = sampleB * defaultExposureValue;

        return new double[]{r, g, b};
    }

    public TransformableOnGPU process(TransformableOnGPU img) {
        img.applyShaderInPlace(fragmentShader());
        return img;
    }

    public String fragmentShader() {
        return """
        #version 130
        out vec3 color;
    
        in vec2 TexCoord;
    
        uniform sampler2D ourTexture;
    
        void main()
        {          
            color.r = texture(ourTexture, TexCoord).r * """ + defaultExposureValue + ";" + """
            color.g = texture(ourTexture, TexCoord).g * """ + defaultExposureValue + ";" + """
            color.b = texture(ourTexture, TexCoord).b * """ + defaultExposureValue + ";" + """
        }
        """;
    }
}
