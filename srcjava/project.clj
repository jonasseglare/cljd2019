(defproject pres-clojured "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main pres.main

  ;; This is needed, because the stack can get very deep
  ;; when we generate code.
  :jvm-opts ["-Xss16M"]
  
  :java-source-paths ["srcjava/"]
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [venantius/glow "0.1.5"]
                 [clygments "1.0.0"]
                 [bluebell/utils "0.1.7"]
                 [geex "0.1.0"]
                 [zprint "0.4.15"]
                 [cheshire "5.8.1"]

                 ;; For benchmarks
                 [benjmark "0.1.0"]

                 ;; For extracting code examples
                 [exampler "0.1.0"]])
