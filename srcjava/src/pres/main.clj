(ns pres.main
  (:require [exampler.core :as exampler]
            [exampler.render :as render]
            [clojure.java.io :as io]
            [cljd.circle]
            [cljd.square]
            [pres.speech :as speech]
            ))

(def settings (merge exampler/default-settings
                     render/default-settings
                     {::source-path "./src/cljd"}))

(defn list-files [settings]
  (transduce
   (filter (fn [file]
             (and (.isFile file)
                  (.endsWith (.getAbsolutePath file) ".clj"))))
   conj
   []
   (file-seq (io/file (::source-path settings)))))

(defn process-files [settings]
  (doseq [file (list-files settings)]
    (println "Process file" file)
    (render/process-file file settings)
    (println "Done file"))
  (println "Done"))

(defn -main [& args]
  (speech/process-speech)
  (process-files settings))

;; (-main)
