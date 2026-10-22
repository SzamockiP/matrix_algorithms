package common;

import java.util.Arrays;
import java.util.Random;

public final class DoubleOps extends MatrixOps<double[]> {

    public double get(MatrixView<double[]> v, int i, int j) { return v.data[v.checkedIdx(i, j)]; }
    public void set(MatrixView<double[]> v, int i, int j, double value) { v.data[v.checkedIdx(i, j)] = value; }

    public Matrix<double[]> random(int rows, int cols, double min, double max, Random rnd) {
        Matrix<double[]> m = newMatrix(rows, cols);
        MatrixView<double[]> v = m.view();
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                set(v, i, j, min + rnd.nextDouble() * (max - min));
        return m;
    }

    @Override
    protected double[] alloc(int n) { return new double[n]; }

    @Override
    protected void zeroImpl(MatrixView<double[]> c) {
        for (int i = 0; i < c.rows; i++) {
            int from = c.idx(i, 0);
            Arrays.fill(c.data, from, from + c.cols, 0.0);
        }
    }

    @Override
    protected void addImpl(MatrixView<double[]> a, MatrixView<double[]> b, MatrixView<double[]> c) {
        for (int i = 0; i < a.rows; i++)
            for (int j = 0; j < a.cols; j++)
                c.data[c.idx(i, j)] = a.data[a.idx(i, j)] + b.data[b.idx(i, j)];
    }

    @Override
    protected void subtractImpl(MatrixView<double[]> a, MatrixView<double[]> b, MatrixView<double[]> c) {
        for (int i = 0; i < a.rows; i++)
            for (int j = 0; j < a.cols; j++)
                c.data[c.idx(i, j)] = a.data[a.idx(i, j)] - b.data[b.idx(i, j)];
    }

    @Override
    protected void multiplyAddImpl(MatrixView<double[]> a, MatrixView<double[]> b, MatrixView<double[]> c) {
        for (int i = 0; i < a.rows; i++)
            for (int k = 0; k < a.cols; k++) {
                double aik = a.data[a.idx(i, k)];
                int cBase = c.idx(i, 0), bBase = b.idx(k, 0);
                for (int j = 0; j < b.cols; j++)
                    c.data[cBase + j] += aik * b.data[bBase + j];
            }
    }
}
