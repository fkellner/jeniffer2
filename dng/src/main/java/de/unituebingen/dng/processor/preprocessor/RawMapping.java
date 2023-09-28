package de.unituebingen.dng.processor.preprocessor;

import de.unituebingen.dng.processor.util.Lookup;
import de.unituebingen.dng.reader.ImageFileDirectory;
import de.unituebingen.dng.reader.util.Math;
import de.unituebingen.dng.reader.util.Rational;
import de.unituebingen.dng.reader.util.SignedRational;

/**
 * Currently only SamplesPerPixel = 1 is supported (Linear DNG not supported!)
 */
public class RawMapping extends PreProcessorOperation {

    private long[] activeArea;
    private int[] linearizationTable;
    private int[] blackLevelRepeatDim;
    private Rational[] blackLevel;
    private SignedRational[] blackLevelDeltaH;
    private SignedRational[] blackLevelDeltaV;
    private long[] whiteLevel;
    private int[] cfaRepeatPatternDim;

    private int imageWidth;
    private int imageLength;
    private int samplesPerPixel;

    private int startXCoordinate;
    private int startYCoordinate;
    private int width;
    private int height;

    // private double scaleFactor;

    private Lookup<Integer, Integer> linearizationTableLookup;
    private Lookup<Integer, Double> blackLevelDeltaHLookup;
    private Lookup<Integer, Double> blackLevelDeltaVLookup;
    private Lookup<Integer, Double> blackLevelLookup;

    public RawMapping(ImageFileDirectory baselineIFD, ImageFileDirectory highResolutionIFD) {
        super(baselineIFD, highResolutionIFD);
        init();
    }

    protected void init() {
        this.activeArea = highResolutionIFD.getActiveArea();
        this.linearizationTable = highResolutionIFD.getLinearizationTable();
        this.blackLevelRepeatDim = highResolutionIFD.getBlackLevelRepeatDim();
        this.blackLevel = highResolutionIFD.getBlackLevel();
        this.blackLevelDeltaH = highResolutionIFD.getBlackLevelDeltaH();
        this.blackLevelDeltaV = highResolutionIFD.getBlackLevelDeltaV();
        this.whiteLevel = highResolutionIFD.getWhiteLevel();
        this.imageWidth = (int) highResolutionIFD.getImageWidth();
        this.imageLength = (int) highResolutionIFD.getImageLength();
        this.samplesPerPixel = highResolutionIFD.getSamplesPerPixel();
        this.cfaRepeatPatternDim = highResolutionIFD.getCFARepeatPatternDim();

        startYCoordinate = activeArea == null ? 0 : (int) activeArea[0];
        startXCoordinate = activeArea == null ? 0 : (int) activeArea[1];
        height = activeArea == null ? imageLength : (int) activeArea[2];
        width = activeArea == null ? imageWidth : (int) activeArea[3];
        samplesPerPixel = java.lang.Math.max(1, samplesPerPixel);

        if (linearizationTable != null) {
            linearizationTableLookup = (value -> value > linearizationTable.length ? linearizationTable[linearizationTable.length - 1] : linearizationTable[value]);
        } else {
            linearizationTableLookup = (value -> value);
        }

        if (blackLevelDeltaV != null) {
            blackLevelDeltaVLookup = (value -> blackLevelDeltaV[value].doubleValue());
        } else {
            blackLevelDeltaVLookup = (value -> 0D);
        }

        if (blackLevelDeltaH != null) {
            blackLevelDeltaHLookup = (value -> blackLevelDeltaH[value].doubleValue());
        } else {
            blackLevelDeltaHLookup = (value -> 0D);
        }

        if (blackLevel != null) {
            blackLevelLookup = (value -> blackLevel[value].doubleValue());
        } else {
            blackLevelLookup = (value -> 0D);
        }
    }

    @Override
    public int process(int sample, int index) {
        int x = getXByIndex(index);
        int y = getYByIndex(index);
        double linearizedSample = linearizationTableLookup.lookup(sample);
        double blackLevelSubtrahend = calcBlackLevelSubtrahend(x, y);
        linearizedSample = linearizedSample - blackLevelSubtrahend;
        linearizedSample = linearizedSample * (1.0 / (whiteLevel[0] - blackLevelSubtrahend)); //todo white level
        return Math.in(0, (int) (linearizedSample * 65535), 65535);
    }

    private double calcBlackLevelSubtrahend(int x, int y) {
        double finalBlackLevel = blackLevelLookup.lookup(0);
        //origin of  BlackLevel is top-left corner of ActiveArea rectangle. See DNG Specification 1.5.0.0, page 26.
        if (x >= startXCoordinate && y >= startYCoordinate && x < width && y < height) {
            //TODO: Currenty only BlackLevelRepeatDim={1 1} and BlackLevelRepeatDim={2 2} are supported.
            if (blackLevelRepeatDim != null && blackLevelRepeatDim[0] != 1) {
                finalBlackLevel = blackLevelLookup.lookup(y % cfaRepeatPatternDim[0] * cfaRepeatPatternDim[1] + x % cfaRepeatPatternDim[1]);
            }
            int index = y - startYCoordinate;
            finalBlackLevel += blackLevelDeltaVLookup.lookup(index);
            index = x - startXCoordinate;
            finalBlackLevel += blackLevelDeltaHLookup.lookup(index);
        }

        return finalBlackLevel;
    }
}
