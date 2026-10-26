package multiplication;

import common.MatrixOps;
import common.MatrixView;

import java.util.concurrent.ForkJoinPool;

// c = a * b dowolnym algorytmem mnożenia; pool == null oznacza wersję sekwencyjną
@FunctionalInterface
public interface Multiplier<A> {
    void multiply(MatrixOps<A> ops, MatrixView<A> a, MatrixView<A> b, MatrixView<A> c, int leaf, ForkJoinPool pool);

    static <A> Multiplier<A> binet() {
        return (ops, a, b, c, leaf, pool) -> {
            if (pool == null) Binet.multiply(ops, a, b, c, leaf);
            else Binet.multiply(ops, a, b, c, leaf, pool);
        };
    }

    static <A> Multiplier<A> strassen() {
        return (ops, a, b, c, leaf, pool) -> {
            if (pool == null) Strassen.multiply(ops, a, b, c, leaf);
            else Strassen.multiply(ops, a, b, c, leaf, pool);
        };
    }

    static <A> Multiplier<A> laderman() {
        return (ops, a, b, c, leaf, pool) -> {
            if (pool == null) Laderman.multiply(ops, a, b, c, leaf);
            else Laderman.multiply(ops, a, b, c, leaf, pool);
        };
    }
}
