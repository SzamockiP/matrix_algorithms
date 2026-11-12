// Sprawozdanie — Algorytmy macierzowe, faktoryzacja LU (jeden plik, do typst.app)
// Ramki "DO UZUPEŁNIENIA" wyłącza się zmienną `pokaz-todo` poniżej.

// Ustaw na `false`, żeby ukryć wszystkie ramki "DO UZUPEŁNIENIA"
// przy kompilacji wersji oddawanej.
#let pokaz-todo = true

#let akcent = rgb("#1b3fbf")      // kolor słów kluczowych w pseudokodach
#let todo-kolor = rgb("#d98e04")

// ---------------------------------------------------------------------
//  Szablon dokumentu
// ---------------------------------------------------------------------
#let sprawozdanie(
  tytul: none,
  podtytul: none,
  przedmiot: none,
  autorzy: (),
  data: none,
  body,
) = {
  set document(title: tytul, author: autorzy)
  set text(lang: "pl", font: "New Computer Modern", size: 11pt)
  set page(
    paper: "a4",
    margin: (x: 2.3cm, y: 2.4cm),
    numbering: "1",
    header: context {
      if counter(page).get().first() > 1 [
        #set text(9pt, fill: luma(90))
        #przedmiot #h(1fr) #tytul
        #v(-6pt)
        #line(length: 100%, stroke: 0.4pt + luma(150))
      ]
    },
  )
  set par(justify: true, leading: 0.62em, spacing: 0.95em)
  set heading(numbering: "1.1.")
  show heading.where(level: 1): it => {
    v(0.9em)
    it
    v(0.35em)
  }
  set math.equation(numbering: "(1)")
  show raw: set text(font: "DejaVu Sans Mono", size: 0.9em)
  show figure.where(kind: table): set figure.caption(position: top)
  set table(stroke: 0.5pt + luma(120), inset: 6pt)

  // --- strona tytułowa (skrócona) ---
  align(center)[
    #text(10pt)[Akademia Górniczo-Hutnicza im. Stanisława Staszica w Krakowie \
      Wydział Informatyki]
    #v(1.6cm)
    #text(13pt, przedmiot)
    #v(0.3cm)
    #text(20pt, weight: "bold", tytul)
    #if podtytul != none {
      v(0.15cm)
      text(13pt, podtytul)
    }
    #v(0.8cm)
    #text(12pt, autorzy.join([ #h(0.5em)·#h(0.5em) ]))
    #v(0.2cm)
    #text(10.5pt, data)
    #v(1.2cm)
  ]

  outline(title: [Spis treści], indent: auto, depth: 1)
  pagebreak()

  body
}

// ---------------------------------------------------------------------
//  Miejsca do uzupełnienia
// ---------------------------------------------------------------------
#let todo(body) = if pokaz-todo {
  block(
    width: 100%,
    inset: (x: 10pt, y: 8pt),
    radius: 2pt,
    fill: todo-kolor.lighten(88%),
    stroke: (left: 3pt + todo-kolor),
  )[#text(weight: "bold", fill: todo-kolor.darken(25%))[DO UZUPEŁNIENIA:] #body]
}

// Pole na wykres: zastąp `miejsce-na-wykres(...)` przez
// `image("../wykresy/nazwa.png", width: 100%)` (ścieżka względem pliku sekcji).
#let miejsce-na-wykres(opis, wysokosc: 7.5cm) = rect(
  width: 100%,
  height: wysokosc,
  stroke: (paint: luma(140), thickness: 0.8pt, dash: "dashed"),
  radius: 3pt,
  align(center + horizon, text(fill: luma(110))[Miejsce na wykres \ #emph(opis)]),
)

#let uwaga(tytul: [Uwaga], body) = block(
  width: 100%,
  inset: (x: 10pt, y: 8pt),
  radius: 2pt,
  fill: akcent.lighten(92%),
  stroke: (left: 3pt + akcent),
)[*#tytul.* #body]

// ---------------------------------------------------------------------
//  Pseudokody w notacji FLAME
//  (por. https://www.cs.utexas.edu/~flame/laff/alaff/chapter03-cgs-algorithm.html)
// ---------------------------------------------------------------------
#let kw(s) = text(fill: akcent, weight: "bold", s)

// Jeden wiersz pseudokodu z wcięciem `p` poziomów.
#let w(p: 0, body) = block(
  above: 0.5em,
  below: 0.5em,
  breakable: false,
  pad(left: p * 1.4em, body),
)

// Pozioma kreska rozdzielająca fazy algorytmu (jak w FLAME).
#let kreska(p: 1) = block(
  above: 0.55em,
  below: 0.55em,
  pad(left: p * 1.4em, right: 3em, line(length: 100%, stroke: 0.5pt)),
)

// Ramka algorytmu: nagłówek (sygnatura) + ciało.
#let algorytm(naglowek, opis, body) = figure(
  kind: "algorytm",
  supplement: [Algorytm],
  caption: opis,
  block(width: 100%, stroke: 0.7pt, {
    set align(left)
    set par(justify: false)
    show math.mat: it => math.display(it)
    block(
      width: 100%,
      inset: (x: 9pt, y: 7pt),
      stroke: (bottom: 0.7pt),
      below: 0pt,
      naglowek,
    )
    block(width: 100%, inset: (x: 9pt, top: 4pt, bottom: 8pt), above: 0pt, body)
  }),
)

// =====================================================================
//  Treść
// =====================================================================

#show: sprawozdanie.with(
  tytul: [Rekurencyjna faktoryzacja LU],
  podtytul: [Metody Bineta, Strassena i Laderman — sprawozdanie z zadania 2],
  przedmiot: [Algorytmy macierzowe],
  autorzy: ("Piotr Szamocki", "Mateusz Stoch", "Patrycja Piasecka"),
  data: [Kraków, listopad 2026],
)

// ---------------------------------------------------------------------
// 01-wstep
// ---------------------------------------------------------------------

= Cel zadania

Zaimplementowaliśmy rekurencyjną faktoryzację LU macierzy kwadratowej, $A = L U$. $L$ jest dolnotrójkątna z jedynkową przekątną, $U$ jest górnotrójkątna. Wszystkie mnożenia macierzy w algorytmie wykonuje jedna z metod z zadania 1:

+ metoda Bineta (bloki $2 times 2$, 8 mnożeń bloków),
+ metoda Strassena (bloki $2 times 2$, 7 mnożeń bloków),
+ metoda Laderman (bloki $3 times 3$, 23 mnożenia bloków) — dodatek ponad wymagania polecenia.

Wymagania z polecenia:

- macierze kwadratowe o dowolnym rozmiarze,
- elementy losowe z przedziału $(10^(-8), 1)$,
- zliczanie operacji zmiennoprzecinkowych ($+ - * slash$),
- mnożenia macierzy wykonują nasze algorytmy z zadania 1.

Każdy wariant ma wersję jednowątkową i wielowątkową. Obie wykonują te same operacje.

== Macierze testowe i brak pivotowania <sec-macierze>

Faktoryzacja jest bez pivotowania. Na zwykłej macierzy losowej z $(10^(-8), 1)$ takie LU jest niestabilne: dla $n = 2500$ największy element $U$ ma około $1.2 dot 10^5$, a wynik Strassena różni się od klasycznego LU nawet o 2% (względnie). Dlatego w pomiarach elementy są losowe z $(10^(-8), 1)$, a do każdego elementu przekątnej dodajemy $n$. Macierz jest wtedy ściśle diagonalnie dominująca, więc LU bez pivotowania istnieje i jest stabilne. Elementy poza przekątną pochodzą z przedziału z polecenia. Dla macierzy z zerowym minorem głównym program zgłasza `ArithmeticException`.

// ---------------------------------------------------------------------
// 02-srodowisko
// ---------------------------------------------------------------------

= Język i środowisko <sec-srodowisko>

== Dlaczego Java nie jest językiem interpretowanym

Kompilator `javac` tłumaczy kod źródłowy na kod bajtowy. Podczas działania maszyna wirtualna (HotSpot) kompiluje często używane fragmenty do kodu maszynowego (kompilacja JIT). Pętle faktoryzacji i mnożenia wykonują się więc jako kod maszynowy.

Konsekwencja: pierwsze wywołania są wolniejsze, bo kod nie jest jeszcze skompilowany. Dlatego powtarzamy pomiary i uśredniamy wyniki (@sec-metodyka).

== Sprzęt

#figure(
  caption: [Konfiguracja pomiarowa],
  table(
    columns: (auto, 1fr),
    align: (left, left),
    [Procesor], [Intel Core i7-13700HX (16 rdzeni, 24 wątki)],
    [Pamięć RAM], [32 GB],
    [System operacyjny], [Windows 10 Home],
    [JDK], [OpenJDK 25],
    [Maks. sterta JVM], [8,5 GB (domyślnie, 1/4 RAM)],
  ),
) <tab-sprzet>

// ---------------------------------------------------------------------
// 03-implementacja
// ---------------------------------------------------------------------

= Implementacja <sec-implementacja>

Z zadania 1 używamy bez zmian widoków na tablicę 1D (podział na bloki bez kopiowania danych), algorytmów mnożenia Bineta, Strassena i Laderman (ze Strassenem i Laderman dla dowolnych rozmiarów dzięki peelingowi) oraz puli `ForkJoinPool`. Nowe są: rekurencja LU, rekurencyjne rozwiązywanie układów trójkątnych i kernele klasyczne na liściach rekursji.

== Algorytm blokowy <sec-blokowy>

Dzielimy macierz na cztery bloki, $A_(11)$ ma $floor(n slash 2)$ wierszy:
$ mat(A_(11), A_(12); A_(21), A_(22); augment: #(hline: 1, vline: 1)) =
  mat(L_(11), 0; L_(21), L_(22); augment: #(hline: 1, vline: 1))
  mat(U_(11), U_(12); 0, U_(22); augment: #(hline: 1, vline: 1)). $
Mnożąc bloki, dostajemy cztery równania:
$ A_(11) = L_(11) U_(11), quad A_(12) = L_(11) U_(12), quad A_(21) = L_(21) U_(11), quad A_(22) = L_(21) U_(12) + L_(22) U_(22). $
Stąd kolejność obliczeń:

+ $L_(11) U_(11) = A_(11)$ — rekurencyjna faktoryzacja LU,
+ $U_(12) = L_(11)^(-1) A_(12)$ — układ trójkątny z wieloma prawymi stronami,
+ $L_(21) = A_(21) U_(11)^(-1)$ — układ trójkątny z lewej strony,
+ $S = A_(22) - L_(21) U_(12)$ — jedno mnożenie macierzy i odejmowanie (dopełnienie Schura),
+ $L_(22) U_(22) = S$ — rekurencyjna faktoryzacja LU.

Układy z punktów 2 i 3 też rozwiązujemy rekurencyjnie (@alg-solve-l, @alg-solve-u). Cała praca poza liśćmi to mnożenia macierzy, więc zamiana algorytmu mnożenia zmienia koszt faktoryzacji.

Faktoryzacja działa w miejscu. Program kopiuje $A$ do macierzy wynikowej i nadpisuje ją: $L$ leży poniżej przekątnej (jedynki z przekątnej nie są przechowywane), $U$ leży na przekątnej i nad nią.

== Zliczanie operacji <sec-liczenie>

Program zlicza operacje podczas działania. Dzielenia liczymy jako mnożenia, odejmowania jako dodawania. Kernele liściowe zliczają operacje wzorami, raz na wywołanie (@tab-liczniki-lu). Mnożenia i odejmowania bloków liczą się jak w zadaniu 1.

#figure(
  caption: [Koszt kerneli klasycznych ($L$, $U$: $n times n$; prawa strona ma $m$ kolumn albo wierszy)],
  table(
    columns: (auto, auto, auto),
    align: (left, center, center),
    table.header([*Działanie*], [*Mnożenia*], [*Dodawania*]),
    [LU klasyczne $n times n$], [$(n^3 - n) slash 3$], [$(n - 1) n (2n - 1) slash 6$],
    [$B := L^(-1) B$, $B$: $n times m$], [$m n (n - 1) slash 2$], [$m n (n - 1) slash 2$],
    [$B := B U^(-1)$, $B$: $m times n$], [$m n (n - 1) slash 2 + m n$], [$m n (n - 1) slash 2$],
  ),
) <tab-liczniki-lu>

W $B U^(-1)$ dodatkowe $m n$ mnożeń to dzielenia przez elementy przekątnej $U$.

== Wersja jedno- i wielowątkowa

- *Jednowątkowa* — wszystko wykonuje się po kolei.
- *Wielowątkowa* — `ForkJoinPool` z liczbą wątków równą liczbie wątków procesora (24). Równolegle idą wnętrza mnożeń (jak w zadaniu 1) oraz para układów trójkątnych $U_(12)$ i $L_(21)$, które nie zależą od siebie. Reszta jest sekwencyjna, bo każdy krok potrzebuje wyniku poprzedniego.

== Próg rekursji <sec-prog>

Rekursja kończy się, gdy $n <= n_0$ (parametr `leaf`) albo $n < 2$. Wtedy używamy kernela klasycznego: LU klasycznego, a w układach trójkątnych podstawień w przód i w tył. Ten sam $n_0$ dostają algorytmy mnożenia.

Wpływ progu zmierzyliśmy dla Bineta: $n_0 = 8, 16, dots, 256$, rozmiary $n = 1900, 1910, dots, 1990$, 3 przebiegi (@fig-leaf). Użyliśmy macierzy diagonalnie dominujących. Czas nie zależy od wartości elementów, bo liczba i rodzaj operacji są takie same.

#figure(
  grid(
    columns: 2,
    column-gutter: 6pt,
    image("img_2/binet_wykres_leaf_czas.png"),
    image("img_2/binet_threads_wykres_leaf_czas.png"),
  ),
  caption: [Średni czas LU metodą Bineta w zależności od $n_0$: wersja jednowątkowa (po lewej) i wielowątkowa (po prawej)],
) <fig-leaf>

W wersji wielowątkowej czas spada do $n_0 approx 64$, potem jest prawie stały (215–230 ms) i rośnie dla $n_0 >= 240$. W wersji jednowątkowej czas maleje do końca badanego zakresu (1006 ms dla $n_0 = 256$). Jedna wartość $n_0$ musi obsłużyć obie wersje, żeby wykonywały te same operacje. Wybraliśmy $n_0 = 176$: ma najmniejszą sumę czasów obu wersji, każdego podzielonego przez jego minimum. Daje 1083 ms w wersji jednowątkowej (najlepszy: 1006 ms) i 222 ms w wielowątkowej (najlepszy: 213 ms). Wszystkie pomiary w sprawozdaniu wykonano z $n_0 = 176$.

// ---------------------------------------------------------------------
// 04-algorytmy
// ---------------------------------------------------------------------

= Pseudokody <sec-algorytmy>

#let kom(t) = text(fill: luma(110), size: 0.92em)[▷ #t]

#uwaga(tytul: [Notacja])[Bloki to widoki na macierz, zapis do bloku trafia od razu do macierzy. Iloczyny $L_(21) B_1$ i $A_(21) A_(12)$ wykonuje dowolny algorytm mnożenia z zadania 1 (Binet albo Strassen). W bloku #kw[równolegle] oba zadania wykonują się na puli wątków w wersji wielowątkowej, a po kolei w wersji jednowątkowej.]

== Faktoryzacja LU

#algorytm(
  [$[A] := "LU"(A)$ #h(1fr) #kom[w miejscu: $A := L \\ U$]],
  [Rekurencyjna faktoryzacja LU],
)[
  #w[#kw[jeśli] $n <= n_0$ #kw[lub] $n < 2$ #kw[to]]
  #w(p: 1)[$A := "LU"_"klasyczne" (A)$]
  #w(p: 1)[#kw[zwróć]]
  #w[#kw[koniec jeśli]]
  #w[
    $A -> mat(A_(T L), A_(T R); A_(B L), A_(B R); augment: #(hline: 1, vline: 1))$
  ]
  #w(p: 1)[#kw[gdzie] $A_(T L)$ ma $floor(n slash 2)$ wierszy i kolumn]
  #kreska()
  #w(p: 1)[$A_(T L) := "LU"(A_(T L))$ #h(1fr) #kom[$L_(11) \\ U_(11)$]]
  #w(p: 1)[#kw[równolegle]]
  #w(p: 2)[$A_(T R) := "SolveL"(A_(T L), A_(T R))$ #h(1fr) #kom[$U_(12) = L_(11)^(-1) A_(12)$ (@alg-solve-l)]]
  #w(p: 2)[$A_(B L) := "SolveU"(A_(T L), A_(B L))$ #h(1fr) #kom[$L_(21) = A_(21) U_(11)^(-1)$ (@alg-solve-u)]]
  #w(p: 1)[#kw[koniec równolegle]]
  #w(p: 1)[$T := A_(B L) A_(T R)$ #h(1fr) #kom[mnożenie macierzy]]
  #w(p: 1)[$A_(B R) := A_(B R) - T$ #h(1fr) #kom[dopełnienie Schura]]
  #w(p: 1)[$A_(B R) := "LU"(A_(B R))$ #h(1fr) #kom[$L_(22) \\ U_(22)$]]
] <alg-lu>

== Układy trójkątne

Oba algorytmy rozwiązują układ w miejscu. Rekurencja idzie po macierzy trójkątnej, a prawa strona jest dzielona razem z nią.

#algorytm(
  [$[B] := "SolveL"(L, B)$ #h(1fr) #kom[$B := L^(-1) B$, $L$ dolnotrójkątna z jedynkową przekątną]],
  [Układ z macierzą dolnotrójkątną],
)[
  #w[#kw[jeśli] $n <= n_0$ #kw[lub] $n < 2$ #kw[to]]
  #w(p: 1)[$B := L^(-1) B$ #h(1fr) #kom[podstawianie w przód]]
  #w(p: 1)[#kw[zwróć]]
  #w[#kw[koniec jeśli]]
  #w[
    $L -> mat(L_(T L), 0; L_(B L), L_(B R); augment: #(hline: 1, vline: 1)),
    B -> mat(B_T; B_B; augment: #(hline: 1))$
  ]
  #kreska()
  #w(p: 1)[$B_T := "SolveL"(L_(T L), B_T)$]
  #w(p: 1)[$T := L_(B L) B_T$ #h(1fr) #kom[mnożenie macierzy]]
  #w(p: 1)[$B_B := B_B - T$]
  #w(p: 1)[$B_B := "SolveL"(L_(B R), B_B)$]
] <alg-solve-l>

#algorytm(
  [$[B] := "SolveU"(U, B)$ #h(1fr) #kom[$B := B U^(-1)$, $U$ górnotrójkątna]],
  [Układ z macierzą górnotrójkątną z prawej strony],
)[
  #w[#kw[jeśli] $n <= n_0$ #kw[lub] $n < 2$ #kw[to]]
  #w(p: 1)[$B := B U^(-1)$ #h(1fr) #kom[podstawianie, dzielenie przez przekątną]]
  #w(p: 1)[#kw[zwróć]]
  #w[#kw[koniec jeśli]]
  #w[
    $U -> mat(U_(T L), U_(T R); 0, U_(B R); augment: #(hline: 1, vline: 1)),
    B -> mat(B_L, B_R; augment: #(vline: 1))$
  ]
  #kreska()
  #w(p: 1)[$B_L := "SolveU"(U_(T L), B_L)$]
  #w(p: 1)[$T := B_L U_(T R)$ #h(1fr) #kom[mnożenie macierzy]]
  #w(p: 1)[$B_R := B_R - T$]
  #w(p: 1)[$B_R := "SolveU"(U_(B R), B_R)$]
] <alg-solve-u>

// ---------------------------------------------------------------------
// 05-fragmenty-kodu
// ---------------------------------------------------------------------

= Najważniejsze fragmenty kodu <sec-kod>

#show raw.where(block: true): set text(size: 7.6pt)
#show raw.where(block: true): it => block(width: 100%, fill: luma(246), inset: 7pt, radius: 2pt, it)

== Krok rekurencyjny LU

Blok $A_(11)$ jest faktoryzowany, potem dwa niezależne układy trójkątne (jeden w puli, drugi w bieżącym wątku), mnożenie i odejmowanie, rekurencja na dopełnieniu Schura.

```java
int n1 = n / 2, n2 = n - n1;
var a11 = a.block(0, 0, n1, n1);   var a12 = a.block(0, n1, n1, n2);
var a21 = a.block(n1, 0, n2, n1);  var a22 = a.block(n1, n1, n2, n2);

factorRec(ops, mul, a11, leaf, pool);
Runnable u12 = () -> solveLower(ops, mul, a11, a12, leaf, pool);       // U12 = L11^-1 A12
Runnable l21 = () -> solveUpperRight(ops, mul, a11, a21, leaf, pool);  // L21 = A21 U11^-1
if (pool != null) {
    var f = ForkJoinTask.adapt(u12).fork();
    l21.run();
    f.join();
} else {
    u12.run();
    l21.run();
}
var t = ops.newMatrix(n2, n2).view();
mul.multiply(ops, a21, a12, t, leaf, pool);   // T = L21 U12 (Binet / Strassen / Laderman)
ops.subtract(a22, t, a22);                    // S = A22 - T
factorRec(ops, mul, a22, leaf, pool);
```

== Układ z macierzą dolnotrójkątną

Dolna połowa prawej strony jest poprawiana po rozwiązaniu górnej. Układ $B U^(-1)$ jest zbudowany symetrycznie (podział $B$ na kolumny).

```java
var b1 = b.block(0, 0, n1, m);
var b2 = b.block(n1, 0, n2, m);

solveLower(ops, mul, l.block(0, 0, n1, n1), b1, leaf, pool);
var t = ops.newMatrix(n2, m).view();
mul.multiply(ops, l.block(n1, 0, n2, n1), b1, t, leaf, pool);
ops.subtract(b2, t, b2);
solveLower(ops, mul, l.block(n1, n1, n2, n2), b2, leaf, pool);
```

== LU klasyczne (liście rekursji)

Kernel nadpisuje macierz w miejscu: mnożniki trafiają pod przekątną, a reszta jest aktualizowana.

```java
protected void luInPlaceImpl(MatrixView<double[]> a) {
    int n = a.rows;
    for (int k = 0; k < n; k++) {
        int top = a.idx(k, 0);
        double p = a.data[top + k];
        if (p == 0.0)
            throw new ArithmeticException("Zerowy element główny (brak pivotowania)");
        for (int i = k + 1; i < n; i++) {
            int row = a.idx(i, 0);
            double f = a.data[row + k] / p;
            a.data[row + k] = f;
            for (int j = k + 1; j < n; j++)
                a.data[row + j] -= f * a.data[top + j];
        }
    }
}
```

// ---------------------------------------------------------------------
// 07-wyniki
// ---------------------------------------------------------------------

= Wyniki pomiarów <sec-wyniki>

== Metodyka <sec-metodyka>

- Rozmiary: $n = 10, 20, dots, 2500$ (krok 10).
- Macierze: elementy losowe z $(10^(-8), 1)$, do przekątnej dodane $n$ (@sec-macierze). To samo ziarno dla wszystkich wariantów.
- Próg rekursji: $n_0 = 176$ (@sec-prog).
- Każdy wariant: czas, liczba mnożeń, liczba dodawań.
- 3 przebiegi, czasy uśrednione. Liczby operacji są deterministyczne.
- Wersje wielowątkowe: 24 wątki (oznaczone `_threads`). Klasyczne LU (`naive`) jest punktem odniesienia.

== Czas działania

#figure(
  image("img_2/wykres_czas.png"),
  caption: [Czas faktoryzacji LU macierzy $n times n$],
) <fig-czas>

Do $n approx 1000$ różnice między wariantami jednowątkowymi są małe, od $n approx 1500$ rosną. Dla $n = 2500$ klasyczne LU potrzebuje 2529 ms, Binet 1970 ms (o 22% mniej), Strassen 1607 ms (o 36% mniej, czyli 1,57 razy szybciej), a Laderman 1756 ms (o 31% mniej, 1,44 razy szybciej). Binet wykonuje tyle samo operacji co wersja klasyczna, więc przewaga bierze się prawdopodobnie z lepszej lokalności pamięci: rekursja pracuje na małych blokach.

Wersje wielowątkowe są kilka razy szybsze od jednowątkowych tej samej metody: dla $n = 2500$ Binet potrzebuje 396 ms (4,97 razy szybciej), Strassen 348 ms (4,62 razy szybciej), a Laderman 368 ms (4,78 razy szybciej). Przyspieszenie jest dużo mniejsze od liczby wątków (24). Szkielet LU jest sekwencyjny, a kernele liściowe działają w jednym wątku.

== Liczba operacji

#figure(
  image("img_2/wykres_mnozenia.png"),
  caption: [Liczba mnożeń zmiennoprzecinkowych (skala logarytmiczna)],
) <fig-mnozenia>

#figure(
  image("img_2/wykres_dodawania.png"),
  caption: [Liczba dodawań zmiennoprzecinkowych (skala logarytmiczna)],
) <fig-dodawania>

Wersje jedno- i wielowątkowa wykonują te same operacje, więc rysujemy jedną krzywą na metodę. Klasyczne LU wykonuje $(n^3 - n) slash 3$ mnożeń i $(n - 1) n (2n - 1) slash 6$ dodawań, czyli około $n^3 slash 3$. Dla $n = 2500$ wzory dają 5 208 332 500 mnożeń i 5 205 208 750 dodawań, dokładnie tyle zliczył program. Binet wykonuje identyczną liczbę operacji we wszystkich zmierzonych rozmiarach (krzywe się pokrywają), bo rekursja zmienia tylko kolejność iloczynów $l_(i k) u_(k j)$, a nie ich zbiór.

Strassen wykonuje mniej mnożeń od $n > n_0$. Dla $n = 2500$ ma 3 782 919 315 mnożeń i 3 914 660 800 dodawań, czyli 72,6% i 75,2% wartości klasycznych. Laderman ma 3 960 207 500 mnożeń i 4 197 676 438 dodawań (76,0% i 80,6%). Oszczędność dodawań jest mniejsza, bo obie metody dodają bloki; Laderman potrzebuje ich więcej niż Strassen.

== Maksymalny rozmiar macierzy <sec-maxn>

Sprawdziliśmy, jak dużą macierz da się zfaktoryzować przy domyślnej stercie JVM (8,5 GB, @tab-sprzet). Szukaliśmy największego $n$, dla którego program kończy się bez `OutOfMemoryError`: podwajanie $n$ od 2500 do pierwszego niepowodzenia, potem wyszukiwanie binarne z dokładnością do 100. Każda próba to osobny proces JVM, wersja wielowątkowa, $n_0 = 176$ (@tab-maxn).

#figure(
  caption: [Największy rozmiar macierzy mieszczący się w pamięci (sterta 8,5 GB)],
  table(
    columns: (auto, auto, auto),
    align: (left, center, center),
    table.header([*Metoda*], [*Maks. $n$*], [*Pierwszy $n$ z brakiem pamięci*]),
    [Binet], [21 328], [21 406],
    [Strassen], [11 796], [11 875],
    [Laderman], [11 562], [11 640],
  ),
) <tab-maxn>

Binet trzyma w pamięci tylko macierz wejściową i wynik, po $8 n^2$ bajtów każda: dla $n = 21328$ to 7,3 GB z 8,5 GB sterty. Strassen potrzebuje jeszcze buforów na iloczyny pośrednie $M_1, dots, M_7$ i sumy argumentów, które istnieją jednocześnie z macierzami, więc jego granica jest niższa. Laderman ma podobne bufory (iloczyny pośrednie 23 bloków) i mieści się w nieco mniejszych macierzach niż Strassen. Liczyliśmy tylko wersje wielowątkowe, bo bufory są przydzielane z góry i zużycie pamięci powinno być takie samo w wersjach jednowątkowych.

// ---------------------------------------------------------------------
// 08-poprawnosc
// ---------------------------------------------------------------------

= Poprawność algorytmu <sec-octave>

Dla każdego rozmiaru w pomiarach program porównuje wynik każdego wariantu z klasycznym LU liczonym na całej macierzy (element po elemencie, tolerancja względna $10^(-9)$). Wszystkie warianty przeszły to porównanie. Poniżej pokazujemy dodatkowo przykład dla małej macierzy i porównanie z Octave.

== Przykład dla macierzy $4 times 4$

Wylosowaliśmy $A$ o rozmiarze $4 times 4$ z przedziału $(10^(-8), 1)$ (bez dodawania $n$ do przekątnej). Wykonaliśmy faktoryzację każdą metodą z $n_0 = 1$, w wersji jedno- i wielowątkowej, żeby rekursja wykonała się do końca. Wynik programu (zaokrąglony do 4 miejsc):
$ A = mat(0.6186, 0.8492, 0.7904, 0.5296; 0.8417, 0.1483, 0.8438, 0.5848; 0.1667, 0.7158, 0.5393, 0.2795; 0.0557, 0.0131, 0.3578, 0.7500), $
$ L = mat(1, 0, 0, 0; 1.3605, 1, 0, 0; 0.2695, -0.4835, 1, 0; 0.0900, 0.0629, 1.4057, 1), quad
  U = mat(0.6186, 0.8492, 0.7904, 0.5296; 0, -1.0071, -0.2316, -0.1358; 0, 0, 0.2143, 0.0711; 0, 0, 0, 0.6109). $
Pierwszy krok sprawdzony ręcznie: $l_(21) = a_(21) slash a_(11) = 0.8417 slash 0.6186 = 1.3605$, $u_(22) = a_(22) - l_(21) a_(12) = 0.1483 - 1.3605 dot 0.8492 approx -1.007$. Zgadza się z macierzami wyżej.

== Porównanie z referencyjnym LU

Procedura:

+ Zapisaliśmy $A$ i spakowane $L backslash U$ z każdego wariantu z pełną precyzją (`%.17g`).
+ Policzyliśmy referencyjne LU bez pivotowania (ta sama kolejność działań co w programie) i porównaliśmy z wynikami programu.

```matlab
A = load('A.txt');  n = rows(A);
L = eye(n); U = A;
for k = 1:n-1
  for i = k+1:n
    L(i,k) = U(i,k) / U(k,k);
    U(i,k:n) = U(i,k:n) - L(i,k) * U(k,k:n);
  end
end
U = triu(U);
M = load('LU_binet.txt');  Lj = tril(M,-1) + eye(n);  Uj = triu(M);
max(max(abs(L - Lj))),  max(max(abs(U - Uj))),  max(max(abs(Lj*Uj - A)))
```

#figure(
  caption: [Porównanie z referencyjnym LU bez pivotowania, macierz $4 times 4$, $n_0 = 1$ ($epsilon approx 2.2 dot 10^(-16)$)],
  table(
    columns: (auto, auto, 1fr, 1fr, 1fr),
    align: (left, center, center, center, center),
    table.header([*Metoda*], [*Mnożenia / dodawania*], [*$max |L - L_j|$*], [*$max |U - U_j|$*], [*$max |L_j U_j - A|$*]),
    [Binet], [20 / 14], [$0$], [$0$], [$1.1 dot 10^(-16)$],
    [Strassen], [19 / 28], [$0$], [$1.1 dot 10^(-16)$], [$1.1 dot 10^(-16)$],
    [Laderman], [20 / 14], [$0$], [$0$], [$1.1 dot 10^(-16)$],
  ),
) <tab-octave>

Wersje jedno- i wielowątkowe dały identyczne wyniki. Binet i Laderman zgadzają się z referencją co do bitu (dla $n = 4$ Laderman dzieli macierz tak, że wykonuje te same operacje co Binet, więc ten przykład nie sprawdza jego bloków $3 times 3$; robią to testy na dużych macierzach), Strassen różni się o jedną jednostkę ostatniego miejsca w $U$, bo wykonuje więcej dodawań i odejmowań. Reszta $L_j U_j - A$ jest rzędu $epsilon$. Dla $n = 4$ Binet wykonuje tyle samo operacji co klasyczne LU ($(4^3 - 4) slash 3 = 20$ mnożeń i $3 dot 4 dot 7 slash 6 = 14$ dodawań). Wyniki są poprawne.

// ---------------------------------------------------------------------
// 09-wnioski
// ---------------------------------------------------------------------
#pagebreak()
= Wnioski

+ Faktoryzację LU można zapisać blokowo tak, że cała praca poza liśćmi to mnożenia macierzy, bo układy trójkątne też rozwiązujemy przez mnożenia. Zamiana algorytmu mnożenia zmienia więc koszt faktoryzacji.
+ Binet wykonuje dokładnie tyle samo operacji co klasyczne LU (około $n^3 slash 3$ mnożeń i dodawań), a dla $n = 2500$ jest od niego o 22% szybszy.
+ Strassen wykonuje dla $n = 2500$ 72,6% mnożeń i 75,2% dodawań wersji klasycznej. Jest najszybszą metodą: 1607 ms wobec 2529 ms dla klasycznego LU. Laderman (76,0% mnożeń, 80,6% dodawań) daje 1756 ms, czyli jest między Strassenem a Binetem.
+ Wersje wielowątkowe (`ForkJoinPool`) dają przyspieszenie 4,6–5,0 razy dla $n = 2500$.
+ Strassen potrzebuje dodatkowych buforów, więc przy domyślnej stercie 8,5 GB mieści macierze do $n approx 11800$, a Binet do $n approx 21300$ (Laderman do $n approx 11560$).
+ Wyniki zgadzają się z referencją z dokładnością do błędu zaokrągleń (około $epsilon$ dla macierzy $4 times 4$).
+ Faktoryzacja nie używa pivotowania, więc wymaga macierzy z niezerowymi minorami głównymi. Na zwykłych macierzach losowych jest niestabilna (@sec-macierze), dlatego testowaliśmy macierze diagonalnie dominujące.
