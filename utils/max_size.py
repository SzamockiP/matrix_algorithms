#!/usr/bin/env python3
"""Szuka największego n, dla którego faktoryzacja się liczy (wersja wielowątkowa, leaf=240).

Każda próba to osobny proces JVM (OOM nie psuje kolejnych). Najpierw podwajanie n, potem szukanie binarne.
Użycie: python utils/max_size.py --java ścieżka/do/java --cp ścieżka/do/classes [--xmx 8g] [binet strassen laderman]
"""
import argparse
import subprocess

N_MAX = 46340  # n*n musi się mieścić w int (długość tablicy)


def probe(args, name, n):
    cmd = [args.java, *([f"-Xmx{args.xmx}"] if args.xmx else []), "-cp", args.cp, "factorization.Main", "3", name, str(n)]
    try:
        r = subprocess.run(cmd, capture_output=True, text=True, timeout=args.timeout)
    except subprocess.TimeoutExpired:
        return "timeout"
    return "ok" if r.returncode == 0 else ("oom" if r.returncode == 2 else "error: " + r.stderr[-300:])


def search(args, name):
    def run(n):
        s = probe(args, name, n)
        print(f"  {name} n={n}: {s}", flush=True)
        return s

    lo, hi = 0, None  # lo = największe znane OK, hi = najmniejsze znane niepowodzenie
    n = args.start
    while hi is None:
        s = run(n)
        if s == "ok":
            lo = n
            if n >= N_MAX:
                return lo, None, "limit tablicy"
            n = min(2 * n, N_MAX)
        else:
            hi, why = n, s
    while hi - lo > args.step:
        mid = (lo + hi) // 2
        s = run(mid)
        if s == "ok":
            lo = mid
        else:
            hi, why = mid, s
    return lo, hi, why


def main():
    p = argparse.ArgumentParser()
    p.add_argument("algorithms", nargs="*", default=["binet", "strassen", "laderman"])
    p.add_argument("--java", default="java")
    p.add_argument("--cp", default="out/production/matrix_algorithms")
    p.add_argument("--xmx", default=None, help="np. 8g; domyślnie domyślny limit JVM")
    p.add_argument("--start", type=int, default=2500)
    p.add_argument("--step", type=int, default=100, help="dokładność wyniku")
    p.add_argument("--timeout", type=int, default=1800, help="limit sekund na próbę")
    args = p.parse_args()

    for name in args.algorithms:
        lo, hi, why = search(args, name)
        print(f"==> {name}: max n = {lo} (n={hi} nie przechodzi: {why})", flush=True)


if __name__ == "__main__":
    main()
