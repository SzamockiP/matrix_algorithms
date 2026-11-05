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
    protected void negateImpl(MatrixView<double[]> a, MatrixView<double[]> c) {
        for (int i = 0; i < a.rows; i++)
            for (int j = 0; j < a.cols; j++)
                c.data[c.idx(i, j)] = -a.data[a.idx(i, j)];
    }

    // Gauss-Jordan z częściowym wyborem elementu głównego; c startuje jako I
    @Override
    protected void invertImpl(MatrixView<double[]> a, MatrixView<double[]> c) {
        int n = a.rows;
        double[] w = new double[n * n];
        for (int i = 0; i < n; i++)
            System.arraycopy(a.data, a.idx(i, 0), w, i * n, n);
        zeroImpl(c);
        for (int i = 0; i < n; i++)
            c.data[c.idx(i, i)] = 1.0;

        for (int p = 0; p < n; p++) {
            int best = p;
            for (int r = p + 1; r < n; r++)
                if (Math.abs(w[r * n + p]) > Math.abs(w[best * n + p])) best = r;
            if (w[best * n + p] == 0.0)
                throw new ArithmeticException("Macierz osobliwa");
            if (best != p) {
                for (int j = 0; j < n; j++) {
                    double t = w[p * n + j]; w[p * n + j] = w[best * n + j]; w[best * n + j] = t;
                    int ip = c.idx(p, j), ib = c.idx(best, j);
                    t = c.data[ip]; c.data[ip] = c.data[ib]; c.data[ib] = t;
                }
            }
            double inv = 1.0 / w[p * n + p];
            for (int j = 0; j < n; j++) {
                w[p * n + j] *= inv;
                c.data[c.idx(p, j)] *= inv;
            }
            for (int r = 0; r < n; r++) {
                double f = w[r * n + p];
                if (r == p || f == 0.0) continue;
                for (int j = 0; j < n; j++) {
                    w[r * n + j] -= f * w[p * n + j];
                    c.data[c.idx(r, j)] -= f * c.data[c.idx(p, j)];
                }
            }
        }
    }

    @Override
    protected void copyImpl(MatrixView<double[]> a, MatrixView<double[]> c) {
        for (int i = 0; i < a.rows; i++)
            System.arraycopy(a.data, a.idx(i, 0), c.data, c.idx(i, 0), a.cols);
    }

    @Override
    protected void luInPlaceImpl(MatrixView<double[]> a) {
        int n = a.rows;
        for (int k = 0; k < n; k++) {
            int top = a.idx(k, 0);
            double p = a.data[top + k];
            if (p == 0.0)
                throw new ArithmeticException("Zerowy element główny (brak pivotowania)");
            for (int i = k + 1; i < n; i++) {
                int row = a.idx(i, 0);
                double f = a.data[row + k] / p;
                a.data[row + k] = f;
                for (int j = k + 1; j < n; j++)
                    a.data[row + j] -= f * a.data[top + j];
            }
        }
    }

    @Override
    protected void solveUnitLowerImpl(MatrixView<double[]> l, MatrixView<double[]> b) {
        for (int i = 1; i < l.rows; i++)
            for (int k = 0; k < i; k++) {
                double f = l.data[l.idx(i, k)];
                int bi = b.idx(i, 0), bk = b.idx(k, 0);
                for (int j = 0; j < b.cols; j++)
                    b.data[bi + j] -= f * b.data[bk + j];
            }
    }

    @Override
    protected void solveUpperRightImpl(MatrixView<double[]> u, MatrixView<double[]> b) {
        int n = u.rows;
        for (int r = 0; r < b.rows; r++) {
            int base = b.idx(r, 0);
            for (int j = 0; j < n; j++) {
                double s = b.data[base + j];
                for (int k = 0; k < j; k++)
                    s -= b.data[base + k] * u.data[u.idx(k, j)];
                b.data[base + j] = s / u.data[u.idx(j, j)];
            }
        }
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
