package common;

import inversion.Inversion;
import factorization.Lu;
import multiplication.Multiplier;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;

public final class TestOps {
    private TestOps() {}


    @FunctionalInterface
    public interface Algorithm {
        void run(DoubleOps ops, List<MatrixView<double[]>> in, MatrixView<double[]> out, int leaf, ForkJoinPool pool);

        // false = performTests nie uruchamia wersji na puli
        default boolean parallel() { return true; }
    }

    public static Algorithm sequential(Algorithm alg) {
        return new Algorithm() {
            public void run(DoubleOps ops, List<MatrixView<double[]>> in, MatrixView<double[]> out, int leaf, ForkJoinPool pool) {
                alg.run(ops, in, out, leaf, null);
            }

            public boolean parallel() { return false; }
        };
    }

    @FunctionalInterface
    public interface Inputs {
        List<Matrix<double[]>> create(DoubleOps ops, int n, Random rnd);
    }


    public static Algorithm multiplication(Multiplier<double[]> mul) {
        return (ops, in, out, leaf, pool) -> mul.multiply(ops, in.get(0), in.get(1), out, leaf, pool);
    }

    public static Algorithm inversion(Multiplier<double[]> mul) {
        return (ops, in, out, leaf, pool) -> {
            if (pool == null) Inversion.invert(ops, mul, in.get(0), out, leaf);
            else Inversion.invert(ops, mul, in.get(0), out, leaf, pool);
        };
    }

    // wynik: spakowane LU (patrz MatrixOps.luInPlace)
    public static Algorithm lu(Multiplier<double[]> mul) {
        return (ops, in, out, leaf, pool) -> {
            ops.copy(in.get(0), out);
            if (pool == null) Lu.factor(ops, mul, out, leaf);
            else Lu.factor(ops, mul, out, leaf, pool);
        };
    }

    public static final Algorithm NAIVE_LU = sequential((ops, in, out, leaf, pool) -> {
        ops.copy(in.get(0), out);
        ops.luInPlace(out);
    });

    public static final Algorithm NAIVE_MULTIPLICATION = sequential((ops, in, out, leaf, pool) -> ops.multiply(in.get(0), in.get(1), out));
    public static final Algorithm NAIVE_INVERSION = sequential((ops, in, out, leaf, pool) -> ops.invert(in.get(0), out));

    public static Inputs random(int count) {
        return (ops, n, rnd) -> {
            var list = new java.util.ArrayList<Matrix<double[]>>(count);
            for (int i = 0; i < count; i++)
                list.add(ops.random(n, n, 1e-8, 1, rnd));
            return list;
        };
    }

    public static Inputs diagonallyDominant() {
        return (ops, n, rnd) -> {
            var a = ops.random(n, n, -1, 1, rnd);
            for (int i = 0; i < n; i++)
                ops.set(a.view(), i, i, ops.get(a.view(), i, i) + n);
            return List.of(a);
        };
    }

    // elementy losowe z (1e-8, 1), do przekątnej dodane n: diagonalna dominacja, więc LU bez pivotowania jest stabilne
    public static Inputs dominantRandom() {
        return (ops, n, rnd) -> {
            var a = ops.random(n, n, 1e-8, 1, rnd);
            for (int i = 0; i < n; i++)
                ops.set(a.view(), i, i, ops.get(a.view(), i, i) + n);
            return List.of(a);
        };
    }

    public static void performTests(int leaf, int iterations, int minMatrixSize, int maxMatrixSize, int step,
                                    String module, Map<String, Algorithm> functions, Inputs inputs,
                                    Algorithm reference) throws IOException {
        Files.createDirectories(Path.of("test_data", module));
        int threads = Runtime.getRuntime().availableProcessors();
        for (int i = 0; i < iterations; i++) {
            var time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            try (var pool = new ForkJoinPool(threads)) {
                var ops = new DoubleOps();
                for (int n = minMatrixSize; n <= maxMatrixSize; n += step) {
                    var in = inputs.create(ops, n, new Random(42)).stream().map(Matrix::view).toList();

                    Matrix<double[]> expected = null;
                    if (reference != null) {
                        expected = ops.newMatrix(n, n);
                        reference.run(ops, in, expected.view(), leaf, null);
                    }

                    for (var f : functions.entrySet()) {
                        var pools = f.getValue().parallel() ? new ForkJoinPool[]{null, pool} : new ForkJoinPool[]{null};
                        for (var p : pools) {
                            var name = p == null ? f.getKey() : f.getKey() + "_threads";
                            var out = ops.newMatrix(n, n);
                            ops.resetCounters();
                            long t = System.nanoTime();
                            f.getValue().run(ops, in, out.view(), leaf, p);
                            saveData(n, module, name, ops, t, time);
                            if (expected != null)
                                check(ops, expected, out, name, n);
                        }
                    }
                }
            }
        }
    }

    public static void performTests(int leaf, int iterations, int maxMatrixSize, int step, String module,
                                    Map<String, Algorithm> functions, Inputs inputs,
                                    Algorithm reference) throws IOException {
        performTests(leaf, iterations, 0, maxMatrixSize, step, module, functions, inputs, reference);
    }

    public static void saveData(int size, String module, String name, MatrixOps<?> ops, long start, String time) throws IOException {
        long ms = (System.nanoTime() - start) / 1_000_000;
        try (PrintWriter writer = new PrintWriter(
                new FileWriter(String.format("test_data/%s/%s_%s.csv", module, name, time), true))) {
            writer.printf("%d;%s;%d;%d;%d%n", size, name, ms, ops.muls.sum(), ops.adds.sum());
        }
    }

    public static void check(DoubleOps ops, Matrix<double[]> expected, Matrix<double[]> actual, String name, int n) {
        for (int i = 0; i < expected.rows(); i++)
            for (int j = 0; j < expected.cols(); j++) {
                double e = ops.get(expected.view(), i, j), a = ops.get(actual.view(), i, j);
                if (Math.abs(e - a) > 1e-9 * Math.max(1, Math.abs(e)))
                    throw new AssertionError(String.format("%s, n=%d: różnica w (%d,%d)", name, n, i, j));
            }
    }
}
