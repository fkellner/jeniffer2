package de.unituebingen.dng.reader.util;

/**
 * @author Eugen Ljavin
 * <p>
 * A util class for math operations.
 */
public class Math {

    /**
     * Calculates the greatest common divisor of two integers m and n.
     *
     * @param m The first integer
     * @param n The second integer
     * @return The greatest common divisor of m and n
     */
    public static long greatestCommonDivisor(long m, long n) {
        if (n == 0) {
            return m;
        } else {
            return greatestCommonDivisor(n, m % n);
        }
    }

    /**
     * Calculates the least common multiple of two integers m and n.
     *
     * @param m The first integer
     * @param n The second integer
     * @return The least common multiple of m and n
     */
    public static long leastCommonMultiple(long m, long n) {
        long gcd = greatestCommonDivisor(m, n);
        long p = (m * n) / gcd;
        return p;
    }

    public static double in(double min, double x, double max) {
        return java.lang.Math.max(min, java.lang.Math.min(x, max));
    }

    public static int in(int min, int x, int max) {
        return java.lang.Math.max(min, java.lang.Math.min(x, max));
    }
}
