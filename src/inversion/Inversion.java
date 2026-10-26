package inversion;

import common.MatrixOps;
import common.MatrixView;
import multiplication.Multiplier;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

public final class Inversion {
    private Inversion() {}

    // out = a^-1 sekwencyjnie (out nie może nachodzić na a)
    public static <A> void invert(MatrixOps<A> ops, Multiplier<A> mul, MatrixView<A> a, MatrixView<A> out, int leaf) {
        check(a, out, leaf);
        invertRec(ops, mul, a, out, leaf, null);
    }

    // out = a^-1, mnożenia na wątkach z puli
    public static <A> void invert(MatrixOps<A> ops, Multiplier<A> mul, MatrixView<A> a, MatrixView<A> out, int leaf,
                                  ForkJoinPool pool) {
        check(a, out, leaf);
        // forki w invertRec mają trafiać do tej puli, a nie do commonPool
        pool.invoke(ForkJoinTask.adapt(() -> invertRec(ops, mul, a, out, leaf, pool)));
    }

    private static void check(MatrixView<?> a, MatrixView<?> out, int leaf) {
        if (leaf < 1)
            throw new IllegalArgumentException("leaf musi być >= 1");
        if (a.rows() != a.cols() || out.rows() != a.rows() || out.cols() != a.cols())
            throw new IllegalArgumentException("Wymagane dwie kwadratowe macierze tego samego rozmiaru");
    }

    // dwie rekurencje (A11^-1 i dopełnienie Schura) są zależne, więc równolegle idą tylko mnożenia:
    // wewnątrz każdego oraz niezależne pary (R2, R3) i (C12, C21)
    private static <A> void invertRec(MatrixOps<A> ops, Multiplier<A> mul, MatrixView<A> a, MatrixView<A> out,
                                      int leaf, ForkJoinPool pool) {
        int n = a.rows();
        if (n <= leaf || n < 2) {
            ops.invert(a, out);
            return;
        }

        int n1 = n / 2, n2 = n - n1;

        var a11 = a.block(0, 0, n1, n1);   var a12 = a.block(0, n1, n1, n2);
        var a21 = a.block(n1, 0, n2, n1);  var a22 = a.block(n1, n1, n2, n2);

        var c11 = out.block(0, 0, n1, n1);   var c12 = out.block(0, n1, n1, n2);
        var c21 = out.block(n1, 0, n2, n1);  var c22 = out.block(n1, n1, n2, n2);

        var r2 = ops.newMatrix(n2, n1).view();
        var r3 = ops.newMatrix(n1, n2).view();
        var r4 = ops.newMatrix(n2, n2).view();

        invertRec(ops, mul, a11, c11, leaf, pool);                  // R1 = A11^-1 (w c11)
        Runnable t2 = () -> mul.multiply(ops, a21, c11, r2, leaf, pool);  // R2 = A21 R1
        Runnable t3 = () -> mul.multiply(ops, c11, a12, r3, leaf, pool);  // R3 = R1 A12
        if (pool != null) {
            var f = ForkJoinTask.adapt(t2).fork();
            t3.run();
            f.join();
        } else {
            t2.run();
            t3.run();
        }
        mul.multiply(ops, a21, r3, r4, leaf, pool);                 // R4 = A21 R3
        ops.subtract(r4, a22, r4);                                  // R5 = R4 - A22 = -S
        invertRec(ops, mul, r4, c22, leaf, pool);                   // R6 = R5^-1 = -S^-1 (w c22)
        Runnable t12 = () -> mul.multiply(ops, r3, c22, c12, leaf, pool);  // C12 = R3 R6
        Runnable t21 = () -> mul.multiply(ops, c22, r2, c21, leaf, pool);  // C21 = R6 R2
        if (pool != null) {
            var f = ForkJoinTask.adapt(t12).fork();
            t21.run();
            f.join();
        } else {
            t12.run();
            t21.run();
        }
        var r7 = r4.block(0, 0, n1, n1);                            // R4 już niepotrzebne, n1 <= n2
        mul.multiply(ops, r3, c21, r7, leaf, pool);                 // R7 = R3 C21
        ops.subtract(c11, r7, c11);                                 // C11 = R1 - R7
        ops.negate(c22, c22);                                       // C22 = -R6 = S^-1
    }
}
