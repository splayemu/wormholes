(ns app.graphics
  (:require
   [com.fulcrologic.fulcro.dom :as dom]
   [clojure.edn :as edn]))

(defn style-string->style-map [style-string]
  (->> (clojure.string/split style-string  #";|:")
    (apply hash-map)))

(def spiral-length 2161.78125)

;; a
(def svg-path-re (re-pattern (str "([" "a-zA-Z" "])([^" "a-zA-Z" "]+)")))

(defn translate-matched-path-str [[match op args]
                                  {:keys [x-translation y-translation]
                                    :or {x-translation 0
                                         y-translation 0}}]
  (if (#{"C" "M"} op)
    (let [path-str (-> args
                    clojure.string/trim
                    (clojure.string/split #",| "))
         args (->> path-str
                (map edn/read-string)
                (partition 2)
                (map (fn [[x y]]
                       (str (+ x x-translation) "," (+ y y-translation))))
                (interpose " ")
                (apply str))]
      (str op " " args))
    (when match
      (clojure.string/trim match))))

(comment 
  (let [path-str "D 50.577096,294.57755 9.8503282,266.71236 1.6353212,225.90524 -6.912028,183.44726 21.942089,141.30158 64.19037,132.8286"
        transform 130.67721
        matched-path-str (re-matches svg-path-re path-str)]
    (translate-matched-path-str matched-path-str {:y-translation (- transform)})
    )

  )

(comment
  (let [transform 130.67721
       path-str "M 78.364487,212.2036 c 0.284267,1.59189 -2.005595,1.16857 -2.645834,0.47247 -1.735007,-1.88639 -0.232517,-4.74557 1.700893,-5.76414 3.458415,-1.82198 7.51609,0.51027 8.882441,3.87426 2.005174,4.93679 -1.25027,10.33316 -6.047618,12.00074 -6.39411,2.22263 -13.167925,-1.9876 -15.119049,-8.22098 -2.45529,-7.84407 2.723253,-16.01141 10.394343,-18.23735 9.290718,-2.69592 18.859867,3.45787 21.355657,12.5677 2.941202,10.73563 -4.191835,21.71143 -14.741069,24.47396 -12.179508,3.18946 -24.565066,-4.92536 -27.592264,-16.91443 -3.439695,-13.62274 5.658582,-27.42015 19.087794,-30.71056 15.065546,-3.69135 30.276309,6.39158 33.828869,21.26115 3.94403,16.50804 -7.12443,33.13325 -23.434516,36.94718 -17.950315,4.19747 -35.990805,-7.85715 -40.06548,-25.60788 -4.451524,-19.39242 8.589772,-38.84885 27.781245,-43.18379 20.834396,-4.70604 41.707261,9.32232 46.302091,29.95461 4.96094,22.27627 -10.05481,44.566 -32.127974,49.42039 -23.718067,5.21615 -47.424991,-10.78725 -52.538695,-34.30133 -5.471603,-25.1598 11.519645,-50.2842 36.474695,-55.657 26.60147,-5.72727 53.143594,12.25201 58.775304,38.64806 5.98312,28.0431 -12.98435,56.00313 -40.821422,61.8936 -29.484691,6.23912 -58.862809,-13.71665 -65.01191,-42.99478 -6.495247,-30.92625 14.448944,-61.7226 45.168146,-68.13021 32.367786,-6.75149 64.582486,15.18121 71.248516,47.3415 7.00783,33.8093 -15.91347,67.44247 -49.51487,74.36683 -35.250778,7.26425 -70.302516,-16.64572 -77.485126,-51.68824 -7.5207508,-36.69225 17.377945,-73.16263 53.861598,-80.60343 38.133698,-7.77732 76.022818,18.11017 83.721738,56.03496 8.03394,39.57514 -18.84238,78.88305 -58.208327,86.84004 C 50.577096,294.57755 9.8503282,266.71236 1.6353212,225.90524 -6.912028,183.44726 21.942089,141.30158 64.19037,132.8286 c 43.89939,-8.80412 87.46402,21.03895 96.19495,64.72841 9.06093,45.34078 -21.77114,90.32443 -66.901775,99.31325"]
    (translate-path-str path-str {:y-translation (- transform)}))


  )

(defn translate-path-str [path-str translation]
  (->> (re-seq svg-path-re path-str)
    (map #(translate-matched-path-str % translation))
    (clojure.string/join " ")))
