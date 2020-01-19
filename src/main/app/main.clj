(ns app.main
  (:require
   [app.system :as system]))

(defn -main []
  (system/start-system!))
