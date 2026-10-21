package multiplication;

import common.*;

import java.util.concurrent.ForkJoinPool;

public class Main {
    public static void main(String[] args) {
        int threads = Runtime.getRuntime().availableProcessors();

        try (var pool = new ForkJoinPool(threads)) {
            var ops = new DoubleOps();
            int n = 2000;

            var rnd = new java.util.Random(42);
            var x = ops.random(n, n, -1, 1, rnd);
            var y = ops.random(n, n, -1, 1, rnd);
            var naive = ops.newMatrix(n, n);
            var binet = ops.newMatrix(n, n);
            var binetPar = ops.newMatrix(n, n);
            var strassen = ops.newMatrix(n, n);
            var strassenPar = ops.newMatrix(n, n);

            ops.resetCounters();
            long t = System.nanoTime();
            ops.multiply(x.view(), y.view(), naive.view());
            report("naiwne", ops, t);

            ops.resetCounters();
            t = System.nanoTime();
            Binet.multiply(ops, x.view(), y.view(), binet.view(), 64);
            report("Binet", ops, t);

            ops.resetCounters();
            t = System.nanoTime();
            Binet.multiply(ops, x.view(), y.view(), binetPar.view(), 64, pool);
            report("Binet (" + threads + " wątków)", ops, t);

            ops.resetCounters();
            t = System.nanoTime();
            Strassen.multiply(ops, x.view(), y.view(), strassen.view(), 64);
            report("Strassen (" + threads + " wątków)", ops, t);

            ops.resetCounters();
            t = System.nanoTime();
            Strassen.multiply(ops, x.view(), y.view(), strassenPar.view(), 64, pool);
            report("Strassen (" + threads + " wątków)", ops, t);

            check(ops, naive, binet);
            check(ops, naive, binetPar);
            check(ops, naive, strassen);
            check(ops, naive, strassenPar);
        }
    }

    private static void report(String name, MatrixOps<?> ops, long start) {
        System.out.printf("%s: %d ms, mnożeń %d, dodawań %d%n",
                name, (System.nanoTime() - start) / 1_000_000, ops.muls.sum(), ops.adds.sum());
    }

    private static void check(DoubleOps ops, Matrix<double[]> expected, Matrix<double[]> actual) {
        for (int i = 0; i < expected.rows(); i++)
            for (int j = 0; j < expected.cols(); j++)
                if (Math.abs(ops.get(expected.view(), i, j) - ops.get(actual.view(), i, j)) > 1e-9)
                    throw new AssertionError("Różnica w (" + i + "," + j + ")");
    }
}
