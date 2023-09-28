package de.unituebingen.dng.reader;

/**
 * @author Eugen Ljavin
 * <p>
 * Each type which is definied in the Tiff 6.0 Specification on page 15+16 is given as an enum.
 */
public enum DNGFieldType {

    BYTE(1), //8-bit unsigned integer. --> Java short since byte is signed in Java
    ASCII(1), //8-bit byte that contains a 7-bit ASCII code; the last byte must be NUL (binary zero). --> Java String
    SHORT(2), //16-bit (2-byte) unsigned integer. --> Java int since short is signed in Java
    LONG(4), //32-bit (4-byte) unsigned integer. --> Java long
    RATIONAL(8), //Two LONGs: the first represents the numerator of fraction; the second, the denominator. --> Java longs
    SBYTE(1), //An 8-bit signed (twos-complement) integer. --> Java byte
    UNDEFINED(1), //An 8-bit byte that may contain anything, depending on the definition of the field. --> Java byte
    SSHORT(2), //A 16-bit (2-byte) signed (twos-complement) integer. --> Java short
    SLONG(4), //A 32-bit (4-byte) signed (twos-complement) integer. --> Java int since int is 4 byte signed in Java
    SRATIONAL(8), //Two SLONGs: the first represents the numerator of a fraction, the second the denominator. --> Java ints
    FLOAT(4), //Single precision (4-byte) IEEE format. --> Java float
    DOUBLE(8); //Double precision (8-byte) IEEE format. --> Java double

    public final int bytes;

    DNGFieldType(int bytes) {
        this.bytes = bytes;
    }

    /**
     * Returns the field type as defined in TIFF 6.0 specification, page 15 and 16. <br/>
     * Note that the field type numbers start with 1 and not with 0.
     *
     * @param fieldType The number of the field type
     * @return The field type
     */
    public static DNGFieldType getFieldType(int fieldType) {
        DNGFieldType[] fieldTypes = DNGFieldType.values();
        return fieldType < 1 || fieldType > fieldTypes.length ? null : fieldTypes[fieldType - 1];
    }
}
