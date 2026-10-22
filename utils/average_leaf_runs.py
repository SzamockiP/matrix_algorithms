#!/usr/bin/env python3

import sys
import os
import re
import csv
import tempfile
from pathlib import Path
from collections import defaultdict

# Ustawienie katalogu tymczasowego dla cache matplotlib przed importem
os.environ.setdefault("MPLCONFIGDIR", tempfile.gettempdir())
try:
    import matplotlib.pyplot as plt
    HAS_MATPLOTLIB = True
except ImportError:
    HAS_MATPLOTLIB = False


LEAF_PATTERN = re.compile(
    r"^([a-zA-Z0-9_]+?)_(\d+)-leaf_+(.*)\.csv$",
    re.IGNORECASE
)


def find_default_directory() -> Path:
    """Zwraca domyślny katalog z danymi testowymi."""
    candidates = [
        Path("test_data/multiplication"),
        Path("../test_data/multiplication"),
        Path("test_data"),
        Path("../test_data"),
        Path("."),
    ]
    for c in candidates:
        if c.exists() and c.is_dir():
            if any(LEAF_PATTERN.match(p.name) for p in c.glob("*.csv")):
                return c
    return Path("test_data/multiplication")


def process_directory(directory: Path):
    """
    Przetwarza wszystkie pliki CSV z parametrem leaf w podanym katalogu.
    Grupuje pliki według algorytmu i wartości leaf, a następnie uśrednia 3. kolumnę (czas).
    """
    # files_by_algo_leaf[algo_name][leaf_size] = [Path, Path, ...]
    files_by_algo_leaf = defaultdict(lambda: defaultdict(list))

    for file_path in sorted(directory.glob("*.csv")):
        # Ignorujemy pliki wynikowe z uśrednieniami
        if (file_path.name.endswith("_avg.csv") or
                "_summary" in file_path.name or
                "_averages" in file_path.name):
            continue

        match = LEAF_PATTERN.match(file_path.name)
        if match:
            algo_name = match.group(1)
            leaf_size = int(match.group(2))
            files_by_algo_leaf[algo_name][leaf_size].append(file_path)

    if not files_by_algo_leaf:
        print(f"Nie znaleziono plików przebiegów leaf w: {directory}")
        return

    for algo_name, leaves_dict in sorted(files_by_algo_leaf.items()):
        print(f"\n{'='*70}")
        print(f"Przetwarzanie algorytmu: {algo_name}")
        print(f"Liczba różnych wielkości leaf: {len(leaves_dict)}")
        total_files = sum(len(fps) for fps in leaves_dict.values())
        print(f"Łączna liczba plików z przebiegami: {total_files}")
        print(f"{'='*70}")

        # Struktura do przechowywania danych zbiorczych dla danego algorytmu
        # summary_data = { leaf_size: { 'avg_time': float, 'all_times': list, 'by_size': {size: avg_time} } }
        summary_data = {}
        all_sizes_seen = []

        for leaf_size in sorted(leaves_dict.keys()):
            file_paths = leaves_dict[leaf_size]
            print(f"\n-> Leaf {leaf_size} ({len(file_paths)} plików):")
            for fp in file_paths:
                print(f"     * {fp.name}")

            all_runs = []
            for fp in file_paths:
                with open(fp, "r", encoding="utf-8") as f:
                    reader = csv.reader(f, delimiter=";")
                    rows = [row for row in reader if row]
                    if rows:
                        all_runs.append(rows)

            if not all_runs:
                continue

            max_rows = max(len(run) for run in all_runs)
            avg_rows = []
            leaf_all_times = []
            by_size = {}

            for row_idx in range(max_rows):
                times = []
                sample_row = None

                for run in all_runs:
                    if row_idx < len(run):
                        row = run[row_idx]
                        if sample_row is None:
                            sample_row = row
                        try:
                            t_val = float(row[2])
                            times.append(t_val)
                            leaf_all_times.append(t_val)
                        except (ValueError, IndexError):
                            pass

                if not times or sample_row is None:
                    continue

                avg_time = sum(times) / len(times)
                new_row = list(sample_row)
                new_row[2] = f"{avg_time:.2f}" if avg_time % 1 != 0 else f"{int(avg_time)}"
                avg_rows.append(new_row)

                try:
                    matrix_size = int(new_row[0])
                    by_size[matrix_size] = avg_time
                    if matrix_size not in all_sizes_seen:
                        all_sizes_seen.append(matrix_size)
                except (ValueError, IndexError):
                    pass

            # Zapis uśrednionego pliku dla danego leaf: <algo>_<leaf>-leaf_avg.csv
            leaf_out_path = directory / f"{algo_name}_{leaf_size}-leaf_avg.csv"
            with open(leaf_out_path, "w", encoding="utf-8", newline="") as f:
                writer = csv.writer(f, delimiter=";")
                writer.writerows(avg_rows)

            overall_leaf_avg = sum(leaf_all_times) / len(leaf_all_times) if leaf_all_times else 0.0
            summary_data[leaf_size] = {
                "avg_time": overall_leaf_avg,
                "all_times": leaf_all_times,
                "by_size": by_size,
                "runs_count": len(all_runs),
            }

            print(f"   Zapisano uśredniony plik: {leaf_out_path.name}")
            print(f"   Średni czas dla leaf={leaf_size}: {overall_leaf_avg:.2f} ms")

        all_sizes_seen.sort()

        # Zapis pliku podsumowującego ze średnimi dla każdego leaf (prosty format: leaf;avg_time_ms)
        averages_csv_path = directory / f"{algo_name}_leaf_averages.csv"
        with open(averages_csv_path, "w", encoding="utf-8", newline="") as f:
            writer = csv.writer(f, delimiter=";")
            writer.writerow(["leaf", "avg_time_ms"])
            for leaf_size in sorted(summary_data.keys()):
                writer.writerow([leaf_size, f"{summary_data[leaf_size]['avg_time']:.2f}"])
        print(f"\n[OK] Zapisano listę średnich: {averages_csv_path.name}")

        # Zapis szczegółowego pliku podsumowującego (leaf;avg_time_ms;min_time_ms;max_time_ms;...)
        summary_csv_path = directory / f"{algo_name}_leaf_summary.csv"
        with open(summary_csv_path, "w", encoding="utf-8", newline="") as f:
            writer = csv.writer(f, delimiter=";")
            header = ["leaf", "avg_time_ms", "min_time_ms", "max_time_ms", "runs_count"]
            for s in all_sizes_seen:
                header.append(f"time_n_{s}")
            writer.writerow(header)

            for leaf_size in sorted(summary_data.keys()):
                d = summary_data[leaf_size]
                min_t = min(d["all_times"]) if d["all_times"] else 0.0
                max_t = max(d["all_times"]) if d["all_times"] else 0.0
                row = [
                    leaf_size,
                    f"{d['avg_time']:.2f}",
                    f"{min_t:.2f}",
                    f"{max_t:.2f}",
                    d["runs_count"],
                ]
                for s in all_sizes_seen:
                    s_avg = d["by_size"].get(s, None)
                    row.append(f"{s_avg:.2f}" if s_avg is not None else "")
                writer.writerow(row)
        print(f"[OK] Zapisano szczegółowe podsumowanie: {summary_csv_path.name}")

        # Tabela w konsoli z podsumowaniem
        print_summary_table(algo_name, summary_data)

        # Generowanie wykresów
        if HAS_MATPLOTLIB and summary_data:
            generate_leaf_plots(directory, algo_name, summary_data, all_sizes_seen)
        elif not HAS_MATPLOTLIB:
            print("\nUwaga: Matplotlib nie jest zainstalowany. Aby wygenerować wykresy: pip install matplotlib")


def print_summary_table(algo_name: str, summary_data: dict):
    """Wypisuje czytelną tabelę podsumowującą w terminalu."""
    print(f"\n{'='*75}")
    print(f"PODSUMOWANIE WYNIKÓW LEAF DLA: {algo_name}")
    print(f"{'='*75}")
    print(f"{'Leaf':>6} | {'Średni czas [ms]':>18} | {'Min [ms]':>10} | {'Max [ms]':>10} | {'Liczba przebiegów':>17}")
    print(f"{'-'*7}-+-{'-'*18}-+-{'-'*10}-+-{'-'*10}-+-{'-'*17}")

    best_leaf = None
    best_time = float("inf")

    for leaf in sorted(summary_data.keys()):
        d = summary_data[leaf]
        avg_t = d["avg_time"]
        min_t = min(d["all_times"]) if d["all_times"] else 0.0
        max_t = max(d["all_times"]) if d["all_times"] else 0.0
        runs_c = d["runs_count"]

        if avg_t < best_time:
            best_time = avg_t
            best_leaf = leaf

        print(f"{leaf:6d} | {avg_t:18.2f} | {min_t:10.2f} | {max_t:10.2f} | {runs_c:17d}")

    print(f"{'='*75}")
    if best_leaf is not None:
        print(f">>> NAJLEPSZA (NAJSZYBSZA) WARTOŚĆ LEAF: {best_leaf} (średni czas: {best_time:.2f} ms) <<<")
    print(f"{'='*75}\n")


def generate_leaf_plots(output_dir: Path, algo_name: str, summary_data: dict, sizes: list):
    """Generuje wykresy zależności czasu wykonania od wielkości liścia (leaf)."""
    sorted_leaves = sorted(summary_data.keys())
    avg_times = [summary_data[leaf]["avg_time"] for leaf in sorted_leaves]

    best_leaf = None
    best_time = float("inf")
    for leaf, t in zip(sorted_leaves, avg_times):
        if t < best_time:
            best_time = t
            best_leaf = leaf

    # 1. Wykres główny: Średni czas vs Leaf
    plt.figure(figsize=(11, 6))
    plt.plot(
        sorted_leaves,
        avg_times,
        marker="o",
        linewidth=2,
        markersize=6,
        color="#1f77b4",
        label=f"{algo_name} (średnia)"
    )

    if best_leaf is not None:
        plt.plot(
            [best_leaf],
            [best_time],
            marker="*",
            markersize=15,
            color="#d62728",
            label=f"Najlepszy leaf = {best_leaf} ({best_time:.2f} ms)"
        )
        plt.annotate(
            f"Min: leaf={best_leaf}\n{best_time:.2f} ms",
            xy=(best_leaf, best_time),
            xytext=(best_leaf + 2, best_time + (max(avg_times) - min(avg_times)) * 0.08),
            arrowprops=dict(facecolor="#d62728", shrink=0.08, width=1.5, headwidth=8),
            fontweight="bold",
            color="#a00000"
        )

    plt.title(f"Zależność czasu wykonania od parametru leaf ({algo_name})", fontsize=13, fontweight="bold")
    plt.xlabel("Wielkość liścia (leaf size)", fontsize=11)
    plt.ylabel("Średni czas wykonania [ms]", fontsize=11)
    plt.grid(True, linestyle=":", alpha=0.6)
    plt.legend(loc="upper right", frameon=True)
    plt.tight_layout()

    out_plot_path = output_dir / f"{algo_name}_wykres_leaf_czas.png"
    plt.savefig(out_plot_path, dpi=300)
    plt.close()
    print(f"[OK] Wygenerowano wykres: {out_plot_path.name}")

    # 2. Wykres dodatkowy: Czas vs Leaf dla wybranych rozmiarów macierzy N
    if sizes:
        plt.figure(figsize=(11, 6))
        # Wybierz do 5 równomiernie rozłożonych rozmiarów
        step = max(1, len(sizes) // 5)
        selected_sizes = sizes[::step]
        if sizes[-1] not in selected_sizes:
            selected_sizes.append(sizes[-1])

        colors = plt.cm.viridis([i / max(1, len(selected_sizes) - 1) for i in range(len(selected_sizes))])

        for s, col in zip(selected_sizes, colors):
            s_times = [summary_data[leaf]["by_size"].get(s, None) for leaf in sorted_leaves]
            valid_pts = [(l, t) for l, t in zip(sorted_leaves, s_times) if t is not None]
            if valid_pts:
                xs, ys = zip(*valid_pts)
                plt.plot(xs, ys, marker="s", markersize=4, linestyle="--", linewidth=1.5, color=col, label=f"N = {s}")

        plt.title(f"Czas wykonania w zależności od leaf dla różnych rozmiarów macierzy N", fontsize=13, fontweight="bold")
        plt.xlabel("Wielkość liścia (leaf size)", fontsize=11)
        plt.ylabel("Czas wykonania [ms]", fontsize=11)
        plt.grid(True, linestyle=":", alpha=0.6)
        plt.legend(loc="upper right", frameon=True)
        plt.tight_layout()

        sizes_plot_path = output_dir / f"{algo_name}_wykres_leaf_rozmiary.png"
        plt.savefig(sizes_plot_path, dpi=300)
        plt.close()
        print(f"[OK] Wygenerowano wykres dla rozmiarów: {sizes_plot_path.name}")


def main():
    if len(sys.argv) > 1:
        target_dir = Path(sys.argv[1])
    else:
        target_dir = find_default_directory()

    if not target_dir.exists():
        print(f"Błąd: Katalog {target_dir} nie istnieje!")
        sys.exit(1)

    if target_dir.is_dir():
        subdirs = [p for p in target_dir.iterdir() if p.is_dir()]
        csv_in_current = [p for p in target_dir.glob("*.csv") if LEAF_PATTERN.match(p.name)]

        if csv_in_current or not subdirs:
            process_directory(target_dir)
        else:
            for subdir in subdirs:
                csv_in_sub = [p for p in subdir.glob("*.csv") if LEAF_PATTERN.match(p.name)]
                if csv_in_sub:
                    process_directory(subdir)
    else:
        print(f"Ścieżka {target_dir} nie jest katalogiem!")
        sys.exit(1)


if __name__ == "__main__":
    main()
