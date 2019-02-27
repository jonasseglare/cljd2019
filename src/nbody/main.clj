(ns nbody.main
  (:import [NBodySystem])
  (:require [benjmark.core :as bj]
            [nbody.clojure :as nbodyclj]
            [nbody.geex :as nbodygeex]
            [cheshire.core :as cheshire]))

(def sizes (bj/exponential-sizes 15 1000 10000000))

(defn nbody-java [problem]
  (let [system (NBodySystem.)]
    (.iterate system
              (:iterations problem)
              (:step-size problem))
    {:energy (.energy system)}))

(defn round-energy [energy]
  (let [f 1.0e6]
    (/ (Math/round (* f energy)) f)))

(defn trim-output [output]
  (update output :energy round-energy))

(def project (merge
              bj/default-project
              {::bj/root "benchmarks/nbody"
               ::bj/max-duration-seconds 60.0

               ;;::bj/problem-indices [0 1 2 3]
               ;;::bj/try-count 1

               ::bj/trim-output trim-output
               
               ::bj/candidates
               {"clojure" {::bj/name "Clojure"
                           ::bj/fn
                           (bj/wrap-fn nbodyclj/run)}
                "geex" {::bj/name "Geex"
                        ::bj/fn
                        (bj/wrap-fn nbodygeex/run)}
                "cpp" {::bj/name "C++"
                       ::bj/fn
                       (bj/wrap-executable "cpp/nbody")}
                "java" {::bj/name "Java"
                        ::bj/fn (bj/wrap-fn nbody-java)}}}))

(defn wrap-size [size]
  {:iterations size
   :step-size 0.01})

(defn generate-problem []
  (bj/generate-and-save-problems project
                                 wrap-size
                                 sizes))

;; (generate-problem)
;; (bj/clear-results project)
;; (bj/run-benchmark project)





;;;------- Outputting some data for illustrations -------
(def dst-file (bj/project-file "stateseq.json" project))

(defn save-state-seq []
  (->> 0.01
       nbodygeex/system-seq
       (take-nth 30)
       (take 40)
       cheshire/encode
       (spit dst-file)))

;; (save-state-seq)
