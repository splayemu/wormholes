(ns user
  (:require
   [app.server :as server]
   [clojure.tools.namespace.repl :as tools-ns :refer [set-refresh-dirs refresh]]))

(set-refresh-dirs "src/dev" "src/main")

;;(defn start []
;;  (server/start!))
;;
;;(defn restart
;;  "Stop the server, reload all source code, then restart the server."
;;  []
;;  (server/stop!)
;;  (refresh :after 'user/start!))

#_(comment
  ;; when compile errors run
  (tools-ns/refresh)
  (start)

  )



