(ns pres.speech
  (:require [clojure.string :as cljstr]
            [clojure.spec.alpha :as spec]))

(def part-pattern #"^(\s)*part:(\s)+((\d+):(\d\d))(\s)*$")

(defn make-minutes [minutes seconds]
  (+ (Integer/parseInt minutes)
     (/ (Integer/parseInt seconds)
        60)))

(defn parse-part [x]
  (let [[_ _ _ _ minutes seconds _] (re-matches part-pattern x)]
    (if (and minutes seconds)
      (make-minutes minutes seconds))))

(def acc-pattern #"^\s*(\d+):(\d\d)\s*$")

(defn parse-acc [x]
  (let [[_ minutes seconds] (re-matches acc-pattern x)]
    (if (and minutes seconds)
      (make-minutes minutes seconds))))

(defn not-acc-or-part [x]
  (not (or (parse-part x)
           (parse-acc x))))

(spec/def ::line (spec/alt :acc parse-acc
                           :part parse-part
                           :text not-acc-or-part))
(spec/def ::lines (spec/* ::line))

(def input-filename "latex/speech.md")
(def output-filename input-filename)

(defn load-lines [filename]
  (-> filename
      slurp
      cljstr/split-lines))

(defn not-acc [x]
  (not= :acc (first x)))

(defn format-total-time [t]
  (let [seconds (int (* 60 t))]
    (format "%d:%02d"
            (quot seconds 60)
            (rem seconds 60))))

(defn add-line
  ([[total-time result]]
   result)
  ([[total-time result] [line-type line-data]]
   (if (= line-type :part)
     (let [total-time (+ total-time (parse-part line-data))]
       [total-time
        (into result [(format-total-time total-time) line-data])])
     [total-time (conj result line-data)])))

(defn process-file [filename output-filename]
  (let [parsed (spec/conform ::lines (load-lines filename))
        processed (transduce
                   (comp (filter not-acc))
                   add-line
                   [0 []]
                   parsed)]
    (spit output-filename (cljstr/join "\n" processed))))

(defn process-speech []
  (process-file input-filename output-filename))
;; (process-speech)
