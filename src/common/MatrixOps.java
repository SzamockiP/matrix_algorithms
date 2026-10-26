package common;

import java.util.concurrent.atomic.LongAdder;

public abstract class MatrixOps<A> {
    public final LongAdder adds = new LongAdder();
    public final LongAdder muls = new LongAdder();

    public Matrix<A> newMatrix(int rows, int cols) {
        return new Matrix<>(alloc(rows * cols), rows, cols);
    }

    public void resetCounters() {
        adds.reset();
        muls.reset();
    }

    // c = a + b (c może być a lub b)
    public final void add(MatrixView<A> a, MatrixView<A> b, MatrixView<A> c) {
        checkSame(a, b, c);
        addImpl(a, b, c);
        adds.add((long) a.rows * a.cols);
    }

    // c = a - b (c może być a lub b)
    public final void subtract(MatrixView<A> a, MatrixView<A> b, MatrixView<A> c) {
        checkSame(a, b, c);
        subtractImpl(a, b, c);
        adds.add((long) a.rows * a.cols);
    }

    // c = a * b (c nie może nachodzić na a ani b)
    public final void multiply(MatrixView<A> a, MatrixView<A> b, MatrixView<A> c) {
        checkMul(a, b, c);
        zeroImpl(c);
        multiplyAddImpl(a, b, c);
        muls.add((long) a.rows * a.cols * b.cols);
        adds.add((long) a.rows * (a.cols - 1) * b.cols);  // k-1 dodawań na element
    }

    // c += a * b (c nie może nachodzić na a ani b)
    public final void multiplyAdd(MatrixView<A> a, MatrixView<A> b, MatrixView<A> c) {
        checkMul(a, b, c);
        multiplyAddImpl(a, b, c);
        long n = (long) a.rows * a.cols * b.cols;
        muls.add(n);
        adds.add(n);
    }

    // c = -a (c może być a)
    public final void negate(MatrixView<A> a, MatrixView<A> c) {
        checkSame(a, a, c);
        negateImpl(a, c);
        adds.add((long) a.rows * a.cols);
    }

    // c = a^-1 metodą Gaussa-Jordana (a bez zmian, c nie może nachodzić na a)
    public final void invert(MatrixView<A> a, MatrixView<A> c) {
        checkSame(a, a, c);
        if (a.rows != a.cols)
            throw new IllegalArgumentException("Macierz musi być kwadratowa");
        invertImpl(a, c);
        long n = a.rows;
        muls.add(2 * n * n * n);  // z grubsza: n pivotów * n wierszy * 2n kolumn
        adds.add(2 * n * n * n);
    }

    protected abstract A alloc(int n);
    protected abstract void negateImpl(MatrixView<A> a, MatrixView<A> c);
    protected abstract void invertImpl(MatrixView<A> a, MatrixView<A> c);
    protected abstract void zeroImpl(MatrixView<A> c);
    protected abstract void addImpl(MatrixView<A> a, MatrixView<A> b, MatrixView<A> c);
    protected abstract void subtractImpl(MatrixView<A> a, MatrixView<A> b, MatrixView<A> c);
    protected abstract void multiplyAddImpl(MatrixView<A> a, MatrixView<A> b, MatrixView<A> c);

    private static void checkSame(MatrixView<?> a, MatrixView<?> b, MatrixView<?> c) {
        if (a.rows != b.rows || a.cols != b.cols || a.rows != c.rows || a.cols != c.cols)
            throw new IllegalArgumentException("Niezgodne wymiary macierzy");
    }

    private static void checkMul(MatrixView<?> a, MatrixView<?> b, MatrixView<?> c) {
        if (a.cols != b.rows || c.rows != a.rows || c.cols != b.cols)
            throw new IllegalArgumentException("Niezgodne wymiary macierzy");
    }
}
