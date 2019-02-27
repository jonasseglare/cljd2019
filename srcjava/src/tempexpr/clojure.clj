(ns tempexpr.clojure)

(defn column-vector-from-array [^doubles arr]
  {:rows (alength arr)
   :cols 1
   :get (fn [i j] (aget arr i))})

(defn numel [mat]
  (* (:rows mat)
     (:cols mat)))



(defn compute-index [rows i j]
  (+ i (* rows j)))

(defn realize [mat]
  "Evalutes all the elements of the input matrix and puts them in an array, then forms a new matrix referring to that array"
  (let [n (numel mat)
        dst-array ^doubles (make-array Double/TYPE n)
        rows (:rows mat)
        cols (:cols mat)
        get-element (:get mat)]
    (dotimes [i (int rows)]
      (dotimes [j (int cols)]
        (aset dst-array (compute-index rows i j) (get-element i j))))
    {:rows rows
     :cols cols
     :data dst-array
     :get (fn [i j] (aget dst-array (compute-index rows i j)))}))

(defn reshape [column-matrix new-rows]
  "Changes a shape of a column matrix"
  (let [new-cols (quot (:rows column-matrix) new-rows)
        g (:get column-matrix)]
    {:rows new-rows
     :cols new-cols
     :get (fn [i j] (g (+ i (* j new-rows)) 0))}))

(defn transpose [mat]
  (let [g (:get mat)]
    {:rows (:cols mat)
     :cols (:rows mat)
     :get (fn [i j] (g j i))}))

(defn sub-mat [a b]
  (let [ga (:get a)
        gb (:get b)]
    {:rows (:rows a)
     :cols (:cols a)
     :get (fn [i j] (- (ga i j) (gb i j)))}))

(defn mul-mat [a b]
  (let [ga (:get a)
        gb (:get b)]
    {:rows (:rows a)
     :cols (:cols b)
     :get (fn [i j]
            (transduce
             (map (fn [k] (* (ga i k) (gb k j))))
             +
             0.0
             (range (:cols a))))}))

(defn ones [rows cols]
  {:rows rows
   :cols cols
   :get (constantly 1.0)})

(defn scale-mat [scale mat]
  (update mat :get (fn [g] (fn [i j] (* scale (g i j))))))

(defn disp-mat [x]
  (println (partition (:cols x)
                      (vec (:data (realize x))))))

(defn covariance-matrix [vector-dim vector-data]
  (let [V (column-vector-from-array vector-data)
        X (reshape V vector-dim)
        N (quot (count vector-data) vector-dim)
        mu (realize
            (scale-mat (/ 1.0 N)
                       (mul-mat (ones 1 N)
                                (transpose X))))
        ;;_ (disp-mat mu)
        mu-repeated (transpose (mul-mat (ones N 1) mu))
        ;;_ (disp-mat (mul-mat mu-repeated (transpose mu-repeated)))

        Xc (sub-mat X mu-repeated)
        covariance (scale-mat (/ 1.0 (- N 1))
                              (mul-mat Xc (transpose Xc)))]
    (-> covariance
        realize)))

(defn run [problem]
  (covariance-matrix (:dim problem) (:data problem)))
