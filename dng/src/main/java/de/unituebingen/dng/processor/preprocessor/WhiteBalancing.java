package de.unituebingen.dng.processor.preprocessor;

import de.unituebingen.dng.reader.ImageFileDirectory;
import de.unituebingen.dng.reader.dng.util.CFAPattern;
import de.unituebingen.dng.reader.util.Rational;
import de.unituebingen.dng.reader.util.Math;

public class WhiteBalancing extends PreProcessorOperation {

    private Rational[] analogBalance;
    private Rational[] asShotNeutral;
    // private Rational[] asShotWhiteXY;
    private int[] cfaRepeatPatternDim;
    private CFAPattern cfaPattern;

    //TODO: assumes we are using RGB
    private double[] whiteBalanceFactors;

    public WhiteBalancing(ImageFileDirectory baselineIFD, ImageFileDirectory highResolutionIFD) {
        super(baselineIFD, highResolutionIFD);
        init();
    }

    protected void init() {
        this.analogBalance = baselineIFD.getAnalogBalance();
        this.asShotNeutral = baselineIFD.getAsShotNeutral();
        // this.asShotWhiteXY = baselineIFD.getAsShotWhiteXY();
        this.cfaRepeatPatternDim = highResolutionIFD.getCFARepeatPatternDim();
        this.cfaPattern = highResolutionIFD.getCFAPattern();
        this.whiteBalanceFactors = new double[]{1D, 1D, 1D};

        if (asShotNeutral == null) {
            //TODO: calculate asShotNeutral using asShotWhiteXY
        }

        if (analogBalance != null) {
            whiteBalanceFactors[0] = analogBalance[0].reciprocal();
            whiteBalanceFactors[1] = analogBalance[1].reciprocal();
            whiteBalanceFactors[2] = analogBalance[2].reciprocal();
        }
        whiteBalanceFactors[0] *= asShotNeutral[0].reciprocal();
        whiteBalanceFactors[1] *= asShotNeutral[1].reciprocal();
        whiteBalanceFactors[2] *= asShotNeutral[2].reciprocal();
    }

    @Override
    public int process(int sample, int index) {
        int x = getXByIndex(index);
        int y = getYByIndex(index);
        short channel = cfaPattern.getCfaPattern()[y % cfaRepeatPatternDim[0] * cfaRepeatPatternDim[1] + x % cfaRepeatPatternDim[1]];
        return Math.in(0, (int) (whiteBalanceFactors[channel] * sample), 65535);
    }
}
