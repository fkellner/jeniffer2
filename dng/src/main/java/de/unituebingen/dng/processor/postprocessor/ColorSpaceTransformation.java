package de.unituebingen.dng.processor.postprocessor;

import de.unituebingen.dng.processor.GlobalValues;
import de.unituebingen.dng.processor.Processor;
import de.unituebingen.dng.processor.util.DNGUtils;
import de.unituebingen.dng.processor.util.CalibrationIlluminant;
import de.unituebingen.dng.processor.util.ColorSpaceUtils;
import de.unituebingen.dng.reader.ImageFileDirectory;
import de.unituebingen.dng.reader.util.Math;
import de.unituebingen.dng.reader.util.Rational;
import de.unituebingen.dng.reader.util.SignedRational;
import de.unituebingen.opengl.TransformableOnGPU;


import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;

public class ColorSpaceTransformation extends PostProcessorOperation implements Processor<TransformableOnGPU> {

    private CalibrationIlluminant calibrationIlluminant1;
    private CalibrationIlluminant calibrationIlluminant2;

    private RealMatrix colorMatrix1;
    private RealMatrix colorMatrix2;
    private RealMatrix cameraCalibration1;
    private RealMatrix cameraCalibration2;
    private RealMatrix forwardMatrix1;
    private RealMatrix forwardMatrix2;
    private RealMatrix reductionMatrix1;
    private RealMatrix reductionMatrix2;
    private RealMatrix analogBalance;

    private RealMatrix colorMatrix;
    private RealMatrix cameraCalibration;
    private RealMatrix forwardMatrix;
    private RealMatrix reductionMatrix;

    private RealMatrix XYZToCamera;
    private RealMatrix cameraToXYZD50;
    private double[][] cameraToXYZD50Data;

    private RealMatrix cameraNeutral;

    private int dimensionM;
    private static final int dimensionN = 3;

    public ColorSpaceTransformation(int width, int height, ImageFileDirectory baselineIFD) {
        super(width, height);

        this.calibrationIlluminant1 = baselineIFD.getCalibrationIlluminant1() != -1 ? CalibrationIlluminant.getByID(baselineIFD.getCalibrationIlluminant1()) : null;
        this.calibrationIlluminant2 = baselineIFD.getCalibrationIlluminant2() != -1 ? CalibrationIlluminant.getByID(baselineIFD.getCalibrationIlluminant2()) : null;

        String cameraCalibrationSig = baselineIFD.getCameraCalibrationSignature();
        String profileCalibrationSig = baselineIFD.getProfileCalibrationSignature();

        SignedRational[] colorMatrix1 = baselineIFD.getColorMatrix1();
        SignedRational[] colorMatrix2 = baselineIFD.getColorMatrix2();
        this.dimensionM = colorMatrix1 == null ? colorMatrix2.length / dimensionN : colorMatrix1.length / dimensionN;

        this.colorMatrix1 = DNGUtils.signedRationalsToRealMatrix(colorMatrix1, dimensionM, dimensionN);
        this.colorMatrix2 = DNGUtils.signedRationalsToRealMatrix(colorMatrix2, dimensionM, dimensionN);
        this.cameraCalibration1 = DNGUtils.signedRationalsToRealMatrix(baselineIFD.getCameraCalibration1(), dimensionM, dimensionM);
        this.cameraCalibration2 = DNGUtils.signedRationalsToRealMatrix(baselineIFD.getCameraCalibration2(), dimensionM, dimensionM);
        this.forwardMatrix1 = DNGUtils.signedRationalsToRealMatrix(baselineIFD.getForwardMatrix1(), dimensionN, dimensionM);
        this.forwardMatrix2 = DNGUtils.signedRationalsToRealMatrix(baselineIFD.getForwardMatrix2(), dimensionN, dimensionM);
        this.reductionMatrix1 = DNGUtils.signedRationalsToRealMatrix(baselineIFD.getReductionMatrix1(), dimensionN, dimensionM);
        this.reductionMatrix2 = DNGUtils.signedRationalsToRealMatrix(baselineIFD.getReductionMatrix2(), dimensionN, dimensionM);

        Rational[] analogBalance = baselineIFD.getAnalogBalance();
        this.analogBalance = analogBalance != null ? MatrixUtils.createRealDiagonalMatrix(DNGUtils.rationalsToDoubles(analogBalance))
                : MatrixUtils.createRealDiagonalMatrix(new double[]{1, 1, 1});

        if (cameraCalibrationSig == null || profileCalibrationSig == null || !cameraCalibrationSig.equals(profileCalibrationSig)) {
            this.cameraCalibration1 = MatrixUtils.createRealIdentityMatrix(dimensionM);
            this.cameraCalibration2 = MatrixUtils.createRealIdentityMatrix(dimensionM);
        }

        Rational[] asShotWhiteXY = baselineIFD.getAsShotWhiteXY();
        Rational[] asShotNeutral = baselineIFD.getAsShotNeutral();

        RealMatrix xy;
        if (asShotWhiteXY != null) {
            xy = MatrixUtils.createColumnRealMatrix(DNGUtils.rationalsToDoubles(asShotWhiteXY));
            cameraNeutral = xyWhiteBalancedToCameraNeutral(MatrixUtils.createColumnRealMatrix(DNGUtils.rationalsToDoubles(asShotWhiteXY)));
        } else {
            cameraNeutral = MatrixUtils.createColumnRealMatrix(DNGUtils.rationalsToDoubles(asShotNeutral));
            xy = cameraNeutralWhiteBalancedToxy(cameraNeutral);
        }
        XYZToCamera = calcXYZToCameraMatrix(xy);

        RealMatrix XYZD50ToCamera = MatrixUtils.inverse(calcCameraToXYZD50Matrix());
        RealMatrix XYZD50ToCameraNormalized = DNGUtils.normalizeRows(XYZD50ToCamera);
        cameraToXYZD50 = MatrixUtils.inverse(XYZD50ToCameraNormalized);
        cameraToXYZD50Data = cameraToXYZD50.getData();
    }

    //See DNG Specification 1.5.0.0, page 86
    private RealMatrix xyWhiteBalancedToCameraNeutral(RealMatrix xyCoordinate) {
        if (XYZToCamera == null) {
            XYZToCamera = calcXYZToCameraMatrix(xyCoordinate);
        }
        return XYZToCamera.multiply(ColorSpaceUtils.xyToXYZ(xyCoordinate));
    }

    private RealMatrix cameraNeutralWhiteBalancedToxy(RealMatrix cameraNeutral) {
        RealMatrix xyCoordinate = ColorSpaceUtils.XYZToxy(ColorSpaceUtils.CIE_XYZ_D50);
        RealMatrix xy = null;
        for (int i = 0; i < 128; i++) {
            XYZToCamera = calcXYZToCameraMatrix(xyCoordinate);
            RealMatrix XYZ = MatrixUtils.inverse(XYZToCamera).multiply(cameraNeutral);
            xy = ColorSpaceUtils.XYZToxy(XYZ);
        }

        return xy;
    }

    private RealMatrix calcXYZToCameraMatrix(RealMatrix xyCoordinates) {
        double x = xyCoordinates.getEntry(0, 0);
        double y = xyCoordinates.getEntry(1, 0);
        double cct = ColorSpaceUtils.calcCorrelatedColorTemperature(x, y);
        double weightingFactor = 1 / cct;
        GlobalValues.setCCT(cct);

        if (calibrationIlluminant1 != null && calibrationIlluminant2 != null) {
            if (cct < calibrationIlluminant1.getCCT()) {
                weightingFactor = 1;
            } else if (cct > calibrationIlluminant2.getCCT()) {
                weightingFactor = 0;
            } else {
                weightingFactor = (weightingFactor - (1.0 / calibrationIlluminant2.getCCT()))
                        / ((1.0 / calibrationIlluminant1.getCCT()) - (1.0 / calibrationIlluminant2.getCCT()));
            }
        }

        this.colorMatrix = calcInterpolatedMatrix(colorMatrix1, colorMatrix2, weightingFactor);
        this.cameraCalibration = calcInterpolatedMatrix(cameraCalibration1, cameraCalibration2, weightingFactor);
        this.forwardMatrix = calcInterpolatedMatrix(forwardMatrix1, forwardMatrix2, weightingFactor);
        this.reductionMatrix = calcInterpolatedMatrix(reductionMatrix1, reductionMatrix2, weightingFactor);

        return analogBalance.multiply(cameraCalibration).multiply(colorMatrix);
    }

    private RealMatrix calcCameraToXYZD50Matrix() {
        RealMatrix cameraToXYZD50;
        if (forwardMatrix != null) {
            RealMatrix analogToCameraReference = MatrixUtils.inverse(analogBalance.multiply(cameraCalibration));
            RealMatrix referenceNeutral = analogToCameraReference.multiply(cameraNeutral);
            RealMatrix d = MatrixUtils.inverse(MatrixUtils.createRealDiagonalMatrix(referenceNeutral.getColumn(0)));
            cameraToXYZD50 = forwardMatrix.multiply(d).multiply(analogToCameraReference);
        } else {
            RealMatrix cameraToXYZ;
            if (dimensionM == dimensionN) {
                cameraToXYZ = MatrixUtils.inverse(XYZToCamera);
            } else if (dimensionM > dimensionN && reductionMatrix != null) {
                cameraToXYZ = MatrixUtils.inverse(reductionMatrix.multiply(XYZToCamera)).multiply(reductionMatrix);
            } else {
                //we only need this if we use non bayer cfa
                SingularValueDecomposition svd = new SingularValueDecomposition(XYZToCamera);
                DecompositionSolver solver = svd.getSolver();
                cameraToXYZ = solver.getInverse();
            }
            RealMatrix XYZ = MatrixUtils.inverse(XYZToCamera).multiply(cameraNeutral);
            RealMatrix ca = ColorSpaceUtils.calcChromaticAdaptionMatrix(XYZ, ColorSpaceUtils.CIE_XYZ_D50);

            cameraToXYZD50 = ca.multiply(cameraToXYZ);
        }

        return cameraToXYZD50;
    }

    private RealMatrix calcInterpolatedMatrix(RealMatrix matrix1, RealMatrix matrix2, double interpolationWeightingFactor) {
        if (matrix1 != null && matrix2 != null) {
            return matrix1.scalarMultiply(interpolationWeightingFactor).add(matrix2.scalarMultiply(1.0 - interpolationWeightingFactor));
        } else if (matrix1 != null) {
            return matrix1;
        }
        return matrix2;
    }

    @Override
    public int[] process(int sampleR, int sampleG, int sampleB, int index) {
        int processedSampleR = Math.in(0, (int) (cameraToXYZD50Data[0][0] * sampleR + cameraToXYZD50Data[0][1] * sampleG + cameraToXYZD50Data[0][2] * sampleB), 65535);
        int processedSampleG = Math.in(0, (int) (cameraToXYZD50Data[1][0] * sampleR + cameraToXYZD50Data[1][1] * sampleG + cameraToXYZD50Data[1][2] * sampleB), 65535);
        int processedSampleB = Math.in(0, (int) (cameraToXYZD50Data[2][0] * sampleR + cameraToXYZD50Data[2][1] * sampleG + cameraToXYZD50Data[2][2] * sampleB), 65535);

        return new int[]{processedSampleR, processedSampleG, processedSampleB};
    }

    /**
     * Perform colorspace transformation by multiplying with the matrix we calculated from the DNG tags.
     * After this, color channels are no longer independent, each color influences each other channel.
     * @param sampleR
     * @param sampleG
     * @param sampleB
     * @return
     */
    public double[] process(double sampleR, double sampleG, double sampleB) {
        double processedSampleR = cameraToXYZD50Data[0][0] * sampleR + cameraToXYZD50Data[0][1] * sampleG + cameraToXYZD50Data[0][2] * sampleB;
        double processedSampleG = cameraToXYZD50Data[1][0] * sampleR + cameraToXYZD50Data[1][1] * sampleG + cameraToXYZD50Data[1][2] * sampleB;
        double processedSampleB = cameraToXYZD50Data[2][0] * sampleR + cameraToXYZD50Data[2][1] * sampleG + cameraToXYZD50Data[2][2] * sampleB;

        return new double[]{processedSampleR, processedSampleG, processedSampleB};
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

            color.r = clamp(""" + cameraToXYZD50Data[0][0] + " * X + " + cameraToXYZD50Data[0][1] + " * Y + " + cameraToXYZD50Data[0][2] + " * Z, 0.0, 1.0);" + """
            color.g = clamp(""" + cameraToXYZD50Data[1][0] + " * X + " + cameraToXYZD50Data[1][1] + " * Y + " + cameraToXYZD50Data[1][2] + " * Z, 0.0, 1.0);" + """
            color.b = clamp(""" + cameraToXYZD50Data[2][0] + " * X + " + cameraToXYZD50Data[2][1] + " * Y + " + cameraToXYZD50Data[2][2] + " * Z, 0.0, 1.0);" + """
            
        }
        """;
    }
}
