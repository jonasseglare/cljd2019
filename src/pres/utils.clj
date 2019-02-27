(ns pres.utils
  (:require [clojure.core :as c]
                                        ;[zprint.core :as zp]
            [clojure.pprint :as pp]
            )
  (:refer-clojure :exclude [macroexpand]))

(defn macroexpand [x]
  (let [output (c/macroexpand x)]
    (pp/with-pprint-dispatch pp/code-dispatch
      (pp/pprint output))))
