package multiplication;

import common.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Date;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ForkJoinPool;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length < 1)
            testAll();
//            throw new Exception("Brak parametru wejściowego");
        switch (args[0]) {
            case "0":
                testAll();
                break;
            case "1":
                gatherData();
                break;
            case "2":
                findBestLeaf();
                break;
        }

    }

    private static void testAll() {
        int threads = Runtime.getRuntime().availableProcessors();

        try (var pool = new ForkJoinPool(threads)) {
            var ops = new DoubleOps();
            int n = 5000;
            int leaf = 240;

            var rnd = new java.util.Random(42);
            var x = ops.random(n, n, 1e-8, 1, rnd);
            var y = ops.random(n, n, 1e-8, 1, rnd);
            var naive = ops.newMatrix(n, n);
            var binet = ops.newMatrix(n, n);
            var binetPar = ops.newMatrix(n, n);
            var strassen = ops.newMatrix(n, n);
            var strassenPar = ops.newMatrix(n, n);
            var laderman = ops.newMatrix(n, n);
            var ladermanPar = ops.newMatrix(n, n);

            ops.resetCounters();
            long t = System.nanoTime();
            ops.multiply(x.view(), y.view(), naive.view());
            report("naiwne", ops, t);

            ops.resetCounters();
            t = System.nanoTime();
            Binet.multiply(ops, x.view(), y.view(), binet.view(), leaf);
            report("Binet", ops, t);

            ops.resetCounters();
            t = System.nanoTime();
            Binet.multiply(ops, x.view(), y.view(), binetPar.view(), leaf, pool);
            report("Binet (" + threads + " wątków)", ops, t);

            ops.resetCounters();
            t = System.nanoTime();
            Strassen.multiply(ops, x.view(), y.view(), strassen.view(), leaf);
            report("Strassen", ops, t);

            ops.resetCounters();
            t = System.nanoTime();
            Strassen.multiply(ops, x.view(), y.view(), strassenPar.view(), leaf, pool);
            report("Strassen (" + threads + " wątków)", ops, t);

            ops.resetCounters();
            t = System.nanoTime();
            Laderman.multiply(ops, x.view(), y.view(), laderman.view(), leaf);
            report("Laderman", ops, t);

            ops.resetCounters();
            t = System.nanoTime();
            Laderman.multiply(ops, x.view(), y.view(), ladermanPar.view(), leaf, pool);
            report("Laderman (" + threads + " wątków)", ops, t);

            check(ops, naive, binet);
            check(ops, naive, binetPar);
            check(ops, naive, strassen);
            check(ops, naive, strassenPar);
            check(ops, naive, laderman);
            check(ops, naive, ladermanPar);
        }
    }

    private static void gatherData() throws IOException {
        int leaf = 240;
        int threads = Runtime.getRuntime().availableProcessors();
        for (int i = 0; i < 3; i++) {
            var time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            try (var pool = new ForkJoinPool(threads)) {
                var ops = new DoubleOps();
                for (int n = 0; n <= 2500; n += 10) {

                    var rnd = new java.util.Random(42);
                    var x = ops.random(n, n, 1e-8, 1, rnd);
                    var y = ops.random(n, n, 1e-8, 1, rnd);
                    var naive = ops.newMatrix(n, n);
                    var binet = ops.newMatrix(n, n);
                    var binetPar = ops.newMatrix(n, n);
                    var strassen = ops.newMatrix(n, n);
                    var strassenPar = ops.newMatrix(n, n);
                    var laderman = ops.newMatrix(n, n);
                    var ladermanPar = ops.newMatrix(n, n);

                    ops.resetCounters();
                    long t = System.nanoTime();
                    ops.multiply(x.view(), y.view(), naive.view());
                    saveData(n, "naive", ops, t, time);

                    ops.resetCounters();
                    t = System.nanoTime();
                    Binet.multiply(ops, x.view(), y.view(), binet.view(), leaf);
                    saveData(n, "binet", ops, t, time);

                    ops.resetCounters();
                    t = System.nanoTime();
                    Binet.multiply(ops, x.view(), y.view(), binetPar.view(), leaf, pool);
                    saveData(n, "binet_threads", ops, t, time);

                    ops.resetCounters();
                    t = System.nanoTime();
                    Strassen.multiply(ops, x.view(), y.view(), strassen.view(), leaf);
                    saveData(n, "strassen", ops, t, time);

                    ops.resetCounters();
                    t = System.nanoTime();
                    Strassen.multiply(ops, x.view(), y.view(), strassenPar.view(), leaf, pool);
                    saveData(n, "strassen_threads", ops, t, time);

                    ops.resetCounters();
                    t = System.nanoTime();
                    Laderman.multiply(ops, x.view(), y.view(), laderman.view(), leaf);
                    saveData(n, "laderman", ops, t, time);

                    ops.resetCounters();
                    t = System.nanoTime();
                    Laderman.multiply(ops, x.view(), y.view(), ladermanPar.view(), leaf, pool);
                    saveData(n, "laderman_threads", ops, t, time);

                    check(ops, naive, binet);
                    check(ops, naive, binetPar);
                    check(ops, naive, strassen);
                    check(ops, naive, strassenPar);
                    check(ops, naive, laderman);
                    check(ops, naive, ladermanPar);

                }
            }
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

    private static void findBestLeaf() throws IOException {

        int threads = Runtime.getRuntime().availableProcessors();
        long t = System.nanoTime();
        var rnd = new java.util.Random(42);
        for (int leaf = 8; leaf <= 256; leaf += 8)
            for (int i = 0; i < 3; i++) {
                var time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
                try (var pool = new ForkJoinPool(threads)) {
                    var ops = new DoubleOps();
                    for (int n = 1900; n < 2000; n += 10) {

                        var x = ops.random(n, n, 1e-8, 1, rnd);
                        var y = ops.random(n, n, 1e-8, 1, rnd);

                        var binetPar = ops.newMatrix(n, n);
                        t = System.nanoTime();
                        Binet.multiply(ops, x.view(), y.view(), binetPar.view(), leaf, pool);
                        saveData(n, String.format("binet_threads_%d-leaf_", leaf), ops, t, time);
                    }
                }
            }
    }

    private static void saveData(int size, String name, MatrixOps<?> ops, long start, String time) throws IOException {
        try (PrintWriter writer = new PrintWriter(
                new FileWriter(String.format("test_data/multiplication/%s_%s.csv", name, time), true))) {
            writer.printf("%d;%s;%d;%d;%d%n",
                    size, name, (System.nanoTime() - start) / 1_000_000, ops.muls.sum(), ops.adds.sum());
        }
    }
}
