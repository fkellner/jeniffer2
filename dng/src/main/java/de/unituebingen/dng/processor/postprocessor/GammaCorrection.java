package de.unituebingen.dng.processor.postprocessor;

import de.unituebingen.dng.processor.Processor;
import de.unituebingen.dng.reader.util.Math;
import de.unituebingen.opengl.TransformableOnGPU;

public class GammaCorrection extends PostProcessorOperation implements Processor<TransformableOnGPU> {

    private static final double GAMMA = 1 / 2.4;

    public GammaCorrection(int width, int height) {
        super(width, height);
    }

    @Override
    public int[] process(int sampleR, int sampleG, int sampleB, int index) {
        double r = calcNonlinearValue(Math.in(0, sampleR / 65535.0, 1));
        double g = calcNonlinearValue(Math.in(0, sampleG / 65535.0, 1));
        double b = calcNonlinearValue(Math.in(0, sampleB / 65535.0, 1));

        return new int[]{(int) (r * 65535), (int) (g * 65535), (int) (b * 65535)};
    }

    /**
     * Perform nonlinear Gammacorrection with a fixed GAMMA defined in this class.
     * @param sampleR between 0 and 1
     * @param sampleG between 0 and 1
     * @param sampleB between 0 and 1
     * @return gammacorrected samples between 0 and 1
     */
    public double[] process(double sampleR, double sampleG, double sampleB) {
        double r = calcNonlinearValue(sampleR);
        double g = calcNonlinearValue(sampleG);
        double b = calcNonlinearValue(sampleB);

        return new double[]{r, g, b};
    }

    private double calcNonlinearValue(double sample) {
        return sample < 0.0031308 ? 12.92 * sample : 1.055 * java.lang.Math.pow(sample, GAMMA) - 0.055;
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
            float r = texture(ourTexture, TexCoord).r;
            float g = texture(ourTexture, TexCoord).g;
            float b = texture(ourTexture, TexCoord).b;
            
            color.r = clamp( r < 0.0031308 ? 12.92 * r : 1.055 * pow(r, """ + GAMMA + ") - 0.055, 0.0, 1.0);" + """
            color.g = clamp( g < 0.0031308 ? 12.92 * g : 1.055 * pow(g, """ + GAMMA + ") - 0.055, 0.0, 1.0);" + """
            color.b = clamp( b < 0.0031308 ? 12.92 * b : 1.055 * pow(b, """ + GAMMA + ") - 0.055, 0.0, 1.0);" + """
        }
        """;
    }
}
