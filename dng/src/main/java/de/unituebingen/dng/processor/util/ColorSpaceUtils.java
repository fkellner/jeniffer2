package de.unituebingen.dng.processor.util;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

public class ColorSpaceUtils {

    //See http://www.brucelindbloom.com/index.html?Eqn_RGB_XYZ_Matrix.html
    public static final RealMatrix SRGB_TO_XYZD50 = MatrixUtils.createRealMatrix(new double[][]
            {
                    {0.4360747, 0.3850649, 0.1430804},
                    {0.2225045, 0.7168786, 0.0606169},
                    {0.0139322, 0.0971045, 0.7141733}
            });

    //Linear Bradford Adaption matrix. See http://www.brucelindbloom.com/index.html?Eqn_ChromAdapt.html
    //See https://onlinelibrary.wiley.com/doi/pdf/10.1002/9781119021780.app3
    public static final RealMatrix BRADFORD_TRANSFORM_MATRIX = MatrixUtils.createRealMatrix(new double[][]
            {
                    {0.8951, 0.2664, -0.1614},
                    {-0.7502, 1.7135, 0.0367},
                    {0.0389, -0.0685, 1.0296}
            });

    public static final RealMatrix XYZD50_TO_RIMM = MatrixUtils.createRealMatrix(new double[][]
            {
                    {1.3460, -0.2556, -0.0511},
                    {-0.5446, 1.5082, 0.0205},
                    {0.0000, 0.0000, 1.2123}
            });

    //-----------------------------------------------------------------------------------------------------------------
    public static final RealMatrix RIMMRGBtoXYZD50 = DNGUtils.normalizeRows(MatrixUtils.createRealMatrix(new double[][]
            {
                    {0.7977, 0.1352, 0.0313},
                    {0.2880, 0.7119, 0.0001},
                    {0.0000, 0.0000, 0.8249}
            }));

    public static final RealMatrix XYZD50toRIMMRGB = DNGUtils.normalizeRows(MatrixUtils.createRealMatrix(new double[][]
            {
                    {1.3460, -0.2556, -0.0511},
                    {-0.5446, 1.5082, 0.0205},
                    {0.0, 0.0, 1.2123}
            }));

    public static final RealMatrix CIE_XYZ_D50 = MatrixUtils.createColumnRealMatrix(new double[]{0.9642, 1.0000, 0.8251});

    public static final RealMatrix CIE_XYZ_D65 = MatrixUtils.createColumnRealMatrix(new double[]{0.9642, 1.0000, 0.8251});

    /**
     * Translaes CIE XYZ coordinates to xy coordinates. <br/>
     * See http://www.brucelindbloom.com/index.html?Eqn_XYZ_to_xyY.html
     *
     * @param XYZ A vector with the X, Y, and Z coordinates which should be translated
     * @return The translated CIE XYZ to xy coordinates
     */
    public static RealMatrix XYZToxy(RealMatrix XYZ) {
        double X = XYZ.getEntry(0, 0);
        double Y = XYZ.getEntry(1, 0);
        double Z = XYZ.getEntry(2, 0);

        double sum = X + Y + Z;
        double x = X / sum;
        double y = Y / sum;
        return MatrixUtils.createColumnRealMatrix(new double[]{x, y, Y});
    }

    /**
     * Translates xy coordinates to CIE XYZ coordinates. <br/>
     * See http://www.brucelindbloom.com/index.html?Eqn_xyY_to_XYZ.html
     *
     * @param xy A vector with the y and y coordinates which should be translated
     * @return The translated xy to CIE XYZ coordinates
     */
    public static RealMatrix xyToXYZ(RealMatrix xy) {
        double x = xy.getEntry(0, 0);
        double y = xy.getEntry(1, 0);

        double X = y != 0 ? x / y : 0;
        double Y = y != 0 ? 1.0 : 0;
        double Z = y != 0 ? (1.0 - x - y) / y : 0;
        return MatrixUtils.createColumnRealMatrix(new double[]{X, Y, Z});
    }

    //See linear Bradford algorithm
    //Seee https://onlinelibrary.wiley.com/doi/pdf/10.1002/9781119021780.app3 , C.2
    public static RealMatrix calcChromaticAdaptionMatrix(RealMatrix XYZReferenceIlluminant, RealMatrix XYZTestIlluminant) {
        RealMatrix referenceIlluminantRGB = BRADFORD_TRANSFORM_MATRIX.multiply(XYZReferenceIlluminant);
        RealMatrix testIlluminantRGB = BRADFORD_TRANSFORM_MATRIX.multiply(XYZTestIlluminant);

        referenceIlluminantRGB.setEntry(0, 0, Math.max(referenceIlluminantRGB.getEntry(0, 0), 0));
        referenceIlluminantRGB.setEntry(1, 0, Math.max(referenceIlluminantRGB.getEntry(1, 0), 0));
        referenceIlluminantRGB.setEntry(2, 0, Math.max(referenceIlluminantRGB.getEntry(2, 0), 0));

        testIlluminantRGB.setEntry(0, 0, Math.max(testIlluminantRGB.getEntry(0, 0), 0));
        testIlluminantRGB.setEntry(1, 0, Math.max(testIlluminantRGB.getEntry(1, 0), 0));
        testIlluminantRGB.setEntry(2, 0, Math.max(testIlluminantRGB.getEntry(2, 0), 0));

        //TODO: Check ... what to do if e.g. referenceIlluminant.getEntry(0,0) is 0?
        double rScale = referenceIlluminantRGB.getEntry(0, 0) / testIlluminantRGB.getEntry(0, 0);
        double gScale = referenceIlluminantRGB.getEntry(1, 0) / testIlluminantRGB.getEntry(1, 0);
        double bScale = referenceIlluminantRGB.getEntry(2, 0) / testIlluminantRGB.getEntry(2, 0);

        RealMatrix ADT = MatrixUtils.createRealDiagonalMatrix(new double[]{rScale, gScale, bScale});
        RealMatrix chromaticAdaptionMatrix = MatrixUtils.inverse(BRADFORD_TRANSFORM_MATRIX).multiply(ADT).multiply(BRADFORD_TRANSFORM_MATRIX);

        return chromaticAdaptionMatrix;
    }

    /**
     * TODO: This is only a very simple and inaccurate method to approximate the cct. For better cct approximation an other method should be used ... but for now this method is sufficient
     *
     * @param x
     * @param y
     * @return
     */
    public static double calcCorrelatedColorTemperature(double x, double y) {
        double n = (x - 0.3320) / (0.1858 - y);
        return 437 * Math.pow(n, 3) + 3601 * Math.pow(n, 2) + 6861 * n + 5517;
    }

    public static double[] rgbToHSV(double[] rgb) {
        double r = rgb[0];
        double g = rgb[1];
        double b = rgb[2];

        double max = Math.max(Math.max(r, g), b);
        double min = Math.min(Math.min(r, g), b);
        double maxMinDiff = max - min;

        double v = max;
        double h = 0.0;
        double s = 0.0;

        if (maxMinDiff > 0) {
            if (max == r) {
                h = 60 * ((g - b) / maxMinDiff);
            } else if (max == g) {
                h = 60 * (2.0f + (b - r) / maxMinDiff);
            } else {
                h = 60 * (4.0f + (r - g) / maxMinDiff);
            }
            if (h < 0.0f) {
                h += 360.0f;
            }
            s = maxMinDiff / max;
        }

        return new double[]{h, s, v};
    }

    public static double[] hsvToRGB(double[] hsv) {
        double h = hsv[0];
        double s = hsv[1];
        double v = hsv[2];

        double r = 0;
        double g = 0;
        double b = 0;

        if (s > 0.0) {
            int hi = (int) (h / 60);
            double f = (h / 60) - hi;

            double p = v * (1 - s);
            double q = v * (1 - s * f);
            double t = v * (1 - s * (1 - f));

            if (hi == 0 || hi == 6) {
                r = v;
                g = t;
                b = p;
            } else if (hi == 1) {
                r = q;
                g = v;
                b = p;
            } else if (hi == 2) {
                r = p;
                g = v;
                b = t;
            } else if (hi == 3) {
                r = p;
                g = q;
                b = v;
            } else if (hi == 4) {
                r = t;
                g = p;
                b = v;
            } else if (hi == 5) {
                r = v;
                g = p;
                b = q;
            }
        } else {
            r = v;
            g = v;
            b = v;
        }

        return new double[]{r, g, b};
    }
}
