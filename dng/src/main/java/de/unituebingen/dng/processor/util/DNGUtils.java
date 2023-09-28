package de.unituebingen.dng.processor.util;

import de.unituebingen.dng.reader.util.Rational;
import de.unituebingen.dng.reader.util.SignedRational;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import java.util.Objects;

public class DNGUtils {

    public static double[] rationalsToDoubles(Rational[] rationals) {
        Objects.requireNonNull(rationals);
        double[] doubles = new double[rationals.length];
        for (int i = 0; i < rationals.length; i++) {
            doubles[i] = rationals[i].doubleValue();
        }
        return doubles;
    }

    public static double[] signedRationalsToDoubles(SignedRational[] rationals) {
        Objects.requireNonNull(rationals);
        double[] doubles = new double[rationals.length];
        for (int i = 0; i < rationals.length; i++) {
            doubles[i] = rationals[i].doubleValue();
        }
        return doubles;
    }

    public static RealMatrix signedRationalsToRealMatrix(SignedRational[] rationals, int rows, int cols) {
        if (rationals == null) {
            return null;
        }
        if (rows * cols != rationals.length) {
            throw new IllegalArgumentException("rows and cols dimensions doesnt match.");
        }
        double[][] doubles = new double[rows][cols];
        int index = 0;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                doubles[row][col] = rationals[index].doubleValue();
                index++;
            }
        }
        return MatrixUtils.createRealMatrix(doubles);
    }

    public static RealMatrix rationalsToRealMatrix(Rational[] rationals, int rows, int cols) {
        if (rationals == null) {
            return null;
        }
        if (rows * cols != rationals.length) {
            throw new IllegalArgumentException("rows and cols dimensions doesnt match.");
        }
        double[][] doubles = new double[rows][cols];
        int index = 0;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                doubles[row][col] = rationals[index].doubleValue();
                index++;
            }
        }
        return MatrixUtils.createRealMatrix(doubles);
    }

    public static RealMatrix normalizeRows(RealMatrix matrix) {
        double[] sums = new double[matrix.getRowDimension()];

        for (int i = 0; i < matrix.getRowDimension(); i++) {
            for (int j = 0; j < matrix.getColumnDimension(); j++) {
                sums[i] += matrix.getEntry(i, j);
            }
        }

        for (int i = 0; i < matrix.getRowDimension(); i++) {
            for (int j = 0; j < matrix.getColumnDimension(); j++) {
                matrix.setEntry(i, j, matrix.getEntry(i, j) / sums[i]);
            }
        }

        return MatrixUtils.createRealMatrix(matrix.getData());
    }
}
