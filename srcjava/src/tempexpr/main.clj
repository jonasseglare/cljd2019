(ns tempexpr.main
  (:import [TempExpr])
  (:require [benjmark.core :as bj]
            [tempexpr.geex :as te-geex]
            [tempexpr.clojure :as te-clj]))

(def sizes (bj/exponential-sizes 15 1000 1000000))

(defn generate-problem [size]
  (let [dim 2]
    {:dim dim
     :data (repeatedly (* dim size) rand)}))

(defn input-clojure [problem]
  {:dim (long (:dim problem))
   :data (double-array (:data problem))})

(defn output-clojure [out-matrix]
  (partition (:cols out-matrix) (vec (:data out-matrix))))

(defn input-java [problem]
  (update problem :data double-array))

(defn run-java [problem]
  (let [te (TempExpr.)]
    (.run te
          (int (:dim problem))
          (:data problem))))

(defn output-java [arr]
  (partition 2 (vec arr)))

(def project (merge
              bj/default-project
              {::bj/root "benchmarks/tempexpr"
               ::bj/max-duration-seconds 60.0

               ;;::bj/problem-indices [0]
               ;;::bj/try-count 1

               ;;::bj/trim-output trim-output
               
               ::bj/candidates
               {"clojure" {::bj/name "Clojure"
                           ::bj/fn
                           (bj/wrap-fn te-clj/run
                                       input-clojure
                                       output-clojure)}
                "geex" {::bj/name "Geex"
                        ::bj/fn
                        (bj/wrap-fn te-geex/run
                                    input-clojure
                                    output-clojure)}
                "cpp" {::bj/name "C++"
                       ::bj/fn
                       (bj/wrap-executable "cpp/tempexpr")}
                "java" {::bj/name "Java"
                        ::bj/fn (bj/wrap-fn run-java
                                            input-java
                                            output-java)}}}))

;; (bj/generate-and-save-problems project generate-problem sizes)
;; (bj/run-benchmark project)

