package de.unituebingen.dng.reader;

import java.nio.ByteOrder;
import java.util.Objects;

/**
 * @author Eugen Ljavin
 * <p>
 * A class which represents an image file header as definied in TIFF 6.0 specification.
 */
public class ImageFileHeader {

    public static final int LITTLE_ENDIAN_IDENTIFIER = 0x49;
    public static final int BIG_ENDIAN_IDENTIFIER = 0x4D;
    public static final int TIFF_FILE_IDENTIFIER = 0x2A;

    private ByteOrder byteOrder;
    private long firstIFDOffset;

    public ImageFileHeader(ByteOrder byteOrder, long firstIFDOffset) {
        this.byteOrder = Objects.requireNonNull(byteOrder);
        this.firstIFDOffset = firstIFDOffset;
    }

    public ByteOrder getByteOrder() {
        return byteOrder;
    }

    public long getFirstIFDOffset() {
        return firstIFDOffset;
    }
}
