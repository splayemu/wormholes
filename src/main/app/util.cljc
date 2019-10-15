(ns app.util)

(defn log [& str-args]
  (let [full-string (apply str (interpose " " str-args))]
    #?(:clj (println full-string)
       :cljs (js/console.log full-string))))

(defn inspect* [form]
  `(let [result# ~form]
     (log (str '~form " => " result#))
     result#))

(defmacro inspect [form]
  (inspect* form))

(comment
  (inspect* `(+ 1 1))
  (inspect (+ 1 1))

  )

