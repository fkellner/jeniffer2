package de.unituebingen.dng.reader;

/**
 * @author Eugen Ljavin
 * <p>
 * Some important TIFF conatsnts. <br/>
 * Note that the tags with the suffix 'SIZE' are always given in byte.
 */
public class DNGConstants {

    public static final int IFD_ENTRY_FIELD_TAG_SIZE = 2;
    public static final int IFD_ENTRY_FIELD_TYPE_SIZE = 2;
    public static final int IFD_ENTRY_FIELD_COUNT_SIZE = 4;
    public static final int IFD_ENTRY_FIELD_VALUE_OFFSET_SIZE = 4;
    public static final int IFD_ENTRY_TOTAL_SIZE = IFD_ENTRY_FIELD_TAG_SIZE + IFD_ENTRY_FIELD_TYPE_SIZE
            + IFD_ENTRY_FIELD_COUNT_SIZE + IFD_ENTRY_FIELD_VALUE_OFFSET_SIZE;
}
