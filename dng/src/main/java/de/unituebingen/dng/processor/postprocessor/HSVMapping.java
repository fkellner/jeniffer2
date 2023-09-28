package de.unituebingen.dng.processor.postprocessor;

import de.unituebingen.dng.processor.GlobalValues;
import de.unituebingen.dng.processor.util.CalibrationIlluminant;
import de.unituebingen.dng.processor.util.ColorSpaceUtils;
import de.unituebingen.dng.reader.ImageFileDirectory;

public class HSVMapping extends PostProcessorOperation {

    private static final double HUE_FACTOR = 1 / 60.0;

    private int hDivisions;
    private int sDivisions;
    private int vDivisions;

    private double hMappingFactor;
    private double sMappingFactor;

    private int hIndexMax;
    private int sIndexMax;

    private float[][] profileHueSatMapData;

    double[][] XYZD50toRIMMRGB = ColorSpaceUtils.XYZD50toRIMMRGB.getData();
    double[][] RIMMRGBtoXYZD50 = ColorSpaceUtils.RIMMRGBtoXYZD50.getData();

    public HSVMapping(int width, int height, ImageFileDirectory baselineIFD) {
        super(width, height);

        long[] profileHueSatMapDims = baselineIFD.getProfileHueSatMapDims();
        float[] profileHueSatMapData1 = baselineIFD.getProfileHueSatMapData1();
        float[] profileHueSatMapData2 = baselineIFD.getProfileHueSatMapData2();
        float[] profileHueSatMapDataTmp = profileHueSatMapData2;

        if (profileHueSatMapData1 != null && profileHueSatMapData2 != null) {
            CalibrationIlluminant calibrationIlluminant1 = baselineIFD.getCalibrationIlluminant1() != -1
                    ? CalibrationIlluminant.getByID(baselineIFD.getCalibrationIlluminant1()) : null;
            CalibrationIlluminant calibrationIlluminant2 = baselineIFD.getCalibrationIlluminant2() != -1
                    ? CalibrationIlluminant.getByID(baselineIFD.getCalibrationIlluminant2()) : null;

            double cct = GlobalValues.getCCT();
            double weightingFactor = 1 / cct;
            if (cct < calibrationIlluminant1.getCCT()) {
                weightingFactor = 1;
            } else if (cct > calibrationIlluminant2.getCCT()) {
                weightingFactor = 0;
            } else {
                weightingFactor = (weightingFactor - (1.0 / calibrationIlluminant2.getCCT()))
                        / ((1.0 / calibrationIlluminant1.getCCT()) - (1.0 / calibrationIlluminant2.getCCT()));
            }

            for (int i = 0; i < profileHueSatMapDataTmp.length; i++) {
                profileHueSatMapDataTmp[i] = (float) (profileHueSatMapData1[i] * weightingFactor + profileHueSatMapData2[i] * (1 - weightingFactor));
            }
        } else if (profileHueSatMapData1 != null) {
            profileHueSatMapDataTmp = profileHueSatMapData1;
        }

        this.hDivisions = (int) profileHueSatMapDims[0];
        this.sDivisions = (int) profileHueSatMapDims[1];
        this.vDivisions = (int) profileHueSatMapDims[2];

        this.hIndexMax = this.hDivisions - 1;
        this.sIndexMax = this.sDivisions - 2;

        this.hMappingFactor = this.hDivisions < 2 ? 0.0 : this.hDivisions * (HUE_FACTOR * 10);
        this.sMappingFactor = this.sDivisions - 1;

        profileHueSatMapData = new float[profileHueSatMapDataTmp.length / 3][3];
        for (int i = 0; i < profileHueSatMapDataTmp.length / 3; i++) {
            profileHueSatMapData[i][0] = profileHueSatMapDataTmp[i * 3];
            profileHueSatMapData[i][1] = profileHueSatMapDataTmp[i * 3 + 1];
            profileHueSatMapData[i][2] = profileHueSatMapDataTmp[i * 3 + 2];
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

            hShift = hWeightingFactor * profileHueSatMapData[index][0] + hWeightingFactor2nd * profileHueSatMapData[index2nd][0];
            sScaleFactor = hWeightingFactor * profileHueSatMapData[index][1] + hWeightingFactor2nd * profileHueSatMapData[index2nd][1];
            vScaleFactor = hWeightingFactor * profileHueSatMapData[index][2] + hWeightingFactor2nd * profileHueSatMapData[index2nd][2];

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
     * Perform HSV mapping. Correlates color channels, after this, they are no longer independent.
     * Assumes ColorSpaceConversion is returning the samples in XYZD50 color space.     * 
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

            hShift = hWeightingFactor * profileHueSatMapData[index][0] + hWeightingFactor2nd * profileHueSatMapData[index2nd][0];
            sScaleFactor = hWeightingFactor * profileHueSatMapData[index][1] + hWeightingFactor2nd * profileHueSatMapData[index2nd][1];
            vScaleFactor = hWeightingFactor * profileHueSatMapData[index][2] + hWeightingFactor2nd * profileHueSatMapData[index2nd][2];

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
