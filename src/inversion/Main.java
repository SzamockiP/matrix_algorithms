package inversion;

import common.*;
import multiplication.Multiplier;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
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
        functions.put("binet", TestOps.inversion(Multiplier.binet()));
        functions.put("strassen", TestOps.inversion(Multiplier.strassen()));
        functions.put("laderman", TestOps.inversion(Multiplier.laderman()));
        return functions;
    }

    // sprawdzenie: max |A * A^-1 - I| dla każdego mnożenia, sekwencyjnie i na puli
    private static void testAll() {
        int threads = Runtime.getRuntime().availableProcessors();
        int n = 501;
        int leaf = 32;

        try (var pool = new ForkJoinPool(threads)) {
            var ops = new DoubleOps();
            var a = TestOps.diagonallyDominant().create(ops, n, new Random(42)).getFirst();
            var in = java.util.List.of(a.view());

            var functions = new LinkedHashMap<String, TestOps.Algorithm>();
            functions.put("naiwne", TestOps.NAIVE_INVERSION);
            functions.putAll(algorithms());

            for (var f : functions.entrySet()) {
                var pools = f.getValue().parallel() ? new ForkJoinPool[]{null, pool} : new ForkJoinPool[]{null};
                for (var p : pools) {
                    var name = p == null ? f.getKey() : f.getKey() + " (" + threads + " wątków)";
                    var inv = ops.newMatrix(n, n);
                    ops.resetCounters();
                    long t = System.nanoTime();
                    f.getValue().run(ops, in, inv.view(), leaf, p);
                    report(name, ops, t);

                    var prod = ops.newMatrix(n, n);
                    ops.multiply(a.view(), inv.view(), prod.view());
                    double err = 0;
                    for (int i = 0; i < n; i++)
                        for (int j = 0; j < n; j++)
                            err = Math.max(err, Math.abs(ops.get(prod.view(), i, j) - (i == j ? 1 : 0)));
                    System.out.printf("  err=%.2e%n", err);
                    if (err > 1e-9) throw new AssertionError("zły wynik: " + name);
                }
            }
        }
    }

    private static void gatherData() throws IOException {
        var functions = new LinkedHashMap<String, TestOps.Algorithm>();
        functions.put("naive", TestOps.NAIVE_INVERSION);
        functions.putAll(algorithms());
        TestOps.performTests(240, 3, 2500, 10, "inversion", functions, TestOps.diagonallyDominant(),
                TestOps.NAIVE_INVERSION);
    }

    private static void findBestLeaf() throws IOException {
        var binet = TestOps.inversion(Multiplier.binet());
        for (int leaf = 8; leaf <= 256; leaf += 8)
            TestOps.performTests(leaf, 3, 1900, 1990, 10, "inversion",
                    Map.of(String.format("binet_%d-leaf", leaf), binet), TestOps.diagonallyDominant(), null);
    }

    private static void report(String name, MatrixOps<?> ops, long start) {
        System.out.printf("%s: %d ms, mnożeń %d, dodawań %d%n",
                name, (System.nanoTime() - start) / 1_000_000, ops.muls.sum(), ops.adds.sum());
    }
}
