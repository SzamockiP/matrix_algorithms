package factorization;

import common.MatrixOps;
import common.MatrixView;
import multiplication.Multiplier;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

// Blokowa faktoryzacja LU bez pivotowania (A = L U, L z jedynkową przekątną).
// Wynik w miejscu: L pod przekątną, U na i nad nią. Wymaga niezerowych minorów głównych (np. diagonalna dominacja).
public final class Lu {
    private Lu() {}

    // a := spakowane LU, sekwencyjnie
    public static <A> void factor(MatrixOps<A> ops, Multiplier<A> mul, MatrixView<A> a, int leaf) {
        check(a, leaf);
        factorRec(ops, mul, a, leaf, null);
    }

    // a := spakowane LU, mnożenia na wątkach z puli
    public static <A> void factor(MatrixOps<A> ops, Multiplier<A> mul, MatrixView<A> a, int leaf, ForkJoinPool pool) {
        check(a, leaf);
        pool.invoke(ForkJoinTask.adapt(() -> factorRec(ops, mul, a, leaf, pool)));
    }

    private static void check(MatrixView<?> a, int leaf) {
        if (leaf < 1)
            throw new IllegalArgumentException("leaf musi być >= 1");
        if (a.rows() != a.cols())
            throw new IllegalArgumentException("Macierz musi być kwadratowa");
    }

    // A11 = L11 U11; U12 = L11^-1 A12 i L21 = A21 U11^-1 (niezależne, idą równolegle);
    // S = A22 - L21 U12 = L22 U22
    private static <A> void factorRec(MatrixOps<A> ops, Multiplier<A> mul, MatrixView<A> a, int leaf,
                                      ForkJoinPool pool) {
        int n = a.rows();
        if (n <= leaf || n < 2) {
            ops.luInPlace(a);
            return;
        }

        int n1 = n / 2, n2 = n - n1;
        var a11 = a.block(0, 0, n1, n1);   var a12 = a.block(0, n1, n1, n2);
        var a21 = a.block(n1, 0, n2, n1);  var a22 = a.block(n1, n1, n2, n2);

        factorRec(ops, mul, a11, leaf, pool);
        Runnable u12 = () -> solveLower(ops, mul, a11, a12, leaf, pool);
        Runnable l21 = () -> solveUpperRight(ops, mul, a11, a21, leaf, pool);
        if (pool != null) {
            var f = ForkJoinTask.adapt(u12).fork();
            l21.run();
            f.join();
        } else {
            u12.run();
            l21.run();
        }
        var t = ops.newMatrix(n2, n2).view();
        mul.multiply(ops, a21, a12, t, leaf, pool);
        ops.subtract(a22, t, a22);
        factorRec(ops, mul, a22, leaf, pool);
    }

    // b := L^-1 b, L = dolny trójkąt l z jedynkową przekątną
    private static <A> void solveLower(MatrixOps<A> ops, Multiplier<A> mul, MatrixView<A> l, MatrixView<A> b,
                                       int leaf, ForkJoinPool pool) {
        int n = l.rows();
        if (n <= leaf || n < 2) {
            ops.solveUnitLower(l, b);
            return;
        }
        int n1 = n / 2, n2 = n - n1, m = b.cols();
        var b1 = b.block(0, 0, n1, m);
        var b2 = b.block(n1, 0, n2, m);

        solveLower(ops, mul, l.block(0, 0, n1, n1), b1, leaf, pool);
        var t = ops.newMatrix(n2, m).view();
        mul.multiply(ops, l.block(n1, 0, n2, n1), b1, t, leaf, pool);
        ops.subtract(b2, t, b2);
        solveLower(ops, mul, l.block(n1, n1, n2, n2), b2, leaf, pool);
    }

    // b := b U^-1, U = górny trójkąt u z przekątną
    private static <A> void solveUpperRight(MatrixOps<A> ops, Multiplier<A> mul, MatrixView<A> u, MatrixView<A> b,
                                            int leaf, ForkJoinPool pool) {
        int n = u.rows();
        if (n <= leaf || n < 2) {
            ops.solveUpperRight(u, b);
            return;
        }
        int n1 = n / 2, n2 = n - n1, m = b.rows();
        var b1 = b.block(0, 0, m, n1);
        var b2 = b.block(0, n1, m, n2);

        solveUpperRight(ops, mul, u.block(0, 0, n1, n1), b1, leaf, pool);
        var t = ops.newMatrix(m, n2).view();
        mul.multiply(ops, b1, u.block(0, n1, n1, n2), t, leaf, pool);
        ops.subtract(b2, t, b2);
        solveUpperRight(ops, mul, u.block(n1, n1, n2, n2), b2, leaf, pool);
    }
}
