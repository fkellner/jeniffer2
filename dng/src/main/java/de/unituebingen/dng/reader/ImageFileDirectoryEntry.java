package de.unituebingen.dng.reader;

import de.unituebingen.dng.reader.util.Rational;
import de.unituebingen.dng.reader.util.SignedRational;

import java.util.Objects;

/**
 * @author Eugen Ljavin
 * <p>
 * A class which represents an image file directory entry as specified in TIFF 6.0 specification.
 */
public class ImageFileDirectoryEntry {

    private DNGTag tag;
    private int tagId;
    private DNGFieldType fieldType;
    private long count;

    private Object values;

    public ImageFileDirectoryEntry(int tag, DNGFieldType fieldType, long count, Object values) {
        this.tag = DNGTag.getById(tag);
        this.fieldType = Objects.requireNonNull(fieldType);
        this.count = count;
        this.values = values;
        this.tagId = tag;
    }

    public Object getValues() {
        return values;
    }

    public DNGTag getTag() {
        return tag;
    }

    public int getTagId() {
        return tagId;
    }

    /**
     * Checks if the entry has multiple values.
     *
     * @return {@code true} if the entry has multiple values, {@code false if not}
     */
    public boolean hasMultipleValues() {
        return count > 1;
    }

    public String valuesAsString() {
        StringBuilder stringBuilder = new StringBuilder();
        if (values instanceof String[]) {
            String[] values = (String[]) this.values;
            for (String s : values) {
                stringBuilder.append(s);
            }
        } else if (values instanceof Rational[]) {
            Rational[] values = (Rational[]) this.values;
            for (Rational rational : values) {
                stringBuilder.append(rational.toString());
                stringBuilder.append(", ");
            }
        } else if (values instanceof SignedRational[]) {
            SignedRational[] values = (SignedRational[]) this.values;
            for (SignedRational rational : values) {
                stringBuilder.append(rational.toString());
                stringBuilder.append(", ");
            }
        } else if (values instanceof short[]) {
            short[] values = (short[]) this.values;
            for (short s : values) {
                stringBuilder.append(s);
                stringBuilder.append(", ");
            }
        } else if (values instanceof float[]) {
            float[] values = (float[]) this.values;
            for (float s : values) {
                stringBuilder.append(s);
                stringBuilder.append(", ");
            }
        } else if (values instanceof double[]) {
            double[] values = (double[]) this.values;
            for (double s : values) {
                stringBuilder.append(s);
                stringBuilder.append(", ");
            }
        } else if (values instanceof byte[]) {
            byte[] values = (byte[]) this.values;
            for (byte s : values) {
                stringBuilder.append(s);
                stringBuilder.append(", ");
            }
        } else if (values instanceof int[]) {
            int[] values = (int[]) this.values;
            for (int s : values) {
                stringBuilder.append(s);
                stringBuilder.append(", ");
            }
        } else if (values instanceof long[]) {
            long[] values = (long[]) this.values;
            for (long s : values) {
                stringBuilder.append(s);
                stringBuilder.append(", ");
            }
        } else {
            stringBuilder.append(values.toString());
        }
        return stringBuilder.toString();
    }
}
