package de.unituebingen.dng.processor.postprocessor;

import de.unituebingen.dng.processor.util.ColorSpaceUtils;
import de.unituebingen.dng.reader.ImageFileDirectory;

public class ProfileLookTableMapping extends PostProcessorOperation{

    private static final double HUE_FACTOR = 1 / 60.0;

    private int hDivisions;
    private int sDivisions;
    private int vDivisions;

    private double hMappingFactor;
    private double sMappingFactor;

    private int hIndexMax;
    private int sIndexMax;

    private float[][] profileLookTableData;

    double[][] XYZD50toRIMMRGB = ColorSpaceUtils.XYZD50toRIMMRGB.getData();
    double[][] RIMMRGBtoXYZD50 = ColorSpaceUtils.RIMMRGBtoXYZD50.getData();

    public ProfileLookTableMapping(int width, int height, ImageFileDirectory baselineIFD) {
        super(width, height);

        long[] profileLookTableDims = baselineIFD.getProfileLookTableDims();
        float[] profileLookTableData1 = baselineIFD.getProfileLookTableData();

        this.hDivisions = (int) profileLookTableDims[0];
        this.sDivisions = (int) profileLookTableDims[1];
        this.vDivisions = (int) profileLookTableDims[2];

        this.hIndexMax = this.hDivisions - 1;
        this.sIndexMax = this.sDivisions - 2;

        this.hMappingFactor = this.hDivisions < 2 ? 0.0 : this.hDivisions * (HUE_FACTOR * 10);
        this.sMappingFactor = this.sDivisions - 1;

        profileLookTableData = new float[profileLookTableData1.length / 3][3];
        for (int i = 0; i < profileLookTableData1.length / 3; i++) {
            profileLookTableData[i][0] = profileLookTableData1[i * 3];
            profileLookTableData[i][1] = profileLookTableData1[i * 3 + 1];
            profileLookTableData[i][2] = profileLookTableData1[i * 3 + 2];
        }
    }

    @Override
    public int[] process(int sampleR, int sampleG, int sampleB, int indexx) {
        //assuming ColorSpaceConversion is returning the samples in XYZD50 color space
        double x = sampleR / 65535.0;
        double y = sampleG / 65535.0;
        double z = sampleB / 65535.0;

        double r = XYZD50toRIMMRGB[0][0] * x + XYZD50toRIMMRGB[0][1] * y + XYZD50toRIMMRGB[0][2] * z;
        double g = XYZD50toRIMMRGB[1][0] * x + XYZD50toRIMMRGB[1][1] * y + XYZD50toRIMMRGB[1][2] * z;
        double b = XYZD50toRIMMRGB[2][0] * x + XYZD50toRIMMRGB[2][1] * y + XYZD50toRIMMRGB[2][2] * z;

        double[] hsv = ColorSpaceUtils.rgbToHSV(new double[]{r, g, b});
        double h = hsv[0] / 60.0;
        double s = hsv[1];
        double v = hsv[2];

        if (vDivisions < 2) {
            double hShift;
            double sScaleFactor;
            double vScaleFactor;

            double hMapped = hMappingFactor * h;
            double sMapped = sMappingFactor * s;

            int hIndex = (int) hMapped;
            int sIndex = Math.min((int) sMapped, sIndexMax);

            if (hIndex >= hIndexMax) {
                hIndex = hIndexMax;
            }
            int hIndex2nd = (hIndex + 1) % hIndexMax;

            double hDiff = hMapped - hIndex;
            double hWeightingFactor = 1 - hDiff;
            double hWeightingFactor2nd = hDiff;

            int index = (hIndex * this.sDivisions) + sIndex;
            int index2nd = index + ((hIndex2nd - hIndex) * this.sDivisions);

            hShift = hWeightingFactor * profileLookTableData[index][0] + hWeightingFactor2nd * profileLookTableData[index2nd][0];
            sScaleFactor = hWeightingFactor * profileLookTableData[index][1] + hWeightingFactor2nd * profileLookTableData[index2nd][1];
            vScaleFactor = hWeightingFactor * profileLookTableData[index][2] + hWeightingFactor2nd * profileLookTableData[index2nd][2];

            hShift *= HUE_FACTOR;
            h += hShift;

            s = Math.min(s * sScaleFactor, 1);
            v = Math.min(v * vScaleFactor, 1);

            double[] rgb = ColorSpaceUtils.hsvToRGB(new double[]{h * 60.0, s, v});

            r = rgb[0];
            g = rgb[1];
            b = rgb[2];
        }

        x = RIMMRGBtoXYZD50[0][0] * r + RIMMRGBtoXYZD50[0][1] * g + RIMMRGBtoXYZD50[0][2] * b;
        y = RIMMRGBtoXYZD50[1][0] * r + RIMMRGBtoXYZD50[1][1] * g + RIMMRGBtoXYZD50[1][2] * b;
        z = RIMMRGBtoXYZD50[2][0] * r + RIMMRGBtoXYZD50[2][1] * g + RIMMRGBtoXYZD50[2][2] * b;

        return new int[]{(int) (x * 65535), (int) (y * 65535), (int) (z * 65535)};
    }
    /**
     * Perform Profile Lookup Table mapping. Correlates color channels, after this, they are no longer independent.
     * Assumes ColorSpaceConversion is returning the samples in XYZD50 color space.
     * @param sampleR between 0 and 1
     * @param sampleG between 0 and 1
     * @param sampleB between 0 and 1
     * @return processed samples between 0 and 1
     */
    public double[] process(double x, double y, double z) {
        double r = XYZD50toRIMMRGB[0][0] * x + XYZD50toRIMMRGB[0][1] * y + XYZD50toRIMMRGB[0][2] * z;
        double g = XYZD50toRIMMRGB[1][0] * x + XYZD50toRIMMRGB[1][1] * y + XYZD50toRIMMRGB[1][2] * z;
        double b = XYZD50toRIMMRGB[2][0] * x + XYZD50toRIMMRGB[2][1] * y + XYZD50toRIMMRGB[2][2] * z;

        double[] hsv = ColorSpaceUtils.rgbToHSV(new double[]{r, g, b});
        double h = hsv[0] / 60.0;
        double s = hsv[1];
        double v = hsv[2];

        if (vDivisions < 2) {
            double hShift;
            double sScaleFactor;
            double vScaleFactor;

            double hMapped = hMappingFactor * h;
            double sMapped = sMappingFactor * s;

            int hIndex = (int) hMapped;
            int sIndex = Math.min((int) sMapped, sIndexMax);

            if (hIndex >= hIndexMax) {
                hIndex = hIndexMax;
            }
            int hIndex2nd = (hIndex + 1) % hIndexMax;

            double hDiff = hMapped - hIndex;
            double hWeightingFactor = 1 - hDiff;
            double hWeightingFactor2nd = hDiff;

            int index = (hIndex * this.sDivisions) + sIndex;
            int index2nd = index + ((hIndex2nd - hIndex) * this.sDivisions);

            hShift = hWeightingFactor * profileLookTableData[index][0] + hWeightingFactor2nd * profileLookTableData[index2nd][0];
            sScaleFactor = hWeightingFactor * profileLookTableData[index][1] + hWeightingFactor2nd * profileLookTableData[index2nd][1];
            vScaleFactor = hWeightingFactor * profileLookTableData[index][2] + hWeightingFactor2nd * profileLookTableData[index2nd][2];

            hShift *= HUE_FACTOR;
            h += hShift;

            s = Math.min(s * sScaleFactor, 1);
            v = Math.min(v * vScaleFactor, 1);

            double[] rgb = ColorSpaceUtils.hsvToRGB(new double[]{h * 60.0, s, v});

            r = rgb[0];
            g = rgb[1];
            b = rgb[2];
        }

        x = RIMMRGBtoXYZD50[0][0] * r + RIMMRGBtoXYZD50[0][1] * g + RIMMRGBtoXYZD50[0][2] * b;
        y = RIMMRGBtoXYZD50[1][0] * r + RIMMRGBtoXYZD50[1][1] * g + RIMMRGBtoXYZD50[1][2] * b;
        z = RIMMRGBtoXYZD50[2][0] * r + RIMMRGBtoXYZD50[2][1] * g + RIMMRGBtoXYZD50[2][2] * b;

        return new double[]{x, y, z};
    }

    public String fragmentShader() {
        throw new IllegalStateException("Not yet implemented");
    }
}
