package de.unituebingen.dng.reader.util;

/**
 * @author Eugen Ljavin
 * <p>
 * A signed rational type as defined in TIFF 6.0 specification.
 */
public class SignedRational extends Number {

    private int numerator;
    private int denominator;

    public SignedRational(int numerator, int denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
    }

    @Override
    public int intValue() {
        return numerator / denominator;
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

    public int getNumerator() {
        return numerator;
    }

    public int getDenominator() {
        return denominator;
    }

    @Override
    public String toString() {
        return numerator + "/" + denominator;
    }
}
