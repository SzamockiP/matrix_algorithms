package multiplication;

import common.*;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

public class Main {
    public static void main(String[] args) throws Exception {
        switch (args.length < 1 ? "0" : args[0]) {
            case "0" -> testAll();
            case "1" -> gatherData();
            case "2" -> findBestLeaf();
            default -> throw new IllegalArgumentException("Nieznany tryb: " + args[0]);
        }
    }

    private static Map<String, TestOps.Algorithm> algorithms() {
        var functions = new LinkedHashMap<String, TestOps.Algorithm>();
        functions.put("binet", TestOps.multiplication(Multiplier.binet()));
        functions.put("strassen", TestOps.multiplication(Multiplier.strassen()));
        functions.put("laderman", TestOps.multiplication(Multiplier.laderman()));
        return functions;
    }

    // jednorazowy test na dużej macierzy z wypisaniem wyników na konsolę
    private static void testAll() {
        int threads = Runtime.getRuntime().availableProcessors();
        int n = 5000;
        int leaf = 240;

        try (var pool = new ForkJoinPool(threads)) {
            var ops = new DoubleOps();
            var rnd = new java.util.Random(42);
            var in = TestOps.random(2).create(ops, n, rnd).stream().map(Matrix::view).toList();

            var naive = ops.newMatrix(n, n);
            ops.resetCounters();
            long t = System.nanoTime();
            TestOps.NAIVE_MULTIPLICATION.run(ops, in, naive.view(), leaf, null);
            report("naiwne", ops, t);

            for (var f : algorithms().entrySet())
                for (var p : new ForkJoinPool[]{null, pool}) {
                    var name = p == null ? f.getKey() : f.getKey() + " (" + threads + " wątków)";
                    var out = ops.newMatrix(n, n);
                    ops.resetCounters();
                    t = System.nanoTime();
                    f.getValue().run(ops, in, out.view(), leaf, p);
                    report(name, ops, t);
                    TestOps.check(ops, naive, out, name, n);
                }
        }
    }

    private static void gatherData() throws IOException {
        var functions = new LinkedHashMap<String, TestOps.Algorithm>();
        functions.put("naive", TestOps.NAIVE_MULTIPLICATION);
        functions.putAll(algorithms());
        TestOps.performTests(240, 3, 2500, 10, "multiplication", functions, TestOps.random(2),
                TestOps.NAIVE_MULTIPLICATION);
    }

    private static void findBestLeaf() throws IOException {
        var binet = TestOps.multiplication(Multiplier.binet());
        for (int leaf = 8; leaf <= 256; leaf += 8)
            TestOps.performTests(leaf, 3, 1900, 1990, 10, "multiplication",
                    Map.of(String.format("binet_%d-leaf", leaf), binet), TestOps.random(2), null);
    }

    private static void report(String name, MatrixOps<?> ops, long start) {
        System.out.printf("%s: %d ms, mnożeń %d, dodawań %d%n",
                name, (System.nanoTime() - start) / 1_000_000, ops.muls.sum(), ops.adds.sum());
    }
}
