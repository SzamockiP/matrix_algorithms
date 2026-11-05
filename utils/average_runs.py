#!/usr/bin/env python3
import sys
import re
import csv
from pathlib import Path
from collections import defaultdict

try:
    import matplotlib.pyplot as plt
    HAS_MATPLOTLIB = True
except ImportError:
    HAS_MATPLOTLIB = False


def process_directory(directory: Path, exclude=()):
    # Wzorzec dopasowujący pliki przebiegów: <algorytm>_<data>.csv
    # Ignoruje pliki już uśrednione (*_avg.csv)
    pattern = re.compile(r"^(.*)_(\d{4}-\d{2}-\d{2}_\d{2}-\d{2}-\d{2})\.csv$")

    files_by_algo = defaultdict(list)

    for file_path in sorted(directory.glob("*.csv")):
        if file_path.name.endswith("_avg.csv"):
            continue
        match = pattern.match(file_path.name)
        if match and not any(match.group(1).startswith(x) for x in exclude):
            algo_name = match.group(1)
            files_by_algo[algo_name].append(file_path)

    if not files_by_algo:
        print(f"Nie znaleziono plików przebiegów w: {directory}")
        return

    # Słownik przechowujący dane do wykresów:
    # plot_data[algo_name] = { 'sizes': [], 'times': [], 'muls': [], 'adds': [] }
    plot_data = {}

    for algo_name, file_paths in sorted(files_by_algo.items()):
        print(f"\nPrzetwarzanie algorytmu: {algo_name}")
        print(f"Znaleziono {len(file_paths)} plików:")
        for fp in file_paths:
            print(f"  - {fp.name}")

        all_runs = []
        for fp in file_paths:
            with open(fp, "r", encoding="utf-8") as f:
                reader = csv.reader(f, delimiter=";")
                rows = [row for row in reader if row]
                all_runs.append(rows)

        max_rows = max(len(run) for run in all_runs)
        avg_rows = []

        sizes = []
        times_avg = []
        muls_list = []
        adds_list = []

        for row_idx in range(max_rows):
            times = []
            sample_row = None

            for run in all_runs:
                if row_idx < len(run):
                    row = run[row_idx]
                    if sample_row is None:
                        sample_row = row
                    try:
                        times.append(float(row[2]))
                    except (ValueError, IndexError):
                        pass

            if not times or sample_row is None:
                continue

            avg_time = sum(times) / len(times)
            new_row = list(sample_row)
            new_row[2] = f"{avg_time:.2f}" if avg_time % 1 != 0 else f"{int(avg_time)}"
            avg_rows.append(new_row)

            try:
                sizes.append(int(new_row[0]))
                times_avg.append(avg_time)
                muls_list.append(int(new_row[3]))
                adds_list.append(int(new_row[4]))
            except (ValueError, IndexError):
                pass

        out_path = directory / f"{algo_name}_avg.csv"
        with open(out_path, "w", encoding="utf-8", newline="") as f:
            writer = csv.writer(f, delimiter=";")
            writer.writerows(avg_rows)

        print(f"Zapisano uśredniony plik: {out_path.name} ({len(avg_rows)} wierszy)")

        plot_data[algo_name] = {
            "sizes": sizes,
            "times": times_avg,
            "muls": muls_list,
            "adds": adds_list,
        }

    # Generowanie 3 wykresów
    if HAS_MATPLOTLIB and plot_data:
        generate_plots(directory, plot_data)
    elif not HAS_MATPLOTLIB:
        print("\nUwaga: Biblioteka matplotlib nie jest zainstalowana. Aby wygenerować wykresy, zainstaluj ją: pip install matplotlib")


def generate_plots(output_dir: Path, data: dict):
    # Kolory i style linii dla czytelności
    colors = {
        "naive": "#1f77b4",
        "binet": "#ff7f0e",
        "binet_threads": "#ffbb78",
        "strassen": "#2ca02c",
        "strassen_threads": "#98df8a",
        "laderman": "#d62728",
        "laderman_threads": "#ff9896",
    }

    def get_style(name):
        color = colors.get(name, None)
        linestyle = "-"
        return color, linestyle

    # 1. Wykres Czas vs Rozmiar (ilość wierszy)
    plt.figure(figsize=(10, 6))
    for algo_name, vals in data.items():
        color, ls = get_style(algo_name)
        plt.plot(vals["sizes"], vals["times"], label=algo_name, color=color, linestyle=ls, linewidth=1.8)
    plt.title("Czas wykonania w zależności od rozmiaru macierzy (N)")
    plt.xlabel("Rozmiar macieży (N)")
    plt.ylabel("Czas wykonania [ms]")
    plt.grid(True, linestyle=":", alpha=0.6)
    plt.legend()
    plt.tight_layout()
    time_plot_path = output_dir / "wykres_czas.png"
    plt.savefig(time_plot_path, dpi=300)
    plt.close()
    print(f"\nWygenerowano wykres czasu: {time_plot_path}")

    data = {k: v for k, v in data.items() if "threads" not in k}
    # 2. Wykres Mnożenia vs Rozmiar (ilość wierszy)
    plt.figure(figsize=(10, 6))
    for algo_name, vals in data.items():
        color, ls = get_style(algo_name)
        # Wersje jednowątkowe i wielowątkowe mają tyle samo mnożeń, więc rysujemy
        plt.plot(vals["sizes"], vals["muls"], label=algo_name, color=color, linestyle=ls, linewidth=1.8)
    plt.title("Liczba operacji mnożenia w zależności od rozmiaru macierzy (N)")
    plt.xlabel("Liczba wierszy / kolumn (N)")
    plt.ylabel("Liczba mnożeń")
    plt.yscale('log')
    plt.grid(True, linestyle=":", alpha=0.6)
    plt.legend()
    plt.tight_layout()
    muls_plot_path = output_dir / "wykres_mnozenia.png"
    plt.savefig(muls_plot_path, dpi=300)
    plt.close()
    print(f"Wygenerowano wykres mnożeń: {muls_plot_path}")

    # 3. Wykres Dodawania vs Rozmiar (ilość wierszy)
    plt.figure(figsize=(10, 6))
    for algo_name, vals in data.items():
        color, ls = get_style(algo_name)
        plt.plot(vals["sizes"], vals["adds"], label=algo_name, color=color, linestyle=ls, linewidth=1.8)
    plt.title("Liczba operacji dodawania w zależności od rozmiaru macierzy (N)")
    plt.xlabel("Liczba wierszy / kolumn (N)")
    plt.ylabel("Liczba dodawań")
    plt.grid(True, linestyle=":", alpha=0.6)
    plt.legend()
    plt.yscale('log')
    plt.tight_layout()
    adds_plot_path = output_dir / "wykres_dodawania.png"
    plt.savefig(adds_plot_path, dpi=300)
    plt.close()
    print(f"Wygenerowano wykres dodawań: {adds_plot_path}")


def main():
    # użycie: average_runs.py [katalog] [prefiksy algorytmów do pominięcia, np. laderman]
    target_dir = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("test_data/multiplication")
    exclude = tuple(sys.argv[2:])
    if not target_dir.exists():
        target_dir = Path("test_data")

    if target_dir.is_dir():
        subdirs = [p for p in target_dir.iterdir() if p.is_dir()]
        csv_in_current = list(target_dir.glob("*.csv"))

        if csv_in_current or not subdirs:
            process_directory(target_dir, exclude)
        else:
            for subdir in subdirs:
                process_directory(subdir, exclude)
    else:
        print(f"Ścieżka {target_dir} nie jest katalogiem!")


if __name__ == "__main__":
    main()
