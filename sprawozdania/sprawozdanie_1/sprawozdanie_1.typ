// Sprawozdanie — Algorytmy macierzowe, zadanie 1 (jeden plik, do typst.app)
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
  tytul: [Rekurencyjne mnożenie macierzy],
  podtytul: [Metody Bineta, Strassena i Ladermana — sprawozdanie z zadania 1],
  przedmiot: [Algorytmy macierzowe],
  autorzy: ("Piotr Szamocki", "Mateusz Stoch"),
  data: [Kraków, październik 2026],
)

// ---------------------------------------------------------------------
// 01-wstep.typ
// ---------------------------------------------------------------------

= Cel zadania

Zaimplementowaliśmy trzy rekurencyjne algorytmy mnożenia macierzy:

+ metodę Bineta (bloki $2 times 2$, 8 mnożeń bloków),
+ metodę Strassena (bloki $2 times 2$, 7 mnożeń bloków),
+ metodę Ladermana (bloki $3 times 3$, 23 mnożenia bloków).

Wymagania:

- macierze kwadratowe o dowolnym rozmiarze,
- elementy losowe z przedziału $(10^(-8), 1)$,
- zliczanie operacji zmiennoprzecinkowych ($+ - * slash$).

Każdy algorytm ma wersję jednowątkową i wielowątkową. Obie wersje wykonują te same operacje.

// ---------------------------------------------------------------------
// 02-srodowisko.typ
// ---------------------------------------------------------------------

= Język i środowisko <sec-srodowisko>

== Dlaczego Java nie jest językiem interpretowanym

Kompilator `javac` tłumaczy kod źródłowy na kod bajtowy. Podczas działania maszyna wirtualna (HotSpot) kompiluje często używane fragmenty do kodu maszynowego (kompilacja JIT). Pętle mnożenia macierzy wykonują się więc jako kod maszynowy.

Konsekwencja: pierwsze wywołania są wolniejsze, bo kod nie jest jeszcze skompilowany. Dlatego powtarzamy pomiary i uśredniamy wyniki (@sec-metodyka).

== Sprzęt

#figure(
  caption: [Konfiguracja pomiarowa],
  table(
    columns: (auto, 1fr),
    align: (left, left),
    [Procesor], [AMD Ryzen 7 7700 8-Core Processor (3.80 GHz)],
    [Pamięć RAM], [32,0 GB, DDR5, 6000MT/s],
    [System operacyjny], [Windows 11],
    [JDK], [OpenJDK 25],
  ),
) <tab-sprzet>

// ---------------------------------------------------------------------
// 03-implementacja.typ
// ---------------------------------------------------------------------

= Implementacja <sec-implementacja>

== Macierz i widoki <sec-widoki>

Dane macierzy mają jednego właściciela: jedną tablicę 1D. Elementy leżą w niej wiersz po wierszu.

Algorytmy nie używają tej tablicy bezpośrednio. Używają *widoków*. Widok to prostokątny fragment tablicy. Opisują go cztery liczby:

- `offset` — indeks pierwszego elementu,
- `rows`, `cols` — wymiary,
- `stride` — przeskok między kolejnymi wierszami.

Element $(i, j)$ widoku ma indeks
$ "offset" + i dot "stride" + j. $

Widok na podblok jest nowym widokiem na tę samą tablicę. Zmienia się tylko `offset` i wymiary. `stride` się nie zmienia (@fig-widok).

#let komorka(i, zaznaczona, rozmiar) = box(
  width: rozmiar,
  height: rozmiar,
  fill: if zaznaczona { akcent.lighten(70%) } else { white },
  stroke: 0.4pt + luma(80),
  align(center + horizon, text(6.5pt, str(i))),
)
#let w-bloku(i) = {
  let (r, c) = (calc.quo(i, 6), calc.rem(i, 6))
  r >= 1 and r <= 3 and c >= 2 and c <= 4
}

#figure(
  kind: image,
  caption: [Macierz $6 times 6$ w tablicy 1D i widok na blok $3 times 3$ od pozycji $(1, 2)$:
    $"offset" = 8$, $"stride" = 6$.],
  {
    grid(
      columns: 6,
      ..range(36).map(i => komorka(i, w-bloku(i), 0.55cm)),
    )
    v(0.5em)
    text(9pt)[tablica 1D (indeksy 0–35):]
    v(0.2em)
    grid(
      columns: 36,
      ..range(36).map(i => komorka(i, w-bloku(i), 0.42cm)),
    )
  },
) <fig-widok>

Zalety:

- *Tani podział.* Podział na bloki nie kopiuje danych. Tworzy tylko nowe widoki.
- *Mało alokacji.* Wynik podproblemu trafia przez widok od razu do macierzy $C$.
- *Dobra lokalność.* Wiersz widoku jest ciągłym fragmentem pamięci.
- *Prosty kod.* Bloki $2 times 2$ i $3 times 3$ tworzy się jednym wywołaniem.

== Zliczanie operacji <sec-liczenie>

Program zlicza operacje podczas działania. Każda operacja podstawowa dodaje do licznika wartość z @tab-liczniki. Odejmowanie liczy się jako dodawanie.

#figure(
  caption: [Operacje podstawowe i ich koszt ($A$: $m times k$, $B$: $k times n$; przy dodawaniu macierze mają wymiar $p times q$)],
  table(
    columns: (auto, auto, auto),
    align: (left, center, center),
    table.header([*Działanie*], [*Mnożenia*], [*Dodawania*]),
    [$C := A plus.minus B$], [$0$], [$p q$],
    [$C := A B$ (klasycznie)], [$m k n$], [$m (k-1) n$],
    [$C := C + A B$ (klasycznie)], [$m k n$], [$m k n$],
  ),
) <tab-liczniki>

== Wersja jedno- i wielowątkowa

Każdy algorytm ma dwie wersje:

- *jednowątkową* — podproblemy wykonują się po kolei,
- *wielowątkową* — używa `ForkJoinPool` z liczbą wątków równą liczbie wątków procesora. Wszystkie podproblemy oprócz ostatniego trafiają do puli. Ostatni podproblem wykonuje bieżący wątek. Potem wątek czeka na pozostałe.

== Podział macierzy <sec-podzial>

- *Binet* dzieli każdy wymiar na $floor(d slash 2)$ i $ceil(d slash 2)$.
- *Strassen* dzieli wymiary na połowy. Nieparzysty wymiar → peeling (@alg-peel).
- *Laderman* dzieli wymiary na trzy części. Niepodzielny wymiar → peeling (@alg-peel).

== Próg rekursji

Rekursja kończy się, gdy największy wymiar bloku jest $<= n_0$ (parametr `leaf`). Taki blok mnożymy algorytmem klasycznym. Dla małych bloków koszt rekursji (widoki, zadania, bufory) jest większy niż zysk z mniejszej liczby mnożeń. Pomiary wykonano dla $n_0 = 240$.

#figure(
image("img_1/binet_threads_wykres_leaf_czas.png"), caption: [])

== Weryfikacja poprawności

Dla każdego rozmiaru program liczy iloczyn referencyjny algorytmem klasycznym. Wynik każdego wariantu porównuje z referencją element po elemencie. Tolerancja wynosi $10^(-9)$.

// ---------------------------------------------------------------------
// 04-algorytmy.typ
// ---------------------------------------------------------------------

= Pseudokody <sec-algorytmy>

#let kom(t) = text(fill: luma(110), size: 0.92em)[▷ #t]

#uwaga(tytul: [Notacja])[Bloki to widoki na macierz. Zapis do $C_(T L)$ trafia od razu do $C$. W bloku #kw[równolegle] każdy wiersz to osobne zadanie w wersji wielowątkowej. W wersji jednowątkowej wiersze wykonują się po kolei.]

== Metoda Bineta

#algorytm(
  [$[C] := "Binet"(A, B, C)$ #h(1fr) #kom[oblicza $C := C + A B$; na starcie $C = 0$]],
  [Metoda Bineta],
)[
  #w[#kw[jeśli] $max(m, k, n) <= n_0$ #kw[to]]
  #w(p: 1)[$C := C + A B$ #h(1fr) #kom[klasycznie]]
  #w(p: 1)[#kw[zwróć]]
  #w[#kw[koniec jeśli]]
  #w[
    $A -> mat(A_(T L), A_(T R); A_(B L), A_(B R); augment: #(hline: 1, vline: 1)),
    B -> mat(B_(T L), B_(T R); B_(B L), B_(B R); augment: #(hline: 1, vline: 1)),
    C -> mat(C_(T L), C_(T R); C_(B L), C_(B R); augment: #(hline: 1, vline: 1))$
  ]
  #w(p: 1)[#kw[gdzie] bloki mają połowę wymiarów (zaokrąglenie w dół dla $T L$)]
  #kreska()
  #w(p: 1)[#kw[równolegle]]
  #w(p: 2)[$C_(T L) := C_(T L) + A_(T L) B_(T L)$; #h(0.6em) $C_(T L) := C_(T L) + A_(T R) B_(B L)$]
  #w(p: 2)[$C_(T R) := C_(T R) + A_(T L) B_(T R)$; #h(0.6em) $C_(T R) := C_(T R) + A_(T R) B_(B R)$]
  #w(p: 2)[$C_(B L) := C_(B L) + A_(B L) B_(T L)$; #h(0.6em) $C_(B L) := C_(B L) + A_(B R) B_(B L)$]
  #w(p: 2)[$C_(B R) := C_(B R) + A_(B L) B_(T R)$; #h(0.6em) $C_(B R) := C_(B R) + A_(B R) B_(B R)$]
  #w(p: 1)[#kw[koniec równolegle] #h(1fr) #kom[każdy iloczyn: wywołanie rekurencyjne Binet]]
] <alg-binet>

== Metoda Strassena

#algorytm(
  [$[C] := "Strassen"(A, B)$ #h(1fr) #kom[oblicza $C := A B$]],
  [Metoda Strassena],
)[
  #w[#kw[jeśli] $max(m, k, n) <= n_0$ #kw[lub] $min(m, k, n) < 2$ #kw[to]]
  #w(p: 1)[$C := A B$ #h(1fr) #kom[klasycznie]]
  #w(p: 1)[#kw[zwróć]]
  #w[#kw[koniec jeśli]]
  #w[#kw[jeśli] któryś wymiar nie dzieli się przez 2 #kw[to]]
  #w(p: 1)[$C := "Peeling"(A, B, 2, "Strassen")$ #h(1fr) #kom[@alg-peel]]
  #w(p: 1)[#kw[zwróć]]
  #w[#kw[koniec jeśli]]
  #w[
    $A -> mat(A_(T L), A_(T R); A_(B L), A_(B R); augment: #(hline: 1, vline: 1)),
    B -> mat(B_(T L), B_(T R); B_(B L), B_(B R); augment: #(hline: 1, vline: 1)),
    C -> mat(C_(T L), C_(T R); C_(B L), C_(B R); augment: #(hline: 1, vline: 1))$
  ]
  #w(p: 1)[#kw[gdzie] bloki mają połowę wymiarów]
  #kreska()
  #w(p: 1)[#kw[równolegle]]
  #w(p: 2)[$M_1 := "Strassen"(A_(T L) + A_(B R), thin B_(T L) + B_(B R))$]
  #w(p: 2)[$M_2 := "Strassen"(A_(B L) + A_(B R), thin B_(T L))$]
  #w(p: 2)[$M_3 := "Strassen"(A_(T L), thin B_(T R) - B_(B R))$]
  #w(p: 2)[$M_4 := "Strassen"(A_(B R), thin B_(B L) - B_(T L))$]
  #w(p: 2)[$M_5 := "Strassen"(A_(T L) + A_(T R), thin B_(B R))$]
  #w(p: 2)[$M_6 := "Strassen"(A_(B L) - A_(T L), thin B_(T L) + B_(T R))$]
  #w(p: 2)[$M_7 := "Strassen"(A_(T R) - A_(B R), thin B_(B L) + B_(B R))$]
  #w(p: 1)[#kw[koniec równolegle]]
  #kreska()
  #w(p: 1)[#kw[równolegle]]
  #w(p: 2)[$C_(T L) := M_1 + M_4 - M_5 + M_7$]
  #w(p: 2)[$C_(T R) := M_3 + M_5$]
  #w(p: 2)[$C_(B L) := M_2 + M_4$]
  #w(p: 2)[$C_(B R) := M_1 - M_2 + M_3 + M_6$]
  #w(p: 1)[#kw[koniec równolegle]]
] <alg-strassen>

Jeden poziom: 7 mnożeń i 18 dodawań bloków.
== Metoda Ladermana

Bloki $3 times 3$ numerujemy od 1 do 3, jak w oryginalnym schemacie.

#algorytm(
  [$[C] := "Laderman"(A, B)$ #h(1fr) #kom[oblicza $C := A B$]],
  [Metoda Ladermana],
)[
  #w[#kw[jeśli] $max(m, k, n) <= n_0$ #kw[lub] $min(m, k, n) < 3$ #kw[to]]
  #w(p: 1)[$C := A B$ #h(1fr) #kom[klasycznie]]
  #w(p: 1)[#kw[zwróć]]
  #w[#kw[koniec jeśli]]
  #w[#kw[jeśli] któryś wymiar nie dzieli się przez 3 #kw[to]]
  #w(p: 1)[$C := "Peeling"(A, B, 3, "Laderman")$ #h(1fr) #kom[@alg-peel]]
  #w(p: 1)[#kw[zwróć]]
  #w[#kw[koniec jeśli]]
  #w[
    $A -> mat(A_(11), A_(12), A_(13); A_(21), A_(22), A_(23); A_(31), A_(32), A_(33); augment: #(hline: (1, 2), vline: (1, 2))),
    B -> mat(B_(11), B_(12), B_(13); B_(21), B_(22), B_(23); B_(31), B_(32), B_(33); augment: #(hline: (1, 2), vline: (1, 2))),
    C -> mat(C_(11), C_(12), C_(13); C_(21), C_(22), C_(23); C_(31), C_(32), C_(33); augment: #(hline: (1, 2), vline: (1, 2)))$
  ]
  #w(p: 1)[#kw[gdzie] bloki mają jedną trzecią wymiarów]
  #kreska()
  #w(p: 1)[#kw[równolegle] #h(1fr) #kom[każdy iloczyn: wywołanie rekurencyjne Laderman]]
  #pad(left: 2 * 1.4em, grid(
    columns: 1,
    row-gutter: 0.62em,
      [$M_(1) := (A_(11) + A_(12) + A_(13) - A_(21) - A_(22) - A_(32) - A_(33)) B_(22)$],
      [$M_(2) := (A_(11) - A_(21)) (- B_(12) + B_(22))$],
      [$M_(3) := A_(22) (- B_(11) + B_(12) + B_(21) - B_(22) - B_(23) - B_(31) + B_(33))$],
      [$M_(4) := (- A_(11) + A_(21) + A_(22)) (B_(11) - B_(12) + B_(22))$],
      [$M_(5) := (A_(21) + A_(22)) (- B_(11) + B_(12))$],
      [$M_(6) := A_(11) B_(11)$],
      [$M_(7) := (- A_(11) + A_(31) + A_(32)) (B_(11) - B_(13) + B_(23))$],
      [$M_(8) := (- A_(11) + A_(31)) (B_(13) - B_(23))$],
      [$M_(9) := (A_(31) + A_(32)) (- B_(11) + B_(13))$],
      [$M_(10) := (A_(11) + A_(12) + A_(13) - A_(22) - A_(23) - A_(31) - A_(32)) B_(23)$],
      [$M_(11) := A_(32) (- B_(11) + B_(13) + B_(21) - B_(22) - B_(23) - B_(31) + B_(32))$],
      [$M_(12) := (- A_(13) + A_(32) + A_(33)) (B_(22) + B_(31) - B_(32))$],
      [$M_(13) := (A_(13) - A_(33)) (B_(22) - B_(32))$],
      [$M_(14) := A_(13) B_(31)$],
      [$M_(15) := (A_(32) + A_(33)) (- B_(31) + B_(32))$],
      [$M_(16) := (- A_(13) + A_(22) + A_(23)) (B_(23) + B_(31) - B_(33))$],
      [$M_(17) := (A_(13) - A_(23)) (B_(23) - B_(33))$],
      [$M_(18) := (A_(22) + A_(23)) (- B_(31) + B_(33))$],
      [$M_(19) := A_(12) B_(21)$],
      [$M_(20) := A_(23) B_(32)$],
      [$M_(21) := A_(21) B_(13)$],
      [$M_(22) := A_(31) B_(12)$],
      [$M_(23) := A_(33) B_(33)$],
  ))
  #w(p: 1)[#kw[koniec równolegle]]
  #kreska()
  #w(p: 1)[#kw[równolegle]]
  #w(p: 2)[$C_(11) := M_(6) + M_(14) + M_(19)$]
  #w(p: 2)[$C_(12) := M_(1) + M_(4) + M_(5) + M_(6) + M_(12) + M_(14) + M_(15)$]
  #w(p: 2)[$C_(13) := M_(6) + M_(7) + M_(9) + M_(10) + M_(14) + M_(16) + M_(18)$]
  #w(p: 2)[$C_(21) := M_(2) + M_(3) + M_(4) + M_(6) + M_(14) + M_(16) + M_(17)$]
  #w(p: 2)[$C_(22) := M_(2) + M_(4) + M_(5) + M_(6) + M_(20)$]
  #w(p: 2)[$C_(23) := M_(14) + M_(16) + M_(17) + M_(18) + M_(21)$]
  #w(p: 2)[$C_(31) := M_(6) + M_(7) + M_(8) + M_(11) + M_(12) + M_(13) + M_(14)$]
  #w(p: 2)[$C_(32) := M_(12) + M_(13) + M_(14) + M_(15) + M_(22)$]
  #w(p: 2)[$C_(33) := M_(6) + M_(7) + M_(8) + M_(9) + M_(23)$]
  #w(p: 1)[#kw[koniec równolegle]]
] <alg-laderman>

Jeden poziom: 23 mnożenia i 98 dodawań bloków (56 przy tworzeniu argumentów, 42 przy składaniu $C$).

=== Peeling

Peeling jest wspólny dla obu metod. Parametr $q$ to liczba części podziału: $q = 2$ dla Strassena, $q = 3$ dla Ladermana. Gdy wymiar nie dzieli się przez $q$, odcinamy ostatnie wiersze (kolumny): najwyżej $q - 1$. Część główna idzie do rekursji danej metody. Paski brzegowe liczymy klasycznie.

#algorytm(
  [$[C] := "Peeling"(A, B, q, "Metoda")$ #h(1fr) #kom[oblicza $C := A B$]],
  [Peeling dla wymiarów niepodzielnych przez $q$ (Strassen: $q = 2$, Laderman: $q = 3$)],
)[
  #w[$m' := m - (m mod q), quad k' := k - (k mod q), quad n' := n - (n mod q)$]
  #w[
    $A -> mat(A_(T L), A_(T R); A_(B L), A_(B R); augment: #(hline: 1, vline: 1)),
    B -> mat(B_(T L), B_(T R); B_(B L), B_(B R); augment: #(hline: 1, vline: 1)),
    C -> mat(C_(T L), C_(T R); C_(B L), C_(B R); augment: #(hline: 1, vline: 1))$
  ]
  #w(p: 1)[#kw[gdzie] $A_(T L)$ jest $m' times k'$, $B_(T L)$ jest $k' times n'$, $C_(T L)$ jest $m' times n'$]
  #kreska()
  #w(p: 1)[#kw[równolegle]]
  #w(p: 2)[$C_(T L) := "Metoda"(A_(T L), B_(T L))$; #h(0.6em) $C_(T L) := C_(T L) + A_(T R) B_(B L)$]
  #w(p: 2)[$mat(C_(T R); C_(B R); augment: #(hline: 1)) := A mat(B_(T R); B_(B R); augment: #(hline: 1))$]
  #w(p: 2)[$C_(B L) := mat(A_(B L), A_(B R); augment: #(vline: 1)) mat(B_(T L); B_(B L); augment: #(hline: 1))$]
  #w(p: 1)[#kw[koniec równolegle] #h(1fr) #kom[paski: klasycznie]]
] <alg-peel>

// ---------------------------------------------------------------------
// 05-fragmenty-kodu.typ
// ---------------------------------------------------------------------

= Najważniejsze fragmenty kodu <sec-kod>

#show raw.where(block: true): set text(size: 7.6pt)
#show raw.where(block: true): it => block(width: 100%, fill: luma(246), inset: 7pt, radius: 2pt, it)

== Widok na tablicę

Indeks elementu i tworzenie widoku na podblok. Podblok dostaje tę samą tablicę i ten sam `stride`.

```java
int idx(int i, int j) { return offset + i * stride + j; }

public MatrixView<A> block(int row0, int col0, int height, int width) {
    if (row0 < 0 || col0 < 0 || height < 0 || width < 0
            || row0 + height > rows || col0 + width > cols)
        throw new IndexOutOfBoundsException("Blok poza macierzą");
    return new MatrixView<>(data, idx(row0, col0), height, width, stride);
}
```

== Zliczanie operacji

Liczniki rosną o wartość wyliczoną z wymiarów, raz na wywołanie.

```java
// c = a * b (c nie może nachodzić na a ani b)
public final void multiply(MatrixView<A> a, MatrixView<A> b, MatrixView<A> c) {
    checkMul(a, b, c);
    zeroImpl(c);
    multiplyAddImpl(a, b, c);
    muls.add((long) a.rows * a.cols * b.cols);
    adds.add((long) a.rows * (a.cols - 1) * b.cols);  // k-1 dodawań na element
}

// c += a * b (c nie może nachodzić na a ani b)
public final void multiplyAdd(MatrixView<A> a, MatrixView<A> b, MatrixView<A> c) {
    checkMul(a, b, c);
    multiplyAddImpl(a, b, c);
    long n = (long) a.rows * a.cols * b.cols;
    muls.add(n);
    adds.add(n);
}
```

== Mnożenie klasyczne (liście rekursji)

Kolejność pętli $i$–$k$–$j$. Pętla wewnętrzna idzie po ciągłym wierszu $B$ i $C$.

```java
protected void multiplyAddImpl(MatrixView<double[]> a, MatrixView<double[]> b, MatrixView<double[]> c) {
    for (int i = 0; i < a.rows; i++)
        for (int k = 0; k < a.cols; k++) {
            double aik = a.data[a.idx(i, k)];
            int cBase = c.idx(i, 0), bBase = b.idx(k, 0);
            for (int j = 0; j < b.cols; j++)
                c.data[cBase + j] += aik * b.data[bBase + j];
        }
}
```

== Binet: krok rekurencyjny

Podział na bloki to tylko widoki. Cztery zadania liczą cztery bloki $C$. Trzy trafiają do puli, czwarte liczy bieżący wątek.

```java
int m1 = m / 2, k1 = k / 2, n1 = n / 2;
int m2 = m - m1, k2 = k - k1, n2 = n - n1;

var a11 = a.block(0, 0, m1, k1);   var a12 = a.block(0, k1, m1, k2);
var a21 = a.block(m1, 0, m2, k1);  var a22 = a.block(m1, k1, m2, k2);

var b11 = b.block(0, 0, k1, n1);   var b12 = b.block(0, n1, k1, n2);
var b21 = b.block(k1, 0, k2, n1);  var b22 = b.block(k1, n1, k2, n2);

var c11 = c.block(0, 0, m1, n1);   var c12 = c.block(0, n1, m1, n2);
var c21 = c.block(m1, 0, m2, n1);  var c22 = c.block(m1, n1, m2, n2);

Runnable t11 = () -> { binetMultiply(ops, a11, b11, c11, leaf, pool, accumulate); binetMultiplyAdd(ops, a12, b21, c11, leaf, pool); };
// ... t12, t21, t22 analogicznie

if (pool != null) {
    var f11 = ForkJoinTask.adapt(t11).fork();
    var f12 = ForkJoinTask.adapt(t12).fork();
    var f21 = ForkJoinTask.adapt(t21).fork();
    t22.run();
    f21.join();
    f12.join();
    f11.join();
} else {
    t11.run();
    t12.run();
    t21.run();
    t22.run();
}
```

== Strassen: bufory, zadanie i składanie wyniku

$M_1, dots, M_7$ to widoki jednego bufora. Sumy argumentów też leżą w jednym buforze. Dzięki temu na węzeł są tylko trzy alokacje.

```java
// Bufory na wyniki M1–M7 (7 bloków jako widoki jednego bufora)
var mBuf = ops.newMatrix(7 * m2, n2).view();
var m1r = mBuf.block(0 * m2, 0, m2, n2);
var m2r = mBuf.block(1 * m2, 0, m2, n2);
var m3r = mBuf.block(2 * m2, 0, m2, n2);
var m4r = mBuf.block(3 * m2, 0, m2, n2);
var m5r = mBuf.block(4 * m2, 0, m2, n2);
var m6r = mBuf.block(5 * m2, 0, m2, n2);
var m7r = mBuf.block(6 * m2, 0, m2, n2);

// Arena na tymczasowe sumy: po jednym lA (5) i lB (5) na zadanie
var tA = ops.newMatrix(5 * m2, k2).view();
var tB = ops.newMatrix(5 * k2, n2).view();

// M1 = strassen(A11 + A22, B11 + B22)
Runnable t1 = () -> {
    var lA = tA.block(0 * m2, 0, m2, k2);
    var lB = tB.block(0 * k2, 0, k2, n2);
    ops.add(a11, a22, lA);
    ops.add(b11, b22, lB);
    strassenMultiply(ops, lA, lB, m1r, leaf, pool);
};
// ... t2–t7 analogicznie, potem fork/join jak w metodzie Bineta

// Składanie wyniku z M1–M7
Runnable s11 = () -> {
    // C11 = M1 + M4 - M5 + M7
    ops.add(m1r, m4r, c11);
    ops.subtract(c11, m5r, c11);
    ops.add(c11, m7r, c11);
};
// ... C12, C21, C22 analogicznie
```

== Peeling (Strassen)

Część parzysta idzie do rekursji. Paski brzegowe liczymy klasycznie, równolegle z częścią główną. Laderman ma ten sam kod z `% 3`.

```java
int m1 = m - m % 2;
int k1 = k - k % 2;
int n1 = n - n % 2;

if (m1 != m || k1 != k || n1 != n) {
    if (pool != null) {
        ForkJoinTask<?> fRight = null;
        ForkJoinTask<?> fBottom = null;

        if (n1 < n) {
            fRight = ForkJoinTask.adapt(() -> {
                ops.multiply(a, b.block(0, n1, k, n - n1), c.block(0, n1, m, n - n1));
            }).fork();
        }

        if (m1 < m) {
            fBottom = ForkJoinTask.adapt(() -> {
                ops.multiply(a.block(m1, 0, m - m1, k), b.block(0, 0, k, n1), c.block(m1, 0, m - m1, n1));
            }).fork();
        }

        strassenMultiply(ops, a.block(0, 0, m1, k1), b.block(0, 0, k1, n1), c.block(0, 0, m1, n1), leaf, pool);

        if (k1 < k) {
            ops.multiplyAdd(a.block(0, k1, m1, k - k1), b.block(k1, 0, k - k1, n1), c.block(0, 0, m1, n1));
        }

        if (fBottom != null) fBottom.join();
        if (fRight != null) fRight.join();
    } else {
        // ... to samo sekwencyjnie
    }
    return;
}
```

// ---------------------------------------------------------------------
// 06-zlozonosc.typ
// ---------------------------------------------------------------------

= Złożoność obliczeniowa <sec-zlozonosc>

Oznaczenia dla macierzy $n times n$:

- $M(n)$ — liczba mnożeń,
- $D(n)$ — liczba dodawań (z odejmowaniami).

Algorytm klasyczny: $M(n) = n^3$, $D(n) = n^3 - n^2$.

== Binet

Rekursja zmienia tylko kolejność sumowania. Każdy iloczyn $a_(i l) b_(l j)$ liczymy raz. Dlatego:
$ M(n) = n^3, quad D(n) = n^3 - n^2. $
To samo co w algorytmie klasycznym, dla każdego $n$ i $n_0$.

*Złożoność obliczeniowa: $O(n^3)$.*

== Strassen

Jeden poziom: 7 wywołań na blokach $n slash 2$ i 18 dodawań bloków:
$ M(n) = 7 M(n/2), quad D(n) = 7 D(n/2) + 18 (n/2)^2. $
Dla $n = 2^k$ i rekursji do bloków $1 times 1$:
$ M(n) = 7^k = n^(log_2 7), quad D(n) = 6 (n^(log_2 7) - n^2). $

*Złożoność obliczeniowa: $O(n^(log_2 7)) approx O(n^(2.81))$.*

== Laderman

Jeden poziom: 23 wywołania na blokach $n slash 3$ i 98 dodawań bloków:
$ M(n) = 23 M(n/3), quad D(n) = 23 D(n/3) + 98 (n/3)^2. $
Dla $n = 3^k$ i rekursji do bloków $1 times 1$:
$ M(n) = 23^k = n^(log_3 23), quad D(n) = 7 (n^(log_3 23) - n^2). $

*Złożoność obliczeniowa: $O(n^(log_3 23)) approx O(n^(2.85))$.*

== Wpływ peelingu i progu $n_0$

W każdym węźle rekursji Strassen i Laderman i tak dodają bloki. Koszt tych dodawań jest rzędu $n^2$ (Strassen: $18 (n slash 2)^2$, Laderman: $98 (n slash 3)^2$).

Peeling dodaje w węźle mnożenie pasków o szerokości 1 lub 2. Pasek ma około $n$ wierszy i $n$ kolumn, więc jego koszt też jest rzędu $n^2$. Rekurencja ma więc tę samą postać co bez peelingu:
$ T(n) = 7 T(n/2) + c n^2 quad "(Strassen)", quad quad T(n) = 23 T(n/3) + c n^2 quad "(Laderman)". $
Peeling zwiększa tylko stałą $c$.

O złożoności decyduje liczba wywołań rekurencyjnych (7 lub 23), a nie praca $c n^2$ w węźle. Wykładniki $log_2 7 approx 2.81$ i $log_3 23 approx 2.85$ są większe od 2, więc składnik $c n^2$ nie zmienia rzędu. Z peelingiem złożoność to nadal $O(n^(log_2 7))$ i $O(n^(log_3 23))$.

Próg $n_0$ działa podobnie. Rekursja kończy się na blokach $n_0 times n_0$, które liczymy klasycznie. Zmieniają się stałe, a nie złożoność.

// ---------------------------------------------------------------------
// 07-wyniki.typ
// ---------------------------------------------------------------------

= Wyniki pomiarów <sec-wyniki>

== Metodyka <sec-metodyka>

- Rozmiary: $n = 10, 20, 30, dots, 2500$ (krok 10).
- Wartości elementów: losowe z $(10^(-8), 1)$.
- Próg rekursji: $n_0 = 240$.
- Każdy wariant: czas, liczba mnożeń, liczba dodawań.
- 3 przebiegi, czasy uśrednione.

== Czas działania

#figure(
  image("img_1/wykres_czas.png"),
  caption: [Czas mnożenia macierzy $n times n$],
) <fig-czas>

== Liczba mnożeń

#figure(
  image("img_1/wykres_mnozenia.png"),
  caption: [Liczba mnożeń zmiennoprzecinkowych],
) <fig-mnozenia>

== Liczba dodawań

#figure(
  image("img_1/wykres_dodawania.png"),
  caption: [Liczba dodawań zmiennoprzecinkowych],
) <fig-dodawania>

// ---------------------------------------------------------------------
// 08-octave.typ
// ---------------------------------------------------------------------
#pagebreak()
= Porównanie z Octave <sec-octave>

Procedura:

+ Wylosowaliśmy $A$, $B$ o rozmiarze $4 times 4$ z przedziału $(10^(-8), 1)$.
+ Pomnożyliśmy je każdą metodą z $n_0 = 1$, w wersji jedno- i wielowątkowej. Przy dużym $n_0$ rekursja by się nie wykonała.
+ Wypisaliśmy macierze z pełną precyzją (`%.17g`) i porównaliśmy wyniki z `A * B` w Octave 8.4.0:

```matlab
A = load('A.txt');  B = load('B.txt');  C_java = load('C_java.txt');
C = A * B;
max_blad  = max(abs(C(:) - C_java(:)))
wzgl_blad = norm(C - C_java, 'fro') / norm(C, 'fro')
```

Dane i wynik (zaokrąglone do 4 miejsc):
$ A = mat(0.6186, 0.8492, 0.7904, 0.5296; 0.8417, 0.1483, 0.8438, 0.5848; 0.1667, 0.7158, 0.5393, 0.2795; 0.0557, 0.0131, 0.3578, 0.7500), quad
  B = mat(0.5786, 0.2522, 0.5377, 0.5431; 0.0891, 0.2529, 0.2611, 0.3720; 0.8833, 0.9669, 0.1329, 0.0050; 0.8571, 0.8844, 0.5446, 0.5092) $
$ C = A B = mat(1.5857, 1.6033, 0.9478, 0.9255; 1.7467, 1.5828, 0.9219, 0.8143; 0.8762, 0.9917, 0.5004, 0.5018; 0.9922, 1.0266, 0.4893, 0.4188) $

Wszystkie metody dają tę samą macierz $C$ z dokładnością do 4 miejsc. Różnice w pełnej precyzji są w @tab-octave[tabeli].

#figure(
  caption: [Porównanie z Octave, macierz $4 times 4$, $n_0 = 1$ ($epsilon approx 2.2 dot 10^(-16)$)],
  table(
    columns: (auto, auto, 1fr, 1fr),
    align: (left, center, center, center),
    table.header([*Metoda*], [*Mnożenia / dodawania*], [*Maks. błąd bezwzględny*], [*Błąd względny*]),
    [Binet], [64 / 48], [$0$], [$0$],
    [Strassen], [49 / 198], [$8.9 dot 10^(-16)$], [$3.2 dot 10^(-16)$],
    [Laderman], [60 / 128], [$2.2 dot 10^(-16)$], [$1.1 dot 10^(-16)$],
  ),
) <tab-octave>

Wersje jedno- i wielowątkowe dały identyczne wyniki.

Binet zgadza się z Octave co do bitu. Strassen i Laderman różnią się o kilka ostatnich bitów (najwyżej 6 jednostek ostatniego miejsca). Powód: wykonują więcej dodawań i odejmowań, więc błędy zaokrągleń są inne. Błąd względny jest rzędu $epsilon$. Wyniki są poprawne.

// ---------------------------------------------------------------------
// 09-wnioski.typ
// ---------------------------------------------------------------------
#pagebreak()
= Wnioski

+ Binet wykonuje tyle samo operacji co algorytm klasyczny. Złożoność: $O(n^3)$.
+ Strassen i Laderman wykonują mniej mnożeń bloków (7 zamiast 8, 23 zamiast 27). Złożoność: $O(n^(2.81))$ i $O(n^(2.85))$. Kosztem jest więcej dodawań i dodatkowe bufory.
+ Peeling pozwala mnożyć macierze o dowolnym rozmiarze. Nie zmienia złożoności.
+ Widoki na tablicę 1D dają tani podział na bloki bez kopiowania danych.
+ Wyniki wszystkich metod zgadzają się z Octave z dokładnością do błędu zaokrągleń.
+ Algorytm Strassena okazał się najszybszą metodą spośród wszystkich testowanych.
+ Zrównoleglenie obliczeń za pomocą ForkJoinPool przyniosło stabilne, ponad 6-krotne przyspieszenie (średnio ~6,3x dla Strassena i ~7,2x dla Bineta) względem wersji jednowątkowej tego samego algorytmu.

