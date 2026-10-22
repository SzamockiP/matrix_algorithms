package multiplication;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

import common.MatrixOps;
import common.MatrixView;

public class Laderman {
    private Laderman() {
    }

    // c = a * b sekwencyjnie
    public static <A> void multiply(MatrixOps<A> ops, MatrixView<A> a, MatrixView<A> b, MatrixView<A> c, int leaf) {
        checkLeaf(leaf);
        ladermanMultiply(ops, a, b, c, leaf, null);
    }

    // c = a * b na wątkach z puli
    public static <A> void multiply(MatrixOps<A> ops, MatrixView<A> a, MatrixView<A> b, MatrixView<A> c, int leaf,
            ForkJoinPool pool) {
        checkLeaf(leaf);
        pool.invoke(ForkJoinTask.adapt(() -> ladermanMultiply(ops, a, b, c, leaf, pool)));
    }

    private static void checkLeaf(int leaf) {
        if (leaf < 1)
            throw new IllegalArgumentException("leaf musi być >= 1");
    }

    private static <A> void ladermanMultiply(MatrixOps<A> ops, MatrixView<A> a, MatrixView<A> b, MatrixView<A> c,
            int leaf, ForkJoinPool pool) {
        int m = a.rows(), k = a.cols(), n = b.cols();

        if (Math.max(m, Math.max(k, n)) <= leaf || m < 3 || k < 3 || n < 3) {
            ops.multiply(a, b, c);
            return;
        }

        int m1 = m - m % 3;
        int k1 = k - k % 3;
        int n1 = n - n % 3;

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

                ladermanMultiply(ops, a.block(0, 0, m1, k1), b.block(0, 0, k1, n1), c.block(0, 0, m1, n1), leaf, pool);

                if (k1 < k) {
                    ops.multiplyAdd(a.block(0, k1, m1, k - k1), b.block(k1, 0, k - k1, n1), c.block(0, 0, m1, n1));
                }

                if (fBottom != null) fBottom.join();
                if (fRight != null) fRight.join();
            } else {
                ladermanMultiply(ops, a.block(0, 0, m1, k1), b.block(0, 0, k1, n1), c.block(0, 0, m1, n1), leaf, pool);

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

        int m3 = m / 3, k3 = k / 3, n3 = n / 3;

        // Bloki A (wiersz, kolumna, wysokość, szerokość)
        var a11 = a.block(0, 0, m3, k3);
        var a12 = a.block(0, k3, m3, k3);
        var a13 = a.block(0, 2 * k3, m3, k3);
        var a21 = a.block(m3, 0, m3, k3);
        var a22 = a.block(m3, k3, m3, k3);
        var a23 = a.block(m3, 2 * k3, m3, k3);
        var a31 = a.block(2 * m3, 0, m3, k3);
        var a32 = a.block(2 * m3, k3, m3, k3);
        var a33 = a.block(2 * m3, 2 * k3, m3, k3);

        // Bloki B
        var b11 = b.block(0, 0, k3, n3);
        var b12 = b.block(0, n3, k3, n3);
        var b13 = b.block(0, 2 * n3, k3, n3);
        var b21 = b.block(k3, 0, k3, n3);
        var b22 = b.block(k3, n3, k3, n3);
        var b23 = b.block(k3, 2 * n3, k3, n3);
        var b31 = b.block(2 * k3, 0, k3, n3);
        var b32 = b.block(2 * k3, n3, k3, n3);
        var b33 = b.block(2 * k3, 2 * n3, k3, n3);

        // Bloki C (wynik)
        var c11 = c.block(0, 0, m3, n3);
        var c12 = c.block(0, n3, m3, n3);
        var c13 = c.block(0, 2 * n3, m3, n3);
        var c21 = c.block(m3, 0, m3, n3);
        var c22 = c.block(m3, n3, m3, n3);
        var c23 = c.block(m3, 2 * n3, m3, n3);
        var c31 = c.block(2 * m3, 0, m3, n3);
        var c32 = c.block(2 * m3, n3, m3, n3);
        var c33 = c.block(2 * m3, 2 * n3, m3, n3);

        // Jeden bufor na wyniki M1–M23 (23 bloki jako widoki)
        var mBuf = ops.newMatrix(23 * m3, n3).view();
        var m1r = mBuf.block(0 * m3, 0, m3, n3);
        var m2r = mBuf.block(1 * m3, 0, m3, n3);
        var m3r = mBuf.block(2 * m3, 0, m3, n3);
        var m4r = mBuf.block(3 * m3, 0, m3, n3);
        var m5r = mBuf.block(4 * m3, 0, m3, n3);
        var m6r = mBuf.block(5 * m3, 0, m3, n3);
        var m7r = mBuf.block(6 * m3, 0, m3, n3);
        var m8r = mBuf.block(7 * m3, 0, m3, n3);
        var m9r = mBuf.block(8 * m3, 0, m3, n3);
        var m10r = mBuf.block(9 * m3, 0, m3, n3);
        var m11r = mBuf.block(10 * m3, 0, m3, n3);
        var m12r = mBuf.block(11 * m3, 0, m3, n3);
        var m13r = mBuf.block(12 * m3, 0, m3, n3);
        var m14r = mBuf.block(13 * m3, 0, m3, n3);
        var m15r = mBuf.block(14 * m3, 0, m3, n3);
        var m16r = mBuf.block(15 * m3, 0, m3, n3);
        var m17r = mBuf.block(16 * m3, 0, m3, n3);
        var m18r = mBuf.block(17 * m3, 0, m3, n3);
        var m19r = mBuf.block(18 * m3, 0, m3, n3);
        var m20r = mBuf.block(19 * m3, 0, m3, n3);
        var m21r = mBuf.block(20 * m3, 0, m3, n3);
        var m22r = mBuf.block(21 * m3, 0, m3, n3);
        var m23r = mBuf.block(22 * m3, 0, m3, n3);

        // Arena na tymczasowe sumy: po jednym lA (14) i lB (14) na zadanie
        var tA = ops.newMatrix(14 * m3, k3).view();
        var tB = ops.newMatrix(14 * k3, n3).view();

        // M1 = (A11 + A12 + A13 - A21 - A22 - A32 - A33) * B22
        Runnable t1 = () -> {
            var lA = tA.block(0 * m3, 0, m3, k3);
            ops.add(a11, a12, lA);
            ops.add(lA, a13, lA);
            ops.subtract(lA, a21, lA);
            ops.subtract(lA, a22, lA);
            ops.subtract(lA, a32, lA);
            ops.subtract(lA, a33, lA);
            ladermanMultiply(ops, lA, b22, m1r, leaf, pool);
        };

        // M2 = (A11 - A21) * (-B12 + B22)
        Runnable t2 = () -> {
            var lA = tA.block(1 * m3, 0, m3, k3);
            var lB = tB.block(0 * k3, 0, k3, n3);
            ops.subtract(a11, a21, lA);
            ops.subtract(b22, b12, lB);
            ladermanMultiply(ops, lA, lB, m2r, leaf, pool);
        };

        // M3 = A22 * (-B11 + B12 + B21 - B22 - B23 - B31 + B33)
        Runnable t3 = () -> {
            var lB = tB.block(1 * k3, 0, k3, n3);
            ops.subtract(b12, b11, lB);
            ops.add(lB, b21, lB);
            ops.subtract(lB, b22, lB);
            ops.subtract(lB, b23, lB);
            ops.subtract(lB, b31, lB);
            ops.add(lB, b33, lB);
            ladermanMultiply(ops, a22, lB, m3r, leaf, pool);
        };

        // M4 = (-A11 + A21 + A22) * (B11 - B12 + B22)
        Runnable t4 = () -> {
            var lA = tA.block(2 * m3, 0, m3, k3);
            var lB = tB.block(2 * k3, 0, k3, n3);
            ops.subtract(a21, a11, lA);
            ops.add(lA, a22, lA);
            ops.subtract(b11, b12, lB);
            ops.add(lB, b22, lB);
            ladermanMultiply(ops, lA, lB, m4r, leaf, pool);
        };

        // M5 = (A21 + A22) * (-B11 + B12)
        Runnable t5 = () -> {
            var lA = tA.block(3 * m3, 0, m3, k3);
            var lB = tB.block(3 * k3, 0, k3, n3);
            ops.add(a21, a22, lA);
            ops.subtract(b12, b11, lB);
            ladermanMultiply(ops, lA, lB, m5r, leaf, pool);
        };

        // M6 = A11 * B11
        Runnable t6 = () -> {
            ladermanMultiply(ops, a11, b11, m6r, leaf, pool);
        };

        // M7 = (-A11 + A31 + A32) * (B11 - B13 + B23)
        Runnable t7 = () -> {
            var lA = tA.block(4 * m3, 0, m3, k3);
            var lB = tB.block(4 * k3, 0, k3, n3);
            ops.subtract(a31, a11, lA);
            ops.add(lA, a32, lA);
            ops.subtract(b11, b13, lB);
            ops.add(lB, b23, lB);
            ladermanMultiply(ops, lA, lB, m7r, leaf, pool);
        };

        // M8 = (-A11 + A31) * (B13 - B23)
        Runnable t8 = () -> {
            var lA = tA.block(5 * m3, 0, m3, k3);
            var lB = tB.block(5 * k3, 0, k3, n3);
            ops.subtract(a31, a11, lA);
            ops.subtract(b13, b23, lB);
            ladermanMultiply(ops, lA, lB, m8r, leaf, pool);
        };

        // M9 = (A31 + A32) * (-B11 + B13)
        Runnable t9 = () -> {
            var lA = tA.block(6 * m3, 0, m3, k3);
            var lB = tB.block(6 * k3, 0, k3, n3);
            ops.add(a31, a32, lA);
            ops.subtract(b13, b11, lB);
            ladermanMultiply(ops, lA, lB, m9r, leaf, pool);
        };

        // M10 = (A11 + A12 + A13 - A22 - A23 - A31 - A32) * B23
        Runnable t10 = () -> {
            var lA = tA.block(7 * m3, 0, m3, k3);
            ops.add(a11, a12, lA);
            ops.add(lA, a13, lA);
            ops.subtract(lA, a22, lA);
            ops.subtract(lA, a23, lA);
            ops.subtract(lA, a31, lA);
            ops.subtract(lA, a32, lA);
            ladermanMultiply(ops, lA, b23, m10r, leaf, pool);
        };

        // M11 = A32 * (-B11 + B13 + B21 - B22 - B23 - B31 + B32)
        Runnable t11 = () -> {
            var lB = tB.block(7 * k3, 0, k3, n3);
            ops.subtract(b13, b11, lB);
            ops.add(lB, b21, lB);
            ops.subtract(lB, b22, lB);
            ops.subtract(lB, b23, lB);
            ops.subtract(lB, b31, lB);
            ops.add(lB, b32, lB);
            ladermanMultiply(ops, a32, lB, m11r, leaf, pool);
        };

        // M12 = (-A13 + A32 + A33) * (B22 + B31 - B32)
        Runnable t12 = () -> {
            var lA = tA.block(8 * m3, 0, m3, k3);
            var lB = tB.block(8 * k3, 0, k3, n3);
            ops.subtract(a32, a13, lA);
            ops.add(lA, a33, lA);
            ops.add(b22, b31, lB);
            ops.subtract(lB, b32, lB);
            ladermanMultiply(ops, lA, lB, m12r, leaf, pool);
        };

        // M13 = (A13 - A33) * (B22 - B32)
        Runnable t13 = () -> {
            var lA = tA.block(9 * m3, 0, m3, k3);
            var lB = tB.block(9 * k3, 0, k3, n3);
            ops.subtract(a13, a33, lA);
            ops.subtract(b22, b32, lB);
            ladermanMultiply(ops, lA, lB, m13r, leaf, pool);
        };

        // M14 = A13 * B31
        Runnable t14 = () -> {
            ladermanMultiply(ops, a13, b31, m14r, leaf, pool);
        };

        // M15 = (A32 + A33) * (-B31 + B32)
        Runnable t15 = () -> {
            var lA = tA.block(10 * m3, 0, m3, k3);
            var lB = tB.block(10 * k3, 0, k3, n3);
            ops.add(a32, a33, lA);
            ops.subtract(b32, b31, lB);
            ladermanMultiply(ops, lA, lB, m15r, leaf, pool);
        };

        // M16 = (-A13 + A22 + A23) * (B23 + B31 - B33)
        Runnable t16 = () -> {
            var lA = tA.block(11 * m3, 0, m3, k3);
            var lB = tB.block(11 * k3, 0, k3, n3);
            ops.subtract(a22, a13, lA);
            ops.add(lA, a23, lA);
            ops.add(b23, b31, lB);
            ops.subtract(lB, b33, lB);
            ladermanMultiply(ops, lA, lB, m16r, leaf, pool);
        };

        // M17 = (A13 - A23) * (B23 - B33)
        Runnable t17 = () -> {
            var lA = tA.block(12 * m3, 0, m3, k3);
            var lB = tB.block(12 * k3, 0, k3, n3);
            ops.subtract(a13, a23, lA);
            ops.subtract(b23, b33, lB);
            ladermanMultiply(ops, lA, lB, m17r, leaf, pool);
        };

        // M18 = (A22 + A23) * (-B31 + B33)
        Runnable t18 = () -> {
            var lA = tA.block(13 * m3, 0, m3, k3);
            var lB = tB.block(13 * k3, 0, k3, n3);
            ops.add(a22, a23, lA);
            ops.subtract(b33, b31, lB);
            ladermanMultiply(ops, lA, lB, m18r, leaf, pool);
        };

        // M19 = A12 * B21
        Runnable t19 = () -> {
            ladermanMultiply(ops, a12, b21, m19r, leaf, pool);
        };

        // M20 = A23 * B32
        Runnable t20 = () -> {
            ladermanMultiply(ops, a23, b32, m20r, leaf, pool);
        };

        // M21 = A21 * B13
        Runnable t21 = () -> {
            ladermanMultiply(ops, a21, b13, m21r, leaf, pool);
        };

        // M22 = A31 * B12
        Runnable t22 = () -> {
            ladermanMultiply(ops, a31, b12, m22r, leaf, pool);
        };

        // M23 = A33 * B33
        Runnable t23 = () -> {
            ladermanMultiply(ops, a33, b33, m23r, leaf, pool);
        };

        if (pool != null) {
            var f1 = ForkJoinTask.adapt(t1).fork();
            var f2 = ForkJoinTask.adapt(t2).fork();
            var f3 = ForkJoinTask.adapt(t3).fork();
            var f4 = ForkJoinTask.adapt(t4).fork();
            var f5 = ForkJoinTask.adapt(t5).fork();
            var f6 = ForkJoinTask.adapt(t6).fork();
            var f7 = ForkJoinTask.adapt(t7).fork();
            var f8 = ForkJoinTask.adapt(t8).fork();
            var f9 = ForkJoinTask.adapt(t9).fork();
            var f10 = ForkJoinTask.adapt(t10).fork();
            var f11 = ForkJoinTask.adapt(t11).fork();
            var f12 = ForkJoinTask.adapt(t12).fork();
            var f13 = ForkJoinTask.adapt(t13).fork();
            var f14 = ForkJoinTask.adapt(t14).fork();
            var f15 = ForkJoinTask.adapt(t15).fork();
            var f16 = ForkJoinTask.adapt(t16).fork();
            var f17 = ForkJoinTask.adapt(t17).fork();
            var f18 = ForkJoinTask.adapt(t18).fork();
            var f19 = ForkJoinTask.adapt(t19).fork();
            var f20 = ForkJoinTask.adapt(t20).fork();
            var f21 = ForkJoinTask.adapt(t21).fork();
            var f22 = ForkJoinTask.adapt(t22).fork();
            t23.run();
            f22.join();
            f21.join();
            f20.join();
            f19.join();
            f18.join();
            f17.join();
            f16.join();
            f15.join();
            f14.join();
            f13.join();
            f12.join();
            f11.join();
            f10.join();
            f9.join();
            f8.join();
            f7.join();
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
            t8.run();
            t9.run();
            t10.run();
            t11.run();
            t12.run();
            t13.run();
            t14.run();
            t15.run();
            t16.run();
            t17.run();
            t18.run();
            t19.run();
            t20.run();
            t21.run();
            t22.run();
            t23.run();
        }

        // C11 = M6 + M14 + M19
        Runnable s11 = () -> {
            ops.add(m6r, m14r, c11);
            ops.add(c11, m19r, c11);
        };
        // Składanie wyniku z M1–M23
        Runnable s11 = () -> {
            // C11 = M6 + M14 + M19
            ops.add(m6r, m14r, c11);
            ops.add(c11, m19r, c11);
        };

        Runnable s12 = () -> {
            // C12 = M1 + M4 + M5 + M6 + M12 + M14 + M15
            ops.add(m1r, m4r, c12);
            ops.add(c12, m5r, c12);
            ops.add(c12, m6r, c12);
            ops.add(c12, m12r, c12);
            ops.add(c12, m14r, c12);
            ops.add(c12, m15r, c12);
        };

        Runnable s13 = () -> {
            // C13 = M6 + M7 + M9 + M10 + M14 + M16 + M18
            ops.add(m6r, m7r, c13);
            ops.add(c13, m9r, c13);
            ops.add(c13, m10r, c13);
            ops.add(c13, m14r, c13);
            ops.add(c13, m16r, c13);
            ops.add(c13, m18r, c13);
        };

        Runnable s21 = () -> {
            // C21 = M2 + M3 + M4 + M6 + M14 + M16 + M17
            ops.add(m2r, m3r, c21);
            ops.add(c21, m4r, c21);
            ops.add(c21, m6r, c21);
            ops.add(c21, m14r, c21);
            ops.add(c21, m16r, c21);
            ops.add(c21, m17r, c21);
        };

        Runnable s22 = () -> {
            // C22 = M2 + M4 + M5 + M6 + M20
            ops.add(m2r, m4r, c22);
            ops.add(c22, m5r, c22);
            ops.add(c22, m6r, c22);
            ops.add(c22, m20r, c22);
        };

        Runnable s23 = () -> {
            // C23 = M14 + M16 + M17 + M18 + M21
            ops.add(m14r, m16r, c23);
            ops.add(c23, m17r, c23);
            ops.add(c23, m18r, c23);
            ops.add(c23, m21r, c23);
        };

        Runnable s31 = () -> {
            // C31 = M6 + M7 + M8 + M11 + M12 + M13 + M14
            ops.add(m6r, m7r, c31);
            ops.add(c31, m8r, c31);
            ops.add(c31, m11r, c31);
            ops.add(c31, m12r, c31);
            ops.add(c31, m13r, c31);
            ops.add(c31, m14r, c31);
        };

        Runnable s32 = () -> {
            // C32 = M12 + M13 + M14 + M15 + M22
            ops.add(m12r, m13r, c32);
            ops.add(c32, m14r, c32);
            ops.add(c32, m15r, c32);
            ops.add(c32, m22r, c32);
        };

        Runnable s33 = () -> {
            // C33 = M6 + M7 + M8 + M9 + M23
            ops.add(m6r, m7r, c33);
            ops.add(c33, m8r, c33);
            ops.add(c33, m9r, c33);
            ops.add(c33, m23r, c33);
        };

        if (pool != null) {
            var f11 = ForkJoinTask.adapt(s11).fork();
            var f12 = ForkJoinTask.adapt(s12).fork();
            var f13 = ForkJoinTask.adapt(s13).fork();
            var f21 = ForkJoinTask.adapt(s21).fork();
            var f22 = ForkJoinTask.adapt(s22).fork();
            var f23 = ForkJoinTask.adapt(s23).fork();
            var f31 = ForkJoinTask.adapt(s31).fork();
            var f32 = ForkJoinTask.adapt(s32).fork();
            s33.run();
            f32.join();
            f31.join();
            f23.join();
            f22.join();
            f21.join();
            f13.join();
            f12.join();
            f11.join();
        } else {
            s11.run();
            s12.run();
            s13.run();
            s21.run();
            s22.run();
            s23.run();
            s31.run();
            s32.run();
            s33.run();
        }
    }
}
