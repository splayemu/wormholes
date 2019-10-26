(ns app.graphics
  (:require [com.fulcrologic.fulcro.dom :as dom]))

(defn style-string->style-map [style-string]
  (->> (clojure.string/split style-string  #";|:")
    (apply hash-map)))
