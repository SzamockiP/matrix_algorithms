package inversion;

import common.DoubleOps;
import multiplication.Multiplier;

import java.util.Random;
import java.util.concurrent.ForkJoinPool;

// sprawdzenie: max |A * A^-1 - I| dla każdego mnożenia, sekwencyjnie i na puli
public class Main {
    public static void main(String[] args) {
        var ops = new DoubleOps();
        int n = 501, leaf = 32;
        var rnd = new Random(42);
        var a = ops.random(n, n, -1, 1, rnd);
        for (int i = 0; i < n; i++)
            ops.set(a.view(), i, i, ops.get(a.view(), i, i) + n);  // dominująca przekątna = dobrze uwarunkowana

        try (var pool = new ForkJoinPool()) {
            var muls = java.util.Map.of("binet", Multiplier.<double[]>binet(),
                    "strassen", Multiplier.<double[]>strassen(), "laderman", Multiplier.<double[]>laderman());
            for (var e : muls.entrySet())
                for (boolean par : new boolean[]{false, true}) {
                    var inv = ops.newMatrix(n, n);
                    long t = System.nanoTime();
                    if (par) Inversion.invert(ops, e.getValue(), a.view(), inv.view(), leaf, pool);
                    else Inversion.invert(ops, e.getValue(), a.view(), inv.view(), leaf);
                    long ms = (System.nanoTime() - t) / 1_000_000;

                    var prod = ops.newMatrix(n, n);
                    ops.multiply(a.view(), inv.view(), prod.view());
                    double err = 0;
                    for (int i = 0; i < n; i++)
                        for (int j = 0; j < n; j++)
                            err = Math.max(err, Math.abs(ops.get(prod.view(), i, j) - (i == j ? 1 : 0)));
                    System.out.printf("%-9s %-5s %5d ms  err=%.2e%n", e.getKey(), par ? "pool" : "seq", ms, err);
                    if (err > 1e-9) throw new AssertionError("zły wynik: " + e.getKey());
                }
        }
    }
}
