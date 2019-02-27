(ns cljd.circle
  (:require [geex.ebmd.type :as etype]
            [exampler.utils :as exutils]
            [cheshire.core :as cheshire]
            [clojure.pprint :as pp]
            [clojure.java.io :as io]
            [benjmark.core :as benjmark])
  (:import [cljd CircleOpt ADNumber
            CircleParameters ADCircleParameters]))

(require '[geex.common :as c]
         '[geex.java :as java]
         '[geex.core :as g])


(def default-settings {:ranges {:cx [-1 1]
                                :cy [-1 1]
                                :r [0.5 2]}
                       :noise 0.1
                       :count 30

                       :step-size 0.25
                       :iterations 30

                       :output-dir "./circledata"

                       :opt-count 10})

(defn sample-range [[a b]]
  (+ a (* (- b a) (Math/random))))

(defn sample-circle-parameters [settings]
  (let [ranges (:ranges settings)]
    (zipmap (keys ranges)
            (map sample-range (vals ranges)))))


;;;------- Point generation -------

(defn generate-point [params]
  (let [noise #(c/rand (c/- (:noise params))
                       (:noise params))
        angle (c/rand (* 2.0 Math/PI))]
    [(c/+ (:cx params)
          (c/* (:r params) (c/cos angle))
          (noise))
     (c/+ (:cy params)
          (c/* (:r params) (c/sin angle))
          (noise))]))

(java/typed-defn
 generate-circle-points [{:r Double/TYPE
                          :cx Double/TYPE
                          :cy Double/TYPE
                          :count Long/TYPE
                          :noise Double/TYPE} params]
 (let [result (c/make-array Double/TYPE (c/* 2 (:count params)))]
   (g/Loop
    [i 0]
    (g/If (c/< i (:count params))
          (let [[x y] (generate-point params)
                at (c/* 2 i)]
            (c/aset result (c/+ at 0) x)
            (c/aset result (c/+ at 1) y)
            (g/Recur (c/inc i)))
          119))
   result))

;; (vec (generate-circle-points (merge (sample-circle-parameters settings)
{:count 3 :noise 0.1}


;;;------- Evaluating it -------

[:begin-example "evalpoint"]
(require '[geex.common :as c])

(defn sqr [x] (c/* x x))

(defn evaluate-point [{:keys [cx cy r]} ;; $\leftarrow$ Circle parameters
                      [x y]] ;; $\leftarrow$ The point
  (let [dist-to-centre (c/sqrt (c/+ (sqr (c/- x cx))
                                    (sqr (c/- y cy))))
        dist-to-circle (c/- dist-to-centre r)]
    (sqr dist-to-circle)))
[:end-example]

[:begin-example "evalpointexa"]
(evaluate-point
 {:cx 0.0 :cy 0.0 :r 1.0}
 [1.0 0.0])
[:end-example]

[:begin-example "evalpointexb"]
(evaluate-point
 {:cx 0.0 :cy 0.0 :r 1.0}
 [3.0 0.0])
[:end-example]

(java/typed-defn test-eval-pt [{:cx Double/TYPE
                                :cy Double/TYPE
                                :r Double/TYPE} params
                               [Double/TYPE Double/TYPE] pt]
                 (evaluate-point params pt))
;; (test-eval-pt {:cx 0.0 :cy 0.0 :r 1.0} [3.0 0.0])
[:begin-example "getpoint"]
(defn get-point-2d [src-array index] ;; $\leftarrow Helper function$
  (let [offset (c/* 2 index)]
    [(c/aget src-array (c/+ offset 0))
     (c/aget src-array (c/+ offset 1))]))
[:end-example]

[:begin-example "objf"]
(defn circle-fitness-cost [circle-params point-array init-cost]
  (let [n (c/quot (c/cast Long/TYPE (c/count point-array)) 2)]
    (c/* (c// 1.0 n)
         (c/transduce
          (c/map (comp (partial evaluate-point circle-params)
                       (partial get-point-2d point-array)))
          c/+
          init-cost ;; $\leftarrow$ Typically 0
          (c/range n)))))
[:end-example]

(def line-rng (exutils/line-range
               (exutils/index-of-line {:substr "while (true)"})
               (exutils/index-of-line {:substr "continue;"
                                       :offset 8})))

(def remove-code-noise (exutils/remove-lines-with "set-flag!"))


[:begin-example "evalobjf" {:output {:format :java :transform line-rng} :code {:transform remove-code-noise}}]
(java/typed-defn eval-circle-fitness-cost
                 [{:cx Double/TYPE
                   :cy Double/TYPE
                   :r Double/TYPE} circle-params
                  (c/array-class Double/TYPE) points]
                 (g/set-flag! :disp)
                 (circle-fitness-cost
                  circle-params ;; $\leftarrow c_x, c_y, r$
                  points        ;; $\leftarrow$ double-array
                  0.0           ;; $\leftarrow$ initial cost
                  ))
[:end-example]

(defn test-eval-objf []
  (let [true-params {:cx 0.0 :cy 0.0 :r 1.0 :count 10 :noise 0.0}
        bad-params {:cx 0.0 :cy 0.0 :r 2.0}
        pts (generate-circle-points true-params)]
    {:true-fit (eval-circle-fitness-cost true-params pts)
     :bad-fit (eval-circle-fitness-cost bad-params pts)}))

;; (test-eval-objf)



;;;------- Automatic differentiation -------
[:begin-example "advar"]
(defn variable [x]
  {:value x
   :deriv 1.0}) ;; $\frac{dx}{dx} = 1$
[:end-example]

[:begin-example "adcst"]
(defn constant [c]
  {:value c
   :deriv 0.0}) ;; $\frac{dc}{dx} = 0$, $c$ being a constant
[:end-example]

[:begin-example "adspec"]
(require '[bluebell.utils.ebmd :as ebmd])

(defn ad-number? [x]
  (and (map? x) (contains? x :value) (contains? x :deriv)))

(ebmd/def-arg-spec
  ::ad ;; $\leftarrow$ Name of the spec
  {:pred ad-number? ;; $\leftarrow$ Predicate function

   ;; Examples disambiguate overlapping predicates:
   :pos [(variable 3.0) (constant 5.0)] ;; $\leftarrow$ Matching examples
   :neg [2.0 :kwd {:kattskit 119}]}) ;; $\leftarrow$ Non-matching examples
[:end-example]

[:begin-example "adadd"]
(ebmd/def-poly c/binary-add [::ad a
                             ::ad b]
  {:value (c/+ (:value a)
               (:value b))
   :deriv (c/+ (:deriv a)
               (:deriv b))})
[:end-example]

[:begin-example "adaddexa"]
(let [x (variable 3)]
  (c/+ x x))
[:end-example]

[:begin-example "adaddexb"]
;; (c/+ (variable 3) 4)
[:end-example]

[:begin-example "promote"]
(require '[geex.ebmd.type :as etype])

(ebmd/register-promotion
 ::ad ;; Destination type
 constant ;; Promoter
 ::etype/real) ;; Source type
[:end-example]

[:begin-example "adaddexc"]
(c/+ (variable 3) 4)
[:end-example]

(println "Is it being evaled, or what???")

[:begin-example "admul"]
(ebmd/def-poly c/binary-mul [::ad a
                             ::ad b]
  {:value (c/* (:value a) (:value b))

   ;; Recall that $(a \cdot b)^{\prime} = a^{\prime} \cdot b + a \cdot b^{\prime}$
   :deriv (c/+ (c/* (:value a)
                    (:deriv b))
               (c/* (:deriv a)
                    (:value b)))})
[:end-example]

[:begin-example "admulex"]
(let [x (variable 9.0)]
  (c/* x x x))
[:end-example]

[:begin-example "adsub"]
(ebmd/def-poly c/binary-sub [::ad a
                             ::ad b]
  {:value (c/- (:value a) (:value b))
   :deriv (c/- (:deriv a)
               (:deriv b))})
[:end-example]

[:begin-example "adsqrt"]
(ebmd/def-poly c/sqrt [::ad x]
  (let [s (c/sqrt (:value x))]
    {:value s
     :deriv (c/* (c// 0.5 s)
                 (:deriv x))}))
[:end-example]

[:begin-example "adsqrtex"]
(c/sqrt (variable 2.0))
[:end-example]


[:begin-example "evalgrad" {:output {:transform line-rng :format :java} :code {:transform remove-code-noise}}]
(defn derivative-for-key [circle-params points k]
  (:deriv (circle-fitness-cost ;; $\leftarrow$ Derivative of objective function
           (update circle-params k variable) ;; $\leftarrow$ 'k' is a variable
           points
           (constant 0.0))))

(java/typed-defn gradient
   [{:cx Double/TYPE :cy Double/TYPE :r Double/TYPE} circle-params
    (c/array-class Double/TYPE) points]
   (g/set-flag! :disp)
   {:cx (derivative-for-key circle-params points :cx) ;; $\leftarrow \frac{df}{dc_x}$
    :cy (derivative-for-key circle-params points :cy) ;; $\leftarrow \frac{df}{dc_y}$
    :r (derivative-for-key circle-params points :r)}) ;; $\leftarrow \frac{df}{dr}$
[:end-example]

(defn test-eval-objf-dr []
  (let [true-params {:cx 0.0 :cy 0.0 :r 1.0 :count 10 :noise 0.0}
        bad-params {:cx 0.0 :cy 0.0 :r 2.0}
        pts (generate-circle-points true-params)]
    {:true-fit (gradient true-params pts)
     :bad-fit (gradient bad-params pts)}))

;; (test-eval-objf-dr)

(defn gradient-step [params point-array step-size]
  (let [grad (gradient params point-array)
        ks (keys grad)]
    (zipmap ks
            (map (fn [k]
                   (let [derivative (get grad k)
                         value (get params k)]
                     (- value (* derivative step-size))))
                 ks))))

(defn optimize [init-params point-array
                iterations step-size]
  (first
   (drop
    iterations
    (iterate #(gradient-step % point-array step-size)
             init-params))))


(defn test-optimize []
  (let [true-params {:cx 2.0
                     :cy 3.0
                     :r 1.5
                     :noise 0.1
                     :count 100}
        point-array (generate-circle-points true-params)
        init-params {:cx 1.0
                     :cy 1.0
                     :r 1.0}]
    (optimize init-params
              point-array
              10
              0.5)))

;; (test-optimize)



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;;  Produce sample data
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn decorate-parameters-with-objf [params points]
  (merge params
         {:cost (eval-circle-fitness-cost params points)
          :gradient (gradient params points)}))

(defn opt-seq [init-params point-array
               iterations step-size]
  (vec
   (take
    iterations
    (map
     #(decorate-parameters-with-objf % point-array)
     (iterate #(gradient-step % point-array step-size)
              init-params)))))

(defn produce-sample-circles-and-points []
  (let [settings default-settings
        params (sample-circle-parameters settings)
        points (generate-circle-points (merge settings params))
        params (decorate-parameters-with-objf params points)
        iterations (:iterations settings)
        step-size (:step-size settings)
        optimized-params (decorate-parameters-with-objf
                          (optimize params points
                                    iterations
                                    step-size)
                          points)
        
        sample-count 100
        opt-count 20
        
        samples (map #(decorate-parameters-with-objf % points)
                     (repeatedly sample-count #(sample-circle-parameters settings)))
        opt-samples (map #(opt-seq % points iterations step-size)
                         (repeatedly opt-count #(sample-circle-parameters settings)))
        all-data {:params params
                  :opt_params optimized-params
                  :samples samples
                  :opt_samples opt-samples
                  :points (partition 2 (vec points))}
        filename (io/file (:output-dir settings) "samples.json")]
    (io/make-parents filename)
    (spit filename (cheshire/generate-string all-data))
    (println "Saved the data to " filename)
    opt-samples))

(defn make-test-seq []
  (let [settings default-settings
        params (sample-circle-parameters settings)
        points (generate-circle-points (merge settings params))
        iterations 2
        step-size (:step-size settings)]
    (opt-seq (sample-circle-parameters settings) points iterations step-size)))

;; (pp/pprint (make-test-seq))

;; (def opt-samples (produce-sample-circles-and-points))




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;;  Checks and benchmarks
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def sample-points (map (fn [i] (let [a (* Math/PI (/ i 8.0))]
                                  [(Math/cos a) (Math/sin a)]))
                        (range 9)))

(def sample-array (double-array (reduce into [] sample-points)))

(def sample-params0 {:cx 0.0
                     :cy 0.0
                     :r 1.0})

(def sample-params1 {:cx 3.0
                     :cy 4.0
                     :r 5.0})

(def sample-params2 {:cx 0.3
                     :cy 0.4
                     :r 1.2})



(defn to-java-params [p]
  (let [dst (CircleParameters.)]
    (set! (.cx dst) (:cx p))
    (set! (.cy dst) (:cy p))
    (set! (.r dst) (:r p))
    dst))

(defn from-java-params [p]
  {:cx (.cx p)
   :cy (.cy p)
   :r (.r p)})

(defn to-java-ad-params [p]
  (let [dst (CircleParameters.)]
    (set! (.cx dst) (ADNumber/constant (:cx p)))
    (set! (.cy dst) (ADNumber/constant (:cy p)))
    (set! (.r dst) (ADNumber/constant (:r p)))
    dst))


(defmacro output [x]
  `(let [x# ~x]
     (println ~(str x) "=" x#)
     x#))



;;;------- Clojure implementation -------
(defn clj-constant [x]
  {:value x
   :deriv 0.0})

(defn clj-variable [x]
  {:value x
   :deriv 1.0})

(defn clj-add [a b]
  {:value (+ (:value a) (:value b))
   :deriv (+ (:deriv a) (:deriv b))})

(defn clj-sub [a b]
  {:value (- (:value a) (:value b))
   :deriv (- (:deriv a) (:deriv b))})

(defn clj-mul [a b]
  {:value (* (:value a) (:value b))
   :deriv (+ (* (:value a) (:deriv b))
             (* (:deriv a) (:value b)))})

(defn clj-sqrt [x]
  (let [s (Math/sqrt (:value x))]
    {:value s
     :deriv (* (/ 0.5 s) (:deriv x))}))

(defn clj-sqr [x]
  (clj-mul x x))


(defn clj-get-point-2d [array i]
  (let [at (* 2 i)]
    [(clj-constant (aget array (+ at 0)))
     (clj-constant (aget array (+ at 1)))]))

(defn clj-evaluate-point [{:keys [cx cy r]}
                          [x y]] ;; $\leftarrow$ The point
  (let [dist-to-centre (clj-sqrt
                        (clj-add (clj-sqr
                                  (clj-sub x cx))
                                 (clj-sqr
                                  (clj-sub y cy))))
        dist-to-circle (clj-sub dist-to-centre r)]
    (clj-sqr dist-to-circle)))

(defn clj-evaluate [params array]
  (let [N (quot (alength array) 2)]
    (clj-mul (clj-constant (/ 1.0 N))
             (transduce
              (map (comp (partial clj-evaluate-point params)
                         (partial clj-get-point-2d array)))
              (completing clj-add)
              (clj-constant 0.0)
              (range N)))))

(defn clj-derivative [params array k]
  (let [ad-params (zipmap
                   (keys params)
                   (map clj-constant (vals params)))
        ad-params (assoc ad-params k (clj-variable
                                      (get params k)))]
    (:deriv (clj-evaluate ad-params array))))

(defn clj-gradient [params array]
  {:cx (clj-derivative params array :cx)
   :cy (clj-derivative params array :cy)
   :r (clj-derivative params array :r)})

(defn clj-step [params array step-size]
  (let [grad (clj-gradient params array)]
    (into {}
          (map (fn [k]
                 [k (- (get params k)
                       (* step-size (get grad k)))])
               (keys params)))))

(defn clj-optimize [params array iterations step-size]
  (first
   (drop
    iterations
    (iterate
     #(clj-step % array step-size)
     params))))




;;;------- Unit test -------

(defn unit-test []
  (let [copt (CircleOpt.)]
    (assert (= (.evaluate copt sample-array
                          (to-java-params sample-params1))
               (eval-circle-fitness-cost
                sample-params1 sample-array)))
    (assert {:cx 0.0
             :cy 0.0
             :r 0.0}
            (gradient sample-params0 sample-array))

    (let [jp (from-java-params
              (.gradient
               copt sample-array
               (to-java-params sample-params1)))
          g (gradient sample-params1 sample-array)
          gc (clj-gradient sample-params1 sample-array)]
      (assert (= gc g))
      (assert (= jp g)))

    (let [opt (optimize sample-params2 sample-array
                        (.iterations copt)
                        (.stepSize copt))
          clj-opt
          (clj-optimize sample-params2
                        sample-array
                        (.iterations copt)
                        (.stepSize copt))]

      (println "clj-opt" clj-opt)
      
      (assert (= (from-java-params
                  (.optimize copt sample-array
                             (to-java-params sample-params2)))
                 opt))
      (assert (= clj-opt
                 opt)))
    )

  :good!)
;; (unit-test)

(defn corrupt-params [params]
  (let [r (* 0.5 (:r params))]
    (-> params
        (update :cx + r)
        (update :cy + r)
        (update :r * 1.2))))

(defn quick-comparison []
  (let [settings (merge default-settings
                        {:count 1000})
        true-params (sample-circle-parameters settings)
        big-array (generate-circle-points (merge settings
                                                 true-params))
        r (:r true-params)
        bad-params (corrupt-params true-params)
        copt (CircleOpt.)]
    (set! (.stepSize copt) (:step-size settings))
    (set! (.iterations copt) (:iterations settings))
    (println "Geex: "
             (time (optimize bad-params big-array
                    (:iterations settings)
                    (:step-size settings))))
    (println "Clojure:"
             (time (clj-optimize bad-params big-array
                                 (:iterations settings)
                                 (:step-size settings))))
    (println "Java:"
             (time
              (from-java-params
               (.optimize copt
                          big-array
                          (to-java-params bad-params)))))))
;; (quick-comparison)



;;;------- The benchmark -------

(defn generate-problem-data [size]
  (let [settings (merge default-settings {:count (long size)})
        true-params (sample-circle-parameters settings)
        guess-params (sample-circle-parameters settings)
        arr (generate-circle-points (merge settings true-params))
        points (vec (partition 2 arr))]
    (assert (= (count points) size))
    {:settings settings
     :true-params true-params
     :init-params guess-params
     :points points}))

(defn array-from-vecs [v]
  (double-array (reduce into [] v)))

(def problem-points :points)
(def problem-step-size (comp :step-size :settings))
(def problem-iterations (comp :iterations :settings))
(def problem-params :init-params)

(defn benchmark-clj [problem]
  (clj-optimize
   (problem-params problem)
   (problem-points problem)
   (problem-iterations problem)
   (problem-step-size problem)))

(defn clj-import [problem]
  (update problem :points array-from-vecs))

(defn benchmark-geex [problem]
  #_(gradient (:init-params problem)
            (problem-points problem))
  (optimize
   (:init-params problem)
   (problem-points problem)
   (problem-iterations problem)
   (problem-step-size problem)))

(defn java-import [problem]
  (-> problem
      (update :points array-from-vecs)
      (update :init-params to-java-params)))

(defn benchmark-java [problem]
  (let [copt (CircleOpt.)]
    (set! (.stepSize copt) (problem-step-size problem))
    (set! (.iterations copt) (problem-iterations problem))
    (.optimize copt (problem-points problem)
               (problem-params problem))))

(def project (merge
              benjmark/default-project
              {::benjmark/root "benchmarks/circle"
               ::benjmark/max-duration-seconds 60.0

               ;;::benjmark/problem-indices [0]
               ;;::benjmark/try-count 1
               
               ::benjmark/candidates
               {"clojure" {::benjmark/name "Clojure"
                           ::benjmark/fn
                           (benjmark/wrap-fn benchmark-clj clj-import)}
                "geex" {::benjmark/name "Clojure/Geex"
                        ::benjmark/fn
                        (benjmark/wrap-fn benchmark-geex clj-import)}
                "cpp" {::benjmark/name "C++"
                       ::benjmark/fn
                       (benjmark/wrap-executable "cpp/circleopt")}
                "java" {::benjmark/name "Java"
                        ::benjmark/fn (benjmark/wrap-fn
                                       benchmark-java
                                       java-import
                                       from-java-params)}}}))

(def sizes (benjmark/exponential-sizes 15 1000 1000000))

(defn generate-problem []
  (benjmark/generate-and-save-problems project
                                       generate-problem-data
                                       sizes))

;; (benjmark/update-candidates-info project)

;; (generate-problem)
;; (benjmark/clear-results project)
;; (benjmark/run-benchmark project)

(def java-rng (exutils/line-range
               (exutils/index-of-line {:substr "ADNumber evaluate"})
               (exutils/index-of-line {:substr "return sum"
                                       :offset 1})))

(def java-source (slurp "srcjava/cljd/CircleOpt.java"))
[:begin-example "javasource"  {:output {:transform java-rng :format :java}}]
(print java-source)
[:end-example]
