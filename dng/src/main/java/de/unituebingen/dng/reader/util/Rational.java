package de.unituebingen.dng.reader.util;

/**
 * @author Eugen Ljavin
 * <p>
 * An unsigned rational type as defined in TIFF 6.0 specification.
 */
public class Rational extends Number {

    private long numerator;
    private long denominator;

    public Rational(long numerator, long denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
    }

    public Rational(long numerator) {
        this(numerator, 1);
    }

    @Override
    public int intValue() {
        return (int) (numerator / denominator);
    }

    @Override
    public long longValue() {
        return numerator / denominator;
    }

    @Override
    public float floatValue() {
        return ((float) numerator) / denominator;
    }

    @Override
    public double doubleValue() {
        return ((double) numerator) / denominator;
    }

    public double reciprocal() {
        return ((double) denominator) / numerator;
    }

    public long getNumerator() {
        return numerator;
    }

    public long getDenominator() {
        return denominator;
    }

    @Override
    public String toString() {
        return numerator + "/" + denominator;
    }
}
