package multiplication;

import common.MatrixOps;
import common.MatrixView;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

public final class Binet {
    private Binet() {}

    // c = a * b sekwencyjnie
    public static <A> void multiply(MatrixOps<A> ops, MatrixView<A> a, MatrixView<A> b, MatrixView<A> c, int leaf) {
        checkLeaf(leaf);
        binetMultiply(ops, a, b, c, leaf, null, false);
    }

    // c = a * b na wątkach z puli
    public static <A> void multiply(MatrixOps<A> ops, MatrixView<A> a, MatrixView<A> b, MatrixView<A> c, int leaf,
                                    ForkJoinPool pool) {
        checkLeaf(leaf);
        pool.invoke(ForkJoinTask.adapt(() -> binetMultiply(ops, a, b, c, leaf, pool, false)));
    }

    private static void checkLeaf(int leaf) {
        if (leaf < 1)
            throw new IllegalArgumentException("leaf musi być >= 1");
    }

    // c += a * b
    private static <A> void binetMultiplyAdd(MatrixOps<A> ops, MatrixView<A> a, MatrixView<A> b, MatrixView<A> c,
                                             int leaf, ForkJoinPool pool) {
        binetMultiply(ops, a, b, c, leaf, pool, true);
    }


    private static <A> void binetMultiply(MatrixOps<A> ops, MatrixView<A> a, MatrixView<A> b, MatrixView<A> c,
                                          int leaf, ForkJoinPool pool, boolean accumulate) {
        int m = a.rows(), k = a.cols(), n = b.cols();

        // wymiar 1 nie da się podzielić na dwa niepuste bloki
        if (Math.max(m, Math.max(k, n)) <= leaf || Math.min(m, Math.min(k, n)) < 2) {
            if (accumulate) ops.multiplyAdd(a, b, c);
            else ops.multiply(a, b, c);
            return;
        }


        int m1 = m / 2, k1 = k / 2, n1 = n / 2;
        int m2 = m - m1, k2 = k - k1, n2 = n - n1;

        var a11 = a.block(0, 0, m1, k1);   var a12 = a.block(0, k1, m1, k2);
        var a21 = a.block(m1, 0, m2, k1);  var a22 = a.block(m1, k1, m2, k2);

        var b11 = b.block(0, 0, k1, n1);   var b12 = b.block(0, n1, k1, n2);
        var b21 = b.block(k1, 0, k2, n1);  var b22 = b.block(k1, n1, k2, n2);

        var c11 = c.block(0, 0, m1, n1);   var c12 = c.block(0, n1, m1, n2);
        var c21 = c.block(m1, 0, m2, n1);  var c22 = c.block(m1, n1, m2, n2);

        // C11 = A11*B11 + A12*B21 itd.
        // pierwszy iloczyn nadpisuje albo dodaje, tak jak cały węzeł (accumulate)
        // drugi iloczyn zawsze dodaje
        Runnable t11 = () -> { binetMultiply(ops, a11, b11, c11, leaf, pool, accumulate); binetMultiplyAdd(ops, a12, b21, c11, leaf, pool); };
        Runnable t12 = () -> { binetMultiply(ops, a11, b12, c12, leaf, pool, accumulate); binetMultiplyAdd(ops, a12, b22, c12, leaf, pool); };
        Runnable t21 = () -> { binetMultiply(ops, a21, b11, c21, leaf, pool, accumulate); binetMultiplyAdd(ops, a22, b21, c21, leaf, pool); };
        Runnable t22 = () -> { binetMultiply(ops, a21, b12, c22, leaf, pool, accumulate); binetMultiplyAdd(ops, a22, b22, c22, leaf, pool); };

        if (pool != null) {
            var f11 = ForkJoinTask.adapt(t11).fork();
            var f12 = ForkJoinTask.adapt(t12).fork();
            var f21 = ForkJoinTask.adapt(t21).fork();
            t22.run();

            // odwrotna kolejność: ostatnio wrzucone leży na wierzchu własnej kolejki,
            // więc jeśli nikt go nie ukradł, join zdejmie je i policzy w tym wątku
            f21.join();
            f12.join();
            f11.join();
        } else {
            t11.run();
            t12.run();
            t21.run();
            t22.run();
        }
    }
}
