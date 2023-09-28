package de.unituebingen.dng.reader;

import de.unituebingen.dng.reader.dng.opcode.Opcode;
import de.unituebingen.dng.reader.dng.opcode.OpcodeFactory;
import de.unituebingen.dng.reader.dng.util.CFAPattern;
import de.unituebingen.dng.reader.util.ByteUtil;
import de.unituebingen.dng.reader.util.Rational;
import de.unituebingen.dng.reader.util.SignedRational;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Map;

/**
 * @author Eugen Ljavin
 * <p>
 * A class which represents an image file directory as specified in TIFF 6.0 specification.
 */
public class ImageFileDirectory {

    private ImageFileDirectoryType type;
    private final Map<Integer, ImageFileDirectoryEntry> imageFileDirEntries;

    public ImageFileDirectory(Map<Integer, ImageFileDirectoryEntry> imageFileDirEntries, ImageFileDirectoryType imageFileDirectoryType) {
        this.imageFileDirEntries = imageFileDirEntries;
        this.type = imageFileDirectoryType;
    }

    public ImageFileDirectory(Map<Integer, ImageFileDirectoryEntry> imageFileDirEntries) {
        this(imageFileDirEntries, ImageFileDirectoryType.OTHER);
    }

    /**
     * Returns the {@link ImageFileDirectoryEntry} for the given tag.
     *
     * @param tag The {@link ImageFileDirectoryEntry} which should be returned for the given tag
     * @return The image file directory
     */
    public ImageFileDirectoryEntry getIFDEntry(int tag) {
        return imageFileDirEntries.get(tag);
    }

    /**
     * Returns the {@link ImageFileDirectoryEntry} for the given tag.
     *
     * @param tag The {@link ImageFileDirectoryEntry} which should be returned for the given tag
     * @return The image file directory
     */
    public ImageFileDirectoryEntry getIFDEntry(DNGTag tag) {
        return getIFDEntry(tag.getId());
    }

    /**
     * Checks if a given TIFF tag is included in the TIFF file.
     *
     * @param tag The tag to check for
     * @return {@code true} if the tag is included in the TIFF fiel, {@code false} if not
     */
    public boolean hasEntry(DNGTag tag) {
        return hasEntry(tag.getId());
    }

    /**
     * Checks if a given TIFF tag is included in the TIFF file.
     *
     * @param tag The tag to check for
     * @return {@code true} if the tag is included in the TIFF fiel, {@code false} if not
     */
    public boolean hasEntry(int tag) {
        return getIFDEntry(tag) != null;
    }

    public long getImageWidth() {
        return getLongValue(DNGTag.IMAGE_WIDTH);
    }

    public long getImageLength() {
        return getLongValue(DNGTag.IMAGE_LENGTH);
    }

    public int getPlanarConfiguration() {
        int planarConfiguration = getShortValue(DNGTag.PLANAR_CONFIGURATION);
        return planarConfiguration == -1 ? 1 : planarConfiguration;
    }

    public int[] getBitsPerSample() {
        return getShortArray(DNGTag.BITS_PER_SAMPLE);
    }

    public long[] getStripOffsets() {
        return getLongArray(DNGTag.STRIP_OFFSETS);
    }

    public long[] getStripByteCounts() {
        return getLongArray(DNGTag.STRIP_BYTE_COUNTS);
    }

    public int getSamplesPerPixel() {
        return getShortValue(DNGTag.SAMPLES_PER_PIXEL);
    }

    public int getPhotometricInterpretation() {
        return getShortValue(DNGTag.PHOTOMETRIC_INTERPRETATION);
    }

    public int getCompression() {
        return getShortValue(DNGTag.COMPRESSION);
    }

    public long getTileWidth() {
        return getLongValue(DNGTag.TILE_WIDTH);
    }

    public long getTileLength() {
        return getLongValue(DNGTag.TILE_LENGTH);
    }

    public long[] getTileOffsets() {
        return getLongArray(DNGTag.TILE_OFFSETS);
    }

    public long[] getTileByteCounts() {
        return getLongArray(DNGTag.TILE_BYTE_COUNTS);
    }

    public long[] getActiveArea() {
        return getLongArray(DNGTag.ACTIVE_AREA);
    }

    public Rational[] getDefaultCropOrigin() {
        return getRationalArray(DNGTag.DEFAULT_CROP_ORIGIN);
    }

    public Rational[] getDefaultCropSize() {
        return getRationalArray(DNGTag.DEFAULT_CROP_SIZE);
    }

    public int[] getLinearizationTable() {
        return getShortArray(DNGTag.LINEARIZATION_TABLE);
    }

    public int[] getBlackLevelRepeatDim() {
        return getShortArray(DNGTag.BLACK_LEVEL_REPEAT_DIM);
    }

    public Rational[] getAnalogBalance() {
        return getRationalArray(DNGTag.ANALOG_BALANCE);
    }

    public Rational[] getAsShotNeutral() {
        return getRationalArray(DNGTag.AS_SHOT_NEUTRAL);
    }

    public Rational[] getAsShotWhiteXY() {
        return getRationalArray(DNGTag.AS_SHOT_WHITE_XY);
    }

    public int[] getCFARepeatPatternDim() {
        return getShortArray(DNGTag.CFA_REPEAT_PATTERN_DIM);
    }

    public CFAPattern getCFAPattern() {
        short[] cfaPattern = getByteArray(DNGTag.CFA_PATTERN);
        return cfaPattern != null ? CFAPattern.getByCFAPattern(cfaPattern) : null;
    }

    public Rational[] getBlackLevel() {
        return getRationalArray(DNGTag.BLACK_LEVEL);
    }

    public SignedRational[] getBlackLevelDeltaH() {
        return getSignedRationalArray(DNGTag.BLACK_LEVEL_DELTA_H);
    }

    public SignedRational[] getBlackLevelDeltaV() {
        return getSignedRationalArray(DNGTag.BLACK_LEVEL_DELTA_V);
    }

    public long[] getWhiteLevel() {
        return getLongArray(DNGTag.WHITE_LEVEL);
    }

    public long getRowsPerStrip() {
        return getLongValue(DNGTag.ROWS_PER_STRIP);
    }

    public int getCalibrationIlluminant1() {
        return getShortValue(DNGTag.CALIBRATION_ILLUMINANT_1);
    }

    public int getCalibrationIlluminant2() {
        return getShortValue(DNGTag.CALIBRATION_ILLUMINANT_2);
    }

    public String getCameraCalibrationSignature() {
        return getStringValue(DNGTag.CAMERA_CALIBRATION_SIGNATURE);
    }

    public String getProfileCalibrationSignature() {
        return getStringValue(DNGTag.PROFILE_CALIBRATION_SIGNATURE);
    }

    public SignedRational[] getColorMatrix1() {
        return getSignedRationalArray(DNGTag.COLOR_MATRIX_1);
    }

    public SignedRational[] getColorMatrix2() {
        return getSignedRationalArray(DNGTag.COLOR_MATRIX_2);
    }

    public SignedRational[] getCameraCalibration1() {
        return getSignedRationalArray(DNGTag.CAMERA_CALIBRATION_1);
    }

    public SignedRational[] getCameraCalibration2() {
        return getSignedRationalArray(DNGTag.CAMERA_CALIBRATION_2);
    }

    public SignedRational[] getReductionMatrix1() {
        return getSignedRationalArray(DNGTag.REDUCTION_MATRIX_1);
    }

    public SignedRational[] getReductionMatrix2() {
        return getSignedRationalArray(DNGTag.REDUCTION_MATRIX_2);
    }

    public SignedRational[] getForwardMatrix1() {
        return getSignedRationalArray(DNGTag.FORWARD_MATRIX_1);
    }

    public SignedRational[] getForwardMatrix2() {
        return getSignedRationalArray(DNGTag.FORWARD_MATRIX_2);
    }

    public float[] getProfileHueSatMapData1() {
        return getFloatArray(DNGTag.PROFILE_HUE_SAT_MAP_DATA_1);
    }

    public float[] getProfileToneCurve() {
        return getFloatArray(DNGTag.PROFILE_TONE_CURVE);
    }

    public float[] getProfileHueSatMapData2() {
        return getFloatArray(DNGTag.PROFILE_HUE_SAT_MAP_DATA_2);
    }

    public float[] getProfileLookTableData() {
        return getFloatArray(DNGTag.PROFILE_LOOK_TABLE_DATA);
    }

    public long[] getProfileHueSatMapDims() {
        return getLongArray(DNGTag.PROFILE_HUE_SAT_MAP_DIMS);
    }

    public long[] getProfileLookTableDims() {
        return getLongArray(DNGTag.PROFILE_LOOK_TABLE_DIMS);
    }

    public long getProfileHueSatMapEncoding() {
        return getLongValue(DNGTag.PROFILE_HUE_SAT_MAP_ENCODING);
    }

    public int getOrientation() {
        return getShortValue(DNGTag.ORIENTATION);
    }

    //DNG Specification says, that BaselineExposureOffset is only of type "Rational" but there are some
    //DNG files which use SignedRational values. As a Workaround use Rational as SignedRational
    public Rational getBaselineExposureOffset() {
        ImageFileDirectoryEntry ifd = getIFDEntry(DNGTag.BASELINE_EXPOSURE_OFFSET);
        if (ifd != null) {
            Object values = ifd.getValues();
            if (values instanceof Rational) {
                return (Rational) values;
            } else {
                SignedRational value = (SignedRational) values;
                return new Rational(value.getNumerator(), value.getDenominator());
            }
        }
        return null;
    }

    public Rational getApertureValue() {
        return getRationalValue(DNGTag.APERTURE_VALUE);
    }

    public Rational getFocalLength() {
        return getRationalValue(DNGTag.FOCAL_LENGTH);
    }

    public Rational getFNumber() {
        return getRationalValue(DNGTag.F_NUMBER);
    }

    public Rational getExposureTime() {
        return getRationalValue(DNGTag.EXPOSURE_TIME);
    }

    public int getISOSpeedRatings() {
        return getShortValue(DNGTag.ISO_SPEED_RATINGS);
    }

    public SignedRational getBaselineExposure() {
        return getSignedRationalValue(DNGTag.BASELINE_EXPOSURE);
    }

    public SignedRational getShutterSpeedValue() {
        return getSignedRationalValue(DNGTag.SHUTTER_SPEED_VALUE);
    }

    public Opcode[] getOpcodeList1() {
        return getOpcodeList(DNGTag.OPCODE_LIST_1);
    }

    public String getDateTime() {
        return getStringValue(DNGTag.DATE_TIME);
    }

    public Opcode[] getOpcodeList2() {
        return getOpcodeList(DNGTag.OPCODE_LIST_2);
    }

    public Opcode[] getOpcodeList3() {
        return getOpcodeList(DNGTag.OPCODE_LIST_3);
    }

    private Opcode[] getOpcodeList(DNGTag opcode) {
        if (hasEntry(opcode)) {
            ByteBuffer opcodeData = ByteBuffer.wrap(ByteUtil.toByteArray((short[]) getIFDEntry(opcode).getValues())).order(ByteOrder.BIG_ENDIAN);
            int numberOfOpcodes = (int) (opcodeData.getInt() & 0xFFFFFFFFL);
            Opcode[] opcodes = new Opcode[numberOfOpcodes];

            for (int i = 0; i < numberOfOpcodes; i++) {
                long id = opcodeData.getInt() & 0xFFFFFFFFL;
                long dngSpecVersion = opcodeData.getInt() & 0xFFFFFFFFL;
                long flags = opcodeData.getInt() & 0xFFFFFFFFL;
                long numberOfBytes = opcodeData.getInt() & 0xFFFFFFFFL;
                byte[] remainingOpcodeData = new byte[(int) numberOfBytes];
                opcodeData.get(remainingOpcodeData);
                opcodes[i] = OpcodeFactory.getOpcodeByID(id, dngSpecVersion, flags, numberOfBytes, remainingOpcodeData);
            }
            return opcodes;
        }
        return null;
    }

    public void setType(ImageFileDirectoryType type) {
        this.type = type;
    }

    public ImageFileDirectoryType getType() {
        return type;
    }

    public Map<Integer, ImageFileDirectoryEntry> getImageFileDirectoryEntries() {
        return imageFileDirEntries;
    }

    private int getShortValue(DNGTag tag) {
        return hasEntry(tag) ? (int) getIFDEntry(tag).getValues() : -1;
    }

    private long getLongValue(DNGTag tag) {
        return hasEntry(tag) ? (long) getIFDEntry(tag).getValues() : -1;
    }

    private SignedRational getSignedRationalValue(DNGTag tag) {
        return hasEntry(tag) ? (SignedRational) getIFDEntry(tag).getValues() : null;
    }

    private Rational getRationalValue(DNGTag tag) {
        return hasEntry(tag) ? (Rational) getIFDEntry(tag).getValues() : null;
    }

    private float[] getFloatArray(DNGTag tag) {
        if (hasEntry(tag)) {
            ImageFileDirectoryEntry ifd = getIFDEntry(tag);
            return ifd.hasMultipleValues() ? (float[]) ifd.getValues() : new float[]{(float) ifd.getValues()};
        }
        return null;
    }

    private int[] getShortArray(DNGTag tag) {
        if (hasEntry(tag)) {
            ImageFileDirectoryEntry ifd = getIFDEntry(tag);
            return ifd.hasMultipleValues() ? (int[]) ifd.getValues() : new int[]{(int) ifd.getValues()};
        }
        return null;
    }

    private short[] getByteArray(DNGTag tag) {
        if (hasEntry(tag)) {
            ImageFileDirectoryEntry ifd = getIFDEntry(tag);
            return ifd.hasMultipleValues() ? (short[]) ifd.getValues() : new short[]{(short) ifd.getValues()};
        }
        return null;
    }

    private long[] getLongArray(DNGTag tag) {
        if (hasEntry(tag)) {
            ImageFileDirectoryEntry ifd = getIFDEntry(tag);
            Object values = ifd.getValues();
            if (values instanceof long[]) {
                return (long[]) ifd.getValues();
            }
            if (values instanceof int[]) {
                return Arrays.stream((int[]) values).asLongStream().toArray();
            }
            if (values instanceof Long) {
                return new long[]{(long) values};
            }
            if (values instanceof Integer) {
                return new long[]{(int) values};
            }
        }
        return null;
    }

    private String getStringValue(DNGTag tag) {
        String[] value = hasEntry(tag) ? (String[]) getIFDEntry(tag).getValues() : null;

        if (value != null) {
            String joinedString = String.join("", value);
            return joinedString.trim();
        }
        return null;
    }

    private Rational[] getRationalArray(DNGTag tag) {
        if (hasEntry(tag)) {
            ImageFileDirectoryEntry ifd = getIFDEntry(tag);
            Object values = ifd.getValues();
            if (values instanceof int[]) {
                return Arrays.stream(Arrays.stream((int[]) values).mapToObj(value -> new Rational(value, 1)).toArray()).toArray(Rational[]::new);
            } else if (values instanceof long[]) {
                return Arrays.stream(Arrays.stream((long[]) values).mapToObj(value -> new Rational(value, 1)).toArray()).toArray(Rational[]::new);
            } else if (values instanceof Integer) {
                return new Rational[]{new Rational((int) values, 1)};
            } else if (values instanceof Long) {
                return new Rational[]{new Rational((long) values, 1)};
            } else if (values instanceof Rational) {
                return new Rational[]{(Rational) values};
            } else {
                return (Rational[]) values;
            }
        }
        return null;
    }

    private SignedRational[] getSignedRationalArray(DNGTag tag) {
        if (hasEntry(tag)) {
            ImageFileDirectoryEntry ifd = getIFDEntry(tag);
            return ifd.hasMultipleValues() ? (SignedRational[]) ifd.getValues() : new SignedRational[]{(SignedRational) ifd.getValues()};
        }
        return null;
    }
}
