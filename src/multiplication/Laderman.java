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

        if (Math.max(m, Math.max(k, n)) <= leaf || m < 3 || k < 3 || n < 3
                || m % 3 != 0 || k % 3 != 0 || n % 3 != 0) {
            ops.multiply(a, b, c);
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

        // Bufory na wyniki M1–M23
        var m1r = ops.newMatrix(m3, n3).view();
        var m2r = ops.newMatrix(m3, n3).view();
        var m3r = ops.newMatrix(m3, n3).view();
        var m4r = ops.newMatrix(m3, n3).view();
        var m5r = ops.newMatrix(m3, n3).view();
        var m6r = ops.newMatrix(m3, n3).view();
        var m7r = ops.newMatrix(m3, n3).view();
        var m8r = ops.newMatrix(m3, n3).view();
        var m9r = ops.newMatrix(m3, n3).view();
        var m10r = ops.newMatrix(m3, n3).view();
        var m11r = ops.newMatrix(m3, n3).view();
        var m12r = ops.newMatrix(m3, n3).view();
        var m13r = ops.newMatrix(m3, n3).view();
        var m14r = ops.newMatrix(m3, n3).view();
        var m15r = ops.newMatrix(m3, n3).view();
        var m16r = ops.newMatrix(m3, n3).view();
        var m17r = ops.newMatrix(m3, n3).view();
        var m18r = ops.newMatrix(m3, n3).view();
        var m19r = ops.newMatrix(m3, n3).view();
        var m20r = ops.newMatrix(m3, n3).view();
        var m21r = ops.newMatrix(m3, n3).view();
        var m22r = ops.newMatrix(m3, n3).view();
        var m23r = ops.newMatrix(m3, n3).view();

        // M1 = (A11 + A12 + A13 - A21 - A22 - A32 - A33) * B22
        Runnable t1 = () -> {
            var lA = ops.newMatrix(m3, k3).view();
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
            var lA = ops.newMatrix(m3, k3).view();
            var lB = ops.newMatrix(k3, n3).view();
            ops.subtract(a11, a21, lA);
            ops.subtract(b22, b12, lB);
            ladermanMultiply(ops, lA, lB, m2r, leaf, pool);
        };

        // M3 = A22 * (-B11 + B12 + B21 - B22 - B23 - B31 + B33)
        Runnable t3 = () -> {
            var lB = ops.newMatrix(k3, n3).view();
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
            var lA = ops.newMatrix(m3, k3).view();
            var lB = ops.newMatrix(k3, n3).view();
            ops.subtract(a21, a11, lA);
            ops.add(lA, a22, lA);
            ops.subtract(b11, b12, lB);
            ops.add(lB, b22, lB);
            ladermanMultiply(ops, lA, lB, m4r, leaf, pool);
        };

        // M5 = (A21 + A22) * (-B11 + B12)
        Runnable t5 = () -> {
            var lA = ops.newMatrix(m3, k3).view();
            var lB = ops.newMatrix(k3, n3).view();
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
            var lA = ops.newMatrix(m3, k3).view();
            var lB = ops.newMatrix(k3, n3).view();
            ops.subtract(a31, a11, lA);
            ops.add(lA, a32, lA);
            ops.subtract(b11, b13, lB);
            ops.add(lB, b23, lB);
            ladermanMultiply(ops, lA, lB, m7r, leaf, pool);
        };

        // M8 = (-A11 + A31) * (B13 - B23)
        Runnable t8 = () -> {
            var lA = ops.newMatrix(m3, k3).view();
            var lB = ops.newMatrix(k3, n3).view();
            ops.subtract(a31, a11, lA);
            ops.subtract(b13, b23, lB);
            ladermanMultiply(ops, lA, lB, m8r, leaf, pool);
        };

        // M9 = (A31 + A32) * (-B11 + B13)
        Runnable t9 = () -> {
            var lA = ops.newMatrix(m3, k3).view();
            var lB = ops.newMatrix(k3, n3).view();
            ops.add(a31, a32, lA);
            ops.subtract(b13, b11, lB);
            ladermanMultiply(ops, lA, lB, m9r, leaf, pool);
        };

        // M10 = (A11 + A12 + A13 - A22 - A23 - A31 - A32) * B23
        Runnable t10 = () -> {
            var lA = ops.newMatrix(m3, k3).view();
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
            var lB = ops.newMatrix(k3, n3).view();
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
            var lA = ops.newMatrix(m3, k3).view();
            var lB = ops.newMatrix(k3, n3).view();
            ops.subtract(a32, a13, lA);
            ops.add(lA, a33, lA);
            ops.add(b22, b31, lB);
            ops.subtract(lB, b32, lB);
            ladermanMultiply(ops, lA, lB, m12r, leaf, pool);
        };

        // M13 = (A13 - A33) * (B22 - B32)
        Runnable t13 = () -> {
            var lA = ops.newMatrix(m3, k3).view();
            var lB = ops.newMatrix(k3, n3).view();
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
            var lA = ops.newMatrix(m3, k3).view();
            var lB = ops.newMatrix(k3, n3).view();
            ops.add(a32, a33, lA);
            ops.subtract(b32, b31, lB);
            ladermanMultiply(ops, lA, lB, m15r, leaf, pool);
        };

        // M16 = (-A13 + A22 + A23) * (B23 - B31 + B33)
        Runnable t16 = () -> {
            var lA = ops.newMatrix(m3, k3).view();
            var lB = ops.newMatrix(k3, n3).view();
            ops.subtract(a22, a13, lA);
            ops.add(lA, a23, lA);
            ops.subtract(b23, b31, lB);
            ops.add(lB, b33, lB);
            ladermanMultiply(ops, lA, lB, m16r, leaf, pool);
        };

        // M17 = (A13 - A23) * (B23 - B33)
        Runnable t17 = () -> {
            var lA = ops.newMatrix(m3, k3).view();
            var lB = ops.newMatrix(k3, n3).view();
            ops.subtract(a13, a23, lA);
            ops.subtract(b23, b33, lB);
            ladermanMultiply(ops, lA, lB, m17r, leaf, pool);
        };

        // M18 = (A22 + A23) * (-B31 + B33)
        Runnable t18 = () -> {
            var lA = ops.newMatrix(m3, k3).view();
            var lB = ops.newMatrix(k3, n3).view();
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
        ops.add(m6r, m14r, c11);
        ops.add(c11, m19r, c11);

        // C12 = M1 + M2 + M4 + M5 + M6 + M12 + M14 + M15
        ops.add(m1r, m2r, c12);
        ops.add(c12, m4r, c12);
        ops.add(c12, m5r, c12);
        ops.add(c12, m6r, c12);
        ops.add(c12, m12r, c12);
        ops.add(c12, m14r, c12);
        ops.add(c12, m15r, c12);

        // C13 = M6 + M7 + M9 + M10 + M14 + M16 + M18
        ops.add(m6r, m7r, c13);
        ops.add(c13, m9r, c13);
        ops.add(c13, m10r, c13);
        ops.add(c13, m14r, c13);
        ops.add(c13, m16r, c13);
        ops.add(c13, m18r, c13);

        // C21 = M2 + M3 + M4 + M5 + M6 + M14 + M16 + M17
        ops.add(m2r, m3r, c21);
        ops.add(c21, m4r, c21);
        ops.add(c21, m5r, c21);
        ops.add(c21, m6r, c21);
        ops.add(c21, m14r, c21);
        ops.add(c21, m16r, c21);
        ops.add(c21, m17r, c21);

        // C22 = M2 + M4 + M5 + M6 + M20
        ops.add(m2r, m4r, c22);
        ops.add(c22, m5r, c22);
        ops.add(c22, m6r, c22);
        ops.add(c22, m20r, c22);

        // C23 = M14 + M16 + M17 + M18 + M21
        ops.add(m14r, m16r, c23);
        ops.add(c23, m17r, c23);
        ops.add(c23, m18r, c23);
        ops.add(c23, m21r, c23);

        // C31 = M6 + M7 + M8 + M9 + M11 + M12 + M13 + M14
        ops.add(m6r, m7r, c31);
        ops.add(c31, m8r, c31);
        ops.add(c31, m9r, c31);
        ops.add(c31, m11r, c31);
        ops.add(c31, m12r, c31);
        ops.add(c31, m13r, c31);
        ops.add(c31, m14r, c31);

        // C32 = M12 + M13 + M14 + M15 + M22
        ops.add(m12r, m13r, c32);
        ops.add(c32, m14r, c32);
        ops.add(c32, m15r, c32);
        ops.add(c32, m22r, c32);

        // C33 = M6 + M7 + M8 + M9 + M23
        ops.add(m6r, m7r, c33);
        ops.add(c33, m8r, c33);
        ops.add(c33, m9r, c33);
        ops.add(c33, m23r, c33);
    }
}
