package multiplication;

import common.MatrixOps;
import common.MatrixView;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

public final class Strassen {
    private Strassen() {
    }

    // c = a * b sekwencyjnie
    public static <A> void multiply(MatrixOps<A> ops, MatrixView<A> a, MatrixView<A> b, MatrixView<A> c, int leaf) {
        checkLeaf(leaf);
        strassenMultiply(ops, a, b, c, leaf, null);
    }

    // c = a * b na wątkach z puli
    public static <A> void multiply(MatrixOps<A> ops, MatrixView<A> a, MatrixView<A> b, MatrixView<A> c, int leaf,
            ForkJoinPool pool) {
        checkLeaf(leaf);
        pool.invoke(ForkJoinTask.adapt(() -> strassenMultiply(ops, a, b, c, leaf, pool)));
    }

    private static void checkLeaf(int leaf) {
        if (leaf < 1)
            throw new IllegalArgumentException("leaf musi być >= 1");
    }

    private static <A> void strassenMultiply(MatrixOps<A> ops, MatrixView<A> a, MatrixView<A> b, MatrixView<A> c,
            int leaf, ForkJoinPool pool) {
        int m = a.rows(), k = a.cols(), n = b.cols();

        if (Math.max(m, Math.max(k, n)) <= leaf || m < 2 || k < 2 || n < 2) {
            ops.multiply(a, b, c);
            return;
        }

        int m1 = m - m % 2;
        int k1 = k - k % 2;
        int n1 = n - n % 2;

        if (m1 != m || k1 != k || n1 != n) {
            if (pool != null) {
                ForkJoinTask<?> fRight = null;
                ForkJoinTask<?> fBottom = null;

                if (n1 < n) {
                    fRight = ForkJoinTask.adapt(() -> {
                        ops.multiply(a, b.block(0, n1, k, n - n1), c.block(0, n1, m, n - n1));
                    }).fork();
                }

                if (m1 < m) {
                    fBottom = ForkJoinTask.adapt(() -> {
                        ops.multiply(a.block(m1, 0, m - m1, k), b.block(0, 0, k, n1), c.block(m1, 0, m - m1, n1));
                    }).fork();
                }

                strassenMultiply(ops, a.block(0, 0, m1, k1), b.block(0, 0, k1, n1), c.block(0, 0, m1, n1), leaf, pool);

                if (k1 < k) {
                    ops.multiplyAdd(a.block(0, k1, m1, k - k1), b.block(k1, 0, k - k1, n1), c.block(0, 0, m1, n1));
                }

                if (fBottom != null) fBottom.join();
                if (fRight != null) fRight.join();
            } else {
                strassenMultiply(ops, a.block(0, 0, m1, k1), b.block(0, 0, k1, n1), c.block(0, 0, m1, n1), leaf, pool);

                if (k1 < k) {
                    ops.multiplyAdd(a.block(0, k1, m1, k - k1), b.block(k1, 0, k - k1, n1), c.block(0, 0, m1, n1));
                }

                if (n1 < n) {
                    ops.multiply(a, b.block(0, n1, k, n - n1), c.block(0, n1, m, n - n1));
                }

                if (m1 < m) {
                    ops.multiply(a.block(m1, 0, m - m1, k), b.block(0, 0, k, n1), c.block(m1, 0, m - m1, n1));
                }
            }
            return;
        }

        int m2 = m / 2, k2 = k / 2, n2 = n / 2;

        // Bloki A (wiersz, kolumna, wysokość, szerokość)
        var a11 = a.block(0, 0, m2, k2);
        var a12 = a.block(0, k2, m2, k2);
        var a21 = a.block(m2, 0, m2, k2);
        var a22 = a.block(m2, k2, m2, k2);

        // Bloki B
        var b11 = b.block(0, 0, k2, n2);
        var b12 = b.block(0, n2, k2, n2);
        var b21 = b.block(k2, 0, k2, n2);
        var b22 = b.block(k2, n2, k2, n2);

        // Bloki C (wynik)
        var c11 = c.block(0, 0, m2, n2);
        var c12 = c.block(0, n2, m2, n2);
        var c21 = c.block(m2, 0, m2, n2);
        var c22 = c.block(m2, n2, m2, n2);

        // Bufory na wyniki M1–M7 (7 bloków jako widoki jednego bufora)
        var mBuf = ops.newMatrix(7 * m2, n2).view();
        var m1r = mBuf.block(0 * m2, 0, m2, n2);
        var m2r = mBuf.block(1 * m2, 0, m2, n2);
        var m3r = mBuf.block(2 * m2, 0, m2, n2);
        var m4r = mBuf.block(3 * m2, 0, m2, n2);
        var m5r = mBuf.block(4 * m2, 0, m2, n2);
        var m6r = mBuf.block(5 * m2, 0, m2, n2);
        var m7r = mBuf.block(6 * m2, 0, m2, n2);

        // Arena na tymczasowe sumy: po jednym lA (5) i lB (5) na zadanie
        var tA = ops.newMatrix(5 * m2, k2).view();
        var tB = ops.newMatrix(5 * k2, n2).view();

        // M1 = strassen(A11 + A22, B11 + B22)
        Runnable t1 = () -> {
            var lA = tA.block(0 * m2, 0, m2, k2);
            var lB = tB.block(0 * k2, 0, k2, n2);
            ops.add(a11, a22, lA);
            ops.add(b11, b22, lB);
            strassenMultiply(ops, lA, lB, m1r, leaf, pool);
        };

        // M2 = strassen(A21 + A22, B11)
        Runnable t2 = () -> {
            var lA = tA.block(1 * m2, 0, m2, k2);
            ops.add(a21, a22, lA);
            strassenMultiply(ops, lA, b11, m2r, leaf, pool);
        };

        // M3 = strassen(A11, B12 - B22)
        Runnable t3 = () -> {
            var lB = tB.block(1 * k2, 0, k2, n2);
            ops.subtract(b12, b22, lB);
            strassenMultiply(ops, a11, lB, m3r, leaf, pool);
        };

        // M4 = strassen(A22, B21 - B11)
        Runnable t4 = () -> {
            var lB = tB.block(2 * k2, 0, k2, n2);
            ops.subtract(b21, b11, lB);
            strassenMultiply(ops, a22, lB, m4r, leaf, pool);
        };

        // M5 = strassen(A11 + A12, B22)
        Runnable t5 = () -> {
            var lA = tA.block(2 * m2, 0, m2, k2);
            ops.add(a11, a12, lA);
            strassenMultiply(ops, lA, b22, m5r, leaf, pool);
        };

        // M6 = strassen(A21 - A11, B11 + B12)
        Runnable t6 = () -> {
            var lA = tA.block(3 * m2, 0, m2, k2);
            var lB = tB.block(3 * k2, 0, k2, n2);
            ops.subtract(a21, a11, lA);
            ops.add(b11, b12, lB);
            strassenMultiply(ops, lA, lB, m6r, leaf, pool);
        };

        // M7 = strassen(A12 - A22, B21 + B22)
        Runnable t7 = () -> {
            var lA = tA.block(4 * m2, 0, m2, k2);
            var lB = tB.block(4 * k2, 0, k2, n2);
            ops.subtract(a12, a22, lA);
            ops.add(b21, b22, lB);
            strassenMultiply(ops, lA, lB, m7r, leaf, pool);
        };

        if (pool != null) {
            var f1 = ForkJoinTask.adapt(t1).fork();
            var f2 = ForkJoinTask.adapt(t2).fork();
            var f3 = ForkJoinTask.adapt(t3).fork();
            var f4 = ForkJoinTask.adapt(t4).fork();
            var f5 = ForkJoinTask.adapt(t5).fork();
            var f6 = ForkJoinTask.adapt(t6).fork();
            t7.run();
            f6.join();
            f5.join();
            f4.join();
            f3.join();
            f2.join();
            f1.join();
        } else {
            t1.run();
            t2.run();
            t3.run();
            t4.run();
            t5.run();
            t6.run();
            t7.run();
        }

        // Składanie wyniku z M1–M7
        Runnable s11 = () -> {
            // C11 = M1 + M4 - M5 + M7
            ops.add(m1r, m4r, c11);
            ops.subtract(c11, m5r, c11);
            ops.add(c11, m7r, c11);
        };

        Runnable s12 = () -> {
            // C12 = M3 + M5
            ops.add(m3r, m5r, c12);
        };

        Runnable s21 = () -> {
            // C21 = M2 + M4
            ops.add(m2r, m4r, c21);
        };

        Runnable s22 = () -> {
            // C22 = M1 - M2 + M3 + M6
            ops.subtract(m1r, m2r, c22);
            ops.add(c22, m3r, c22);
            ops.add(c22, m6r, c22);
        };

        if (pool != null) {
            var f11 = ForkJoinTask.adapt(s11).fork();
            var f12 = ForkJoinTask.adapt(s12).fork();
            var f21 = ForkJoinTask.adapt(s21).fork();
            s22.run();
            f21.join();
            f12.join();
            f11.join();
        } else {
            s11.run();
            s12.run();
            s21.run();
            s22.run();
        }
    }
}
