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

    // c = a (c nie może nachodzić na a)
    public final void copy(MatrixView<A> a, MatrixView<A> c) {
        checkSame(a, a, c);
        copyImpl(a, c);
    }

    // a := LU w miejscu, bez pivotowania (L z jedynkową przekątną pod przekątną, U na i nad nią)
    public final void luInPlace(MatrixView<A> a) {
        if (a.rows != a.cols)
            throw new IllegalArgumentException("Macierz musi być kwadratowa");
        luInPlaceImpl(a);
        long n = a.rows;
        long upd = (n - 1) * n * (2 * n - 1) / 6;  // sum (n-1-k)^2
        muls.add(upd + n * (n - 1) / 2);           // + dzielenia
        adds.add(upd);
    }

    // b := L^-1 b w miejscu; L = dolny trójkąt l z jedynkową przekątną
    public final void solveUnitLower(MatrixView<A> l, MatrixView<A> b) {
        if (l.rows != l.cols || l.rows != b.rows)
            throw new IllegalArgumentException("Niezgodne wymiary macierzy");
        solveUnitLowerImpl(l, b);
        long n = l.rows, t = n * (n - 1) / 2 * b.cols;
        muls.add(t);
        adds.add(t);
    }

    // b := b U^-1 w miejscu; U = górny trójkąt u razem z przekątną
    public final void solveUpperRight(MatrixView<A> u, MatrixView<A> b) {
        if (u.rows != u.cols || u.rows != b.cols)
            throw new IllegalArgumentException("Niezgodne wymiary macierzy");
        solveUpperRightImpl(u, b);
        long n = u.rows, t = n * (n - 1) / 2 * b.rows;
        muls.add(t + n * b.rows);  // + dzielenia
        adds.add(t);
    }

    protected abstract A alloc(int n);
    protected abstract void copyImpl(MatrixView<A> a, MatrixView<A> c);
    protected abstract void luInPlaceImpl(MatrixView<A> a);
    protected abstract void solveUnitLowerImpl(MatrixView<A> l, MatrixView<A> b);
    protected abstract void solveUpperRightImpl(MatrixView<A> u, MatrixView<A> b);
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
