# Faster Computations with Generative expressions

Presentation material for the ClojureD 2019 conference and the talk "Faster Computations with Generative Expressions".

https://clojured.de/

[Here](latex/top.pdf) are all the slides in PDF format.

## Usage

Type setting code examples with syntax high-lighting etc, for inclusion in Latex:
```
lein run
```

Creating some empty folders:
```
./initialize.sh
```

Building the C++ code used in the benchmark:
```
cd cpp
make circleopt
```

Performing the benchmarks:
Load the ```cljd.circle``` namespace. At the bottom of the function, there
are three lines:
```
;; (generate-problem)
;; (benjmark/clear-results project)
;; (benjmark/run-benchmark project)
```
Evaluate each one of them.

Generating plots:
```
cd plots
python3 benchmark_circle.py
```

Generating illustrations:
```
cd plots
python3 circle.py
```

## Requirements

For the Clojure benchmarks:

  * Clojure and Leiningen

For the C++ benchmark:
  * g++
  * Make

For rendering the plots
  * Python3
  * Matplotlib

## License

Copyright © 2019 Jonas Östlund

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
