package de.unituebingen.dng.reader;

/**
 * @author Eugen Ljavin
 * <p>
 * Some TIFF tag constants as defined in the TIFF 6.0 specification
 */
public class DNGTagConstants {

    //NewSubfileType
    public static final long NEW_SUBFILE_TYPE__HIGH_RESOLUTION = 0;
    public static final long NEW_SUBFILE_TYPE__REDUCED_RESOLUTION = 1;
    public static final long NEW_SUBFILE_TYPE__SINGLE_PAGE = 2;
    public static final long NEW_SUBFILE_TYPE__TRANSPARENCY_MASK = 4;

    //PhotometricInterpretation
    public static final int PHOTOMETRIC_INTERPRETATION__WHITE_IS_ZERO = 0;
    public static final int PHOTOMETRIC_INTERPRETATION__BLACK_IS_ZERO = 1;
    public static final int PHOTOMETRIC_INTERPRETATION__RGB = 2;
    public static final int PHOTOMETRIC_INTERPRETATION__RGB_PALETTE = 3;
    public static final int PHOTOMETRIC_INTERPRETATION__TRANSPARENCY_MASK = 4;
    public static final int PHOTOMETRIC_INTERPRETATION__CMYK = 5;
    public static final int PHOTOMETRIC_INTERPRETATION__YCBCR = 6;
    public static final int PHOTOMETRIC_INTERPRETATION__CIELAB = 8;
    //PhotometricInterpretation defined in TIFF/EP
    public static final int PHOTOMETRIC_INTERPRETATION__CFA = 32803;
    //PhotometricInterpretation defined in DNG
    public static final int PHOTOMETRIC_INTERPRETATION__LINEAR_RAW = 34892;

    //Compression
    public static final int COMPRESSION__UNCOMPRESSED = 1;
    public static final int COMPRESSION__CCITT_1D = 2;
    public static final int COMPRESSION__GROUP_3_FAX = 3;
    public static final int COMPRESSION__GROUP_4_FAX = 4;
    public static final int COMPRESSION__LZW = 5;
    public static final int COMPRESSION__JPEG = 6;
    public static final int COMPRESSION__PACKBITS = 32773;
    //Compression defined in DNG
    public static final int COMPRESSION__JPEG_DCT_OR_LOSSLESS = 7;
    public static final int COMPRESSION__DEFLATE = 8;
    public static final int COMPRESSION__JPEG_LOSSY = 34892;

    //PlanarConfiguration
    public static final int PLANAR_CONFIGURATION__CHUNKY = 1;
    public static final int PLANAR_CONFIGURATION__PLANAR = 2;

    //Orientation
    public static final int ORIENTATION__HORIZONTAL_TOP = 1;
    public static final int ORIENTATION__HORIZONTAL_BOTTOM = 3;
    public static final int ORIENTATION__VETICAL_RIGHT = 8;
    public static final int ORIENTATION__VETICAL_LEFT = 6;
}
