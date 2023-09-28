package de.unituebingen.dng.processor.postprocessor;

import de.unituebingen.dng.processor.util.DNGUtils;
import de.unituebingen.dng.processor.Processor;
import de.unituebingen.dng.processor.util.ColorSpaceUtils;
import de.unituebingen.dng.reader.util.Math;
import de.unituebingen.opengl.TransformableOnGPU;

import org.apache.commons.math3.linear.MatrixUtils;

public class XYZD50ToSRGBMapping extends PostProcessorOperation implements Processor<TransformableOnGPU> {

    private static final double GAMMA = 1 / 2.4;
    private double[][] sRGB_to_XYZD50 = DNGUtils.normalizeRows(MatrixUtils.inverse(ColorSpaceUtils.SRGB_TO_XYZD50)).getData();

    // private double[] gammaReverseLUT;

    public XYZD50ToSRGBMapping(int width, int height) {
        super(width, height);
        // calculateReverseLUT();
    }

    // private void calculateReverseLUT() {
    //     gammaReverseLUT = new double[65536];
    //     for(int i = 0; i <= 65535; i++) {
    //         gammaReverseLUT[i] = java.lang.Math.pow(((i / 65535.0) + 0.055) / 1.055f, 2.4);
    //     }
    // }

    // private double reverseLookupGamma(double v) {
    //     // perform binary search - 16 comparisons
    //     int step = 65536 / 2;
    //     int pos = step;
    //     while(step > 1) {
    //         step = step / 2;
    //         if(v > gammaReverseLUT[pos]) {
    //             pos += step;
    //         } else {
    //             pos -= step;
    //         }
    //     }
    //     return pos / 65535.0;
    // }
    /**
     * Fast power approximation, as described in https://martin.ankerl.com/2007/10/04/optimized-pow-approximation-for-java-and-c-c/
     * (based on https://direct.mit.edu/neco/article/11/4/853/6267/A-Fast-Compact-Approximation-of-the-Exponential).
     * Currently results in an average difference of 1%, maximum 2%, MSE of 6.84 if broken down to 8bit precision,
     * for about a 1.5 speedup.
     * @param a
     * @param b
     * @return
     */
    public static double fastPow(final double a, final double b) {
        final int tmp = (int) (Double.doubleToLongBits(a) >> 32);
        final int tmp2 = (int) (b * (tmp - 1072632447) + 1072632447);
        return Double.longBitsToDouble(((long) tmp2) << 32);
    }    

    @Override
    public int[] process(int sampleR, int sampleG, int sampleB, int index) {
        double X = sampleR / 65535.0;
        double Y = sampleG / 65535.0;
        double Z = sampleB / 65535.0;

        double r = (sRGB_to_XYZD50[0][0] * X + sRGB_to_XYZD50[0][1] * Y + sRGB_to_XYZD50[0][2] * Z);
        double g = (sRGB_to_XYZD50[1][0] * X + sRGB_to_XYZD50[1][1] * Y + sRGB_to_XYZD50[1][2] * Z);
        double b = (sRGB_to_XYZD50[2][0] * X + sRGB_to_XYZD50[2][1] * Y + sRGB_to_XYZD50[2][2] * Z);

        r = calcNonlinearValue(Math.in(0, r, 1));
        g = calcNonlinearValue(Math.in(0, g, 1));
        b = calcNonlinearValue(Math.in(0, b, 1));

        return new int[]{(int) (r * 65535), (int) (g * 65535), (int) (b * 65535)};
    }

    /**
     * Perform XYZD50 to sRGB mapping. Correlates color channels, after this, they are no longer independent.
     * @param sampleR between 0 and 1
     * @param sampleG between 0 and 1
     * @param sampleB between 0 and 1
     * @return processed samples between 0 and 1
     */
    public double[] process(double X, double Y, double Z) {
        double r = (sRGB_to_XYZD50[0][0] * X + sRGB_to_XYZD50[0][1] * Y + sRGB_to_XYZD50[0][2] * Z);
        double g = (sRGB_to_XYZD50[1][0] * X + sRGB_to_XYZD50[1][1] * Y + sRGB_to_XYZD50[1][2] * Z);
        double b = (sRGB_to_XYZD50[2][0] * X + sRGB_to_XYZD50[2][1] * Y + sRGB_to_XYZD50[2][2] * Z);

        r = calcNonlinearValue(Math.in(0, r, 1));
        g = calcNonlinearValue(Math.in(0, g, 1));
        b = calcNonlinearValue(Math.in(0, b, 1));

        return new double[]{r, g, b};
    }

    private double calcNonlinearValue(double sample) {
        return sample < 0.0031308 ? 12.92 * sample : 1.055 * java.lang.Math.pow(sample, GAMMA) - 0.055;
        // return sample < 0.0031308 ? 12.92 * sample : 1.055 * fastPow(sample, GAMMA) - 0.055;
        // return sample < 0.0031308 ? 12.92 * sample : reverseLookupGamma(sample);

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
            float X = texture(ourTexture, TexCoord).r;
            float Y = texture(ourTexture, TexCoord).g;
            float Z = texture(ourTexture, TexCoord).b;

            float r = """ + sRGB_to_XYZD50[0][0] + " * X + " + sRGB_to_XYZD50[0][1] + " * Y + " + sRGB_to_XYZD50[0][2] + " * Z;" + """
            float g = """ + sRGB_to_XYZD50[1][0] + " * X + " + sRGB_to_XYZD50[1][1] + " * Y + " + sRGB_to_XYZD50[1][2] + " * Z;" + """
            float b = """ + sRGB_to_XYZD50[2][0] + " * X + " + sRGB_to_XYZD50[2][1] + " * Y + " + sRGB_to_XYZD50[2][2] + " * Z;" + """
            
            color.r = clamp( r < 0.0031308 ? 12.92 * r : 1.055 * pow(r, """ + GAMMA + ") - 0.055, 0.0, 1.0);" + """
            color.g = clamp( g < 0.0031308 ? 12.92 * g : 1.055 * pow(g, """ + GAMMA + ") - 0.055, 0.0, 1.0);" + """
            color.b = clamp( b < 0.0031308 ? 12.92 * b : 1.055 * pow(b, """ + GAMMA + ") - 0.055, 0.0, 1.0);" + """
        }
        """;
    }
    
}
