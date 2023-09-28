package de.unituebingen.dng.reader;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Eugen Ljavin
 * <p>
 * An enum defining all possible TIFF tags as specified in TIFF 6.0 specification.
 */
public enum DNGTag {

    //TIFF Baseline
    ARTIST("Artist", 315),
    BITS_PER_SAMPLE("BitsPerSample", 258),
    CELL_LENGTH("CellLength", 265),
    CELL_WIDTH("CellWidth", 264),
    COLOR_MAP("ColorMap", 320),
    COMPRESSION("Compression", 259),
    COPYRIGHT("Copyright", 33432),
    DATE_TIME("DateTime", 306),
    EXTRA_SAMPLES("ExtraSamples", 338),
    FILL_ORDER("FillOrder", 266),
    FREE_BYTE_COUNTS("FreeByteCounts", 289),
    FREE_OFFSETS("FreeOffsets", 288),
    GRAY_RESPONSE_CURVE("GrayResponseCurve", 291),
    GRAY_RESPONSE_UNIT("GrayResponseUnit", 290),
    HOST_COMPUTER("HostComputer", 316),
    IMAGE_DESCRIPTION("ImageDescription", 270),
    IMAGE_LENGTH("ImageLength", 257),
    IMAGE_WIDTH("ImageWidth", 256),
    MAKE("Make", 271),
    MAX_SAMPLE_VALUE("MaxSampleValue", 281),
    MIN_SAMPLE_VALUE("MinSampleValue", 280),
    MODEL("Model", 272),
    NEW_SUBFILE_TYPE("NewSubfileType", 254),
    ORIENTATION("Orientation", 274),
    PHOTOMETRIC_INTERPRETATION("PhotometricInterpretation", 262),
    PLANAR_CONFIGURATION("PlanarConfiguration", 284),
    RESOLUTION_UNIT("ResolutionUnit", 296),
    ROWS_PER_STRIP("RowsPerStrip", 278),
    SAMPLES_PER_PIXEL("SamplesPerPixel", 277),
    SOFTWARE("Software", 305),
    STRIP_BYTE_COUNTS("StripByteCounts", 279),
    STRIP_OFFSETS("StripOffsets", 273),
    SUBFILE_TYPE("SubfileType", 255),
    THRESHHOLDING("Threshholding", 263),
    X_RESOLUTION("XResolution", 282),
    Y_RESOLUTION("YResolution", 283),

    //TIFF Extended
    BAD_FAX_LINES("BadFaxLines", 326),
    CLEAN_FAX_DATA("CleanFaxData", 327),
    CLIP_PATH("ClipPath", 343),
    CONSECUTIVE_BAD_FAX_LINES("ConsecutiveBadFaxLines", 328),
    DECODE("Decode", 433),
    DEFAULT_IMAGE_COLOR("DefaultImageColor", 434),
    DOCUMENT_NAME("DocumentName", 269),
    DOT_RANGE("DotRange", 336),
    HALFTONE_HINTS("HalftoneHints", 321),
    INDEXED("Indexed", 346),
    JPEG_TABLES("JPEGTables", 347),
    PAGE_NAME("PageName", 285),
    PAGE_NUMBER("PageNumber", 297),
    PREDICTOR("Predictor", 317),
    PRIMARY_CHROMATICITIES("PrimaryChromaticities", 319),
    REFERENCE_BLACK_WHITE("ReferenceBlackWhite", 532),
    SAMPLE_FORMAT("SampleFormat", 339),
    S_MIN_SAMPLE_VALUE("SMinSampleValue", 340),
    S_MAX_SAMPLE_VALUE("SMaxSampleValue", 341),
    STRIP_ROW_COUNTS("StripRowCounts", 559),
    SUB_IFDS("SubIFDs", 330),
    T4_OPTIONS("T4Options", 292),
    T6_OPTIONS("T6Options", 293),
    TILE_BYTE_COUNTS("TileByteCounts", 325),
    TILE_LENGTH("TileLength", 323),
    TILE_OFFSETS("TileOffsets", 324),
    TILE_WIDTH("TileWidth", 322),
    TRANSFER_FUNCTION("TransferFunction", 301),
    WHITE_POINT("WhitePoint", 318),
    X_CLIP_PATH_UNITS("XClipPathUnits", 344),
    X_POSITION("XPosition", 286),
    YCBCR_COEFFICIENTS("YCbCrCoefficients", 529),
    YCBCR_POSITIONING("YCbCrPositioning", 531),
    YCBCR_SUBSAMPLING("YCbCrSubSampling", 530),
    Y_CLIP_PATH_UNITS("YClipPathUnits", 345),
    Y_POSITION("YPosition", 287),

    //JPEG
    JPEG_PROC("JPEGProc", 512),
    JPEG_INTERCHANGE_FORMAT("JPEGInterchangeFormat", 513),
    JPEG_INTERCHANGE_FORMAT_LENGTH("JPEGInterchangeFormatLength", 514),
    JPEG_RESTART_INTERVAL("JPEGRestartInterval", 515),
    JPEG_LOSSLESS_PREDICTORS("JPEGLosslessPredictors", 517),
    JPEG_POINT_TRANSFORMS("JPEGPointTransforms", 518),
    JPEGQ_TABLES("JPEGQTables", 519),
    JPEGDC_TABLES("JPEGDCTables", 520),
    JPEGAC_TABLES("JPEGACTables", 521),

    //EXIF
    APERTURE_VALUE("ApertureValue", 37378),
    COLOR_SPACE("ColorSpace", 40961),
    DATE_TIME_DIGITIZED("DateTimeDigitized", 36868),
    DATE_TIME_ORIGINAL("DateTimeOriginal", 36867),
    EXIF_IFD("ExifIFD", 34665),
    EXIF_VERSION("ExifVersion", 36864),
    EXPOSURE_TIME("ExposureTime", 33434),
    FILE_SOURCE("FileSource", 41728),
    FLASH("Flash", 37385),
    FLASHPIX_VERSION("FlashpixVersion", 40960),
    F_NUMBER("FNumber", 33437),
    IMAGE_UNIQUE_ID("ImageUniqueID", 42016),
    LIGHT_SOURCE("LightSource", 37384),
    MAKER_NOTE("MakerNote", 37500),
    SHUTTER_SPEED_VALUE("ShutterSpeedValue", 37377),
    USER_COMMENT("UserComment", 37510),
    ISO_SPEED_RATINGS("ISOSpeedRatings", 34855),
    FOCAL_LENGTH("FocalLength", 37386),

    //IPTC
    IPTC("IPTC", 33723),

    //ICC
    ICC_PROFILE("ICCProfile", 34675),

    //XMP
    XMP("XMP", 700),

    //GDAL
    GDAL_METADATA("GDAL_METADATA", 42112),
    GDAL_NODATA("GDAL_NODATA", 42113),

    //Photoshop
    PHOTOSHOP("Photoshop", 34377),

    //GeoTiff
    MODEL_PIXEL_SCALE("ModelPixelScale", 33550),
    MODEL_TIEPOINT("ModelTiepoint", 33922),
    MODEL_TRANSFORMATION("ModelTransformation", 34264),
    GEO_KEY_DIRECTORY("GeoKeyDirectory", 34735),
    GEO_DOUBLE_PARAMS("GeoDoubleParams", 34736),
    GEO_ASCII_PARAMS("GeoAsciiParams", 34737),

    //TIFF-EP
    CFA_REPEAT_PATTERN_DIM("CFARepeatPatternDim", 33421),
    CFA_PATTERN("CFAPattern", 33422),

    //DNG 1.5.0.0
    DNG_VERSION("DNGVersion", 50706),
    DNG_BACKWARD_VERSION("DNGBackwardVersion", 50707),
    UNIQUE_CAMERA_MODEL("UniqueCameraModel", 50708),
    LOCALIZED_CAMERA_MODEL("LocalizedCameraModel", 50709),
    CFA_PLANE_COLOR("CFAPlaneColor", 50710),
    CFA_LAYOUT("CFALayout", 50711),
    LINEARIZATION_TABLE("LinearizationTable", 50712),
    BLACK_LEVEL_REPEAT_DIM("BlackLevelRepeatDim", 50713),
    BLACK_LEVEL("BlackLevel", 50714),
    BLACK_LEVEL_DELTA_H("BlackLevelDeltaH", 50715),
    BLACK_LEVEL_DELTA_V("BlackLevelDeltaV", 50716),
    WHITE_LEVEL("WhiteLevel", 50717),
    DEFAULT_SACLE("DefaultScale", 50718),
    BEST_QUALITY_SCALE("BestQualityScale", 50780),
    DEFAULT_CROP_ORIGIN("DefaultCropOrigin", 50719),
    DEFAULT_CROP_SIZE("DefaultCropSize", 50720),
    CALIBRATION_ILLUMINANT_1("CalibrationIlluminant1", 50778),
    CALIBRATION_ILLUMINANT_2("CalibrationIlluminant2", 50779),
    COLOR_MATRIX_1("ColorMatrix1", 50721),
    COLOR_MATRIX_2("ColorMatrix2", 50722),
    CAMERA_CALIBRATION_1("CameraCalibration1", 50723),
    CAMERA_CALIBRATION_2("CameraCalibration2", 50724),
    REDUCTION_MATRIX_1("ReductionMatrix1", 50725),
    REDUCTION_MATRIX_2("ReductionMatrix2", 50726),
    ANALOG_BALANCE("AnalogBalance", 50727),
    AS_SHOT_NEUTRAL("AsShotNeutral", 50728),
    AS_SHOT_WHITE_XY("AsShotWhiteXY", 50729),
    BASELINE_EXPOSURE("BaselineExposure", 50730),
    BASELINE_NOISE("BaselineNoise", 50731),
    BASELINE_SHARPNESS("BaselineSharpness", 50732),
    BAYER_GREEN_SPLIT("BayerGreenSplit", 50733),
    LINEAR_RESPONSE_LIMIT("LinearResponseLimit", 50734),
    CAMERA_SERIAL_NUMBER("CameraSerialNumber", 50735),
    LENS_INFO("LensInfo", 50736),
    CHROMA_BLUR_RADIUS("ChromaBlurRadius", 50737),
    ANTI_ALIAS_STRENGTH("AntiAliasStrength", 50738),
    SHADOW_SCALE("ShadowScale", 50739),
    DNG_PRIVATE_DATA("DNGPrivateData", 50740),
    MAKER_NOTE_SAFETY("MakerNoteSafety", 50741),
    RAW_DATA_UNIQUE_ID("RawDataUniqueID", 50781),
    ORIGINAL_RAW_FILE_NAME("OriginalRawFileName", 50827),
    ORIGINAL_RAW_FILE_DATA("OriginalRawFileData", 50828),
    ACTIVE_AREA("ActiveArea", 50829),
    MASKED_AREAS("MaskedAreas", 50830),
    AS_SHOT_ICC_PROFILE("AsShotICCProfile", 50831),
    AS_SHOT_PRE_PROFILE_MATRIX("AsShotPreProfileMatrix", 50832),
    CURRENT_ICC_PROFILE("CurrentICCProfile", 50833),
    CURRENT_PRE_PROFILE_MATRIX("CurrentPreProfileMatrix", 50834),
    COLORIMETRIC_REFERENCE("ColorimetrixReference", 50879),
    CAMERA_CALIBRATION_SIGNATURE("CameraCalibrationSignature", 50931),
    PROFILE_CALIBRATION_SIGNATURE("ProfileCalibrationSignature", 50932),
    EXTRA_CAMERA_PROFILES("ExtraCameraProfiles", 50933),
    AS_SHOT_PROFILE_NAME("AsShotProfileName", 50934),
    NOISE_REDUCTION_APPLIED("NoiseReductionApplied", 50935),
    PROFILE_NAME("ProfileName", 50936),
    PROFILE_HUE_SAT_MAP_DIMS("ProfileHueSatMapDims", 50937),
    PROFILE_HUE_SAT_MAP_DATA_1("ProfileHueSatMapData1", 50938),
    PROFILE_HUE_SAT_MAP_DATA_2("ProfileHueSatMapData2", 50939),
    PROFILE_TONE_CURVE("ProfileToneCurve", 50940),
    PROFILE_EMBED_POLICY("ProfileEmbedPolicy", 50941),
    PROFILE_COPYRIGHT("ProfileCopyright", 50942),
    FORWARD_MATRIX_1("ForwardMatrix1", 50964),
    FORWARD_MATRIX_2("ForwardMatrix2", 50965),
    PREVIEW_APPLICATION_NAME("PreviewApplicationName", 50966),
    PREVIEW_APPLICATION_VERSION("PreviewApplicationVersion", 50967),
    PREVIEW_SETTINGS_NAME("PreviewSettingsName", 50968),
    PREVIEW_SETTINGS_DIGEST("PreviewSettingsDigest", 50969),
    PREVIEW_COLOR_SPACE("PreviewColorSpace", 50970),
    PREVIEW_DATE_TIME("PreviewDateTime", 50971),
    RAW_IMAGE_DIGEST("RawImageDigest", 50972),
    ORIGINAL_RAW_FILE_DIGEST("OriginalRawFileDigest", 50973),
    SUB_TILE_BLOCK_SIZE("SubTileBlockSize", 50974),
    ROW_INTERLEAVE_FACTOR("RowInterleaveFactor", 50975),
    PROFILE_LOOK_TABLE_DIMS("ProfileLookTableDims", 50981),
    PROFILE_LOOK_TABLE_DATA("ProfileLookTableData", 50982),
    OPCODE_LIST_1("OpcodeList1", 51008),
    OPCODE_LIST_2("OpcodeList2", 51009),
    OPCODE_LIST_3("OpcodeList3", 51022),
    NOISE_PROFILE("NoiseProfile", 51041),
    DEFAULT_USER_CROP("DefaultUserCrop", 51125),
    DEFAULT_BLACK_RENDERER("DefaultBlackRenderer", 51110),
    BASELINE_EXPOSURE_OFFSET("BaselineExposureOffset", 51109),
    PROFILE_LOOK_TABLE_ENCODING("ProfileLookTableEncoding", 51108),
    PROFILE_HUE_SAT_MAP_ENCODING("ProfileHueSatMapEncoding", 51107),
    ORIGINAL_DEFAULT_FINAL_SIZE("OriginalDefaultFinalSize", 51089),
    ORIGINAL_BEST_QUALITY_FINAL_SIZE("OriginalBestQualityFinalSize", 51090),
    ORIGINAL_DEFAULT_CROP_SIZE("OriginalDefaultCropSize", 51091),
    NEW_RAW_IMAGE_DIGEST("NewRawImageDigest", 51111),
    RAW_TO_PREVIEW_GAIN("RawToPreviewGain", 51112),
    DEPTH_FORMAT("DepthFormat", 51177),
    DEPTH_NEAR("DepthNear", 51178),
    DEPTH_FAR("DepthFar", 51179),
    DEPTH_UTILS("DepthUtils", 51180),
    DEPTH_MEASURE_TYPE("DepthMeasureType", 51181),
    ENHANCED_PARAMS("EnhanceParams", 51182);

    private static final Map<Integer, DNGTag> idMapping = new HashMap<>();

    static {
        for (DNGTag fieldTag : DNGTag.values()) {
            idMapping.put(fieldTag.getId(), fieldTag);
        }
    }

    private final int id;
    private final String label;

    DNGTag(String label, int id) {
        this.label = label;
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public static DNGTag getById(int id) {
        DNGTag fieldTag = idMapping.get(id);
        return fieldTag;
    }
}
