(ns cljd.square
  ;; Extra decl needed here.
  (:require [geex.java :as java]
            [geex.common :as c]
            [exampler.utils :as exutils]
            [pres.utils :as utils :refer [macroexpand]])
  (:refer-clojure :exclude [macroexpand]))

[:begin-example "square"]
(defn square [x]
  (* x x))
[:end-example]

[:begin-example "squarethree"]
(square 3)
[:end-example]

[:begin-example "squarefive"]
(square 5)
[:end-example]

[:begin-example "gsquare"]
(require '[geex.java :as java]
         '[geex.common :as c])

(java/typed-defn geex-square [Double/TYPE x]
   (c/* x x))
[:end-example]

[:begin-example "gsquareexpand" {:output {:format :clojure}}]
(macroexpand
 '(java/typed-defn
   geex-square [Double/TYPE x]
   (c/* x x)))
[:end-example]


[:begin-example "gsquarethree"]
(geex-square 3)
[:end-example]

[:begin-example "gsquarefive"]
(geex-square 5)
[:end-example]

[:begin-example "squareiocall"]
(square 3)
[:end-example]

[:begin-example "gsquareio" {:output {:transform (exutils/line-trimmer 70)}}]
(java/typed-defn geex-square [Double/TYPE x]
  (println "Input:" x)
  (let [output (c/* x x)]
    (println "Output:" output)
    output))
[:end-example]


[:begin-example "gsquaredisp" {:output {:format :java}}]
(require '[geex.core :as geex])

(java/typed-defn geex-square [Double/TYPE x]
  (geex/set-flag! :disp) ;; $\leftarrow$ display generated code
  (c/* x x))
[:end-example]


[:begin-example "gsquareiocall"]
(geex-square 3)
[:end-example]
